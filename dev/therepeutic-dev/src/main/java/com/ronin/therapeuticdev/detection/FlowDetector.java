package com.ronin.therapeuticdev.detection;

import com.ronin.therapeuticdev.metrics.FlowMetrics;

/**
 * Core detection algorithm for flow state classification.
 *
 * Applies weighted scoring across five metric categories:
 *   TYPING    30% — KPM, correction ratio, burst consistency
 *   ERRORS    25% — intro/resolution trajectory, accumulation
 *   FOCUS     20% — file dwell time, context switches, IDE focus
 *   BUILDS    15% — success rate, frequency, streak
 *   CONTEXT   10% — IDE focus %, AI acceptance signal
 *
 * The composite score [0.0, 1.0] maps to one of seven FlowState values.
 */
public class FlowDetector {

    // Category weights (must sum to 1.0)
    private double weightTyping  = 0.30;
    private double weightErr     = 0.25;
    private double weightFocus   = 0.20;
    private double weightBuilds  = 0.15;
    private double weightContext = 0.10;

    // Typing thresholds
    private int kpmLow     = 20;
    private int kpmOptimal = 80;
    private int kpmHigh    = 150;

    // Focus thresholds
    private int fileChangeTolerance = 2;
    private int focusLossTolerance  = 0;

    // Error thresholds
    private int syntaxErrTolerance      = 1;
    private int compilationErrTolerance = 0;

    // Build thresholds
    private int buildFailTolerance = 1;

    // State classification thresholds
    private double deepFlowThreshold       = 0.80;
    private double flowThreshold           = 0.65;
    private double emergingThreshold       = 0.52;
    private double neutralThreshold        = 0.40;
    private double disruptedThreshold      = 0.28;
    private double procrastinatingThreshold = 0.15;
    // below procrastinatingThreshold → NOT_IN_FLOW

    // ==================== MAIN DETECT ====================

    public FlowDetectionResult detect(FlowMetrics m) {
        double typing   = normaliseTypScore(m);
        double errors   = normaliseErrorScore(m);
        double focus    = normaliseFocusScore(m);
        double build    = normaliseBuildScore(m);
        double context  = normaliseContextScore(m);

        double composite = (typing  * weightTyping)
                         + (errors  * weightErr)
                         + (focus   * weightFocus)
                         + (build   * weightBuilds)
                         + (context * weightContext);
        composite = Math.max(0.0, Math.min(1.0, composite));

        FlowState state = mapToState(composite);

        return new FlowDetectionResult(typing, errors, focus, build, context, composite, state);
    }

    // ==================== STATE MAPPING ====================

    private FlowState mapToState(double score) {
        if (score >= deepFlowThreshold)        return FlowState.DEEP_FLOW;
        if (score >= flowThreshold)            return FlowState.FLOW;
        if (score >= emergingThreshold)        return FlowState.EMERGING;
        if (score >= neutralThreshold)         return FlowState.NEUTRAL;
        if (score >= disruptedThreshold)       return FlowState.DISRUPTED;
        if (score >= procrastinatingThreshold) return FlowState.PROCRASTINATING;
        return FlowState.NOT_IN_FLOW;
    }

    // ==================== TYPING SCORE ====================

    /**
     * Combines KPM bell-curve, correction ratio, and burst consistency.
     * KPM 50%  ·  correction ratio 30%  ·  burst consistency 20%
     */
    private double normaliseTypScore(FlowMetrics m) {
        int    kpm        = m.getKeystrokesPerMin();
        int    backspaces = m.getBackspaceCount();
        double burst      = m.getBurstConsistency(); // already [0,1]

        // --- KPM bell curve ---
        double kpmScore;
        if (kpm < kpmLow) {
            kpmScore = (kpm / (double) kpmLow) * 0.3;
        } else if (kpm <= kpmOptimal) {
            double range = kpmOptimal - kpmLow;
            double pos   = kpm - kpmLow;
            kpmScore = 0.3 + (pos / range) * 0.7;
        } else if (kpm <= kpmHigh) {
            double range = kpmHigh - kpmOptimal;
            double pos   = kpm - kpmOptimal;
            kpmScore = 1.0 - (pos / range) * 0.4;
        } else {
            double excess = kpm - kpmHigh;
            kpmScore = Math.max(0.3, 0.6 - excess / 100.0);
        }

        // --- Correction ratio (backspaces / total keys) ---
        double totalKeys   = Math.max(kpm, 1.0);
        double bsRatio     = backspaces / totalKeys;
        double corrPenalty = Math.min(bsRatio * 2.0, 0.3);
        double corrScore   = Math.max(0.0, 1.0 - corrPenalty);

        // Scale correction and burst by typing activity — no keystrokes = no credit
        double activityFactor = Math.min(kpmScore / 0.3, 1.0);

        // Weighted combination
        return (kpmScore * 0.50) + (corrScore * activityFactor * 0.30) + (burst * 0.20);
    }

    // ==================== ERROR SCORE ====================

    /**
     * Rewards tight error cycles (introduced AND resolved = TDD-style).
     * Penalises accumulation (introduced but not resolved).
     * Falls back to raw count when no intro/resolution data is available yet.
     */
    private double normaliseErrorScore(FlowMetrics m) {
        int introduced = m.getErrorsIntroduced();
        int resolved   = m.getErrorsResolved();
        int syntaxErrs = m.getSyntaxErrorCount();
        int compErrs   = m.getCompilationErrors();

        double score = 1.0;

        if (introduced > 0 || resolved > 0) {
            // Intro/resolution model
            int netAccumulation = introduced - resolved;
            if (netAccumulation > 0) {
                // Errors accumulating — penalise
                score -= Math.min(netAccumulation * 0.24, 0.80);
            } else if (netAccumulation < 0 || (introduced > 0 && resolved >= introduced)) {
                // Resolving more than introducing (or tight cycle) — bonus
                score = Math.min(1.0, score + 0.10);
            }
        } else {
            // Fallback: raw count penalties
            if (syntaxErrs > syntaxErrTolerance) {
                score -= Math.min((syntaxErrs - syntaxErrTolerance) * 0.10, 0.60);
            }
            if (compErrs > compilationErrTolerance) {
                score -= Math.min((compErrs - compilationErrTolerance) * 0.15, 0.70);
            }
        }

        // Error-free streak bonus (10+ minutes clean)
        if (m.getTimeSinceLastErrorMs() > 600_000L) {
            score = Math.min(1.0, score + 0.10);
        }

        return Math.max(0.0, score);
    }

    // ==================== FOCUS SCORE ====================

    private double normaliseFocusScore(FlowMetrics m) {
        int  fileChanges = m.getFileChangesLast5Min();
        long timeInFile  = m.getTimeInCurrentFileMs();
        int  focusLosses = m.getFocusLossCount();

        double score = 1.0;

        if (fileChanges > fileChangeTolerance) {
            score -= Math.min((fileChanges - fileChangeTolerance) * 0.19, 0.40);
        }
        if (focusLosses > focusLossTolerance) {
            score -= Math.min((focusLosses - focusLossTolerance) * 0.19, 0.40);
        }
        // Sustained dwell bonus (2+ min in same file)
        if (timeInFile > 120_000L) {
            score = Math.min(1.0, score + 0.05);
        }

        return Math.max(0.0, score);
    }

    // ==================== BUILD SCORE ====================

    /**
     * Uses per-interval build success rate and count where available,
     * falls back to lastBuildSuccess/consecutive-fail streak otherwise.
     */
    private double normaliseBuildScore(FlowMetrics m) {
        double score = 1.0;

        if (m.getBuildsInWindow() > 0) {
            // Prefer per-interval success rate
            double rate = m.getBuildSuccessRate();
            if (rate < 1.0) {
                score -= (1.0 - rate) * 0.6; // up to -0.6 for all-fail interval
            }
            // Moderate frequency bonus (1–3 builds/interval = good cadence)
            int builds = m.getBuildsInWindow();
            if (builds >= 1 && builds <= 3) score = Math.min(1.0, score + 0.05);
        } else {
            // No builds this interval — staleness penalty scales with time
            long msSinceBuild = m.getTimeSinceLastBuildMs();
            if (msSinceBuild > 300_000L) {
                double staleness = Math.min((msSinceBuild - 300_000L) / 1_200_000.0, 0.30);
                score -= staleness;
            }
            if (!m.isLastBuildSuccess()) score -= 0.35;
            int streak = m.getConsecutiveFailedBuilds();
            if (streak > buildFailTolerance) {
                score -= Math.min((streak - buildFailTolerance) * 0.18, 0.55);
            }
        }

        // Long time since any build
        long sinceLastBuild = m.getTimeSinceLastBuildMs();
        if (sinceLastBuild > 1_800_000L) score -= 0.15; // 30+ min, no build

        // Recent success bonus
        if (m.isLastBuildSuccess() && sinceLastBuild < 300_000L) {
            score = Math.min(1.0, score + 0.05);
        }

        return Math.max(0.0, score);
    }

    // ==================== CONTEXT SCORE ====================

    /**
     * Combines IDE focus percentage and the AI acceptance heuristic.
     * High IDE focus + non-zero AI acceptances → developer is engaged and using tools.
     */
    private double normaliseContextScore(FlowMetrics m) {
        double ideFocus = m.getIdeFocusPct(); // already [0,1]
        int    aiAcc    = m.getAiSuggestionsAccepted();
        long   session  = m.getSessionDurationMs() / 1000;

        // Early session: not enough data — return neutral
        if (session < 60) return 0.5;

        double score = ideFocus; // base: how focused in the IDE

        // Small bonus when AI tools are being used productively
        // (accepting completions = engaged with code generation)
        if (aiAcc > 0) score = Math.min(1.0, score + 0.05);

        // Idle penalty (keyboard dormant > 2 min)
        if (m.getKeyboardIdleMs() > 120_000L) {
            double idlePenalty = Math.min((m.getKeyboardIdleMs() - 120_000L) / 300_000.0, 0.5);
            score -= idlePenalty;
        }

        // Sustained active session bonus (15+ min, not idle)
        if (session > 900 && m.getKeyboardIdleMs() < 60_000L) {
            score = Math.min(1.0, score + 0.10);
        }

        return Math.max(0.0, score);
    }

    // ==================== THRESHOLD SETTERS ====================

    public void setFlowThreshold(double t)           { this.flowThreshold = t; }
    public void setProcrastinateThreshold(double t)  { this.procrastinatingThreshold = t; }

    public void setWeights(double typing, double errors, double focus, double builds, double context) {
        this.weightTyping  = typing;
        this.weightErr     = errors;
        this.weightFocus   = focus;
        this.weightBuilds  = builds;
        this.weightContext = context;
    }
}
