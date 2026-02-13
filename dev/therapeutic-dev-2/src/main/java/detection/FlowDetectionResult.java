package detection;

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

            double typingScore, double errorScore, double focusScore, double buildScore,double activityScore,double flowTally,FlowState flowState


    )
    {
        this.typingScore = typingScore;
        this.errorScore = errorScore;
        this.focusScore= errorScore;
        this.buildScore = buildScore;
        this.activityScore = activityScore;
        this.flowTally = flowTally;
        this.flowState = flowState;
        this.stressLevel = calculateStressLevel();
    }

    private double calculateStressLevel()
    {
        double errStress = 1.0 -errorScore;
        double focusStress = 1.0 - focusScore;
        return (errStress*0.6) + (focusStress *0.4);
    }

    //lets get em getters

    public double getTypingScore() { return typingScore; }
    public double getErrorScore() { return errorScore; }
    public double getFocusScore() { return focusScore; }
    public double getBuildScore() { return buildScore; }
    public double getActivityScore() { return activityScore; }
    public double getFlowTally() { return flowTally; }
    public double getStressLevel() { return stressLevel; }


    public FlowState getFlowState() {
        return flowState;
    }

    @Override
    public String toString()
    {
        return String.format("FlowDetectionResult{state=%s, tally=%.2f, typing=%.2f, error=%.2f, focus=%.2f, build=%.2f, activity=%.2f}",
                flowState, flowTally, typingScore, errorScore, focusScore, buildScore, activityScore
        );
    }
}
