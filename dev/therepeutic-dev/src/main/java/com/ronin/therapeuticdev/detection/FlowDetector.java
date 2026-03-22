package com.ronin.therapeuticdev.detection;

import com.ronin.therapeuticdev.metrics.FlowMetrics;

/**
 * the core algorithm — this is the brain of the entire plugin.
 *
 * i take a FlowMetrics snapshot and produce a FlowDetectionResult by:
 *   1. normalising each of five behavioural dimensions to [0.0, 1.0]
 *   2. multiplying each by its configured weight
 *   3. summing to get a composite score
 *   4. mapping the composite to one of seven FlowState values
 *
 * the five categories and their default weights:
 *   TYPING    30% — kpm, correction ratio, burst consistency
 *   ERRORS    25% — intro/resolution trajectory, accumulation
 *   FOCUS     20% — file dwell time, context switches, ide focus
 *   BUILDS    15% — success rate, frequency, streak
 *   CONTEXT   10% — ide focus percentage, ai acceptance signal
 *
 * these weights are a hypothesis from iterative prototyping, not empirically
 * derived values. the architecture keeps them mutable so i can recalibrate
 * after the participant study. the synthetic test suite (SC-12) validates
 * that changing weights actually shifts the output, confirming configurability
 * works at the algorithmic level.
 *
 * this class is deliberately stateless — no mutable fields are modified during
 * detect(). given identical FlowMetrics input, the output is deterministic.
 * this makes it trivially testable and thread-safe without synchronisation.
 */
public class FlowDetector {

    // category weights — must sum to 1.0
    // not final because i need to mutate them in the synthetic test (SC-12)
    // and potentially post-study based on participant data
    private double weightTyping  = 0.30;
    private double weightErr     = 0.25;
    private double weightFocus   = 0.20;
    private double weightBuilds  = 0.15;
    private double weightContext = 0.10;

    // typing thresholds — the bell curve boundaries for kpm scoring
    // below kpmLow: barely typing, score near zero
    // kpmLow to kpmOptimal: ramping up, score climbs to 1.0
    // kpmOptimal to kpmHigh: still good but starting to rush
    // above kpmHigh: rushing or transcribing, score degrades
    private int kpmLow     = 20;
    private int kpmOptimal = 80;
    private int kpmHigh    = 150;

    // focus thresholds — how many file switches and focus losses before penalties kick in
    // i set these tight (2 file changes, 0 focus losses) because even small amounts
    // of context switching disrupt the concentration dimension in flow theory
    private int fileChangeTolerance = 2;
    private int focusLossTolerance  = 0;

    // error thresholds — how many errors before the penalty curve starts
    private int syntaxErrTolerance      = 1;
    private int compilationErrTolerance = 0;

    // build thresholds
    private int buildFailTolerance = 1;

    // state classification thresholds — these map the [0, 1] composite score
    // to the seven FlowState values. the gaps between thresholds define how
    // wide each state's "band" is on the score spectrum
    private double deepFlowThreshold       = 0.80;
    private double flowThreshold           = 0.65;
    private double emergingThreshold       = 0.52;
    private double neutralThreshold        = 0.40;
    private double disruptedThreshold      = 0.28;
    private double procrastinatingThreshold = 0.15;
    // anything below procrastinatingThreshold → NOT_IN_FLOW

    // ___________________ MAIN DETECT ______________________

    /**
     * the entire detection pipeline in one method call.
     * normalise all five categories, apply weights, sum, clamp, classify.
     */
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

    // _____________________ STATE MAPPING ___________________

    /**
     * cascading if-else from highest threshold to lowest.
     * the order matters — first match wins.
     */
    private FlowState mapToState(double score) {
        if (score >= deepFlowThreshold)        return FlowState.DEEP_FLOW;
        if (score >= flowThreshold)            return FlowState.FLOW;
        if (score >= emergingThreshold)        return FlowState.EMERGING;
        if (score >= neutralThreshold)         return FlowState.NEUTRAL;
        if (score >= disruptedThreshold)       return FlowState.DISRUPTED;
        if (score >= procrastinatingThreshold) return FlowState.PROCRASTINATING;
        return FlowState.NOT_IN_FLOW;
    }

    // _______________ TYPING SCORE _______________

    /**
     * combines three sub-dimensions:
     *   kpm bell curve     (50%) — penalises both too slow and too fast
     *   correction ratio   (30%) — backspaces / total keystrokes
     *   burst consistency  (20%) — coefficient of variation of inter-key intervals
     *
     * the bell curve is the key insight: a developer in flow types at a steady,
     * moderate pace (60–100 kpm). extremely fast typing often means transcribing
     * or pasting, not thinking. extremely slow means stuck or distracted.
     */
    private double normaliseTypScore(FlowMetrics m) {
        int    kpm        = m.getKeystrokesPerMin();
        int    backspaces = m.getBackspaceCount();
        double burst      = m.getBurstConsistency(); // already [0,1]

        // --- kpm bell curve ---
        // four zones: below low, low-to-optimal (ramp up), optimal-to-high (slight decay), above high (steep decay)
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

        // --- correction ratio ---
        // high backspace ratio suggests struggling with syntax or iterating heavily.
        // i cap the penalty at 0.3 so it doesn't single-handedly tank the typing score
        double totalKeys   = Math.max(kpm, 1.0);
        double bsRatio     = backspaces / totalKeys;
        double corrPenalty = Math.min(bsRatio * 2.0, 0.3);
        double corrScore   = Math.max(0.0, 1.0 - corrPenalty);

        // if there are zero keystrokes, correction and burst scores shouldn't contribute
        // because there's no typing activity to evaluate
        double activityFactor = Math.min(kpmScore / 0.3, 1.0);

        // weighted combination of the three sub-dimensions
        return (kpmScore * 0.50) + (corrScore * activityFactor * 0.30) + (burst * 0.20);
    }

    // ==================== ERROR SCORE ====================

    /**
     * rewards tight error cycles (introduced AND resolved quickly = tdd-style).
     * penalises accumulation (introduced but not resolved = stuck).
     *
     * i prefer the intro/resolution model over raw error counts because it captures
     * the *trajectory* of errors, not just the current count. a developer who introduces
     * 5 errors and resolves 5 is doing fine. one who introduces 5 and resolves 0 is stuck.
     *
     * falls back to raw count when no intro/resolution data is available yet
     * (e.g. first few seconds of a session before the error highlight listener fires).
     */
    private double normaliseErrorScore(FlowMetrics m) {
        int introduced = m.getErrorsIntroduced();
        int resolved   = m.getErrorsResolved();
        int syntaxErrs = m.getSyntaxErrorCount();
        int compErrs   = m.getCompilationErrors();

        double score = 1.0;

        if (introduced > 0 || resolved > 0) {
            // intro/resolution model — the primary error scoring path
            int netAccumulation = introduced - resolved;
            if (netAccumulation > 0) {
                // errors piling up — penalise proportionally, capped at -0.80
                score -= Math.min(netAccumulation * 0.24, 0.80);
            } else if (netAccumulation < 0 || (introduced > 0 && resolved >= introduced)) {
                // resolving more than introducing, or tight cycle — this is the recovery bonus
                // that the TaskFlow study is specifically designed to trigger
                score = Math.min(1.0, score + 0.10);
            }
        } else {
            // fallback: raw count penalties when intro/resolution data isn't available
            if (syntaxErrs > syntaxErrTolerance) {
                score -= Math.min((syntaxErrs - syntaxErrTolerance) * 0.10, 0.60);
            }
            if (compErrs > compilationErrTolerance) {
                score -= Math.min((compErrs - compilationErrTolerance) * 0.15, 0.70);
            }
        }

        // error-free streak bonus — 10+ minutes with no errors means things are going well
        if (m.getTimeSinceLastErrorMs() > 600_000L) {
            score = Math.min(1.0, score + 0.10);
        }

        return Math.max(0.0, score);
    }

    // _______________ FOCUS SCORE _______________

    /**
     * penalises file switching and ide focus loss beyond their tolerances.
     * rewards sustained dwell time in a single file (2+ minutes = bonus).
     *
     * this maps directly to csikszentmihalyi's "concentration on the task at hand"
     * dimension. the 20% weight reflects its theoretical importance while acknowledging
     * that focus duration alone is insufficient — a developer staring at one file for
     * 30 minutes without typing might be stuck, not in flow.
     */
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
        // sustained dwell bonus — staying in one file for 2+ minutes
        if (timeInFile > 120_000L) {
            score = Math.min(1.0, score + 0.05);
        }

        return Math.max(0.0, score);
    }

    // _______________ BUILD SCORE _______________

    /**
     * uses per-interval build success rate and count where available,
     * falls back to the cumulative lastBuildSuccess/consecutive-fail streak otherwise.
     *
     * the dual-path approach handles the common case where a developer hasn't triggered
     * a build in the current interval (no builds = use staleness penalty instead).
     */
    private double normaliseBuildScore(FlowMetrics m) {
        double score = 1.0;

        if (m.getBuildsInWindow() > 0) {
            // per-interval success rate — preferred path when builds happened recently
            double rate = m.getBuildSuccessRate();
            if (rate < 1.0) {
                score -= (1.0 - rate) * 0.6; // up to -0.6 for all-fail interval
            }
            // moderate frequency bonus: 1–3 builds/interval suggests a good red-green cadence
            int builds = m.getBuildsInWindow();
            if (builds >= 1 && builds <= 3) score = Math.min(1.0, score + 0.05);
        } else {
            // no builds this interval — staleness penalty scales with time since last build
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

        // 30+ minutes with no build at all — slight penalty regardless of path
        long sinceLastBuild = m.getTimeSinceLastBuildMs();
        if (sinceLastBuild > 1_800_000L) score -= 0.15;

        // recent success bonus
        if (m.isLastBuildSuccess() && sinceLastBuild < 300_000L) {
            score = Math.min(1.0, score + 0.05);
        }

        return Math.max(0.0, score);
    }

    // _______________ CONTEXT SCORE _______________

    /**
     * combines ide focus percentage and the ai acceptance heuristic.
     * high ide focus + non-zero ai acceptances = developer is engaged and using tools.
     *
     * the ai acceptance signal is intentionally soft (+0.05 bonus) because it's a
     * heuristic, not a precise measurement. AiSuggestionListener counts large document
     * insertions as "probably ai", which catches copilot completions but also large
     * pastes. the ESM probe asks participants to self-report actual tool usage, which
     * gives me a ground truth to compare against this heuristic in the analysis.
     */
    private double normaliseContextScore(FlowMetrics m) {
        double ideFocus = m.getIdeFocusPct(); // already [0,1]
        int    aiAcc    = m.getAiSuggestionsAccepted();
        long   session  = m.getSessionDurationMs() / 1000;

        // early session guard — not enough data for a meaningful score yet
        if (session < 60) return 0.5;

        double score = ideFocus; // base: how much of the interval was spent in the ide

        // small bonus when ai tools are being used productively
        if (aiAcc > 0) score = Math.min(1.0, score + 0.05);

        // idle penalty — keyboard dormant for over 2 minutes suggests distraction
        if (m.getKeyboardIdleMs() > 120_000L) {
            double idlePenalty = Math.min((m.getKeyboardIdleMs() - 120_000L) / 300_000.0, 0.5);
            score -= idlePenalty;
        }

        // sustained active session bonus (15+ min, not idle)
        if (session > 900 && m.getKeyboardIdleMs() < 60_000L) {
            score = Math.min(1.0, score + 0.10);
        }

        return Math.max(0.0, score);
    }

    // _______________ THRESHOLD SETTERS _______________

    // these exist for the synthetic test suite — SC-12 mutates weights to verify
    // the algorithm's configurability. in production, the defaults are used.

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
