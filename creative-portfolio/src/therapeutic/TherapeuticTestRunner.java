package therapeutic;

import java.util.Map;

/**
 * Main test runner for the therapeutic-dev plugin flow simulator.
 *
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  WHAT THIS DOES                                                 │
 * │  ─────────────────────────────────────────────────────────────  │
 * │  Replicates the FlowDetector scoring algorithm offline so you   │
 * │  can understand and verify how different coding intensities map  │
 * │  to plugin states — without needing to actually type at speed.  │
 * │                                                                 │
 * │  Four test sections:                                            │
 * │    1. Scenario Comparison  — all 6 profiles side by side        │
 * │    2. KPM Curve Analysis   — KPM → score table (20 data points) │
 * │    3. Session Timeline     — 8-phase session with transitions   │
 * │    4. Intensive vs Average — detailed side-by-side breakdown    │
 * │                                                                 │
 * │  LIVE COMPARISON (while this is open in IntelliJ)               │
 * │  ─────────────────────────────────────────────────────────────  │
 * │  The plugin monitors your ACTUAL keystrokes as you read/edit    │
 * │  this file. Open the "Therapeutic Dev" tool window and compare  │
 * │  the plugin's live tally to the simulated predictions below.    │
 * │  Any consistent gap between predicted and actual reveals edge   │
 * │  cases worth investigating in FlowDetector.java.               │
 * └─────────────────────────────────────────────────────────────────┘
 *
 * Run: select TherapeuticTestRunner as the run target in IntelliJ.
 */
public class TherapeuticTestRunner {

    public static void main(String[] args) {
        printHeader();

        section("1", "SCENARIO COMPARISON — All 6 Coding Profiles");
        runScenarioComparison();

        section("2", "KPM CURVE ANALYSIS — How KPM Drives the Score");
        KpmCurveAnalysis.run();

        section("3", "SESSION TIMELINE — State Transitions Over 2 Hours");
        StateTransitionSimulator.run();

        section("4", "INTENSIVE vs AVERAGE — Detailed Side-by-Side");
        runIntensiveVsAverageComparison();

        printFooter();
    }

    // ── Section 1: Scenario comparison ───────────────────────────────────────

    private static void runScenarioComparison() {
        Map<String, MockFlowMetrics> scenarios = CodingScenarios.all();

        System.out.println();
        System.out.printf("  %-26s  %-15s  %-6s  %-6s  %-6s  %-6s  %-6s  %-7s%n",
            "Scenario", "State", "Typin", "Error", "Focus", "Build", "Activ", "Tally");
        System.out.println("  " + "─".repeat(90));

        for (Map.Entry<String, MockFlowMetrics> e : scenarios.entrySet()) {
            FlowSimulator.DetectionResult r = FlowSimulator.detect(e.getValue());
            System.out.printf("  %-26s  %-15s  %-6.2f  %-6.2f  %-6.2f  %-6.2f  %-6.2f  %-7.3f%n",
                e.getKey(), r.state,
                r.typingScore, r.errorScore, r.focusScore, r.buildScore, r.activityScore,
                r.flowTally);
        }

        // Which categories are the biggest bottlenecks?
        System.out.println();
        System.out.println("  BOTTLENECK CHECK (where does 'Average' lose most points?)");
        MockFlowMetrics avg = CodingScenarios.average();
        FlowSimulator.DetectionResult ra = FlowSimulator.detect(avg);
        double[] contributions = {
            ra.typingScore   * FlowSimulator.W_TYPING,
            ra.errorScore    * FlowSimulator.W_ERRORS,
            ra.focusScore    * FlowSimulator.W_FOCUS,
            ra.buildScore    * FlowSimulator.W_BUILDS,
            ra.activityScore * FlowSimulator.W_ACTIVITY
        };
        String[] catNames = { "Typing (30%)", "Errors (25%)", "Focus (20%)", "Build (15%)", "Activity (10%)" };
        double[] maxContrib = { 0.30, 0.25, 0.20, 0.15, 0.10 };

        for (int i = 0; i < catNames.length; i++) {
            double lost = maxContrib[i] - contributions[i];
            String bar = "█".repeat((int)(contributions[i] * 30))
                       + "░".repeat((int)(lost * 30));
            System.out.printf("    %-14s  contribution: %5.3f / %4.2f  lost: %5.3f  %s%n",
                catNames[i], contributions[i], maxContrib[i], lost, bar);
        }
        System.out.printf("    %-14s  TOTAL TALLY : %5.3f (need %.3f for FLOW)%n",
            "", ra.flowTally, FlowSimulator.FLOW_THRESHOLD);
    }

    // ── Section 4: Intensive vs Average deep dive ─────────────────────────────

    private static void runIntensiveVsAverageComparison() {
        MockFlowMetrics intensive = CodingScenarios.intensive();
        MockFlowMetrics average   = CodingScenarios.average();

        FlowSimulator.DetectionResult ri = FlowSimulator.detect(intensive);
        FlowSimulator.DetectionResult ra = FlowSimulator.detect(average);

        System.out.println();
        System.out.println("  INPUT METRICS:");
        System.out.printf("  %-28s  %-15s  %-15s%n", "Metric", "Intensive", "Average");
        System.out.println("  " + "─".repeat(60));

        printMetricRow("KPM",                    intensive.keystrokesPerMin,      average.keystrokesPerMin);
        printMetricRow("Avg Key Interval (ms)",   (int) intensive.avgKeyIntervalMs,(int) average.avgKeyIntervalMs);
        printMetricRow("Backspaces",              intensive.backspaceCount,         average.backspaceCount);
        printMetricRow("Keyboard Idle (ms)",      (int) intensive.keyboardIdleMs,  (int) average.keyboardIdleMs);
        printMetricRow("Syntax Errors",           intensive.syntaxErrorCount,       average.syntaxErrorCount);
        printMetricRow("Compile Errors",          intensive.compilationErrors,      average.compilationErrors);
        printMetricRow("File Changes / 5 min",    intensive.fileChangesLast5Min,    average.fileChangesLast5Min);
        printMetricRow("Time in File (min)",
            (int)(intensive.timeInCurrentFileMs / 60_000),
            (int)(average.timeInCurrentFileMs   / 60_000));
        printMetricRow("Focus Lost Events",       intensive.focusLossCount,         average.focusLossCount);
        printMetricRow("Consecutive Fail Builds", intensive.consecutiveFailedBuilds,average.consecutiveFailedBuilds);

        System.out.println();
        System.out.println("  SCORE OUTPUTS:");
        System.out.printf("  %-26s  %-12s  %-12s  %-10s  %-8s%n",
            "Category (Weight)", "Intensive", "Average", "Delta", "Max Weight");
        System.out.println("  " + "─".repeat(76));

        printScoreRow("Typing   (30%)", ri.typingScore,   ra.typingScore,   0.30);
        printScoreRow("Errors   (25%)", ri.errorScore,    ra.errorScore,    0.25);
        printScoreRow("Focus    (20%)", ri.focusScore,    ra.focusScore,    0.20);
        printScoreRow("Build    (15%)", ri.buildScore,    ra.buildScore,    0.15);
        printScoreRow("Activity (10%)", ri.activityScore, ra.activityScore, 0.10);
        System.out.println("  " + "─".repeat(76));
        printScoreRow("FLOW TALLY",     ri.flowTally,     ra.flowTally,     1.00);
        printScoreRow("Stress Level",   ri.stressLevel,   ra.stressLevel,   1.00);

        System.out.println();
        System.out.printf("  %-20s  State: %-15s  (%.1f%% tally)%n",
            "Intensive", ri.state, ri.flowTally * 100);
        System.out.printf("  %-20s  State: %-15s  (%.1f%% tally)%n",
            "Average",   ra.state, ra.flowTally * 100);
        System.out.printf("  %-20s  Tally gap: +%.3f  |  Stress gap: %.3f%n",
            "Gap (Intensive − Avg)", ri.flowTally - ra.flowTally, ri.stressLevel - ra.stressLevel);

        System.out.println();
        System.out.println("  WHAT WOULD PUSH 'AVERAGE' INTO FLOW?");
        suggestImprovements(ra);
    }

    /** Suggests which single metric change would most improve an average score. */
    private static void suggestImprovements(FlowSimulator.DetectionResult ra) {
        double target = FlowSimulator.FLOW_THRESHOLD;
        double gap    = target - ra.flowTally;
        System.out.printf("    Gap to FLOW = %.3f%n", gap);
        System.out.println("    Top levers (biggest gain per unit of effort):");

        if (ra.typingScore < 0.90) {
            double gain = (0.90 - ra.typingScore) * FlowSimulator.W_TYPING;
            System.out.printf("    ├─ Raise KPM from 50 → 75+      typing 0.90 → +%.3f tally%n", gain);
        }
        if (ra.errorScore < 0.95) {
            double gain = (0.95 - ra.errorScore) * FlowSimulator.W_ERRORS;
            System.out.printf("    ├─ Fix syntax errors             errors 0.95 → +%.3f tally%n", gain);
        }
        if (ra.focusScore < 0.95) {
            double gain = (0.95 - ra.focusScore) * FlowSimulator.W_FOCUS;
            System.out.printf("    ├─ Fewer file switches / alt-tab focus  0.95 → +%.3f tally%n", gain);
        }
        if (ra.buildScore < 0.90) {
            double gain = (0.90 - ra.buildScore) * FlowSimulator.W_BUILDS;
            System.out.printf("    └─ Build more often              build  0.90 → +%.3f tally%n", gain);
        }
    }

    // ── Formatting helpers ────────────────────────────────────────────────────

    private static void printMetricRow(String label, int intensive, int average) {
        System.out.printf("  %-28s  %-15d  %-15d%n", label, intensive, average);
    }

    private static void printScoreRow(String label, double intensive, double average, double maxWeight) {
        double delta = intensive - average;
        String sign  = delta >= 0 ? "+" : "";
        System.out.printf("  %-26s  %-12.3f  %-12.3f  %s%-10.3f  %-8.2f%n",
            label, intensive, average, sign, delta, maxWeight);
    }

    private static void section(String num, String title) {
        System.out.println();
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.printf( "│  [%s] %s%n", num, title);
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");
    }

    private static void printHeader() {
        System.out.println("╔═══════════════════════════════════════════════════════════════════════╗");
        System.out.println("║        THERAPEUTIC-DEV  ·  FLOW SIMULATOR TEST HARNESS               ║");
        System.out.println("║        creative-portfolio / src/therapeutic/                         ║");
        System.out.println("╠═══════════════════════════════════════════════════════════════════════╣");
        System.out.println("║  Replicates FlowDetector scoring to show how KPM & coding patterns   ║");
        System.out.println("║  map to detected developer states.                                   ║");
        System.out.println("║                                                                       ║");
        System.out.println("║  Weights:  Typing 30%  Errors 25%  Focus 20%  Build 15%  Activity 10%║");
        System.out.println("║  States:   FLOW >= 0.65  │  NEUTRAL  │  PROCRASTINATING <= 0.35      ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════════╝");
    }

    private static void printFooter() {
        System.out.println();
        System.out.println("╔═══════════════════════════════════════════════════════════════════════╗");
        System.out.println("║  LIVE COMPARISON GUIDE                                               ║");
        System.out.println("║  ─────────────────────────────────────────────────────────────────   ║");
        System.out.println("║  While editing this file, the plugin tracks your actual keystrokes.  ║");
        System.out.println("║                                                                       ║");
        System.out.println("║  1. Open  View > Tool Windows > Therapeutic Dev                      ║");
        System.out.println("║  2. Edit code actively for 2+ min to build up KPM signal             ║");
        System.out.println("║  3. Compare live tally to simulated 'Average' profile above          ║");
        System.out.println("║  4. Type fast for 2 min → compare to 'Intensive' profile             ║");
        System.out.println("║                                                                       ║");
        System.out.println("║  If simulated ≠ plugin: check MetricCollector's sliding window       ║");
        System.out.println("║  (plugin uses 2-min window; simulator uses snapshot approximation)   ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════════╝");
    }
}
