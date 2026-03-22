package com.ronin.therapeuticdev.detection;

/**
 * the seven discrete states my detection algorithm can classify a developer into.
 *
 * i chose seven rather than a simple binary (flow / not flow) because the granularity
 * matters for the evaluation. with seven states i can track transitions — did the
 * participant move from NEUTRAL through EMERGING into FLOW as they resolved bugs?
 * a binary model loses that signal entirely.
 *
 * the ordering maps loosely onto csikszentmihalyi's flow continuum, but the specific
 * thresholds that separate each state are defined in FlowDetector and are configurable
 * so i can adjust them post-study if participant data suggests different boundaries.
 */
public enum FlowState {
    DEEP_FLOW,       // sustained high composite score, low errors, minimal context switching
    FLOW,            // solid engagement with minor fluctuations
    EMERGING,        // ramping toward flow but not stabilised yet — the transitional zone
    NEUTRAL,         // baseline activity, neither flow nor disengaged
    DISRUPTED,       // previously high score dropped sharply (error spike or focus loss)
    PROCRASTINATING, // low activity, potential avoidance behaviour
    NOT_IN_FLOW;     // fully disengaged

    /**
     * convenience check — true for DEEP_FLOW and FLOW only.
     * used by BreakManager to decide whether the developer is in a productive state
     * worth protecting from interruption.
     */
    public boolean isFlowState() {
        return this == DEEP_FLOW || this == FLOW;
    }

    /**
     * true for the top three states: DEEP_FLOW, FLOW, EMERGING.
     * i use this in the ESM probe logic — if the developer is building toward flow,
     * i delay the self-report dialog to avoid disrupting the ramp-up.
     */
    public boolean shouldAvoidInterruption() {
        return this == DEEP_FLOW || this == FLOW || this == EMERGING;
    }
}
