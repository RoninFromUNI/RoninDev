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

    private double normaliseTypScore (FlowMetrics metrics) {


        //TODO: START WITH THIS AS ITS EASIEST
        int kpm = metrics.getKeystrokesPerMin();
        int bckSpaces = metrics.getBackspcCount();
        //base calculation for kpm score above here which i could later on translate to a curve if i wanted to. sorta like how opera gx measueres ram usage.

        double kpmScore;

        //maybe adjust a backspace penalty for the ratio of corrections due to keystrokes
        //assume backspaces over 10% of keystrokes indicate a slight struggle


        // NORMALISE TYPING SCORE : BELOW MINIMUM THRESHOLD (POOR SCORE -> IDLE OR STUCK)
        if (kpm < kpmLow) {
            kpmScore= (kpm / (double) kpmLow) * 0.3;
        }
        // NORMALISE TYPING SCORE : OPTIMAL RANGE (FULL SCORE!)

        else if (kpm >= kpmLow && kpm <= kpmOptimal) {
            double range = kpmOptimal - kpmLow;
            double pos = kpm - kpmLow;
            kpmScore= 0.3 + (pos / range) * 0.7;
        }

        // NORMALISE TYPING SCORE : optimal but below high due to slight degradation in flow
        else if (kpm > kpmOptimal && kpm <= kpmHigh) {
            //presenting ffirst a lkinear decline from 1.0 at kpm optimal to roughly 0.6 at kpmHigh
            //TODO: adjust this value for comfortbale accurate yield

            double range = kpmHigh - kpmOptimal;
            double pos = kpm - kpmOptimal;
            kpmScore= 1.0 - (pos / range) * 0.4;
        }

        //and then for the above threshold to signify rushing, maybe disable this later or find a better way
        //but very much so theres a clamp at 0.3 minimum if still typing but just too fast
        else {
            double excess = kpm - kpmHigh;
            double penalty = excess / 100.0; //declining gradually
            kpmScore = Math.max(0.3, 0.6 - penalty);
        }

        double totalKeys = (kpm>0) ? kpm : 1;  //avoiding a division by zero
        double backspcRatio = bckSpaces/totalKeys;
        double backspcPenalty = Math.min(backspcRatio*2,0.3); //indicating a maximum 30% penalty here hehe

        return Math.max(0.0,kpmScore - backspcPenalty);

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
