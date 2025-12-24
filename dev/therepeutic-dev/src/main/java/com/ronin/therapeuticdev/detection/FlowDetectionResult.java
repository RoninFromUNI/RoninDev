package com.ronin.therapeuticdev.detection;

import com.ronin.therapeuticdev.metrics.FlowMetrics.FlowState;

public class FlowDetectionResult

{
    private final double typScore;
    private final double errScore;
    private final double FocusScore;
    private final double buildScore;
    private final double activityScore;


private final double flowTally;
private final FlowState flowState;

public FlowDetectionResult(

        double typScore, double errScore, double FocusScore,double buildScore, double activityScore, double flowTally, FlowState flowState
)
{
    this.typScore = typScore;
    this.errScore = errScore;
    this.FocusScore = FocusScore;
    this.buildScore = buildScore;
    this.activityScore = activityScore;
    this.flowTally = flowTally;
    this.flowState = flowState;

    //alright nevermind! that fixed it!
}
//now to apply my getters

    public double getTypScore() {return typScore;}
    public double getErrScore() {return errScore;}
    public double getFocusScore() {return FocusScore;}
    public double getBuildScore() {return buildScore;}
    public double getActivityScore() {return activityScore;}
    public double getFlowTally() {return flowTally;}
    public FlowState getFlowState() {return flowState;}
    //intelliJ auto complete is so cool!
}

