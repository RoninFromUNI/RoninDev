package com.ronin.therapeuticdev.detection;

/**
 * Represents discrete flow states derived from metric analysis.
 * Maps to Csikszentmihalyi's flow continuum with granular thresholds.
 */
public enum FlowState {
    DEEP_FLOW,       // Optimal engagement - uninterrupted, high productivity
    FLOW,            // Sustained focus with minor fluctuations
    EMERGING,        // Transitioning toward flow state
    NEUTRAL,         // Baseline activity - neither flow nor disengaged
    DISRUPTED,       // Recently interrupted, attempting recovery
    PROCRASTINATING, // Low activity, potential avoidance behaviour
    NOT_IN_FLOW;     // Disengaged state

    public boolean isFlowState() {
        return this == DEEP_FLOW || this == FLOW;
    }

    public boolean shouldAvoidInterruption() {
        return this == DEEP_FLOW || this == FLOW || this == EMERGING;
    }
}