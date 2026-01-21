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
    
    /** Interval between flow detection snapshots (seconds) */
    private static final int SNAPSHOT_INTERVAL_SECONDS = 60;
    
    private final ScheduledExecutorService executor;
    private ScheduledFuture<?> scheduledTask;
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
     * Starts the periodic snapshot scheduler.
     * Should be called when the plugin initializes.
     */
    public void start() {
        if (scheduledTask != null && !scheduledTask.isCancelled()) {
            LOG.warn("SnapshotScheduler already running");
            return;
        }
        
        LOG.info("Starting SnapshotScheduler with " + SNAPSHOT_INTERVAL_SECONDS + "s interval");
        
        scheduledTask = executor.scheduleAtFixedRate(
                this::performSnapshot,
                SNAPSHOT_INTERVAL_SECONDS,  // initial delay
                SNAPSHOT_INTERVAL_SECONDS,  // period
                TimeUnit.SECONDS
        );
    }

    /**
     * Stops the periodic scheduler.
     */
    public void stop() {
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
            scheduledTask = null;
            LOG.info("SnapshotScheduler stopped");
        }
    }

    /**
     * Performs a single snapshot and detection cycle.
     * Called periodically by the scheduler.
     */
    private void performSnapshot() {
        try {
            MetricCollector collector = ApplicationManager.getApplication()
                    .getService(MetricCollector.class);
            
            if (collector == null) {
                LOG.warn("MetricCollector service not available");
                return;
            }
            
            // Update syntax error count from code analysis
            ErrorHighlightListener.recordCurrentErrors();
            
            // Create snapshot
            FlowMetrics metrics = collector.snapshot();
            
            // Run detection
            FlowDetectionResult result = detector.detect(metrics);
            lastResult = result;
            
            LOG.debug("Flow detection: score=" + String.format("%.1f", result.getFlowTally() * 100) 
                    + ", state=" + result.getState());
            
            // Notify listeners
            for (FlowDetectionListener listener : listeners) {
                try {
                    listener.onFlowDetected(result, metrics);
                } catch (Exception e) {
                    LOG.error("Error notifying flow detection listener", e);
                }
            }
            
            // Reset interval counters for next snapshot
            collector.resetIntervalCounters();
            
        } catch (Exception e) {
            LOG.error("Error during flow detection snapshot", e);
        }
    }

    /**
     * Triggers an immediate snapshot outside the regular schedule.
     * Useful for manual refresh or when significant events occur.
     */
    public FlowDetectionResult triggerImmediateSnapshot() {
        performSnapshot();
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
        return scheduledTask != null && !scheduledTask.isCancelled();
    }

    @Override
    public void dispose() {
        stop();
        listeners.clear();
    }
}
