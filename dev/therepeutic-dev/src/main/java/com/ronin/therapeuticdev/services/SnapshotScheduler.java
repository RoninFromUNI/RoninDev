package com.ronin.therapeuticdev.services;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.Alarm;
import com.ronin.therapeuticdev.detection.FlowDetector;
import com.ronin.therapeuticdev.detection.FlowDetectionResult;
import com.ronin.therapeuticdev.listeners.ErrorHighlightListener;
import com.ronin.therapeuticdev.metrics.FlowMetrics;
import com.ronin.therapeuticdev.settings.TherapeuticDevSettings;
import com.ronin.therapeuticdev.storage.MetricRepository;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

/**
 * WHAT I AM TRYING TO ESTABLISH:
 *
 * a periodic scheduler aimed to trigger flow detection at regular intervals
 *
 * presented through every LIVE_REFRESH_MS:
 *   - creates a FlowMetrics snapshot from MetricCollector
 *   - runs FlowDetector.detect() on the snapshot
 *   - notifies registered listeners of the result
 *
 * then in every PERSIST_INTERVAL_MS:
 *   - collects current error count from code analysis
 *   - persists snapshot for user study analysis
 *   - resets interval counters
 *   - checks ESM probe delivery
 *
 * uses Alarm with POOLED_THREAD to schedule recurring work without blocking the EDT.
 * Alarm integrates with the IDE's Disposable lifecycle so tasks are automatically
 * cancelled on disposal — no manual executor shutdown required.
 *
 * this avoids the IncorrectOperationException thrown by SchedulingWrapper.scheduleAtFixedRate
 * which the platform prohibits because fixed-rate executors interfere with IDE hibernation.
 *
 * see: IntelliJ Platform SDK — Threading Rules
 */
@Service(Service.Level.APP)
public final class SnapshotScheduler implements Disposable {

    private static final Logger LOG = Logger.getInstance(SnapshotScheduler.class);

    /** how often the UI gets fresh flow detection results (milliseconds) */
    private static final int LIVE_REFRESH_MS = 2_000;

    /** how often interval counters reset and error scan runs (milliseconds) */
    private static final int PERSIST_INTERVAL_MS = 60_000;

    /**
     * live refresh loop alarm.
     *
     * POOLED_THREAD executes the callback on a background thread, keeping the EDT free.
     * passing 'this' as the parent Disposable means the alarm is auto-cancelled
     * when this service is disposed — i need to write about this in the dissertation.
     */
    private final Alarm liveAlarm;

    /**
     * persist cycle alarm — separate instance so the two loops run independently.
     * if a slow persist cycle happens (e.g. large error scan), it wont delay
     * the next live refresh because theyre on different Alarm instances.
     */
    private final Alarm persistAlarm;

    private final FlowDetector detector;

    /** listeners notified on each detection result */
    private final List<FlowDetectionListener> listeners = new CopyOnWriteArrayList<>();

    /** most recent detection result for UI access */
    private volatile FlowDetectionResult lastResult;

    /** guard flag so start() is idempotent — calling it twice wont double-schedule */
    private volatile boolean running;

    /**
     * listener interface for flow detection events.
     * anything that wants to react to a new detection result implements this.
     */
    public interface FlowDetectionListener {
        void onFlowDetected(FlowDetectionResult result, FlowMetrics metrics);
    }

    // ── constructor ──────────────────────────────────────────────────

    /**
     * both alarms use POOLED_THREAD so callbacks run off-EDT on the platform thread pool.
     * passing 'this' ties their lifecycle to this service — when the service disposes,
     * the alarms cancel themselves automatically.
     */
    public SnapshotScheduler() {
        this.liveAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, this);
        this.persistAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, this);
        this.detector = new FlowDetector();
    }

    // ── start / stop ─────────────────────────────────────────────────

    /**
     * kicks off both periodic loops.
     *
     * Alarm.addRequest() is a one-shot timer, not a repeating one —
     * so the pattern is: callback does its work, then re-enqueues itself at the end.
     * this gives a fixed-DELAY schedule (interval measured from task completion, not start).
     * actually better for us — if a persist cycle takes ages, the next one doesnt stack up.
     */
    public void start() {
        if (running) {
            LOG.warn("SnapshotScheduler already running");
            return;
        }
        running = true;

        LOG.info("Starting SnapshotScheduler — live refresh every " + LIVE_REFRESH_MS
                + "ms, persist cycle every " + PERSIST_INTERVAL_MS + "ms");

        scheduleLiveRefresh();
        schedulePersistCycle();
    }

    /**
     * stops both periodic loops.
     * cancels any pending requests without interrupting in-flight callbacks.
     */
    public void stop() {
        running = false;
        liveAlarm.cancelAllRequests();
        persistAlarm.cancelAllRequests();
        LOG.info("SnapshotScheduler stopped");
    }

    // ── scheduling plumbing ──────────────────────────────────────────

    /**
     * the re-enqueue pattern: do work → schedule next run → repeat.
     * the running guard makes sure we dont re-enqueue after stop() was called.
     */
    private void scheduleLiveRefresh() {
        if (!running) return;
        liveAlarm.addRequest(() -> {
            performLiveRefresh();
            scheduleLiveRefresh();   // re-enqueue for next cycle
        }, LIVE_REFRESH_MS);
    }

    /**
     * same re-enqueue pattern for the heavier persist cycle.
     * separate alarm instance means this doesnt compete with live refresh timing.
     */
    private void schedulePersistCycle() {
        if (!running) return;
        persistAlarm.addRequest(() -> {
            performPersistCycle();
            schedulePersistCycle();   // re-enqueue for next cycle
        }, PERSIST_INTERVAL_MS);
    }

    // ── core logic (the actual work which is unchanged from before) ─────────

    /**
     * lightweight cycle: snapshot current metrics, run detection, push to UI.
     * does NOT reset interval counters or run the expensive error scan.
     * this is the fast one that runs every 2 seconds.
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

            // notify all registered listeners — wrapped in try/catch each
            // so one broken listener doesnt take down the whole loop
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
     * heavy cycle: the expensive one that runs every 60 seconds.
     *
     * order matters here:
     *   1. refresh error count (scans all open files which is too frickin expensive, hence once per minute)
     *   2. snapshot + detect with fresh error data
     *   3. persist if enabled and MUST happen BEFORE counter reset so interval
     *      values (file switches, focus losses, compile errors) are captured
     *   4. reset interval counters so next minute starts clean
     *   5. check if an ESM probe should fire
     */
    private void performPersistCycle() {
        try {
            MetricCollector collector = ApplicationManager.getApplication()
                    .getService(MetricCollector.class);

            if (collector == null) return;

            // 1. refresh syntax error count (scans all open files)
            ErrorHighlightListener.recordCurrentErrors();

            // 2. snapshot + detect with the freshly updated error count
            FlowMetrics metrics = collector.snapshot();
            FlowDetectionResult result = detector.detect(metrics);
            lastResult = result;

            // 3. persist if enabled in settings
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

            // 4. reset interval-based counters (file switches, focus losses, compile errors)
            collector.resetIntervalCounters();

            // 5. check whether an ESM probe should be delivered
            EsmProbeService esm = ApplicationManager.getApplication()
                    .getService(EsmProbeService.class);
            if (esm != null) esm.checkAndDeliver(result);

            LOG.debug("Persist cycle complete — interval counters reset");
        } catch (Exception e) {
            LOG.error("Error during persist cycle", e);
        }
    }

    // ── public API ───────────────────────────────────────────────────

    /**
     * triggers an immediate live refresh outside the regular schedule.
     * useful for when the UI needs a result right now (e.g. tool window just opened).
     */
    public FlowDetectionResult triggerImmediateSnapshot() {
        performLiveRefresh();
        return lastResult;
    }

    public void addListener(FlowDetectionListener listener) {
        listeners.add(listener);
    }

    public void removeListener(FlowDetectionListener listener) {
        listeners.remove(listener);
    }

    public FlowDetectionResult getLastResult() {
        return lastResult;
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    public void dispose() {
        stop();
        listeners.clear();
    }
}