package com.ronin.therapeuticdev.detection;

/**
 * immutable container for everything that comes out of a single detection cycle.
 *
 * i made this immutable on purpose — once FlowDetector produces a result, nothing
 * downstream (UI, persistence, break manager) should be able to mutate it. this
 * eliminates a whole class of concurrency bugs since the scheduler, UI timer, and
 * persistence layer all hold references to the same result object.
 *
 * contains the five individual category scores, the weighted composite tally,
 * the classified FlowState, and a derived stress level.
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
     * derives a stress proxy from the error and focus sub-scores.
     * the idea: high errors combined with low focus suggests cognitive overload.
     * i weight errors at 60% and focus loss at 40% because unresolved compilation
     * errors are a stronger stress signal than occasional alt-tabs.
     *
     * this is a secondary metric — the primary classification comes from flowTally.
     * i include it because the dissertation discussion benefits from having an
     * independent stress measure to correlate against self-report data.
     */
    private double calculateStressLevel() {
        double errorStress = 1.0 - errorScore;
        double focusStress = 1.0 - focusScore;
        return (errorStress * 0.6) + (focusStress * 0.4);
    }

    // getters — no setters anywhere, immutability enforced by design

    public double getTypingScore() { return typingScore; }
    public double getErrorScore() { return errorScore; }
    public double getFocusScore() { return focusScore; }
    public double getBuildScore() { return buildScore; }
    public double getActivityScore() { return activityScore; }
    public double getFlowTally() { return flowTally; }
    public double getStressLevel() { return stressLevel; }

    /**
     * named getState() rather than getFlowState() because BreakManager and
     * the status bar widget both call result.getState() — changing this would
     * break two downstream consumers for no real benefit.
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
