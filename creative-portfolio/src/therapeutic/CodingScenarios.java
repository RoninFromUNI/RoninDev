package therapeutic;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Preset coding profiles representing the range of developer states
 * that therapeutic-dev can detect.
 *
 * Each profile is a realistic snapshot of what MetricCollector.snapshot()
 * would return during that kind of session.
 *
 * Profiles (ordered from low to high engagement):
 *   1. struggling          – stuck, errors everywhere, scattered focus
 *   2. warmingUp           – first 5 min, ramping up KPM
 *   3. average             – normal productive pace
 *   4. averageWithBuildIssues – moderate coding, failing build
 *   5. intensiveMidSession – past peak flow, slightly waning
 *   6. intensive           – peak deep-work flow zone
 */
public class CodingScenarios {

    // ── Scenario factories ────────────────────────────────────────────────────

    /**
     * Deep flow / intensive:
     *   KPM 90, zero errors, no context switches, recent clean build.
     *   Plugin would show: FLOW (high tally, low stress).
     */
    public static MockFlowMetrics intensive() {
        return new MockFlowMetrics.Builder()
            .kpm(90)
            .avgInterval(200)
            .backspaces(8)             // very low rework
            .idle(800)                 // barely any pause
            .syntaxErrors(0)
            .compileErrors(0)
            .timeSinceError(900_000)   // 15 min error-free streak
            .fileChanges(1)            // locked onto one file
            .timeInFile(1_200_000)     // 20 min continuous in same file
            .focusLoss(0)
            .buildSuccess(true)
            .failedBuilds(0)
            .timeSinceBuild(90_000)    // built 90s ago, passed
            .sessionDuration(5_400_000)
            .build();
    }

    /**
     * Intensive mid-session:
     *   KPM starting to dip from peak, still very focused but
     *   one small syntax error crept in.
     */
    public static MockFlowMetrics intensiveMidSession() {
        return new MockFlowMetrics.Builder()
            .kpm(72)
            .avgInterval(260)
            .backspaces(14)
            .idle(2_000)
            .syntaxErrors(1)
            .compileErrors(0)
            .timeSinceError(300_000)   // 5 min ago
            .fileChanges(2)
            .timeInFile(600_000)       // 10 min in file
            .focusLoss(1)
            .buildSuccess(true)
            .failedBuilds(0)
            .timeSinceBuild(300_000)
            .sessionDuration(7_200_000)
            .build();
    }

    /**
     * Average / normal coding:
     *   Moderate KPM, occasional errors and file switches.
     *   Plugin would show: NEUTRAL or low FLOW.
     */
    public static MockFlowMetrics average() {
        return new MockFlowMetrics.Builder()
            .kpm(50)
            .avgInterval(420)
            .backspaces(22)
            .idle(4_000)
            .syntaxErrors(2)
            .compileErrors(1)
            .timeSinceError(120_000)   // 2 min since error
            .fileChanges(4)
            .timeInFile(240_000)       // 4 min in file
            .focusLoss(2)
            .buildSuccess(true)
            .failedBuilds(0)
            .timeSinceBuild(600_000)
            .sessionDuration(5_400_000)
            .build();
    }

    /**
     * Average with build issues:
     *   Moderate pace, but the last build failed.
     *   Plugin would show: NEUTRAL (build penalty drags tally down).
     */
    public static MockFlowMetrics averageWithBuildIssues() {
        return new MockFlowMetrics.Builder()
            .kpm(44)
            .avgInterval(480)
            .backspaces(30)
            .idle(6_000)
            .syntaxErrors(3)           // at tolerance limit
            .compileErrors(2)
            .timeSinceError(60_000)
            .fileChanges(5)            // at tolerance limit
            .timeInFile(120_000)
            .focusLoss(3)
            .buildSuccess(false)       // failed build
            .failedBuilds(1)
            .timeSinceBuild(900_000)   // 15 min since last build
            .sessionDuration(5_400_000)
            .build();
    }

    /**
     * Warming up:
     *   First 5 minutes of the session. KPM still ramping up.
     *   Plugin would show: NEUTRAL (activity score low, session too short for bonus).
     */
    public static MockFlowMetrics warmingUp() {
        return new MockFlowMetrics.Builder()
            .kpm(35)
            .avgInterval(600)
            .backspaces(10)
            .idle(3_000)
            .syntaxErrors(1)
            .compileErrors(0)
            .timeSinceError(180_000)
            .fileChanges(3)
            .timeInFile(90_000)
            .focusLoss(1)
            .buildSuccess(true)
            .failedBuilds(0)
            .timeSinceBuild(1_200_000)
            .sessionDuration(300_000)  // 5 min session
            .build();
    }

    /**
     * Struggling:
     *   Low KPM, heavy rework, many errors, repeated build failures.
     *   Plugin would show: PROCRASTINATING (tally likely < 0.35).
     */
    public static MockFlowMetrics struggling() {
        return new MockFlowMetrics.Builder()
            .kpm(15)                   // below KPM_LOW threshold
            .avgInterval(1_500)
            .backspaces(45)            // very high rework
            .idle(50_000)              // 50s idle
            .syntaxErrors(6)           // well above tolerance
            .compileErrors(4)
            .timeSinceError(15_000)
            .fileChanges(9)            // scattered focus
            .timeInFile(25_000)
            .focusLoss(6)
            .buildSuccess(false)
            .failedBuilds(3)           // 3-build failure streak
            .timeSinceBuild(2_400_000) // 40 min without building
            .sessionDuration(7_200_000)
            .build();
    }

    // ── Ordered map of all scenarios ──────────────────────────────────────────

    /** Returns all profiles in engagement order (low → high). */
    public static Map<String, MockFlowMetrics> all() {
        Map<String, MockFlowMetrics> map = new LinkedHashMap<>();
        map.put("Struggling",              struggling());
        map.put("Warming Up",              warmingUp());
        map.put("Average",                 average());
        map.put("Average + Build Issues",  averageWithBuildIssues());
        map.put("Intensive (Mid-Session)", intensiveMidSession());
        map.put("Intensive (Peak Flow)",   intensive());
        return map;
    }
}
