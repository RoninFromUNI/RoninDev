package therapeutic;

/**
 * Replicates the FlowDetector algorithm from therapeutic-dev.
 *
 * Weights (must sum to 1.0):
 *   Typing   → 30%   KPM curve, backspace ratio, idle time
 *   Errors   → 25%   syntax + compile error counts, error-free streak
 *   Focus    → 20%   file switching, time in file, IDE focus loss
 *   Builds   → 15%   last build result, consecutive failures, build age
 *   Activity → 10%   session duration, keyboard idle
 *
 * Flow state thresholds (from TherapeuticDevSettings defaults):
 *   tally >= 0.65  → FLOW
 *   tally <= 0.35  → PROCRASTINATING
 *   otherwise      → NEUTRAL
 *
 * Plugin source: com.ronin.therapeuticdev.detection.FlowDetector
 */
public class FlowSimulator {

    // ── Flow state thresholds ─────────────────────────────────────────────────
    public static final double FLOW_THRESHOLD             = 0.65;
    public static final double PROCRASTINATING_THRESHOLD  = 0.35;

    // ── Category weights ──────────────────────────────────────────────────────
    public static final double W_TYPING   = 0.30;
    public static final double W_ERRORS   = 0.25;
    public static final double W_FOCUS    = 0.20;
    public static final double W_BUILDS   = 0.15;
    public static final double W_ACTIVITY = 0.10;

    // ── KPM thresholds (from plugin source comments) ──────────────────────────
    static final int KPM_LOW     = 20;   // below this → stuck / idle
    static final int KPM_OPTIMAL = 80;   // target zone — peak score
    static final int KPM_HIGH    = 150;  // above this → likely rushing

    // ── Error tolerances ─────────────────────────────────────────────────────
    private static final int SYNTAX_TOLERANCE  = 3;
    private static final int COMPILE_TOLERANCE = 2;

    // ── Focus tolerances ─────────────────────────────────────────────────────
    private static final int FILE_SWITCH_TOLERANCE = 5;
    private static final int FOCUS_LOSS_TOLERANCE  = 3;

    // ─────────────────────────────────────────────────────────────────────────

    /** Full detection result with per-category scores and derived state. */
    public static class DetectionResult {
        public final double typingScore;
        public final double errorScore;
        public final double focusScore;
        public final double buildScore;
        public final double activityScore;
        public final double flowTally;
        public final String state;
        /** stressLevel = (1-errorScore)*0.6 + (1-focusScore)*0.4 */
        public final double stressLevel;

        DetectionResult(double t, double e, double f, double b, double a) {
            typingScore   = t;
            errorScore    = e;
            focusScore    = f;
            buildScore    = b;
            activityScore = a;
            flowTally     = (t * W_TYPING) + (e * W_ERRORS) + (f * W_FOCUS)
                          + (b * W_BUILDS) + (a * W_ACTIVITY);
            stressLevel   = (1 - e) * 0.6 + (1 - f) * 0.4;
            if      (flowTally >= FLOW_THRESHOLD)            state = "FLOW";
            else if (flowTally <= PROCRASTINATING_THRESHOLD) state = "PROCRASTINATING";
            else                                             state = "NEUTRAL";
        }

        /** One-line summary */
        public String summary() {
            return String.format("%-15s tally=%.3f  stress=%.3f  [T:%.2f E:%.2f F:%.2f B:%.2f A:%.2f]",
                state, flowTally, stressLevel,
                typingScore, errorScore, focusScore, buildScore, activityScore);
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public static DetectionResult detect(MockFlowMetrics m) {
        return new DetectionResult(
            typingScore(m),
            errorScore(m),
            focusScore(m),
            buildScore(m),
            activityScore(m)
        );
    }

    // ── Category scorers ──────────────────────────────────────────────────────

    /**
     * Typing score (0.0 – 1.0).
     *
     * KPM curve:
     *   0–5   kpm  → 0.0          (completely idle)
     *   5–20  kpm  → 0.00–0.30    (very slow, warming up)
     *   20–80 kpm  → 0.30–1.00    (productive zone, linear ramp)
     *   80–150 kpm → 1.00–0.80    (slight rush penalty)
     *   150+  kpm  → 0.80–0.40    (clearly rushing)
     *
     * Additional penalties:
     *   backspace ratio > 30%  → up to -0.30 (heavy rework)
     *   keyboard idle  > 30s   → up to -0.30 (disengaged)
     */
    public static double typingScore(MockFlowMetrics m) {
        int kpm = m.keystrokesPerMin;

        double kpmScore;
        if (kpm < 5) {
            kpmScore = 0.0;
        } else if (kpm < KPM_LOW) {
            kpmScore = 0.30 * ((double) kpm / KPM_LOW);
        } else if (kpm <= KPM_OPTIMAL) {
            kpmScore = 0.30 + 0.70 * ((double)(kpm - KPM_LOW) / (KPM_OPTIMAL - KPM_LOW));
        } else if (kpm <= KPM_HIGH) {
            kpmScore = 1.00 - 0.20 * ((double)(kpm - KPM_OPTIMAL) / (KPM_HIGH - KPM_OPTIMAL));
        } else {
            kpmScore = Math.max(0, 0.80 - 0.40 * ((double)(kpm - KPM_HIGH) / 100.0));
        }

        // Backspace-ratio penalty: approximates total chars as kpm * 2 (2-min window)
        double approxTotal = Math.max(1, (double) kpm * 2);
        double bsRatio = m.backspaceCount / approxTotal;
        double bsPenalty = bsRatio > 0.30 ? Math.min(0.30, (bsRatio - 0.30) * 1.5) : 0.0;

        // Idle penalty: significant idle means disengaged
        double idlePenalty = 0.0;
        if (m.keyboardIdleMs > 30_000) {
            idlePenalty = Math.min(0.30, (m.keyboardIdleMs - 30_000) / 300_000.0 * 0.30);
        }

        return clamp(kpmScore - bsPenalty - idlePenalty);
    }

    /**
     * Error score (0.0 – 1.0).
     *
     * Starts at 1.0 and deducts for errors above tolerance.
     * Bonus for a 10+ minute error-free streak.
     */
    public static double errorScore(MockFlowMetrics m) {
        double score = 1.0;

        // Syntax errors beyond tolerance
        if (m.syntaxErrorCount > SYNTAX_TOLERANCE) {
            double excess = m.syntaxErrorCount - SYNTAX_TOLERANCE;
            score -= Math.min(0.40, excess * 0.08);
        }

        // Compilation errors beyond tolerance
        if (m.compilationErrors > COMPILE_TOLERANCE) {
            double excess = m.compilationErrors - COMPILE_TOLERANCE;
            score -= Math.min(0.30, excess * 0.10);
        }

        // 10+ minute error-free streak bonus
        if (m.timeSinceLastErrorMs > 600_000) {
            score = Math.min(1.0, score + 0.05);
        }

        return clamp(score);
    }

    /**
     * Focus score (0.0 – 1.0).
     *
     * Penalties for excessive file switching and IDE focus loss.
     * Bonus for sustained time in a single file (deep focus).
     */
    public static double focusScore(MockFlowMetrics m) {
        double score = 1.0;

        // File switching penalty
        if (m.fileChangesLast5Min > FILE_SWITCH_TOLERANCE) {
            double excess = m.fileChangesLast5Min - FILE_SWITCH_TOLERANCE;
            score -= Math.min(0.35, excess * 0.07);
        }

        // Focus loss penalty
        if (m.focusLossCount > FOCUS_LOSS_TOLERANCE) {
            double excess = m.focusLossCount - FOCUS_LOSS_TOLERANCE;
            score -= Math.min(0.25, excess * 0.08);
        }

        // Sustained focus bonus: 2+ min in same file, scales to +0.10
        if (m.timeInCurrentFileMs >= 120_000) {
            double minutes = m.timeInCurrentFileMs / 60_000.0;
            score = Math.min(1.0, score + Math.min(0.10, minutes * 0.01));
        }

        return clamp(score);
    }

    /**
     * Build score (0.0 – 1.0).
     *
     * Baseline 0.7, adjusted for build health.
     * Recent successful build → bonus. Failed/stale build → penalty.
     */
    public static double buildScore(MockFlowMetrics m) {
        double score = 0.70;

        if (!m.lastBuildSuccess) {
            score -= 0.30;
        } else {
            score += 0.10; // passing build lifts the score
        }

        // Consecutive failure streak penalty
        if (m.consecutiveFailedBuilds > 0) {
            score -= Math.min(0.30, m.consecutiveFailedBuilds * 0.10);
        }

        // Stale build penalty (30+ min without compiling)
        if (m.timeSinceLastBuildMs > 1_800_000) {
            score -= 0.10;
        }

        // Recent success bonus (<5 min since last passing build)
        if (m.lastBuildSuccess && m.timeSinceLastBuildMs < 300_000) {
            score += 0.10;
        }

        return clamp(score);
    }

    /**
     * Activity / session score (0.0 – 1.0).
     *
     * First minute is treated as neutral (0.5).
     * Long idle periods are penalised; sustained active sessions are rewarded.
     */
    public static double activityScore(MockFlowMetrics m) {
        // Very early in session — not enough signal
        if (m.sessionDurationMs < 60_000) {
            return 0.50;
        }

        double score = 0.50;

        if (m.keyboardIdleMs > 120_000) {
            // 2+ min idle — scale penalty with idle duration
            double idleMin = m.keyboardIdleMs / 60_000.0;
            score -= Math.min(0.50, idleMin * 0.10);
        } else {
            score += 0.20; // actively typing
        }

        // Sustained active session bonus (15+ min, idle < 1 min)
        if (m.sessionDurationMs > 900_000 && m.keyboardIdleMs < 60_000) {
            score = Math.min(1.0, score + 0.10);
        }

        return clamp(score);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
