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

    //for my error score tolerances
    private int syntaxErrTolerance = 3;
    private int compilationErrTolerance = 2;

    //builds or consecuritive failure tolerance

    private int buildFailTolerance = 2;

    //state classification thresholds now below

    private double flowThreshold = 0.65; //is the line above to show FLOW
    private double procrastinateThreshold = 0.35; //below the line which is procrastinating

    //between both values issss neutral.

    public FlowDetectionResult detect(FlowMetrics metrics)
    {
         //FLOW DETECTION RESULT : CALCULATE INDIVIDUAL SCORES

        double typingScore = normaliseTypScore(metrics);
        double errScore = normaliseErrorScore(metrics);
        double focusScore = normaliseFocusScore(metrics);
        double buildScore = normaliseBuildScore(metrics);
        double activityScore = normaliseActivityScore(metrics);

        //FLOW DETECTION RESULT:  APPLYING WEIGHTS AND SUM

        double flowTally = (typingScore*weightTyping) + (errScore* weightErr) + (focusScore*weightFocus) + (buildScore*weightBuilds)+(activityScore*weightActivity);

        //then a few more steps and we finish core algorithm baby!

        FlowState state;
        if(flowTally >= flowThreshold)
        {
            state = FlowState.FLOW;
        } else if (flowTally <=procrastinateThreshold)
        {
            state =FlowState.PROCRASTINATING;

        } else
        {
            state = FlowState.NEUTRAL;
        }

        //RETURRRNNN TO SENDERRRRRRRRRR

        return new FlowDetectionResult(
                typingScore,errScore,focusScore,buildScore,activityScore,flowTally,state);
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

       int syntaxErrs = metrics.getSyntaxErrCount();
       int compilationErrs = metrics.getCompilationErr();


       //perfect score start at around 1.0 to deduct for errors
        double score = 1.0;

        //gentle penalties to be associated with the score, happens with normal typing, classed as natural
        //beyond it the sscore degrades

        if(syntaxErrs>syntaxErrTolerance)
        {
            int excess = syntaxErrs - syntaxErrTolerance;
            double syntaxPenalty = Math.min(excess*0.1,0.4); //max 40 percent penalty
            score -= syntaxPenalty;
        }

        if(compilationErrs>compilationErrTolerance)
        {
            int excess = compilationErrs - compilationErrTolerance;
            double compilationPenalty = Math.min(excess*0.15,0.5); //max 50 percent penalty
            score-= compilationPenalty;
        }

        //andddd a lil bonus; time since last error (longer would mean better because developer recovered)
        long timeSinceError = metrics.getTimeSinceLastErrorMs();
        if(timeSinceError>600000)
        {
            //10 mins of error free
            score = Math.min(1.0,score+0.1);
        }
        return Math.max(0.0,score);
    }

    private double normaliseFocusScore(FlowMetrics metrics)
    {
        int fileChanges = metrics.getFileChangesLast10Mins();
        long timeInFile = metrics.getTimeInCurrentFileMs();
        int FocusLosses = metrics.getFocusLossOrIdleCount();

        double score = 1.0;

        //penalise excessive file switching (costs the user)
        if(fileChanges>fileChangeTolerance)

        {
            int excess = fileChanges - fileChangeTolerance;
            double focusPenalty = Math.min(excess*0.1,0.3);
            score -= focusPenalty;
        }
        if(FocusLosses>focusLosstolerance)
        {
            int excess = FocusLosses - focusLosstolerance;
            double focusPenalty = Math.min(excess*0.1,0.3);
            score -= focusPenalty;
        }
        //bonus for sustained focus in one file (2+ minuites preferably)
        if(timeInFile>120000)
        {
            score = Math.min(1.0,score+0.15);
        }
        return Math.max(0.0,score);
    }
    private double normaliseBuildScore(FlowMetrics metrics)
    {
        boolean lastBuildSuccess = metrics.isLastBuildSuccess();
        int consecutiveFails = metrics.getConsecutiveFailedBuilds();
        long timeSinceBuild = metrics.getTimeSinceLastBuildMs();
        double score = 1.0;

        //last build failed is a base penalty
        if(!lastBuildSuccess)
        {
            score -=0.3;
        }

        //for more consecutive failures compound the penalty then
        if(consecutiveFails>buildFailTolerance)
        {
            int excess = consecutiveFails - buildFailTolerance;
            double failpenalty = Math.min(excess*0.15,0.4);
            score -= failpenalty;
        }

        //oh nooo havent builded in a while, slight penalty
        //not to oharsh and some dont need frequent builds
        if(timeSinceBuild>1800000)
        {
            score -=0.1; //30+ minutes
        }

        if(lastBuildSuccess&&timeSinceBuild<300000)
        {
            score = Math.min(1.0,score+0.1);

        }
        return Math.max(0.0,score);
    }
    private double normaliseActivityScore(FlowMetrics metrics)
    {
        long sessionDuration = metrics.getSessionDurSecs();
        long keyboardIdle = metrics.getKeyboardIdleMs();
        double score = 1.0;

        //started? youre neutral then, no data

        if(sessionDuration<60)
        {
            return 0.5;
        }
        //idle too long? penalty, might be distracted or stuck

        if(keyboardIdle>120000) //2 mins idle
        {
            double idlePenalty = Math.min((keyboardIdle - 120000) / 300000.0, 0.5);
            score -= idlePenalty;
        }

        //sustaaaaaaaaaained session bonusesss, 15 minutes of activity

        if(sessionDuration>900 && keyboardIdle<60000)
        {
            score = Math.min(1.0,score+0.1);
        }
        return Math.max(0.0,score);
    }
    //TODO: IMPLEMENT EVERYTHING ABOVE LATER!

}
