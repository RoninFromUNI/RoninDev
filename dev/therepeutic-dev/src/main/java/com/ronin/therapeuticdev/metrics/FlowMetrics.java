package com.ronin.therapeuticdev.metrics;

import java.time.Instant;

/**
 * Immutable snapshot of flow-related metrics at a specific point in time.
 * 
 * Captures 20 variables required for flow state detection algorithm.
 * Uses Builder pattern for clean construction.
 * 
 * Reference: Effective Java (Bloch, 2018), Item 2:
 * "Consider a builder when faced with many constructor parameters"
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

    // ==================== ERROR TRACKING ====================
    private final int syntaxErrorCount;
    private final int compilationErrors;
    private final long timeSinceLastErrorMs;

    // ==================== CONTEXT SWITCHING ====================
    private final int fileChangesLast5Min;
    private final long timeInCurrentFileMs;
    private final int focusLossCount;

    // ==================== BUILD RESULTS ====================
    private final boolean lastBuildSuccess;
    private final int consecutiveFailedBuilds;
    private final long timeSinceLastBuildMs;

    // ==================== CALCULATED SCORES ====================
    private final double flowTally;
    private final double stressLevel;
    private final String notes;

    /**
     * Private constructor - use Builder to instantiate.
     */
    private FlowMetrics(Builder builder) {
        this.timestamp = builder.timestamp;
        this.sessionId = builder.sessionId;
        this.sessionDurationMs = builder.sessionDurationMs;

        this.keystrokesPerMin = builder.keystrokesPerMin;
        this.avgKeyIntervalMs = builder.avgKeyIntervalMs;
        this.backspaceCount = builder.backspaceCount;
        this.keyboardIdleMs = builder.keyboardIdleMs;

        this.syntaxErrorCount = builder.syntaxErrorCount;
        this.compilationErrors = builder.compilationErrors;
        this.timeSinceLastErrorMs = builder.timeSinceLastErrorMs;

        this.fileChangesLast5Min = builder.fileChangesLast5Min;
        this.timeInCurrentFileMs = builder.timeInCurrentFileMs;
        this.focusLossCount = builder.focusLossCount;

        this.lastBuildSuccess = builder.lastBuildSuccess;
        this.consecutiveFailedBuilds = builder.consecutiveFailedBuilds;
        this.timeSinceLastBuildMs = builder.timeSinceLastBuildMs;

        this.flowTally = builder.flowTally;
        this.stressLevel = builder.stressLevel;
        this.notes = builder.notes;
    }

    // ==================== GETTERS ====================
    // Named to match what FlowDetector and other classes expect

    public Instant getTimestamp() { return timestamp; }
    public String getSessionId() { return sessionId; }
    public long getSessionDurationMs() { return sessionDurationMs; }

    // Typing getters
    public int getKeystrokesPerMin() { return keystrokesPerMin; }
    public double getAvgKeyIntervalMs() { return avgKeyIntervalMs; }
    public int getBackspaceCount() { return backspaceCount; }
    public long getKeyboardIdleMs() { return keyboardIdleMs; }

    // Error getters
    public int getSyntaxErrorCount() { return syntaxErrorCount; }
    public int getCompilationErrors() { return compilationErrors; }
    public long getTimeSinceLastErrorMs() { return timeSinceLastErrorMs; }

    // Focus getters
    public int getFileChangesLast5Min() { return fileChangesLast5Min; }
    public long getTimeInCurrentFileMs() { return timeInCurrentFileMs; }
    public int getFocusLossCount() { return focusLossCount; }

    // Build getters
    public boolean isLastBuildSuccess() { return lastBuildSuccess; }
    public int getConsecutiveFailedBuilds() { return consecutiveFailedBuilds; }
    public long getTimeSinceLastBuildMs() { return timeSinceLastBuildMs; }

    // Calculated getters
    public double getFlowTally() { return flowTally; }
    public double getStressLevel() { return stressLevel; }
    public String getNotes() { return notes; }

    // ==================== BUILDER ====================

    public static class Builder {
        // Defaults
        private Instant timestamp = Instant.now();
        private String sessionId = java.util.UUID.randomUUID().toString();
        private long sessionDurationMs = 0;

        private int keystrokesPerMin = 0;
        private double avgKeyIntervalMs = 0;
        private int backspaceCount = 0;
        private long keyboardIdleMs = 0;

        private int syntaxErrorCount = 0;
        private int compilationErrors = 0;
        private long timeSinceLastErrorMs = 0;

        private int fileChangesLast5Min = 0;
        private long timeInCurrentFileMs = 0;
        private int focusLossCount = 0;

        private boolean lastBuildSuccess = true;
        private int consecutiveFailedBuilds = 0;
        private long timeSinceLastBuildMs = 0;

        private double flowTally = 0;
        private double stressLevel = 0;
        private String notes = "";

        // Metadata setters
        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder sessionDuration(long durationMs) {
            this.sessionDurationMs = durationMs;
            return this;
        }

        // Typing setters
        public Builder keystrokesPerMinute(double kpm) {
            this.keystrokesPerMin = (int) kpm;
            return this;
        }

        public Builder avgKeyIntervalMs(double interval) {
            this.avgKeyIntervalMs = interval;
            return this;
        }

        public Builder backspaceCount(int count) {
            this.backspaceCount = count;
            return this;
        }

        public Builder keyboardIdleMs(long idleMs) {
            this.keyboardIdleMs = idleMs;
            return this;
        }

        // Error setters
        public Builder syntaxErrorCount(int count) {
            this.syntaxErrorCount = count;
            return this;
        }

        public Builder compilationErrors(int count) {
            this.compilationErrors = count;
            return this;
        }

        public Builder timeSinceLastErrorMs(long ms) {
            this.timeSinceLastErrorMs = ms;
            return this;
        }

        // Focus setters
        public Builder fileChangesLast5Min(int count) {
            this.fileChangesLast5Min = count;
            return this;
        }

        public Builder timeInCurrentFileMs(long ms) {
            this.timeInCurrentFileMs = ms;
            return this;
        }

        public Builder focusLossCount(int count) {
            this.focusLossCount = count;
            return this;
        }

        // Build setters
        public Builder lastBuildSuccess(boolean success) {
            this.lastBuildSuccess = success;
            return this;
        }

        public Builder consecutiveFailedBuilds(int count) {
            this.consecutiveFailedBuilds = count;
            return this;
        }

        public Builder timeSinceLastBuildMs(long ms) {
            this.timeSinceLastBuildMs = ms;
            return this;
        }

        // Calculated setters
        public Builder flowTally(double tally) {
            this.flowTally = tally;
            return this;
        }

        public Builder stressLevel(double stress) {
            this.stressLevel = stress;
            return this;
        }

        public Builder notes(String notes) {
            this.notes = notes;
            return this;
        }

        /**
         * Builds the immutable FlowMetrics instance.
         */
        public FlowMetrics build() {
            return new FlowMetrics(this);
        }
    }

    @Override
    public String toString() {
        return String.format(
                "FlowMetrics{session=%s, kpm=%d, errors=%d, fileChanges=%d, flowTally=%.2f}",
                sessionId.substring(0, 8), keystrokesPerMin, syntaxErrorCount, fileChangesLast5Min, flowTally
        );
    }
}
