package therapeutic;

/**
 * Visualises how KPM alone drives the typing score (and consequently
 * the flow tally) when all other metrics are held at ideal neutral values.
 *
 * KPM thresholds baked into FlowDetector:
 *   LOW     = 20 kpm  → below this the developer looks stuck / idle
 *   OPTIMAL = 80 kpm  → peak score (1.0 typing score)
 *   HIGH    = 150 kpm → above this the plugin infers rushing
 *
 * Use this to answer: "what KPM do I need to reach FLOW state?"
 */
public class KpmCurveAnalysis {

    public static void run() {
        System.out.println();
        System.out.println("╔═══════════════════════════════════════════════════════════════════════╗");
        System.out.println("║              KPM → FLOW SCORE CURVE ANALYSIS                         ║");
        System.out.println("║  All other metrics held at ideal values to isolate KPM effect.       ║");
        System.out.println("╠═════════╦══════════════╦══════════════╦═══════════════╦══════════════╣");
        System.out.printf( "║  %-6s ║  %-10s  ║  %-10s  ║  %-11s  ║  %-10s  ║%n",
            "KPM", "TypingScore", "FlowTally", "State", "Stress");
        System.out.println("╠═════════╬══════════════╬══════════════╬═══════════════╬══════════════╣");

        int[] kpmValues = {
            0, 5, 10, 15, 20, 25, 30, 40, 50, 60,
            70, 80, 90, 100, 120, 150, 175, 200
        };

        for (int kpm : kpmValues) {
            MockFlowMetrics m = idealExceptKpm(kpm);
            FlowSimulator.DetectionResult r = FlowSimulator.detect(m);

            String tag = "";
            if (kpm == FlowSimulator.KPM_LOW)     tag = " ◄ LOW";
            else if (kpm == FlowSimulator.KPM_OPTIMAL) tag = " ◄ OPTIMAL";
            else if (kpm == FlowSimulator.KPM_HIGH)    tag = " ◄ HIGH";

            String stateCell = r.state + tag;

            System.out.printf("║  %-6d ║  %-10.3f  ║  %-10.3f  ║  %-11s  ║  %-10.3f  ║%n",
                kpm, r.typingScore, r.flowTally, stateCell, r.stressLevel);
        }

        System.out.println("╠═════════╩══════════════╩══════════════╩═══════════════╩══════════════╣");
        System.out.println("║  FLOW threshold = 0.650  |  PROCRASTINATING threshold = 0.350        ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════════╝");

        printKpmInsights();
    }

    /** Builds a metrics snapshot with all non-typing fields held at ideal. */
    private static MockFlowMetrics idealExceptKpm(int kpm) {
        return new MockFlowMetrics.Builder()
            .kpm(kpm)
            .avgInterval(kpm > 0 ? (int)(60_000.0 / kpm) : 5_000)
            .backspaces(2)           // minimal rework — isolates KPM effect
            .idle(500)
            .syntaxErrors(0)
            .compileErrors(0)
            .timeSinceError(900_000) // 15 min error-free
            .fileChanges(1)
            .timeInFile(900_000)     // 15 min in file — max focus bonus
            .focusLoss(0)
            .buildSuccess(true)
            .failedBuilds(0)
            .timeSinceBuild(120_000) // recent successful build
            .sessionDuration(3_600_000)
            .build();
    }

    private static void printKpmInsights() {
        System.out.println();
        System.out.println("  KPM INSIGHTS:");
        System.out.println("  ┌─ Below 20 kpm : typing score < 0.30 → tally unlikely to reach FLOW");
        System.out.println("  ├─ At 20 kpm    : typing score = 0.30 → minimum 'engaged' signal");
        System.out.println("  ├─ At 50 kpm    : typing score ≈ 0.62 → solid but tally ~0.56 (NEUTRAL)");
        System.out.println("  ├─ At 80 kpm    : typing score = 1.00 → full weight contributed");
        System.out.println("  ├─ At 90 kpm    : typing score ≈ 0.97 → still excellent");
        System.out.println("  ├─ At 150 kpm   : typing score = 0.80 → rushing penalty begins");
        System.out.println("  └─ Above 150 kpm: declining score — plugin flags as unsustainable pace");
        System.out.println();
        System.out.println("  NOTE: Even at ideal KPM (80), you still need good scores in");
        System.out.println("  errors/focus/builds/activity for the tally to reach FLOW (>=0.65).");
        System.out.println("  KPM alone (at 80) contributes: 1.00 × 0.30 = 0.30 of the 0.65 target.");
    }

}

