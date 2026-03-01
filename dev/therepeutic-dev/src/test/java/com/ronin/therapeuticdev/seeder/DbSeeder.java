package com.ronin.therapeuticdev.seeder;

import com.ronin.therapeuticdev.detection.FlowDetectionResult;
import com.ronin.therapeuticdev.detection.FlowDetector;
import com.ronin.therapeuticdev.metrics.FlowMetrics;

import java.io.File;
import java.sql.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Seeds the therapeutic-dev metrics database with simulated session data.
 *
 * Runs standalone (no IntelliJ platform required) — uses direct JDBC
 * and the same schema as MetricRepository so rows are immediately
 * readable by the live plugin.
 *
 * Uses the real FlowDetector and FlowMetrics so simulated flow_score /
 * stress_level / flow_state values are identical to what the plugin
 * would calculate from live data.
 *
 * Two sessions are generated:
 *
 *   Session A (yesterday, 50 min):
 *     0–5   min  Warming up
 *     5–15  min  Average pace
 *     15–30 min  Peak flow
 *     30–40 min  Debugging + build failures
 *     40–50 min  Recovery
 *
 *   Session B (today, 35 min):
 *     0–5   min  Warming up
 *     5–20  min  Average pace
 *     20–35 min  Peak flow
 *
 * Run via the Gradle task defined in build.gradle.kts:
 *   ./gradlew seedDb
 *
 * Or directly after compiling tests:
 *   java -cp <test-classes>;<sqlite-jdbc.jar> \
 *        com.ronin.therapeuticdev.seeder.DbSeeder
 */
public class DbSeeder {

    /**
     * Mirrors MetricRepository's path resolution:
     *   PathManager.getConfigPath() + "/therapeutic-dev/metrics.db"
     *
     * On Windows this is:
     *   %APPDATA%\JetBrains\IntelliJIdea<version>\therapeutic-dev\metrics.db
     */
    private static final String DB_PATH = resolveDbPath();

    private static String resolveDbPath() {
        // Try the most recent IntelliJ version first, fall back to 2025.1
        String[] candidates = {
            System.getenv("APPDATA") + "\\JetBrains\\IntelliJIdea2025.3\\therapeutic-dev\\metrics.db",
            System.getenv("APPDATA") + "\\JetBrains\\IntelliJIdea2025.1\\therapeutic-dev\\metrics.db",
        };
        for (String path : candidates) {
            File dir = new File(path).getParentFile();
            if (dir.exists() || dir.mkdirs()) return path;
        }
        // Fallback: project-local DB for CI / non-Windows environments
        return "build/therapeutic-dev-test/metrics.db";
    }

    // ── Entry point ───────────────────────────────────────────────────────────

    public static void main(String[] args) throws Exception {
        Class.forName("org.sqlite.JDBC");

        File dbFile = new File(DB_PATH);
        dbFile.getParentFile().mkdirs();
        boolean isNew = !dbFile.exists();

        System.out.println("DB   : " + DB_PATH);
        System.out.println("New  : " + isNew);

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH)) {
            conn.setAutoCommit(false);
            ensureSchema(conn);

            int a = seedSession(conn, "Session A — yesterday (50 min)",
                Instant.now().minus(1, ChronoUnit.DAYS).minus(50, ChronoUnit.MINUTES),
                buildSessionA());

            int b = seedSession(conn, "Session B — today (35 min)",
                Instant.now().minus(35, ChronoUnit.MINUTES),
                buildSessionB());

            conn.commit();

            System.out.println();
            System.out.printf("Inserted  : %d rows total (%d + %d)%n", a + b, a, b);
            System.out.println("Ready     : open the plugin tool window to see the data");
        }
    }

    // ── Session definitions ───────────────────────────────────────────────────

    private static SeedPoint[] buildSessionA() {
        return new SeedPoint[] {
            // 0–5 min: warming up
            point(0,  TestScenarios.warmingUp()),
            point(1,  TestScenarios.warmingUp()),
            point(2,  TestScenarios.warmingUp()),
            point(3,  TestScenarios.warmingUp()),
            point(4,  TestScenarios.warmingUp()),
            // 5–15 min: average pace
            point(5,  TestScenarios.average()),
            point(6,  TestScenarios.average()),
            point(7,  TestScenarios.average()),
            point(8,  TestScenarios.average()),
            point(9,  TestScenarios.average()),
            point(10, TestScenarios.average()),
            point(11, TestScenarios.average()),
            point(12, TestScenarios.average()),
            point(13, TestScenarios.average()),
            point(14, TestScenarios.average()),
            // 15–30 min: peak flow
            point(15, TestScenarios.intensiveMidSession()),
            point(16, TestScenarios.intensive()),
            point(17, TestScenarios.intensive()),
            point(18, TestScenarios.intensive()),
            point(19, TestScenarios.intensive()),
            point(20, TestScenarios.intensive()),
            point(21, TestScenarios.intensive()),
            point(22, TestScenarios.intensive()),
            point(23, TestScenarios.intensiveMidSession()),
            point(24, TestScenarios.intensiveMidSession()),
            point(25, TestScenarios.intensiveMidSession()),
            point(26, TestScenarios.intensiveMidSession()),
            point(27, TestScenarios.intensiveMidSession()),
            point(28, TestScenarios.intensiveMidSession()),
            point(29, TestScenarios.intensiveMidSession()),
            // 30–40 min: debugging + build failures
            point(30, TestScenarios.averageWithBuildIssues()),
            point(31, TestScenarios.averageWithBuildIssues()),
            point(32, TestScenarios.averageWithBuildIssues()),
            point(33, TestScenarios.struggling()),
            point(34, TestScenarios.struggling()),
            point(35, TestScenarios.struggling()),
            point(36, TestScenarios.struggling()),
            point(37, TestScenarios.averageWithBuildIssues()),
            point(38, TestScenarios.averageWithBuildIssues()),
            point(39, TestScenarios.average()),
            // 40–50 min: recovery
            point(40, TestScenarios.average()),
            point(41, TestScenarios.average()),
            point(42, TestScenarios.average()),
            point(43, TestScenarios.intensiveMidSession()),
            point(44, TestScenarios.intensiveMidSession()),
            point(45, TestScenarios.intensiveMidSession()),
            point(46, TestScenarios.intensiveMidSession()),
            point(47, TestScenarios.intensiveMidSession()),
            point(48, TestScenarios.intensiveMidSession()),
            point(49, TestScenarios.intensiveMidSession()),
        };
    }

    private static SeedPoint[] buildSessionB() {
        return new SeedPoint[] {
            // 0–5 min: warming up
            point(0,  TestScenarios.warmingUp()),
            point(1,  TestScenarios.warmingUp()),
            point(2,  TestScenarios.warmingUp()),
            point(3,  TestScenarios.warmingUp()),
            point(4,  TestScenarios.warmingUp()),
            // 5–20 min: average
            point(5,  TestScenarios.average()),
            point(6,  TestScenarios.average()),
            point(7,  TestScenarios.average()),
            point(8,  TestScenarios.average()),
            point(9,  TestScenarios.average()),
            point(10, TestScenarios.average()),
            point(11, TestScenarios.average()),
            point(12, TestScenarios.average()),
            point(13, TestScenarios.average()),
            point(14, TestScenarios.average()),
            point(15, TestScenarios.intensiveMidSession()),
            point(16, TestScenarios.intensiveMidSession()),
            point(17, TestScenarios.intensiveMidSession()),
            point(18, TestScenarios.intensiveMidSession()),
            point(19, TestScenarios.intensiveMidSession()),
            // 20–35 min: peak flow
            point(20, TestScenarios.intensive()),
            point(21, TestScenarios.intensive()),
            point(22, TestScenarios.intensive()),
            point(23, TestScenarios.intensive()),
            point(24, TestScenarios.intensive()),
            point(25, TestScenarios.intensive()),
            point(26, TestScenarios.intensive()),
            point(27, TestScenarios.intensive()),
            point(28, TestScenarios.intensive()),
            point(29, TestScenarios.intensive()),
            point(30, TestScenarios.intensive()),
            point(31, TestScenarios.intensive()),
            point(32, TestScenarios.intensive()),
            point(33, TestScenarios.intensive()),
            point(34, TestScenarios.intensive()),
        };
    }

    // ── Core seeding logic ────────────────────────────────────────────────────

    private static int seedSession(Connection conn, String label,
                                   Instant sessionStart,
                                   SeedPoint[] points) throws SQLException {
        String sessionId = UUID.randomUUID().toString();
        FlowDetector detector = new FlowDetector();

        System.out.println("\n  " + label + "  [" + sessionId.substring(0, 8) + "...]");

        String sql = """
            INSERT INTO snapshots (
                session_id, timestamp,
                keystrokes_per_minute, avg_key_interval_ms, backspace_count, keyboard_idle_ms,
                syntax_error_count, compilation_errors, time_since_last_error_ms,
                file_changes, time_in_current_file_ms, focus_loss_count,
                last_build_success, consecutive_failed_builds, time_since_last_build_ms,
                session_duration_ms, flow_score, stress_level, flow_state
            ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """;

        String prevState = null;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (SeedPoint pt : points) {
                Instant ts = sessionStart.plus(pt.minuteOffset(), ChronoUnit.MINUTES);

                // Stamp each snapshot with its position in the session
                FlowMetrics metrics = new FlowMetrics.Builder()
                    .sessionId(sessionId)
                    .timestamp(ts)
                    // copy all fields from the scenario template
                    .keystrokesPerMinute(pt.metrics().getKeystrokesPerMin())
                    .avgKeyIntervalMs(pt.metrics().getAvgKeyIntervalMs())
                    .backspaceCount(pt.metrics().getBackspaceCount())
                    .keyboardIdleMs(pt.metrics().getKeyboardIdleMs())
                    .syntaxErrorCount(pt.metrics().getSyntaxErrorCount())
                    .compilationErrors(pt.metrics().getCompilationErrors())
                    .timeSinceLastErrorMs(pt.metrics().getTimeSinceLastErrorMs())
                    .fileChangesLast5Min(pt.metrics().getFileChangesLast5Min())
                    .timeInCurrentFileMs(pt.metrics().getTimeInCurrentFileMs())
                    .focusLossCount(pt.metrics().getFocusLossCount())
                    .lastBuildSuccess(pt.metrics().isLastBuildSuccess())
                    .consecutiveFailedBuilds(pt.metrics().getConsecutiveFailedBuilds())
                    .timeSinceLastBuildMs(pt.metrics().getTimeSinceLastBuildMs())
                    .sessionDuration(pt.metrics().getSessionDurationMs())
                    .build();

                FlowDetectionResult result = detector.detect(metrics);

                ps.setString(1, sessionId);
                ps.setString(2, ts.toString());
                ps.setDouble(3, metrics.getKeystrokesPerMin());
                ps.setDouble(4, metrics.getAvgKeyIntervalMs());
                ps.setInt(5,    metrics.getBackspaceCount());
                ps.setLong(6,   metrics.getKeyboardIdleMs());
                ps.setInt(7,    metrics.getSyntaxErrorCount());
                ps.setInt(8,    metrics.getCompilationErrors());
                ps.setLong(9,   metrics.getTimeSinceLastErrorMs());
                ps.setInt(10,   metrics.getFileChangesLast5Min());
                ps.setLong(11,  metrics.getTimeInCurrentFileMs());
                ps.setInt(12,   metrics.getFocusLossCount());
                ps.setInt(13,   metrics.isLastBuildSuccess() ? 1 : 0);
                ps.setInt(14,   metrics.getConsecutiveFailedBuilds());
                ps.setLong(15,  metrics.getTimeSinceLastBuildMs());
                ps.setLong(16,  metrics.getSessionDurationMs());
                ps.setDouble(17, result.getFlowTally());
                ps.setDouble(18, result.getStressLevel());
                ps.setString(19, result.getState().name());
                ps.addBatch();

                String stateChange = (prevState != null && !prevState.equals(result.getState().name()))
                    ? "  <- " + prevState + " -> " + result.getState().name() : "";
                System.out.printf("    T+%2d min  KPM=%-3d  score=%.3f  %-15s%s%n",
                    pt.minuteOffset(), metrics.getKeystrokesPerMin(),
                    result.getFlowTally(), result.getState(), stateChange);
                prevState = result.getState().name();
            }
            ps.executeBatch();
        }
        return points.length;
    }

    // ── Schema ────────────────────────────────────────────────────────────────

    private static void ensureSchema(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS snapshots (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    session_id TEXT NOT NULL,
                    timestamp TEXT NOT NULL,
                    keystrokes_per_minute REAL,
                    avg_key_interval_ms REAL,
                    backspace_count INTEGER,
                    keyboard_idle_ms INTEGER,
                    syntax_error_count INTEGER,
                    compilation_errors INTEGER,
                    time_since_last_error_ms INTEGER,
                    file_changes INTEGER,
                    time_in_current_file_ms INTEGER,
                    focus_loss_count INTEGER,
                    last_build_success INTEGER,
                    consecutive_failed_builds INTEGER,
                    time_since_last_build_ms INTEGER,
                    session_duration_ms INTEGER,
                    flow_score REAL,
                    stress_level REAL,
                    flow_state TEXT,
                    manual_label TEXT,
                    created_at TEXT DEFAULT CURRENT_TIMESTAMP
                )
                """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_session   ON snapshots(session_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_timestamp ON snapshots(timestamp)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_state     ON snapshots(flow_state)");
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private record SeedPoint(FlowMetrics metrics, int minuteOffset) {}

    private static SeedPoint point(int minuteOffset, FlowMetrics metrics) {
        return new SeedPoint(metrics, minuteOffset);
    }
}
