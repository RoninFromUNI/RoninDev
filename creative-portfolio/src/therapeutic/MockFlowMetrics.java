package therapeutic;

/**
 * Mirrors the plugin's FlowMetrics data class.
 * Contains all 14 real-time variables tracked by therapeutic-dev
 * across the 5 scoring categories.
 *
 * Plugin source: com.ronin.therapeuticdev.metrics.FlowMetrics
 */
public class MockFlowMetrics {

    // ── Typing (30% weight) ───────────────────────────────────────────────────
    /** Keystrokes per minute — 2-minute sliding window in the plugin */
    public final int    keystrokesPerMin;
    /** Average milliseconds between successive keystrokes */
    public final double avgKeyIntervalMs;
    /** Total backspaces typed this interval — proxy for rework / confusion */
    public final int    backspaceCount;
    /** Milliseconds since the last recorded keystroke */
    public final long   keyboardIdleMs;

    // ── Errors (25% weight) ───────────────────────────────────────────────────
    /** Live syntax error count scanned every 60s */
    public final int  syntaxErrorCount;
    /** Compilation errors from the last build attempt */
    public final int  compilationErrors;
    /** How long ago the last error was observed (ms) */
    public final long timeSinceLastErrorMs;

    // ── Focus / Context switching (20% weight) ────────────────────────────────
    /** File switches recorded in the last 5 minutes */
    public final int  fileChangesLast5Min;
    /** Milliseconds spent continuously in the current file */
    public final long timeInCurrentFileMs;
    /** Number of times the IDE lost window focus this interval */
    public final int  focusLossCount;

    // ── Builds (15% weight) ───────────────────────────────────────────────────
    /** Whether the most recent compilation succeeded */
    public final boolean lastBuildSuccess;
    /** Consecutive failed build streak */
    public final int     consecutiveFailedBuilds;
    /** Milliseconds since the last build ran */
    public final long    timeSinceLastBuildMs;

    // ── Activity / Session (10% weight) ───────────────────────────────────────
    /** Total session duration in milliseconds */
    public final long sessionDurationMs;

    // ─────────────────────────────────────────────────────────────────────────

    private MockFlowMetrics(Builder b) {
        keystrokesPerMin       = b.keystrokesPerMin;
        avgKeyIntervalMs       = b.avgKeyIntervalMs;
        backspaceCount         = b.backspaceCount;
        keyboardIdleMs         = b.keyboardIdleMs;
        syntaxErrorCount       = b.syntaxErrorCount;
        compilationErrors      = b.compilationErrors;
        timeSinceLastErrorMs   = b.timeSinceLastErrorMs;
        fileChangesLast5Min    = b.fileChangesLast5Min;
        timeInCurrentFileMs    = b.timeInCurrentFileMs;
        focusLossCount         = b.focusLossCount;
        lastBuildSuccess       = b.lastBuildSuccess;
        consecutiveFailedBuilds = b.consecutiveFailedBuilds;
        timeSinceLastBuildMs   = b.timeSinceLastBuildMs;
        sessionDurationMs      = b.sessionDurationMs;
    }

    // ── Builder ───────────────────────────────────────────────────────────────

    public static class Builder {
        int     keystrokesPerMin       = 50;
        double  avgKeyIntervalMs       = 400;
        int     backspaceCount         = 10;
        long    keyboardIdleMs         = 2_000;
        int     syntaxErrorCount       = 0;
        int     compilationErrors      = 0;
        long    timeSinceLastErrorMs   = 300_000;
        int     fileChangesLast5Min    = 2;
        long    timeInCurrentFileMs    = 300_000;
        int     focusLossCount         = 1;
        boolean lastBuildSuccess       = true;
        int     consecutiveFailedBuilds = 0;
        long    timeSinceLastBuildMs   = 300_000;
        long    sessionDurationMs      = 3_600_000;

        public Builder kpm(int v)            { keystrokesPerMin       = v; return this; }
        public Builder avgInterval(double v) { avgKeyIntervalMs       = v; return this; }
        public Builder backspaces(int v)     { backspaceCount         = v; return this; }
        public Builder idle(long v)          { keyboardIdleMs         = v; return this; }
        public Builder syntaxErrors(int v)   { syntaxErrorCount       = v; return this; }
        public Builder compileErrors(int v)  { compilationErrors      = v; return this; }
        public Builder timeSinceError(long v){ timeSinceLastErrorMs   = v; return this; }
        public Builder fileChanges(int v)    { fileChangesLast5Min    = v; return this; }
        public Builder timeInFile(long v)    { timeInCurrentFileMs    = v; return this; }
        public Builder focusLoss(int v)      { focusLossCount         = v; return this; }
        public Builder buildSuccess(boolean v){ lastBuildSuccess      = v; return this; }
        public Builder failedBuilds(int v)   { consecutiveFailedBuilds = v; return this; }
        public Builder timeSinceBuild(long v){ timeSinceLastBuildMs   = v; return this; }
        public Builder sessionDuration(long v){ sessionDurationMs     = v; return this; }

        public MockFlowMetrics build() { return new MockFlowMetrics(this); }
    }
}
