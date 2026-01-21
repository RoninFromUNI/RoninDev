package com.ronin.therapeuticdev.detection;

import com.ronin.therapeuticdev.metrics.FlowMetrics;

/**
 * Core detection algorithm for flow state classification.
 * Applies weighted scoring across five metric categories:
 * 
 * - TYPING: 30% - Keystroke patterns and rhythm
 * - ERRORS: 25% - Syntax and compilation error rates
 * - FOCUS: 20% - File switching and IDE focus
 * - BUILDS: 15% - Build success/failure patterns
 * - ACTIVITY: 10% - Session duration and idle time
 */
public class FlowDetector {

    // Category weights (must sum to 1.0)
    private double weightTyping = 0.30;
    private double weightErr = 0.25;
    private double weightFocus = 0.20;
    private double weightBuilds = 0.15;
    private double weightActivity = 0.10;

    // Typing thresholds
    private int kpmLow = 20;
    private int kpmOptimal = 80;
    private int kpmHigh = 150; // rushing

    // Focus thresholds
    private int fileChangeTolerance = 5; // per 10 min window
    private int focusLossTolerance = 3;

    // Error thresholds
    private int syntaxErrTolerance = 3;
    private int compilationErrTolerance = 2;

    // Build thresholds
    private int buildFailTolerance = 2;

    // State classification thresholds
    private double flowThreshold = 0.65;       // >= this = FLOW
    private double procrastinateThreshold = 0.35; // <= this = PROCRASTINATING

    /**
     * Performs flow detection on a metrics snapshot.
     *
     * @param metrics current FlowMetrics snapshot
     * @return FlowDetectionResult with scores and state
     */
    public FlowDetectionResult detect(FlowMetrics metrics) {
        // Calculate individual category scores (0.0 - 1.0)
        double typingScore = normaliseTypScore(metrics);
        double errScore = normaliseErrorScore(metrics);
        double focusScore = normaliseFocusScore(metrics);
        double buildScore = normaliseBuildScore(metrics);
        double activityScore = normaliseActivityScore(metrics);

        // Apply weights and sum
        double flowTally = (typingScore * weightTyping)
                + (errScore * weightErr)
                + (focusScore * weightFocus)
                + (buildScore * weightBuilds)
                + (activityScore * weightActivity);

        // Classify state based on tally
        FlowState state;
        if (flowTally >= flowThreshold) {
            state = FlowState.FLOW;
        } else if (flowTally <= procrastinateThreshold) {
            state = FlowState.PROCRASTINATING;
        } else {
            state = FlowState.NEUTRAL;
        }

        return new FlowDetectionResult(
                typingScore, errScore, focusScore, buildScore, activityScore, flowTally, state
        );
    }

    /**
     * Normalises typing metrics to 0.0-1.0 score.
     * Considers KPM and backspace ratio.
     */
    private double normaliseTypScore(FlowMetrics metrics) {
        int kpm = metrics.getKeystrokesPerMin();
        int bckSpaces = metrics.getBackspaceCount();

        double kpmScore;

        // Below minimum threshold (poor score -> idle or stuck)
        if (kpm < kpmLow) {
            kpmScore = (kpm / (double) kpmLow) * 0.3;
        }
        // Optimal range (full score)
        else if (kpm >= kpmLow && kpm <= kpmOptimal) {
            double range = kpmOptimal - kpmLow;
            double pos = kpm - kpmLow;
            kpmScore = 0.3 + (pos / range) * 0.7;
        }
        // Above optimal but below high (slight degradation)
        else if (kpm > kpmOptimal && kpm <= kpmHigh) {
            double range = kpmHigh - kpmOptimal;
            double pos = kpm - kpmOptimal;
            kpmScore = 1.0 - (pos / range) * 0.4;
        }
        // Above high threshold (rushing)
        else {
            double excess = kpm - kpmHigh;
            double penalty = excess / 100.0;
            kpmScore = Math.max(0.3, 0.6 - penalty);
        }

        // Backspace penalty
        double totalKeys = (kpm > 0) ? kpm : 1;
        double backspcRatio = bckSpaces / totalKeys;
        double backspcPenalty = Math.min(backspcRatio * 2, 0.3);

        return Math.max(0.0, kpmScore - backspcPenalty);
    }

    /**
     * Normalises error metrics to 0.0-1.0 score.
     */
    private double normaliseErrorScore(FlowMetrics metrics) {
        int syntaxErrs = metrics.getSyntaxErrorCount();
        int compilationErrs = metrics.getCompilationErrors();

        double score = 1.0;

        // Syntax error penalty
        if (syntaxErrs > syntaxErrTolerance) {
            int excess = syntaxErrs - syntaxErrTolerance;
            double syntaxPenalty = Math.min(excess * 0.1, 0.4);
            score -= syntaxPenalty;
        }

        // Compilation error penalty
        if (compilationErrs > compilationErrTolerance) {
            int excess = compilationErrs - compilationErrTolerance;
            double compilationPenalty = Math.min(excess * 0.15, 0.5);
            score -= compilationPenalty;
        }

        // Bonus for error-free streak (10+ minutes)
        long timeSinceError = metrics.getTimeSinceLastErrorMs();
        if (timeSinceError > 600000) {
            score = Math.min(1.0, score + 0.1);
        }

        return Math.max(0.0, score);
    }

    /**
     * Normalises focus metrics to 0.0-1.0 score.
     */
    private double normaliseFocusScore(FlowMetrics metrics) {
        int fileChanges = metrics.getFileChangesLast5Min();
        long timeInFile = metrics.getTimeInCurrentFileMs();
        int focusLosses = metrics.getFocusLossCount();

        double score = 1.0;

        // File switching penalty
        if (fileChanges > fileChangeTolerance) {
            int excess = fileChanges - fileChangeTolerance;
            double focusPenalty = Math.min(excess * 0.1, 0.3);
            score -= focusPenalty;
        }

        // Focus loss penalty
        if (focusLosses > focusLossTolerance) {
            int excess = focusLosses - focusLossTolerance;
            double focusPenalty = Math.min(excess * 0.1, 0.3);
            score -= focusPenalty;
        }

        // Bonus for sustained focus (2+ minutes in one file)
        if (timeInFile > 120000) {
            score = Math.min(1.0, score + 0.15);
        }

        return Math.max(0.0, score);
    }

    /**
     * Normalises build metrics to 0.0-1.0 score.
     */
    private double normaliseBuildScore(FlowMetrics metrics) {
        boolean lastBuildSuccess = metrics.isLastBuildSuccess();
        int consecutiveFails = metrics.getConsecutiveFailedBuilds();
        long timeSinceBuild = metrics.getTimeSinceLastBuildMs();

        double score = 1.0;

        // Last build failed penalty
        if (!lastBuildSuccess) {
            score -= 0.3;
        }

        // Consecutive failure penalty
        if (consecutiveFails > buildFailTolerance) {
            int excess = consecutiveFails - buildFailTolerance;
            double failPenalty = Math.min(excess * 0.15, 0.4);
            score -= failPenalty;
        }

        // Long time since build penalty
        if (timeSinceBuild > 1800000) { // 30+ minutes
            score -= 0.1;
        }

        // Recent successful build bonus
        if (lastBuildSuccess && timeSinceBuild < 300000) { // within 5 min
            score = Math.min(1.0, score + 0.1);
        }

        return Math.max(0.0, score);
    }

    /**
     * Normalises activity metrics to 0.0-1.0 score.
     */
    private double normaliseActivityScore(FlowMetrics metrics) {
        long sessionDuration = metrics.getSessionDurationMs() / 1000; // convert to seconds
        long keyboardIdle = metrics.getKeyboardIdleMs();

        double score = 1.0;

        // Just started - return neutral
        if (sessionDuration < 60) {
            return 0.5;
        }

        // Idle penalty (2+ minutes)
        if (keyboardIdle > 120000) {
            double idlePenalty = Math.min((keyboardIdle - 120000) / 300000.0, 0.5);
            score -= idlePenalty;
        }

        // Sustained session bonus (15+ minutes active)
        if (sessionDuration > 900 && keyboardIdle < 60000) {
            score = Math.min(1.0, score + 0.1);
        }

        return Math.max(0.0, score);
    }

    // Setters for configurable thresholds (used by settings)

    public void setFlowThreshold(double threshold) {
        this.flowThreshold = threshold;
    }

    public void setProcrastinateThreshold(double threshold) {
        this.procrastinateThreshold = threshold;
    }

    public void setWeights(double typing, double errors, double focus, double builds, double activity) {
        this.weightTyping = typing;
        this.weightErr = errors;
        this.weightFocus = focus;
        this.weightBuilds = builds;
        this.weightActivity = activity;
    }
}
