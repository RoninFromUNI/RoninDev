package therapeutic;

import java.util.ArrayList;
import java.util.List;

/**
 * Simulates a developer's session as it evolves through different states.
 *
 * Models a realistic 2-hour coding session with 8 phases:
 *
 *   T+  0 min  Session Start   — IDE just opened, cold start
 *   T+ 15 min  Finding Rhythm  — warming up, KPM rising
 *   T+ 30 min  Deep Focus      — peak flow zone, high KPM
 *   T+ 60 min  Sustained Flow  — still in flow but slightly waning
 *   T+ 75 min  Debugging       — hit a difficult bug, build failing
 *   T+ 90 min  Stuck           — KPM crashes, errors mounting
 *   T+105 min  Bug Fixed       — recovering, building again
 *   T+120 min  Post-Recovery   — back to mid-session pace
 *
 * Shows exactly when therapeutic-dev would change the displayed state
 * and what triggered each transition.
 */
public class StateTransitionSimulator {

    // ── Internal phase record ─────────────────────────────────────────────────

    static class Phase {
        final int    minuteMark;
        final String label;
        final MockFlowMetrics metrics;
        final FlowSimulator.DetectionResult result;

        Phase(int min, String label, MockFlowMetrics m) {
            this.minuteMark = min;
            this.label      = label;
            this.metrics    = m;
            this.result     = FlowSimulator.detect(m);
        }
    }

    // ── Session definition ────────────────────────────────────────────────────

    public static List<Phase> buildSession() {
        List<Phase> s = new ArrayList<>();
        s.add(new Phase(  0, "Session Start (Cold)",    CodingScenarios.warmingUp()));
        s.add(new Phase( 15, "Finding Rhythm",           CodingScenarios.average()));
        s.add(new Phase( 30, "Deep Focus – Peak",        CodingScenarios.intensive()));
        s.add(new Phase( 60, "Sustained Flow",           CodingScenarios.intensiveMidSession()));
        s.add(new Phase( 75, "Debugging – Build Fails",  CodingScenarios.averageWithBuildIssues()));
        s.add(new Phase( 90, "Stuck on Error",           CodingScenarios.struggling()));
        s.add(new Phase(105, "Bug Fixed, Recovering",    CodingScenarios.average()));
        s.add(new Phase(120, "Post-Recovery Coding",     CodingScenarios.intensiveMidSession()));
        return s;
    }

    // ── Output ────────────────────────────────────────────────────────────────

    public static void run() {
        List<Phase> session = buildSession();

        System.out.println();
        System.out.println("╔═══════════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                      SESSION STATE TRANSITION TIMELINE                            ║");
        System.out.println("╠════════╦═══════════════════════════════╦═══════════════╦══════╦══════════╦═══════╣");
        System.out.printf( "║ %-6s ║ %-29s ║ %-13s ║ %-4s ║ %-8s ║ %-5s ║%n",
            "T(min)", "Phase", "State", "KPM", "Tally", "Stress");
        System.out.println("╠════════╬═══════════════════════════════╬═══════════════╬══════╬══════════╬═══════╣");

        String prevState = null;
        for (Phase p : session) {
            FlowSimulator.DetectionResult r = p.result;
            boolean changed = prevState != null && !prevState.equals(r.state);
            String change   = changed ? " ◄" : "  ";

            System.out.printf("║ %-6d ║ %-29s ║ %-13s ║ %-4d ║ %-8.3f ║ %-5.3f ║%s%n",
                p.minuteMark, p.label, r.state, p.metrics.keystrokesPerMin,
                r.flowTally, r.stressLevel, change);

            prevState = r.state;
        }

        System.out.println("╚════════╩═══════════════════════════════╩═══════════════╩══════╩══════════╩═══════╝");

        printTransitionSummary(session);
        printCategoryProgression(session);
    }

    private static void printTransitionSummary(List<Phase> session) {
        System.out.println();
        System.out.println("  STATE CHANGE LOG:");
        String prev = null;
        boolean any = false;
        for (Phase p : session) {
            String curr = p.result.state;
            if (prev != null && !prev.equals(curr)) {
                System.out.printf("    T+%3d min : %s  →  %s%n", p.minuteMark, prev, curr);
                any = true;
            }
            prev = curr;
        }
        if (!any) {
            System.out.println("    (no state transitions in this session)");
        }
    }

    private static void printCategoryProgression(List<Phase> session) {
        System.out.println();
        System.out.println("  CATEGORY SCORE PROGRESSION (T → Typing, E → Errors, F → Focus, B → Build, A → Activity):");
        System.out.println();
        System.out.printf("  %-6s  %-29s  %5s  %5s  %5s  %5s  %5s  %8s%n",
            "T(min)", "Phase", "T", "E", "F", "B", "A", "Tally");
        System.out.println("  " + "─".repeat(78));

        for (Phase p : session) {
            FlowSimulator.DetectionResult r = p.result;
            System.out.printf("  %-6d  %-29s  %5.2f  %5.2f  %5.2f  %5.2f  %5.2f  %8.3f%n",
                p.minuteMark, p.label,
                r.typingScore, r.errorScore, r.focusScore, r.buildScore, r.activityScore,
                r.flowTally);
        }
    }
}
