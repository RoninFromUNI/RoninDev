package com.ronin.therapeuticdev.metrics;

import java.time.LocalDateTime;

/**
 * i think this should create a snapchot of flow related metrics in specific points of time via
 *CAPTURING:
 * 20 VARS REQUIRED FOR FLOW STATE DETECTION ALGORITHM
 *
 * reference for this is : effective java (bloch,2018), item 2: "consider a builder when faced with man constructtor parameters"
 */


public class FlowMetrics {
    //should act as temporal meta data, reference is:
    //gonna pretend to make this like my unity game programme
    //serialisefield

    // FLOW METRIC : META DATA

    private final LocalDateTime timestamp;
    private final string sessionId;
    private final long sessionDurSecs;

    ///

    // FLOW METRIC: TYPING ACTIVITY

    private final int keystrokesPerMin; //NOTE ITS MEASURED IN MINS BEFORE USING CALC LOGIC
    private final double avgKeyIntervalMs;
    private final int backspcCount;
    private final long keyboardIdleMs;

    ///

    // FLOW METRIC: ERROR TRACKS

    private final int syntaxErrCount;
    private final int compilationErr;
    private final long timeSinceLastErrorMs;

    //FLOW METRIC: CONTEXT SWITCHING

    private final int fileChangesLast10Mins; //10 mins can help smallen the range of data to be read theoretically
    private final long timeInCurrentFileMs;
    private final int FocusLossOrIdleCount;

    // FLOW METRICl: BUILD RESULTS

    private final boolean lastBuildSuccess;
    private final int consecutiveFailedBuilds;
    private final long timeSinceLastBuildMs;

    // FLOW METRIC: CALC SCORES (should finalise the last bit to have a full calculation to center/impose as a
    // rule based algorithm at first utiliing pre assumed values

    private final double flowTally;
    private final double stressLevel;

    public FlowMetrics(LocalDateTime timestamp, string sessionId, long sessionDurSecs, int keystrokesPerMin, double avgKeyIntervalMs, int backspcCount, long keyboardIdleMs, int syntaxErrCount, int compilationErr, long timeSinceLastErrorMs, int fileChangesLast10Mins, long timeInCurrentFileMs, int focusLossOrIdleCount, boolean lastBuildSuccess, int consecutiveFailedBuilds, long timeSinceLastBuildMs, double flowTally, double stressLevel) {
        this.timestamp = timestamp;
        this.sessionId = sessionId;
        this.sessionDurSecs = sessionDurSecs;
        this.keystrokesPerMin = keystrokesPerMin;
        this.avgKeyIntervalMs = avgKeyIntervalMs;
        this.backspcCount = backspcCount;
        this.keyboardIdleMs = keyboardIdleMs;
        this.syntaxErrCount = syntaxErrCount;
        this.compilationErr = compilationErr;
        this.timeSinceLastErrorMs = timeSinceLastErrorMs;
        this.fileChangesLast10Mins = fileChangesLast10Mins;
        this.timeInCurrentFileMs = timeInCurrentFileMs;
        FocusLossOrIdleCount = focusLossOrIdleCount;
        this.lastBuildSuccess = lastBuildSuccess;
        this.consecutiveFailedBuilds = consecutiveFailedBuilds;
        this.timeSinceLastBuildMs = timeSinceLastBuildMs;
        this.flowTally = flowTally;
        this.stressLevel = stressLevel;
    }
}


