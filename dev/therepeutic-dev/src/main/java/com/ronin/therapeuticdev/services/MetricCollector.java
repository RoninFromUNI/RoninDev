package com.ronin.therapeuticdev.services;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.Disposable;
import com.ronin.therapeuticdev.metrics.FlowMetrics;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.*;

/**
 * Central service that collects and aggregates coding metrics in real-time.
 *
 * All listener classes feed raw events into this collector.
 * SnapshotScheduler calls {@link #snapshot()} periodically to produce a
 * FlowMetrics value object for the detection algorithm.
 *
 * Thread-safe: all shared state uses atomic types or ConcurrentLinkedDeque.
 */
@Service(Service.Level.APP)
public final class MetricCollector implements Disposable {

    // ==================== SESSION ====================
    private final String  sessionId;
    private final Instant sessionStart;

    // ==================== TYPING ====================
    private final AtomicInteger keystrokeCount      = new AtomicInteger(0);
    private final AtomicInteger backspaceCount       = new AtomicInteger(0);
    private final AtomicLong    lastKeystrokeTimeMs  = new AtomicLong(0);
    private final AtomicLong    totalKeyIntervalMs   = new AtomicLong(0);
    private final AtomicInteger keyIntervalSamples   = new AtomicInteger(0);

    /** Sliding window (2 min) for real-time KPM. */
    private final Deque<Long> recentKeyTimestamps = new ConcurrentLinkedDeque<>();
    private static final long KPM_WINDOW_MS = 120_000;

    /**
     * Recent inter-keystroke intervals (bounded to 50 entries) for burst-consistency
     * (CoV) calculation. Only intervals < 5 s are stored.
     */
    private final Deque<Long> recentIntervals = new ConcurrentLinkedDeque<>();
    private static final int  INTERVAL_WINDOW = 50;

    // error counts + last errors + error resolution
    private final AtomicInteger syntaxErrorCount       = new AtomicInteger(0);
    private final AtomicInteger compilationErrorCount  = new AtomicInteger(0);
    private final AtomicLong    lastErrorTimeMs        = new AtomicLong(0);
    /** Errors that appeared (count went up) since last reset. */
    private final AtomicInteger errorsIntroduced       = new AtomicInteger(0);
    /** Errors that were cleared (count went down) since last reset. */
    private final AtomicInteger errorsResolved         = new AtomicInteger(0);
    // focus change + focus loss + file start + file path counts and starts
    // all measured in ms and atomic values (initally all at 0)
    private final AtomicInteger fileChangeCount  = new AtomicInteger(0);
    private final AtomicInteger focusLostCount   = new AtomicInteger(0);
    private final AtomicLong    currentFileStartMs = new AtomicLong(0);
    private volatile String     currentFilePath   = "";
    /** Completed file visits: [startMs, endMs], parallel with completedVisitPaths. */
    private final Deque<long[]> completedVisits     = new ConcurrentLinkedDeque<>();
    private final Deque<String> completedVisitPaths = new ConcurrentLinkedDeque<>();
    private final Deque<Long>   fileSwitchTimestamps = new ConcurrentLinkedDeque<>();
    private static final long   ACTIVITY_WINDOW_MS  = 30 * 60_000L;
    // total idel + window start in ms , ide actie start and ide focus
    /** When the current interval started (for ideFocusPct denominator). */
    private final AtomicLong windowStartMs     = new AtomicLong(0);
    /** Accumulated ms IntelliJ was active in this interval. */
    private final AtomicLong totalIdeFocusMs   = new AtomicLong(0);
    /** When IntelliJ most recently became the active window. */
    private final AtomicLong ideActiveStartMs  = new AtomicLong(0);
    private final AtomicBoolean ideIsFocused   = new AtomicBoolean(true);
    // last build success + failed builds + last buildd time
    private final AtomicBoolean lastBuildSuccess       = new AtomicBoolean(true);
    private final AtomicInteger consecutiveFailedBuilds = new AtomicInteger(0);
    private final AtomicLong    lastBuildTimeMs        = new AtomicLong(0);
    /** Build count within the current interval (reset each persist cycle). */
    private final AtomicInteger buildsInWindow         = new AtomicInteger(0);
    /** Successful builds within the current interval. */
    private final AtomicInteger successBuildsInWindow  = new AtomicInteger(0);

    // ==================== AI HEURISTIC ====================
    /**
     * Count of large single-event document insertions (>= 40 chars net gain),
     * used as a proxy for AI suggestion acceptance. Tracked by AiSuggestionListener.
     */
    private final AtomicInteger aiSuggestionsAccepted = new AtomicInteger(0);

    // ==================== CONSTRUCTOR ====================

    public MetricCollector() {
        String uuid = UUID.randomUUID().toString();
        String pid = ParticipantSession.getInstance().getParticipantId();
        this.sessionId = pid.isBlank() ? uuid : pid + "_" + uuid;
        this.sessionStart = Instant.now();
        long now = System.currentTimeMillis();
        this.currentFileStartMs.set(now);
        this.windowStartMs.set(now);
        this.ideActiveStartMs.set(now);
    }

    // ==================== TYPING RECORDING ====================

    /** Called by TypingActivityListener on every character typed. */
    public void recKeystroke(long timestampMs) {
        keystrokeCount.incrementAndGet();

        // Sliding-window KPM
        recentKeyTimestamps.addLast(timestampMs);
        long cutoff = timestampMs - KPM_WINDOW_MS;
        while (!recentKeyTimestamps.isEmpty() && recentKeyTimestamps.peekFirst() < cutoff) {
            recentKeyTimestamps.pollFirst();
        }

        // Inter-keystroke interval for burst consistency
        long lastTime = lastKeystrokeTimeMs.getAndSet(timestampMs);
        if (lastTime > 0) {
            long interval = timestampMs - lastTime;
            if (interval > 0 && interval < 5000) {
                totalKeyIntervalMs.addAndGet(interval);
                keyIntervalSamples.incrementAndGet();

                // Bounded deque for CoV
                recentIntervals.addLast(interval);
                if (recentIntervals.size() > INTERVAL_WINDOW) {
                    recentIntervals.pollFirst();
                }
            }
        }
    }

    /** Called by BackspaceHandler on every backspace/delete. */
    public void recBackspc(long timestampMs) {
        backspaceCount.incrementAndGet();
        lastKeystrokeTimeMs.set(timestampMs); // counts as activity
    }

    // ==================== ERROR RECORDING ====================

    /** Called by ErrorHighlightListener with the current syntax error count. */
    public void recSyntaxErrors(int newCount) {
        int previous = syntaxErrorCount.getAndSet(newCount);
        if (newCount > previous) {
            errorsIntroduced.addAndGet(newCount - previous);
            lastErrorTimeMs.set(System.currentTimeMillis());
        } else if (newCount < previous) {
            errorsResolved.addAndGet(previous - newCount);
        }
    }

    /** Called by BuildListener with compilation error count from a build. */
    public void recCompilationErrors(int count) {
        compilationErrorCount.addAndGet(count);
        if (count > 0) lastErrorTimeMs.set(System.currentTimeMillis());
    }

    // ==================== FOCUS RECORDING ====================

    /** Called by FileActivityListener on file tab switch. */
    public void recFileChange(long timestampMs, String newFilePath) {
        String prevPath = currentFilePath;
        long   prevStart = currentFileStartMs.get();
        if (!prevPath.isEmpty() && prevStart > 0) {
            completedVisits.addLast(new long[]{prevStart, timestampMs});
            completedVisitPaths.addLast(prevPath);
            long cutoff = timestampMs - ACTIVITY_WINDOW_MS;
            while (!completedVisits.isEmpty() && completedVisits.peekFirst()[1] < cutoff) {
                completedVisits.pollFirst();
                completedVisitPaths.pollFirst();
            }
        }
        fileSwitchTimestamps.addLast(timestampMs);
        long switchCutoff = timestampMs - ACTIVITY_WINDOW_MS;
        while (!fileSwitchTimestamps.isEmpty() && fileSwitchTimestamps.peekFirst() < switchCutoff) {
            fileSwitchTimestamps.pollFirst();
        }
        fileChangeCount.incrementAndGet();
        currentFileStartMs.set(timestampMs);
        currentFilePath = newFilePath;
    }

    /** Called by FileActivityListener on first file open. */
    public void recFileOpen(String filePath) {
        if (currentFilePath.isEmpty()) {
            currentFilePath = filePath;
            currentFileStartMs.set(System.currentTimeMillis());
        }
    }

    /** Called by FocusListener when the user alt-tabs away. */
    public void recFocusLost(long timestampMs) {
        focusLostCount.incrementAndGet();
        if (ideIsFocused.compareAndSet(true, false)) {
            // Accumulate the time IntelliJ was active up to now
            long active = timestampMs - ideActiveStartMs.get();
            if (active > 0) totalIdeFocusMs.addAndGet(active);
        }
    }

    /** Called by FocusListener when the user returns to IntelliJ. */
    public void recFocusRegained(long timestampMs, long awayDurationMs) {
        if (ideIsFocused.compareAndSet(false, true)) {
            ideActiveStartMs.set(timestampMs);
        }
    }

    // ==================== BUILD RECORDING ====================

    /** Called by BuildListener on every build completion. */
    public void recBuildResult(long timestampMs, boolean success, int errors, int warnings) {
        lastBuildTimeMs.set(timestampMs);
        lastBuildSuccess.set(success);
        buildsInWindow.incrementAndGet();

        if (success) {
            successBuildsInWindow.incrementAndGet();
            consecutiveFailedBuilds.set(0);
        } else {
            consecutiveFailedBuilds.incrementAndGet();
            recCompilationErrors(errors);
        }
    }

    // ==================== AI HEURISTIC ====================

    /**
     * Called by AiSuggestionListener when a large single-event text insertion
     * is detected (heuristic for AI completion acceptance).
     */
    public void recAiSuggestionAccepted(long timestampMs) {
        aiSuggestionsAccepted.incrementAndGet();
    }

    // ==================== CALCULATIONS ====================

    /** Real-time KPM over the 2-minute sliding window. */
    public double getKeystrokesPerMinute() {
        long now    = System.currentTimeMillis();
        long cutoff = now - KPM_WINDOW_MS;
        long count  = recentKeyTimestamps.stream().filter(t -> t >= cutoff).count();
        return count / (KPM_WINDOW_MS / 60_000.0);
    }

    public double getAverageKeyIntervalMs() {
        int samples = keyIntervalSamples.get();
        return samples == 0 ? 0 : (double) totalKeyIntervalMs.get() / samples;
    }

    public long getKeyboardIdleMs() {
        long last = lastKeystrokeTimeMs.get();
        return last == 0 ? 0 : System.currentTimeMillis() - last;
    }

    /**
     * Burst consistency as a normalised [0, 1] score.
     * 1.0 = perfectly rhythmic typing; 0.0 = completely erratic.
     * Computed from the CoV of recent inter-keystroke intervals.
     */
    public double getBurstConsistency() {
        List<Long> intervals = new ArrayList<>(recentIntervals);
        if (intervals.size() < 3) return 1.0; // insufficient data → assume rhythmic

        double mean = intervals.stream().mapToLong(Long::longValue).average().orElse(1);
        if (mean == 0) return 1.0;
        double variance = intervals.stream()
                .mapToDouble(i -> Math.pow(i - mean, 2))
                .average().orElse(0);
        double cov = Math.sqrt(variance) / mean; // coefficient of variation

        // Map CoV to [0,1]: cov=0 → 1.0, cov=1.5+ → 0.0
        return Math.max(0.0, 1.0 - (cov / 1.5));
    }

    public long getTimeSinceLastErrorMs() {
        long last = lastErrorTimeMs.get();
        return last == 0 ? Long.MAX_VALUE : System.currentTimeMillis() - last;
    }

    public long getTimeInCurrentFileMs() {
        return System.currentTimeMillis() - currentFileStartMs.get();
    }

    public long getTimeSinceLastBuildMs() {
        long last = lastBuildTimeMs.get();
        return last == 0 ? Long.MAX_VALUE : System.currentTimeMillis() - last;
    }

    public long getSessionDurationMs() {
        return System.currentTimeMillis() - sessionStart.toEpochMilli();
    }

    /**
     * Proportion of the current interval that IntelliJ was the active window.
     * 1.0 = fully focused, 0.0 = entirely away.
     */
    public double getIdeFocusPct() {
        long now            = System.currentTimeMillis();
        long windowDuration = now - windowStartMs.get();
        if (windowDuration <= 0) return 1.0;

        long focusMs = totalIdeFocusMs.get();
        if (ideIsFocused.get()) {
            long sinceActive = now - ideActiveStartMs.get();
            if (sinceActive > 0) focusMs += sinceActive;
        }
        return Math.min(1.0, (double) focusMs / windowDuration);
    }

    /** Build success rate within the current interval (NaN-safe). */
    public double getBuildSuccessRate() {
        int total = buildsInWindow.get();
        return total == 0 ? 1.0 : (double) successBuildsInWindow.get() / total;
    }

    // ==================== SNAPSHOT ====================

    /**
     * Creates an immutable FlowMetrics snapshot from current state.
     * Called by SnapshotScheduler every 2 s (live) and every 60 s (persist).
     */
    public FlowMetrics snapshot() {
        return FlowMetrics.builder()
                // Typing
                .keystrokesPerMinute(getKeystrokesPerMinute())
                .avgKeyIntervalMs(getAverageKeyIntervalMs())
                .backspaceCount(backspaceCount.get())
                .keyboardIdleMs(getKeyboardIdleMs())
                .burstConsistency(getBurstConsistency())
                // Errors
                .syntaxErrorCount(syntaxErrorCount.get())
                .compilationErrors(compilationErrorCount.get())
                .timeSinceLastErrorMs(getTimeSinceLastErrorMs())
                .errorsIntroduced(errorsIntroduced.get())
                .errorsResolved(errorsResolved.get())
                // Focus
                .fileChangesLast5Min(fileChangeCount.get())
                .timeInCurrentFileMs(getTimeInCurrentFileMs())
                .focusLossCount(focusLostCount.get())
                // Build
                .lastBuildSuccess(lastBuildSuccess.get())
                .consecutiveFailedBuilds(consecutiveFailedBuilds.get())
                .timeSinceLastBuildMs(getTimeSinceLastBuildMs())
                .buildsInWindow(buildsInWindow.get())
                .buildSuccessRate(getBuildSuccessRate())
                // Context
                .ideFocusPct(getIdeFocusPct())
                .aiSuggestionsAccepted(aiSuggestionsAccepted.get())
                // Session
                .timestamp(Instant.now())
                .sessionId(sessionId)
                .sessionDuration(getSessionDurationMs())
                .build();
    }

    /**
     * Resets interval-based counters after a persist cycle.
     * Must be called AFTER snapshot() and persistence so the values are captured first.
     */
    public void resetIntervalCounters() {
        fileChangeCount.set(0);
        focusLostCount.set(0);
        compilationErrorCount.set(0);
        errorsIntroduced.set(0);
        errorsResolved.set(0);
        buildsInWindow.set(0);
        successBuildsInWindow.set(0);
        aiSuggestionsAccepted.set(0);

        // Reset IDE focus window
        long now = System.currentTimeMillis();
        windowStartMs.set(now);
        totalIdeFocusMs.set(0);
        if (ideIsFocused.get()) {
            ideActiveStartMs.set(now);
        }
    }

    // ==================== ACTIVITY DATA FOR UI ====================

    public List<Long> getFileSwitchTimestamps() {
        return new ArrayList<>(fileSwitchTimestamps);
    }

    public Map<String, Long> getFileActivityMap(long windowMs) {
        long now    = System.currentTimeMillis();
        long cutoff = now - windowMs;
        Map<String, Long> result = new LinkedHashMap<>();

        Iterator<long[]> visitIter = completedVisits.iterator();
        Iterator<String> pathIter  = completedVisitPaths.iterator();
        while (visitIter.hasNext() && pathIter.hasNext()) {
            long[] visit = visitIter.next();
            String path  = pathIter.next();
            if (visit[1] < cutoff) continue;
            long effectiveStart = Math.max(visit[0], cutoff);
            long duration = visit[1] - effectiveStart;
            if (duration > 0) result.merge(extractFilename(path), duration, Long::sum);
        }

        String current      = currentFilePath;
        long   currentStart = currentFileStartMs.get();
        if (!current.isEmpty() && currentStart > 0) {
            long effectiveStart = Math.max(currentStart, cutoff);
            long duration = now - effectiveStart;
            if (duration > 0) result.merge(extractFilename(current), duration, Long::sum);
        }
        return result;
    }

    private String extractFilename(String path) {
        int idx = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return idx >= 0 ? path.substring(idx + 1) : path;
    }

    public List<String[]> getRecentVisits(int max) {
        List<String[]> result = new ArrayList<>();
        List<long[]>   visits = new ArrayList<>(completedVisits);
        List<String>   paths  = new ArrayList<>(completedVisitPaths);

        String current      = currentFilePath;
        long   currentStart = currentFileStartMs.get();
        if (!current.isEmpty() && currentStart > 0) {
            result.add(new String[]{extractFilename(current), String.valueOf(currentStart), "0"});
        }
        for (int i = Math.min(visits.size(), paths.size()) - 1; i >= 0 && result.size() < max; i--) {
            long[] v = visits.get(i);
            result.add(new String[]{extractFilename(paths.get(i)), String.valueOf(v[0]), String.valueOf(v[1])});
        }
        return result;
    }

    // ==================== GETTERS FOR UI ====================

    public String  getSessionId()             { return sessionId; }
    public Instant getSessionStart()          { return sessionStart; }
    public int     getKeystrokeCount()        { return keystrokeCount.get(); }
    public int     getBackspaceCount()        { return backspaceCount.get(); }
    public int     getSyntaxErrorCount()      { return syntaxErrorCount.get(); }
    public int     getFileChangeCount()       { return fileChangeCount.get(); }
    public int     getFocusLostCount()        { return focusLostCount.get(); }
    public boolean isLastBuildSuccess()       { return lastBuildSuccess.get(); }
    public int     getConsecutiveFailedBuilds(){ return consecutiveFailedBuilds.get(); }
    public String  getCurrentFilePath()       { return currentFilePath; }

    @Override
    public void dispose() { /* cleanup if needed */ }
}
