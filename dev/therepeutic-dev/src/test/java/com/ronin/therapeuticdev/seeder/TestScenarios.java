package com.ronin.therapeuticdev.seeder;

import com.ronin.therapeuticdev.metrics.FlowMetrics;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Preset FlowMetrics snapshots representing the range of developer
 * states that therapeutic-dev detects.
 *
 * Used by DbSeeder to populate the metrics database with realistic
 * test data without needing a live coding session.
 *
 * Each scenario reflects what MetricCollector.snapshot() would
 * return during that kind of session.
 */
public class TestScenarios {

    /**
     * Peak deep-work flow: high KPM, zero errors, no context
     * switches, recent clean build, sustained time in one file.
     */
    public static FlowMetrics intensive() {
        return base()
            .keystrokesPerMinute(90)
            .avgKeyIntervalMs(200)
            .backspaceCount(8)
            .keyboardIdleMs(800)
            .syntaxErrorCount(0)
            .compilationErrors(0)
            .timeSinceLastErrorMs(900_000)   // 15 min error-free
            .fileChangesLast5Min(1)
            .timeInCurrentFileMs(1_200_000)  // 20 min in same file
            .focusLossCount(0)
            .lastBuildSuccess(true)
            .consecutiveFailedBuilds(0)
            .timeSinceLastBuildMs(90_000)    // built 90s ago
            .sessionDuration(5_400_000)
            .build();
    }

    /**
     * Intensive mid-session: past peak flow, KPM slightly waning,
     * one small syntax error crept in.
     */
    public static FlowMetrics intensiveMidSession() {
        return base()
            .keystrokesPerMinute(72)
            .avgKeyIntervalMs(260)
            .backspaceCount(14)
            .keyboardIdleMs(2_000)
            .syntaxErrorCount(1)
            .compilationErrors(0)
            .timeSinceLastErrorMs(300_000)
            .fileChangesLast5Min(2)
            .timeInCurrentFileMs(600_000)    // 10 min in file
            .focusLossCount(1)
            .lastBuildSuccess(true)
            .consecutiveFailedBuilds(0)
            .timeSinceLastBuildMs(300_000)
            .sessionDuration(7_200_000)
            .build();
    }

    /**
     * Average / normal coding: moderate KPM, occasional errors
     * and file switches.
     */
    public static FlowMetrics average() {
        return base()
            .keystrokesPerMinute(50)
            .avgKeyIntervalMs(420)
            .backspaceCount(22)
            .keyboardIdleMs(4_000)
            .syntaxErrorCount(2)
            .compilationErrors(1)
            .timeSinceLastErrorMs(120_000)
            .fileChangesLast5Min(4)
            .timeInCurrentFileMs(240_000)    // 4 min in file
            .focusLossCount(2)
            .lastBuildSuccess(true)
            .consecutiveFailedBuilds(0)
            .timeSinceLastBuildMs(600_000)
            .sessionDuration(5_400_000)
            .build();
    }

    /**
     * Average coding with a failing build: moderate pace but
     * last build failed and errors are at tolerance limit.
     */
    public static FlowMetrics averageWithBuildIssues() {
        return base()
            .keystrokesPerMinute(44)
            .avgKeyIntervalMs(480)
            .backspaceCount(30)
            .keyboardIdleMs(6_000)
            .syntaxErrorCount(3)
            .compilationErrors(2)
            .timeSinceLastErrorMs(60_000)
            .fileChangesLast5Min(5)
            .timeInCurrentFileMs(120_000)
            .focusLossCount(3)
            .lastBuildSuccess(false)
            .consecutiveFailedBuilds(1)
            .timeSinceLastBuildMs(900_000)
            .sessionDuration(5_400_000)
            .build();
    }

    /**
     * Warming up: first 5 minutes of a session, KPM still ramping.
     */
    public static FlowMetrics warmingUp() {
        return base()
            .keystrokesPerMinute(35)
            .avgKeyIntervalMs(600)
            .backspaceCount(10)
            .keyboardIdleMs(3_000)
            .syntaxErrorCount(1)
            .compilationErrors(0)
            .timeSinceLastErrorMs(180_000)
            .fileChangesLast5Min(3)
            .timeInCurrentFileMs(90_000)
            .focusLossCount(1)
            .lastBuildSuccess(true)
            .consecutiveFailedBuilds(0)
            .timeSinceLastBuildMs(1_200_000)
            .sessionDuration(300_000)        // 5 min session
            .build();
    }

    /**
     * Struggling: very low KPM, heavy rework, many errors,
     * repeated build failures, scattered focus.
     */
    public static FlowMetrics struggling() {
        return base()
            .keystrokesPerMinute(15)
            .avgKeyIntervalMs(1_500)
            .backspaceCount(45)
            .keyboardIdleMs(50_000)          // 50s idle
            .syntaxErrorCount(6)
            .compilationErrors(4)
            .timeSinceLastErrorMs(15_000)
            .fileChangesLast5Min(9)
            .timeInCurrentFileMs(25_000)
            .focusLossCount(6)
            .lastBuildSuccess(false)
            .consecutiveFailedBuilds(3)
            .timeSinceLastBuildMs(2_400_000) // 40 min without building
            .sessionDuration(7_200_000)
            .build();
    }

    /** Returns all scenarios in engagement order (low → high). */
    public static Map<String, FlowMetrics> all() {
        Map<String, FlowMetrics> map = new LinkedHashMap<>();
        map.put("Struggling",              struggling());
        map.put("Warming Up",              warmingUp());
        map.put("Average",                 average());
        map.put("Average + Build Issues",  averageWithBuildIssues());
        map.put("Intensive (Mid-Session)", intensiveMidSession());
        map.put("Intensive (Peak Flow)",   intensive());
        return map;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Shared builder base — timestamp and sessionId are set by the seeder. */
    private static FlowMetrics.Builder base() {
        return new FlowMetrics.Builder();
    }
}
