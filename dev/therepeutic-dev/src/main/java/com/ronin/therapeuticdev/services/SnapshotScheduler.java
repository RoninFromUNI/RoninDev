package com.ronin.therapeuticdev.services;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.ronin.therapeuticdev.detection.FlowDetector;
import com.ronin.therapeuticdev.detection.FlowDetectionResult;
import com.ronin.therapeuticdev.listeners.ErrorHighlightListener;
import com.ronin.therapeuticdev.metrics.FlowMetrics;
import com.ronin.therapeuticdev.settings.TherapeuticDevSettings;
import com.ronin.therapeuticdev.storage.MetricRepository;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

/**
 * Periodic scheduler that triggers flow detection at regular intervals.
 * 
 * Every SNAPSHOT_INTERVAL_SECONDS:
 * 1. Collects current error count from code analysis
 * 2. Creates a FlowMetrics snapshot from MetricCollector
 * 3. Runs FlowDetector.detect() on the snapshot
 * 4. Notifies registered listeners of the result
 * 5. Optionally persists the snapshot for user study analysis
 * 
 * Uses IntelliJ's AppExecutorUtil for proper thread handling.
 *
 * @see <a href="https://plugins.jetbrains.com/docs/intellij/general-threading-rules.html">
 *      IntelliJ Platform SDK - Threading Rules</a>
 */
@Service(Service.Level.APP)
public final class SnapshotScheduler implements Disposable {

    private static final Logger LOG = Logger.getInstance(SnapshotScheduler.class);
    
    /** How often the UI receives fresh flow detection results (seconds) */
    private static final int LIVE_REFRESH_SECONDS = 2;

    /** How often interval counters reset and error scan runs (seconds) */
    private static final int PERSIST_INTERVAL_SECONDS = 60;

    private final ScheduledExecutorService executor;
    private ScheduledFuture<?> liveTask;
    private ScheduledFuture<?> persistTask;
    private final FlowDetector detector;
    
    /** Listeners notified on each detection result */
    private final List<FlowDetectionListener> listeners = new CopyOnWriteArrayList<>();
    
    /** Most recent detection result for UI access */
    private volatile FlowDetectionResult lastResult;

    /**
     * Listener interface for flow detection events.
     */
    public interface FlowDetectionListener {
        void onFlowDetected(FlowDetectionResult result, FlowMetrics metrics);
    }

    public SnapshotScheduler() {
        this.executor = AppExecutorUtil.getAppScheduledExecutorService();
        this.detector = new FlowDetector();
    }

    /**
     * Starts the periodic scheduler.
     * Two cycles run independently:
     *  - Live refresh (every 5s): snapshot → detect → notify UI listeners
     *  - Persist cycle (every 60s): error scan + interval counter reset
     */
    public void start() {
        if (liveTask != null && !liveTask.isCancelled()) {
            LOG.warn("SnapshotScheduler already running");
            return;
        }

        LOG.info("Starting SnapshotScheduler — live refresh every " + LIVE_REFRESH_SECONDS
                + "s, persist cycle every " + PERSIST_INTERVAL_SECONDS + "s");

        liveTask = executor.scheduleAtFixedRate(
                this::performLiveRefresh,
                LIVE_REFRESH_SECONDS,   // short initial delay so UI populates quickly
                LIVE_REFRESH_SECONDS,
                TimeUnit.SECONDS
        );

        persistTask = executor.scheduleAtFixedRate(
                this::performPersistCycle,
                PERSIST_INTERVAL_SECONDS,
                PERSIST_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );
    }

    /**
     * Stops the periodic scheduler.
     */
    public void stop() {
        if (liveTask != null) {
            liveTask.cancel(false);
            liveTask = null;
        }
        if (persistTask != null) {
            persistTask.cancel(false);
            persistTask = null;
        }
        LOG.info("SnapshotScheduler stopped");
    }

    /**
     * Lightweight cycle: snapshot current metrics, run detection, push to UI.
     * Does NOT reset interval counters or run the expensive error scan.
     * Runs every {@value LIVE_REFRESH_SECONDS} seconds.
     */
    private void performLiveRefresh() {
        try {
            MetricCollector collector = ApplicationManager.getApplication()
                    .getService(MetricCollector.class);

            if (collector == null) {
                LOG.warn("MetricCollector service not available");
                return;
            }

            FlowMetrics metrics = collector.snapshot();
            FlowDetectionResult result = detector.detect(metrics);
            lastResult = result;

            LOG.debug("Live refresh: score=" + String.format("%.1f", result.getFlowTally() * 100)
                    + ", state=" + result.getState());

            for (FlowDetectionListener listener : listeners) {
                try {
                    listener.onFlowDetected(result, metrics);
                } catch (Exception e) {
                    LOG.error("Error notifying flow detection listener", e);
                }
            }
        } catch (Exception e) {
            LOG.error("Error during live refresh", e);
        }
    }

    /**
     * Heavy cycle: scans open files for syntax errors, persists a snapshot, then
     * resets interval counters so the next interval starts clean.
     *
     * Order matters:
     *   1. Refresh error count (expensive scan — once per minute is enough)
     *   2. Snapshot + detect with fresh error data
     *   3. Persist (if enabled) — must happen BEFORE counter reset so interval
     *      values (file switches, focus losses, compile errors) are captured
     *   4. Reset interval counters
     *
     * Runs every {@value PERSIST_INTERVAL_SECONDS} seconds.
     */
    private void performPersistCycle() {
        try {
            MetricCollector collector = ApplicationManager.getApplication()
                    .getService(MetricCollector.class);

            if (collector == null) return;

            // 1. Refresh syntax error count (scans all open files)
            ErrorHighlightListener.recordCurrentErrors();

            // 2. Snapshot + detect with the freshly updated error count
            FlowMetrics metrics = collector.snapshot();
            FlowDetectionResult result = detector.detect(metrics);
            lastResult = result;

            // 3. Persist if enabled in settings
            TherapeuticDevSettings settings = ApplicationManager.getApplication()
                    .getService(TherapeuticDevSettings.class);
            if (settings != null && settings.persistSnapshots) {
                MetricRepository repo = ApplicationManager.getApplication()
                        .getService(MetricRepository.class);
                if (repo != null) {
                    repo.saveSnapshot(metrics, result);
                    LOG.debug("Snapshot persisted: state=" + result.getState()
                            + " score=" + String.format("%.3f", result.getFlowTally()));
                }
            }

            // 4. Reset interval-based counters (file switches, focus losses, compile errors)
            collector.resetIntervalCounters();

            LOG.debug("Persist cycle complete — interval counters reset");
        } catch (Exception e) {
            LOG.error("Error during persist cycle", e);
        }
    }

    /**
     * Triggers an immediate live refresh outside the regular schedule.
     */
    public FlowDetectionResult triggerImmediateSnapshot() {
        performLiveRefresh();
        return lastResult;
    }

    /**
     * Registers a listener to be notified on flow detection.
     */
    public void addListener(FlowDetectionListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a listener.
     */
    public void removeListener(FlowDetectionListener listener) {
        listeners.remove(listener);
    }

    /**
     * Returns the most recent detection result.
     * May be null if no detection has run yet.
     */
    public FlowDetectionResult getLastResult() {
        return lastResult;
    }

    /**
     * Checks if the scheduler is currently running.
     */
    public boolean isRunning() {
        return liveTask != null && !liveTask.isCancelled();
    }

    @Override
    public void dispose() {
        stop();
        listeners.clear();
    }
}
