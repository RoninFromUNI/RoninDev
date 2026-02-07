package detection;
//to timestamp how im working with everything here for myself
//everything starts off from detection first when being built

/**
 * what this piece of code just represents is the referenced detected
 * psychological flow state of a developer. which is entirely based first
 * on Csikszentmihalyi's flow theory, which is just mapped to quantity based
 * IDE behavioral thresholds
 */
public enum FlowState {
    /**
     * quantities should be adjustable this way after
     * creating 8 flow state enums first for better granularity
     * its shapens the flow continum much better and we can attach state definitions
     * to the score thresholds and metadata
     *
     */
    DEEP_FLOW("Deep Flow", 90),
    FLOW("Flow", 75),
    EMERGING("Emerging", 60),
    NEUTRAL("Neutral",40),
    DISRUPTED("Disrupted", 25),
    PROCRASTINATION("Procrastinating",10),
    NOT_IN_FLOW("Not In Flow",0);


    private final String displayName;
    private final int minimumScore;

    FlowState(String displayName, int minimumScore)
    {
        this.displayName=displayName;
        this.minimumScore=minimumScore;
    }

}
