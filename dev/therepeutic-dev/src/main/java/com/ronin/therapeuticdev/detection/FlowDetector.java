package com.ronin.therapeuticdev.detection;

import com.ronin.therapeuticdev.metrics.FlowMetrics;
import com.ronin.therapeuticdev.metrics.FlowMetrics.FlowState;

public class FlowDetector {

    /// this will be my core detection algo for flow state classification
    ///  applies a nice weighrted score across all five metric categories:

    ///  TYPING : gonna start off with 30%
    /// ERRORS:  25% should be chill, not making it too ambigious of the score to help assure consistency
    ///  FOCUS: 20% this measures the whole point of the dev plugin to measure productivity and flow
    ///  BUILDS: 15% just randomising
    /// ACTIVIITY: 10% complerelt justifiable

    private double weightTyping = 0.30;
    private double weightErr = 0.25;
    private double weightFocus = 0.20;
    private double weightBuilds = 0.15;
    private double weightActivity = 0.10;

    /// after weights are esstablished, i need to now go ahead and start tunng in my threshold values

    private int  kpmLow =20;
    private int  kpmOptimal =80;
    private int  kpmHigh=150; //rushing yourself

    private int fileChangeTolerance = 5; //10 min window/s
    private int focusLosstolerance = 3;

    //builds or consecuritive failure tolerance

    private int buildFailTolerance = 2;

    //state classification thresholds now below

    private double flowThreshold = 0.65; //is the line above to show FLOW
    private double procrastinateThreshold = 0.35; //below the line which is procrastinating

    //between both values issss neutral.

    public FlowDetectionResult detect(FlowMetrics metrics)
    {
        return null;
    }

    private double normaliseTypScore (FlowMetrics metrics)
    {
        return 0.0;
    }
    private double normaliseErrorScore(FlowMetrics metrics)
    {
        return 0.0;
    }

    private double normaliseFocusScore(FlowMetrics metrics)
    {
        return 0.0;
    }
    private double normaliseBuildScore(FlowMetrics metrics)
    {
        return 0.0;
    }
    private double normaliseActivityScore(FlowMetrics metrics)
    {
        return 0.0;
    }
    //TODO: IMPLEMENT EVERYTHING ABOVE LATER!

}
