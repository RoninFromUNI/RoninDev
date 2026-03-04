package com.ronin.therapeuticdev.metrics;

import java.time.Instant;

/**
 * Immutable snapshot of flow-related metrics at a specific point in time.
 *
 * Uses Builder pattern for clean construction across 30+ fields.
 */
public class FlowMetrics {

    // ==================== METADATA ====================
    private final Instant timestamp;
    private final String sessionId;
    private final long sessionDurationMs;

    // ==================== TYPING ACTIVITY ====================
    private final int keystrokesPerMin;
    private final double avgKeyIntervalMs;
    private final int backspaceCount;
    private final long keyboardIdleMs;
    /** Coefficient of variation of inter-keystroke intervals, normalised to [0,1].
     *  1.0 = perfectly rhythmic, 0.0 = completely erratic. */
    private final double burstConsistency;

    // ==================== ERROR TRACKING ====================
    private final int syntaxErrorCount;
    private final int compilationErrors;
    private final long timeSinceLastErrorMs;
    /** New errors that appeared since the last interval reset. */
    private final int errorsIntroduced;
    /** Errors that were cleared since the last interval reset. */
    private final int errorsResolved;

    // ==================== CONTEXT SWITCHING ====================
    private final int fileChangesLast5Min;
    private final long timeInCurrentFileMs;
    private final int focusLossCount;

    // ==================== BUILD RESULTS ====================
    private final boolean lastBuildSuccess;
    private final int consecutiveFailedBuilds;
    private final long timeSinceLastBuildMs;
    /** Number of builds triggered in this interval. */
    private final int buildsInWindow;
    /** Proportion of builds in this interval that succeeded (0.0–1.0). */
    private final double buildSuccessRate;

    // ==================== CONTEXTUAL / AI ====================
    /** Proportion of this interval that IntelliJ was the active application (0.0–1.0). */
    private final double ideFocusPct;
    /** Inline AI completions that appeared (heuristic: large single insertions). */
    private final int aiSuggestionsAccepted;

    // ==================== CALCULATED SCORES ====================
    private final double flowTally;
    private final double stressLevel;
    private final String notes;

    private FlowMetrics(Builder b) {
        this.timestamp           = b.timestamp;
        this.sessionId           = b.sessionId;
        this.sessionDurationMs   = b.sessionDurationMs;

        this.keystrokesPerMin    = b.keystrokesPerMin;
        this.avgKeyIntervalMs    = b.avgKeyIntervalMs;
        this.backspaceCount      = b.backspaceCount;
        this.keyboardIdleMs      = b.keyboardIdleMs;
        this.burstConsistency    = b.burstConsistency;

        this.syntaxErrorCount    = b.syntaxErrorCount;
        this.compilationErrors   = b.compilationErrors;
        this.timeSinceLastErrorMs = b.timeSinceLastErrorMs;
        this.errorsIntroduced    = b.errorsIntroduced;
        this.errorsResolved      = b.errorsResolved;

        this.fileChangesLast5Min = b.fileChangesLast5Min;
        this.timeInCurrentFileMs = b.timeInCurrentFileMs;
        this.focusLossCount      = b.focusLossCount;

        this.lastBuildSuccess       = b.lastBuildSuccess;
        this.consecutiveFailedBuilds = b.consecutiveFailedBuilds;
        this.timeSinceLastBuildMs   = b.timeSinceLastBuildMs;
        this.buildsInWindow         = b.buildsInWindow;
        this.buildSuccessRate       = b.buildSuccessRate;

        this.ideFocusPct         = b.ideFocusPct;
        this.aiSuggestionsAccepted = b.aiSuggestionsAccepted;

        this.flowTally    = b.flowTally;
        this.stressLevel  = b.stressLevel;
        this.notes        = b.notes;
    }

    // ==================== GETTERS ====================

    public Instant getTimestamp()        { return timestamp; }
    public String  getSessionId()        { return sessionId; }
    public long    getSessionDurationMs(){ return sessionDurationMs; }

    public int    getKeystrokesPerMin()  { return keystrokesPerMin; }
    public double getAvgKeyIntervalMs()  { return avgKeyIntervalMs; }
    public int    getBackspaceCount()    { return backspaceCount; }
    public long   getKeyboardIdleMs()    { return keyboardIdleMs; }
    public double getBurstConsistency()  { return burstConsistency; }

    public int  getSyntaxErrorCount()     { return syntaxErrorCount; }
    public int  getCompilationErrors()    { return compilationErrors; }
    public long getTimeSinceLastErrorMs() { return timeSinceLastErrorMs; }
    public int  getErrorsIntroduced()     { return errorsIntroduced; }
    public int  getErrorsResolved()       { return errorsResolved; }

    public int  getFileChangesLast5Min()  { return fileChangesLast5Min; }
    public long getTimeInCurrentFileMs()  { return timeInCurrentFileMs; }
    public int  getFocusLossCount()       { return focusLossCount; }

    public boolean isLastBuildSuccess()       { return lastBuildSuccess; }
    public int     getConsecutiveFailedBuilds(){ return consecutiveFailedBuilds; }
    public long    getTimeSinceLastBuildMs()   { return timeSinceLastBuildMs; }
    public int     getBuildsInWindow()         { return buildsInWindow; }
    public double  getBuildSuccessRate()       { return buildSuccessRate; }

    public double getIdeFocusPct()          { return ideFocusPct; }
    public int    getAiSuggestionsAccepted(){ return aiSuggestionsAccepted; }

    public double getFlowTally()   { return flowTally; }
    public double getStressLevel() { return stressLevel; }
    public String getNotes()       { return notes; }

    // ==================== BUILDER ====================

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Instant timestamp      = Instant.now();
        private String  sessionId      = java.util.UUID.randomUUID().toString();
        private long    sessionDurationMs = 0;

        private int    keystrokesPerMin = 0;
        private double avgKeyIntervalMs = 0;
        private int    backspaceCount   = 0;
        private long   keyboardIdleMs   = 0;
        private double burstConsistency = 1.0; // default: perfectly rhythmic

        private int  syntaxErrorCount    = 0;
        private int  compilationErrors   = 0;
        private long timeSinceLastErrorMs = Long.MAX_VALUE;
        private int  errorsIntroduced    = 0;
        private int  errorsResolved      = 0;

        private int  fileChangesLast5Min = 0;
        private long timeInCurrentFileMs = 0;
        private int  focusLossCount      = 0;

        private boolean lastBuildSuccess       = true;
        private int     consecutiveFailedBuilds = 0;
        private long    timeSinceLastBuildMs    = Long.MAX_VALUE;
        private int     buildsInWindow          = 0;
        private double  buildSuccessRate        = 1.0;

        private double ideFocusPct          = 1.0;
        private int    aiSuggestionsAccepted = 0;

        private double flowTally   = 0;
        private double stressLevel = 0;
        private String notes       = "";

        public Builder timestamp(Instant t)          { this.timestamp = t; return this; }
        public Builder sessionId(String s)           { this.sessionId = s; return this; }
        public Builder sessionDuration(long ms)      { this.sessionDurationMs = ms; return this; }

        public Builder keystrokesPerMinute(double kpm){ this.keystrokesPerMin = (int) kpm; return this; }
        public Builder avgKeyIntervalMs(double i)    { this.avgKeyIntervalMs = i; return this; }
        public Builder backspaceCount(int c)         { this.backspaceCount = c; return this; }
        public Builder keyboardIdleMs(long ms)       { this.keyboardIdleMs = ms; return this; }
        public Builder burstConsistency(double c)    { this.burstConsistency = c; return this; }

        public Builder syntaxErrorCount(int c)       { this.syntaxErrorCount = c; return this; }
        public Builder compilationErrors(int c)      { this.compilationErrors = c; return this; }
        public Builder timeSinceLastErrorMs(long ms) { this.timeSinceLastErrorMs = ms; return this; }
        public Builder errorsIntroduced(int c)       { this.errorsIntroduced = c; return this; }
        public Builder errorsResolved(int c)         { this.errorsResolved = c; return this; }

        public Builder fileChangesLast5Min(int c)    { this.fileChangesLast5Min = c; return this; }
        public Builder timeInCurrentFileMs(long ms)  { this.timeInCurrentFileMs = ms; return this; }
        public Builder focusLossCount(int c)         { this.focusLossCount = c; return this; }

        public Builder lastBuildSuccess(boolean s)       { this.lastBuildSuccess = s; return this; }
        public Builder consecutiveFailedBuilds(int c)    { this.consecutiveFailedBuilds = c; return this; }
        public Builder timeSinceLastBuildMs(long ms)     { this.timeSinceLastBuildMs = ms; return this; }
        public Builder buildsInWindow(int c)             { this.buildsInWindow = c; return this; }
        public Builder buildSuccessRate(double r)        { this.buildSuccessRate = r; return this; }

        public Builder ideFocusPct(double p)             { this.ideFocusPct = p; return this; }
        public Builder aiSuggestionsAccepted(int c)      { this.aiSuggestionsAccepted = c; return this; }

        public Builder flowTally(double t)    { this.flowTally = t; return this; }
        public Builder stressLevel(double s)  { this.stressLevel = s; return this; }
        public Builder notes(String n)        { this.notes = n; return this; }

        public FlowMetrics build() { return new FlowMetrics(this); }
    }

    @Override
    public String toString() {
        return String.format(
            "FlowMetrics{session=%s, kpm=%d, burst=%.2f, errors=%d (+%d/-%d), builds=%d, ideFocus=%.0f%%, flowTally=%.2f}",
            sessionId.substring(0, 8), keystrokesPerMin, burstConsistency,
            syntaxErrorCount, errorsIntroduced, errorsResolved,
            buildsInWindow, ideFocusPct * 100, flowTally
        );
    }
}
