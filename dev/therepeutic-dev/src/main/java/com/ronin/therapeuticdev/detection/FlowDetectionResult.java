package com.ronin.therapeuticdev.detection;

/**
 * Immutable result object from FlowDetector analysis.
 * Contains individual category scores, weighted tally, and final state classification.
 */
public class FlowDetectionResult {

    private final double typingScore;
    private final double errorScore;
    private final double focusScore;
    private final double buildScore;
    private final double activityScore;
    private final double flowTally;
    private final double stressLevel;
    private final FlowState flowState;

    public FlowDetectionResult(
            double typingScore,
            double errorScore,
            double focusScore,
            double buildScore,
            double activityScore,
            double flowTally,
            FlowState flowState
    ) {
        this.typingScore = typingScore;
        this.errorScore = errorScore;
        this.focusScore = focusScore;
        this.buildScore = buildScore;
        this.activityScore = activityScore;
        this.flowTally = flowTally;
        this.flowState = flowState;
        this.stressLevel = calculateStressLevel();
    }

    /**
     * Derives stress level from error and focus scores.
     * High errors + low focus = high stress.
     */
    private double calculateStressLevel() {
        // Inverse of error score (more errors = more stress)
        double errorStress = 1.0 - errorScore;
        // Inverse of focus score (less focus = more stress)
        double focusStress = 1.0 - focusScore;
        // Weight errors more heavily for stress calculation
        return (errorStress * 0.6) + (focusStress * 0.4);
    }

    // Getters - using the names that other classes expect

    public double getTypingScore() { return typingScore; }
    public double getErrorScore() { return errorScore; }
    public double getFocusScore() { return focusScore; }
    public double getBuildScore() { return buildScore; }
    public double getActivityScore() { return activityScore; }
    public double getFlowTally() { return flowTally; }
    public double getStressLevel() { return stressLevel; }

    /**
     * Returns the classified flow state.
     * Named getState() to match BreakManager expectations.
     */
    public FlowState getState() { return flowState; }

    @Override
    public String toString() {
        return String.format(
                "FlowDetectionResult{state=%s, tally=%.2f, typing=%.2f, error=%.2f, focus=%.2f, build=%.2f, activity=%.2f}",
                flowState, flowTally, typingScore, errorScore, focusScore, buildScore, activityScore
        );
    }
}
