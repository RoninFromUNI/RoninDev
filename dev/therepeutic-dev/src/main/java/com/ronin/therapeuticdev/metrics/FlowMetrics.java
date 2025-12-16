package com.ronin.therapeuticdev.metrics;

import java.time.LocalDateTime;

/**
 * i think this should create a snapchot of flow related metrics in specific points of time via
 *CAPTURING:
 * 20 VARS REQUIRED FOR FLOW STATE DETECTION ALGORITHM
 * reference for this is : effective java (bloch,2018), item 2: "consider a builder when faced with man constructtor parameters"
 */


public class FlowMetrics {
    //should act as temporal meta data, reference is:
    //gonna pretend to make this like my unity game programme
    //serialisefield

    // FLOW METRIC : META DATA

    private final LocalDateTime timestamp;
    private final String sessionId;
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

    private FlowMetrics(LocalDateTime timestamp, String sessionId, long sessionDurSecs, int keystrokesPerMin, double avgKeyIntervalMs, int backspcCount, long keyboardIdleMs, int syntaxErrCount, int compilationErr, long timeSinceLastErrorMs, int fileChangesLast10Mins, long timeInCurrentFileMs, int focusLossOrIdleCount, boolean lastBuildSuccess, int consecutiveFailedBuilds, long timeSinceLastBuildMs, double flowTally, double stressLevel, FlowState flowState, String notes) {
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

        //SO, with the use of private constructors, i can force users to utilise the builder pattern which i will do in progress next
        // prevent direct instantiation
        //ensures everything is build through controlled api
        //ref: effective java, item 2: consider a builder when faced with many constructor parameters.
        this.flowState = flowState;
        this.notes = notes;
    }

    public enum FlowState
    {FLOW, NEUTRAL, PROCRASTINATING}

    private final FlowState flowState;
    private final String notes;

    // FLOW METRICS : GETTERS












    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getSessionId() {
        return sessionId;
    }
    public long getSessionDurSecs() {
        return sessionDurSecs;
    }

    public int getKeystrokesPerMin() {
        return keystrokesPerMin;

    }
    public double getAvgKeyIntervalMs() {
        return avgKeyIntervalMs;
    }

    public int getBackspcCount() {
        return backspcCount;
    }
    public long getKeyboardIdleMs() {
        return keyboardIdleMs;
    }
    public int getSyntaxErrCount() {
        return syntaxErrCount;
    }
    public int getCompilationErr() {
        return compilationErr;

    }
    public long getTimeSinceLastErrorMs() {
        return timeSinceLastErrorMs;
    }

    public int getFileChangesLast10Mins() {
        return fileChangesLast10Mins;

    }
    public long getTimeInCurrentFileMs() {
        return timeInCurrentFileMs;
    }
    public int getFocusLossOrIdleCount() {
        return FocusLossOrIdleCount;
    }
    public boolean isLastBuildSuccess() {
        return lastBuildSuccess;
    }
    public int getConsecutiveFailedBuilds() {
        return consecutiveFailedBuilds;
    }
    public long getTimeSinceLastBuildMs() {
        return timeSinceLastBuildMs;
    }
    public double getFlowTally() {
        return flowTally;
    }
    public double getStressLevel() {
        return stressLevel;
    }


    //dont know why but its preffered to put it after the getters
    public static class Builder{
        private LocalDateTime timestamp=LocalDateTime.now();
        private String sessionId=java.util.UUID.randomUUID().toString();
        private long sessionDurSecs=0;
        private int keystrokesPerMin=0;
        private double avgKeyIntervalMs=0;
        private int backspcCount=0;
        private long keyboardIdleMs=0;
        private int syntaxErrCount=0;
        private int compilationErr=0;
        private long timeSinceLastErrorMs=0;
        private int fileChangesLast10Mins=0;
        private long timeInCurrentFileMs=0;
        private int focusLossOrIdleCount=0;
        private boolean lastBuildSuccess=true;
        private int consecutiveFailedBuilds=0;
        private long timeSinceLastBuildMs=0;
        private double flowTally=0;
        private double stressLevel=0;
        private FlowState flowState=FlowState.NEUTRAL;
        private String notes;


        public Builder timestamp(LocalDateTime timestamp){
            this.timestamp=timestamp;
            return this;
        }
        public Builder sessionId(String sessionId){
            this.sessionId=sessionId;
            return this;
        }
        public Builder sessionDurSecs(long secs){
            this.sessionDurSecs=secs;
            return this;
        }
        public Builder keystrokesPerMin(int kpm){
            this.keystrokesPerMin=kpm;
            return this;
        }
        public Builder setAvgKeyIntervalMs(double avgKeyIntervalMs){
            this.avgKeyIntervalMs=avgKeyIntervalMs;
            return this;
        }
        public Builder setBackspcCount(int backspcCount){
            this.backspcCount=backspcCount;
            return this;
        }
        public Builder setKeyboardIdleMs(long keyboardIdleMs){
            this.keyboardIdleMs=keyboardIdleMs;
            return this;
        }
        public Builder setSyntaxErrCount(int syntaxErrCount){
            this.syntaxErrCount=syntaxErrCount;
            return this;
        }
        public Builder setCompilationErr(int compilationErr){
            this.compilationErr=compilationErr;
            return this;
        }
        public Builder setTimeSinceLastErrorMs(long timeSinceLastErrorMs){
            this.timeSinceLastErrorMs=timeSinceLastErrorMs;
            return this;


        }
        public Builder setFileChangesLast10Mins(int fileChangesLast10Mins){
            this.fileChangesLast10Mins=fileChangesLast10Mins;
            return this;

        }
        public Builder setTimeInCurrentFileMs(long timeInCurrentFileMs){
            this.timeInCurrentFileMs=timeInCurrentFileMs;
            return this;
        }
        public Builder setFocusLossOrIdleCount(int focusLossOrIdleCount){
            this.focusLossOrIdleCount=focusLossOrIdleCount;
            return this;
        }
        public Builder setLastBuildSuccess(boolean lastBuildSuccess){
            this.lastBuildSuccess=lastBuildSuccess;
            return this;
        }
        public Builder setConsecutiveFailedBuilds(int consecutiveFailedBuilds){
            this.consecutiveFailedBuilds=consecutiveFailedBuilds;
            return this;

        }
        public Builder setTimeSinceLastBuildMs(long timeSinceLastBuildMs){
            this.timeSinceLastBuildMs=timeSinceLastBuildMs;
            return this;

        }
        public Builder setFlowTally(double flowTally){
            this.flowTally=flowTally;
            return this;


        }
        public Builder setStressLevel(double stressLevel){
            this.stressLevel=stressLevel;
            return this;

        }
        public Builder setFlowState(FlowState flowState) {
            this.flowState = flowState;
            return this;

        }
        public Builder setNotes(String notes){
            this.notes=notes;
            return this;

        }





    }

    // TOTAL 20/20 VARIABLES!
    //reminder for myself next time, intelliJ can generate or alt+ insert to select all fields and generates all 20 getters at once
}


