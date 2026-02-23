package com.ronin.therapeuticdev.services;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.Disposable;
import com.ronin.therapeuticdev.metrics.FlowMetrics;

import java.time.Instant;
import java.util.Deque;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Central service that collects and aggregates coding metrics in real-time.
 * 
 * All listener classes feed raw events into this collector.
 * Periodically, the SnapshotScheduler calls {@link #snapshot()} to capture
 * current state and feed it to FlowDetector for analysis.
 * 
 * Thread-safe: uses atomic types for all counters since multiple
 * editor threads may record events concurrently.
 *
 * @see <a href="https://plugins.jetbrains.com/docs/intellij/plugin-services.html">
 *      IntelliJ Platform SDK - Services</a>
 */
@Service(Service.Level.APP)
public final class MetricCollector implements Disposable {

    // ==================== SESSION INFO ====================
    private final String sessionId;
    private final Instant sessionStart;
    
    // ==================== TYPING METRICS ====================
    private final AtomicInteger keystrokeCount = new AtomicInteger(0);
    private final AtomicInteger backspaceCount = new AtomicInteger(0);
    private final AtomicLong lastKeystrokeTimeMs = new AtomicLong(0);
    private final AtomicLong totalKeyIntervalMs = new AtomicLong(0);
    private final AtomicInteger keyIntervalSamples = new AtomicInteger(0);

    /** Sliding window for KPM — stores timestamps of recent keystrokes */
    private final Deque<Long> recentKeyTimestamps = new ConcurrentLinkedDeque<>();
    private static final long KPM_WINDOW_MS = 120_000; // 2-minute sliding window
    
    // ==================== ERROR METRICS ====================
    private final AtomicInteger syntaxErrorCount = new AtomicInteger(0);
    private final AtomicInteger compilationErrorCount = new AtomicInteger(0);
    private final AtomicLong lastErrorTimeMs = new AtomicLong(0);
    
    // ==================== FOCUS METRICS ====================
    private final AtomicInteger fileChangeCount = new AtomicInteger(0);
    private final AtomicInteger focusLostCount = new AtomicInteger(0);
    private final AtomicLong currentFileStartMs = new AtomicLong(0);
    private volatile String currentFilePath = "";
    
    // ==================== BUILD METRICS ====================
    private final AtomicBoolean lastBuildSuccess = new AtomicBoolean(true);
    private final AtomicInteger consecutiveFailedBuilds = new AtomicInteger(0);
    private final AtomicLong lastBuildTimeMs = new AtomicLong(0);
    private final AtomicInteger totalBuilds = new AtomicInteger(0);
    private final AtomicInteger successfulBuilds = new AtomicInteger(0);

    /**
     * Constructor - called by IntelliJ's service system.
     */
    public MetricCollector() {
        this.sessionId = UUID.randomUUID().toString();
        this.sessionStart = Instant.now();
        this.currentFileStartMs.set(System.currentTimeMillis());
    }

    // ==================== TYPING RECORDING ====================

    /**
     * Records a keystroke event.
     * Called by TypingActivityListener.
     */
    public void recKeystroke(long timestampMs) {
        keystrokeCount.incrementAndGet();

        // Add to sliding window and prune entries outside the window
        recentKeyTimestamps.addLast(timestampMs);
        long cutoff = timestampMs - KPM_WINDOW_MS;
        while (!recentKeyTimestamps.isEmpty() && recentKeyTimestamps.peekFirst() < cutoff) {
            recentKeyTimestamps.pollFirst();
        }

        // Calculate interval from last keystroke for rhythm analysis
        long lastTime = lastKeystrokeTimeMs.getAndSet(timestampMs);
        if (lastTime > 0) {
            long interval = timestampMs - lastTime;
            // Only count reasonable intervals (< 5 seconds)
            if (interval < 5000) {
                totalKeyIntervalMs.addAndGet(interval);
                keyIntervalSamples.incrementAndGet();
            }
        }
    }

    /**
     * Records a backspace/delete event.
     * Called by BackspaceHandler.
     */
    public void recBackspc(long timestampMs) {
        backspaceCount.incrementAndGet();
        lastKeystrokeTimeMs.set(timestampMs);
    }

    // ==================== ERROR RECORDING ====================

    /**
     * Records current syntax error count from code analysis.
     * Called by ErrorHighlightListener.
     */
    public void recSyntaxErrors(int count) {
        int previous = syntaxErrorCount.getAndSet(count);
        if (count > previous) {
            lastErrorTimeMs.set(System.currentTimeMillis());
        }
    }

    /**
     * Records compilation errors from build results.
     * Called by BuildListener.
     */
    public void recCompilationErrors(int count) {
        compilationErrorCount.addAndGet(count);
        lastErrorTimeMs.set(System.currentTimeMillis());
    }

    // ==================== FOCUS RECORDING ====================

    /**
     * Records a file switch event.
     * Called by FileActivityListener.
     */
    public void recFileChange(long timestampMs, String newFilePath) {
        fileChangeCount.incrementAndGet();
        currentFileStartMs.set(timestampMs);
        currentFilePath = newFilePath;
    }

    /**
     * Records a file open event.
     */
    public void recFileOpen(String filePath) {
        if (currentFilePath.isEmpty()) {
            currentFilePath = filePath;
            currentFileStartMs.set(System.currentTimeMillis());
        }
    }

    /**
     * Records IDE focus loss (user switched to another app).
     * Called by FocusListener.
     */
    public void recFocusLost(long timestampMs) {
        focusLostCount.incrementAndGet();
    }

    /**
     * Records IDE focus regained.
     * Called by FocusListener.
     */
    public void recFocusRegained(long timestampMs, long awayDurationMs) {
        // Could track total away time if needed
    }

    // ==================== BUILD RECORDING ====================

    /**
     * Records a build result.
     * Called by BuildListener.
     */
    public void recBuildResult(long timestampMs, boolean success, int errors, int warnings) {
        totalBuilds.incrementAndGet();
        lastBuildTimeMs.set(timestampMs);
        lastBuildSuccess.set(success);
        
        if (success) {
            successfulBuilds.incrementAndGet();
            consecutiveFailedBuilds.set(0);
        } else {
            consecutiveFailedBuilds.incrementAndGet();
        }
    }

    // ==================== SNAPSHOT & CALCULATIONS ====================

    /**
     * Calculates keystrokes per minute over a 2-minute sliding window.
     * Reflects current typing rate rather than a diluted session average.
     */
    public double getKeystrokesPerMinute() {
        long now = System.currentTimeMillis();
        long cutoff = now - KPM_WINDOW_MS;
        long recentCount = recentKeyTimestamps.stream()
                .filter(t -> t >= cutoff)
                .count();
        return recentCount / (KPM_WINDOW_MS / 60_000.0);
    }

    /**
     * Calculates average interval between keystrokes (typing rhythm).
     */
    public double getAverageKeyIntervalMs() {
        int samples = keyIntervalSamples.get();
        if (samples == 0) return 0;
        return (double) totalKeyIntervalMs.get() / samples;
    }

    /**
     * Calculates time since last keystroke.
     */
    public long getKeyboardIdleMs() {
        long lastTime = lastKeystrokeTimeMs.get();
        if (lastTime == 0) return 0;
        return System.currentTimeMillis() - lastTime;
    }

    /**
     * Calculates time since last error.
     */
    public long getTimeSinceLastErrorMs() {
        long lastTime = lastErrorTimeMs.get();
        if (lastTime == 0) return Long.MAX_VALUE; // No errors yet
        return System.currentTimeMillis() - lastTime;
    }

    /**
     * Calculates time in current file.
     */
    public long getTimeInCurrentFileMs() {
        return System.currentTimeMillis() - currentFileStartMs.get();
    }

    /**
     * Calculates time since last build.
     */
    public long getTimeSinceLastBuildMs() {
        long lastTime = lastBuildTimeMs.get();
        if (lastTime == 0) return Long.MAX_VALUE;
        return System.currentTimeMillis() - lastTime;
    }

    /**
     * Gets session duration in milliseconds.
     */
    public long getSessionDurationMs() {
        return System.currentTimeMillis() - sessionStart.toEpochMilli();
    }

    /**
     * Creates an immutable snapshot of current metrics.
     * Called by SnapshotScheduler.
     */
    public FlowMetrics snapshot() {
        return new FlowMetrics.Builder()
                // Typing metrics
                .keystrokesPerMinute(getKeystrokesPerMinute())
                .avgKeyIntervalMs(getAverageKeyIntervalMs())
                .backspaceCount(backspaceCount.get())
                .keyboardIdleMs(getKeyboardIdleMs())
                
                // Error metrics
                .syntaxErrorCount(syntaxErrorCount.get())
                .compilationErrors(compilationErrorCount.get())
                .timeSinceLastErrorMs(getTimeSinceLastErrorMs())
                
                // Focus metrics
                .fileChangesLast5Min(fileChangeCount.get())
                .timeInCurrentFileMs(getTimeInCurrentFileMs())
                .focusLossCount(focusLostCount.get())
                
                // Build metrics
                .lastBuildSuccess(lastBuildSuccess.get())
                .consecutiveFailedBuilds(consecutiveFailedBuilds.get())
                .timeSinceLastBuildMs(getTimeSinceLastBuildMs())
                
                // Session metadata
                .timestamp(Instant.now())
                .sessionId(sessionId)
                .sessionDuration(getSessionDurationMs())
                
                .build();
    }

    /**
     * Resets interval-based counters (called after snapshot).
     * Preserves session-wide data and timestamps.
     */
    public void resetIntervalCounters() {
        // Reset counters that should be fresh for each interval
        fileChangeCount.set(0);
        focusLostCount.set(0);
        compilationErrorCount.set(0);
        
        // Note: keystroke counts are cumulative for KPM calculation
        // Note: syntax errors reflect current state, not reset
        // Note: build streak persists across intervals
    }

    // ==================== GETTERS FOR UI ====================

    public String getSessionId() { return sessionId; }
    public Instant getSessionStart() { return sessionStart; }
    public int getKeystrokeCount() { return keystrokeCount.get(); }
    public int getBackspaceCount() { return backspaceCount.get(); }
    public int getSyntaxErrorCount() { return syntaxErrorCount.get(); }
    public int getFileChangeCount() { return fileChangeCount.get(); }
    public int getFocusLostCount() { return focusLostCount.get(); }
    public boolean isLastBuildSuccess() { return lastBuildSuccess.get(); }
    public int getConsecutiveFailedBuilds() { return consecutiveFailedBuilds.get(); }
    public String getCurrentFilePath() { return currentFilePath; }

    @Override
    public void dispose() {
        // Cleanup - could persist final state here
    }
}
