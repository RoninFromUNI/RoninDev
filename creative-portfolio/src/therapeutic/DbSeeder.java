package therapeutic;

import java.io.File;
import java.sql.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Seeds the therapeutic-dev metrics.db with simulated session data.
 *
 * Generates two realistic coding sessions:
 *
 *   Session A (yesterday, 50 min):
 *     0–5   min  Warming up
 *     5–15  min  Average pace
 *     15–30 min  Deep flow
 *     30–40 min  Debugging, build failures
 *     40–50 min  Recovering
 *
 *   Session B (today, 35 min):
 *     0–5   min  Warming up
 *     5–20  min  Average pace
 *     20–35 min  Peak flow
 *
 * Each row = one persist-cycle snapshot (60 second interval).
 * Rows are written directly to the DB so the test harness and
 * live data loader can read them immediately without needing to
 * run the plugin for real.
 *
 * Compile:
 *   javac -encoding UTF-8 -cp "out;%USERPROFILE%\.m2\repository\org\xerial\sqlite-jdbc\3.44.1.0\sqlite-jdbc-3.44.1.0.jar" -d out src/therapeutic/DbSeeder.java
 *
 * Run:
 *   java -Dfile.encoding=UTF-8 -cp "out;%USERPROFILE%\.m2\repository\org\xerial\sqlite-jdbc\3.44.1.0\sqlite-jdbc-3.44.1.0.jar" therapeutic.DbSeeder
 */
public class DbSeeder {

    // ── DB path: matches MetricRepository's PathManager.getConfigPath() location ──
    private static final String DB_PATH =
        System.getProperty("user.home")
        + "\\AppData\\Roaming\\JetBrains\\IntelliJIdea2025.3\\therapeutic-dev\\metrics.db";

    public static void main(String[] args) throws Exception {
        Class.forName("org.sqlite.JDBC");

        File dbFile = new File(DB_PATH);
        dbFile.getParentFile().mkdirs();

        System.out.println("DB path : " + DB_PATH);
        System.out.println("Exists  : " + dbFile.exists());

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH)) {
            conn.setAutoCommit(false);
            createTable(conn);

            int sessionA = seedSession(conn, "Session A (yesterday)",
                Instant.now().minus(1, ChronoUnit.DAYS).minus(50, ChronoUnit.MINUTES),
                buildSessionA());

            int sessionB = seedSession(conn, "Session B (today)",
                Instant.now().minus(35, ChronoUnit.MINUTES),
                buildSessionB());

            conn.commit();
            System.out.println();
            System.out.println("Done. Rows inserted: " + (sessionA + sessionB));
            System.out.println("Session A: " + sessionA + " rows");
            System.out.println("Session B: " + sessionB + " rows");
        }
    }

    // ── Session definitions ───────────────────────────────────────────────────

    /** 50-minute session: warmup → average → flow → debugging → recovery */
    private static SeedPoint[] buildSessionA() {
        return new SeedPoint[] {
            // Warming up (0–5 min)
            new SeedPoint(CodingScenarios.warmingUp(),           0),
            new SeedPoint(CodingScenarios.warmingUp(),           1),
            new SeedPoint(CodingScenarios.warmingUp(),           2),
            new SeedPoint(CodingScenarios.warmingUp(),           3),
            new SeedPoint(CodingScenarios.warmingUp(),           4),
            // Average pace (5–15 min)
            new SeedPoint(CodingScenarios.average(),             5),
            new SeedPoint(CodingScenarios.average(),             6),
            new SeedPoint(CodingScenarios.average(),             7),
            new SeedPoint(CodingScenarios.average(),             8),
            new SeedPoint(CodingScenarios.average(),             9),
            new SeedPoint(CodingScenarios.average(),            10),
            new SeedPoint(CodingScenarios.average(),            11),
            new SeedPoint(CodingScenarios.average(),            12),
            new SeedPoint(CodingScenarios.average(),            13),
            new SeedPoint(CodingScenarios.average(),            14),
            // Deep flow (15–30 min)
            new SeedPoint(CodingScenarios.intensiveMidSession(), 15),
            new SeedPoint(CodingScenarios.intensive(),          16),
            new SeedPoint(CodingScenarios.intensive(),          17),
            new SeedPoint(CodingScenarios.intensive(),          18),
            new SeedPoint(CodingScenarios.intensive(),          19),
            new SeedPoint(CodingScenarios.intensive(),          20),
            new SeedPoint(CodingScenarios.intensive(),          21),
            new SeedPoint(CodingScenarios.intensive(),          22),
            new SeedPoint(CodingScenarios.intensiveMidSession(), 23),
            new SeedPoint(CodingScenarios.intensiveMidSession(), 24),
            new SeedPoint(CodingScenarios.intensiveMidSession(), 25),
            new SeedPoint(CodingScenarios.intensiveMidSession(), 26),
            new SeedPoint(CodingScenarios.intensiveMidSession(), 27),
            new SeedPoint(CodingScenarios.intensiveMidSession(), 28),
            new SeedPoint(CodingScenarios.intensiveMidSession(), 29),
            // Debugging + build failures (30–40 min)
            new SeedPoint(CodingScenarios.averageWithBuildIssues(), 30),
            new SeedPoint(CodingScenarios.averageWithBuildIssues(), 31),
            new SeedPoint(CodingScenarios.averageWithBuildIssues(), 32),
            new SeedPoint(CodingScenarios.struggling(),          33),
            new SeedPoint(CodingScenarios.struggling(),          34),
            new SeedPoint(CodingScenarios.struggling(),          35),
            new SeedPoint(CodingScenarios.struggling(),          36),
            new SeedPoint(CodingScenarios.averageWithBuildIssues(), 37),
            new SeedPoint(CodingScenarios.averageWithBuildIssues(), 38),
            new SeedPoint(CodingScenarios.average(),             39),
            // Recovery (40–50 min)
            new SeedPoint(CodingScenarios.average(),             40),
            new SeedPoint(CodingScenarios.average(),             41),
            new SeedPoint(CodingScenarios.average(),             42),
            new SeedPoint(CodingScenarios.intensiveMidSession(), 43),
            new SeedPoint(CodingScenarios.intensiveMidSession(), 44),
            new SeedPoint(CodingScenarios.intensiveMidSession(), 45),
            new SeedPoint(CodingScenarios.intensiveMidSession(), 46),
            new SeedPoint(CodingScenarios.intensiveMidSession(), 47),
            new SeedPoint(CodingScenarios.intensiveMidSession(), 48),
            new SeedPoint(CodingScenarios.intensiveMidSession(), 49),
        };
    }

    /** 35-minute session: warmup → average → peak flow */
    private static SeedPoint[] buildSessionB() {
        return new SeedPoint[] {
            // Warming up (0–5 min)
            new SeedPoint(CodingScenarios.warmingUp(),           0),
            new SeedPoint(CodingScenarios.warmingUp(),           1),
            new SeedPoint(CodingScenarios.warmingUp(),           2),
            new SeedPoint(CodingScenarios.warmingUp(),           3),
            new SeedPoint(CodingScenarios.warmingUp(),           4),
            // Average (5–20 min)
            new SeedPoint(CodingScenarios.average(),             5),
            new SeedPoint(CodingScenarios.average(),             6),
            new SeedPoint(CodingScenarios.average(),             7),
            new SeedPoint(CodingScenarios.average(),             8),
            new SeedPoint(CodingScenarios.average(),             9),
            new SeedPoint(CodingScenarios.average(),            10),
            new SeedPoint(CodingScenarios.average(),            11),
            new SeedPoint(CodingScenarios.average(),            12),
            new SeedPoint(CodingScenarios.average(),            13),
            new SeedPoint(CodingScenarios.average(),            14),
            new SeedPoint(CodingScenarios.intensiveMidSession(), 15),
            new SeedPoint(CodingScenarios.intensiveMidSession(), 16),
            new SeedPoint(CodingScenarios.intensiveMidSession(), 17),
            new SeedPoint(CodingScenarios.intensiveMidSession(), 18),
            new SeedPoint(CodingScenarios.intensiveMidSession(), 19),
            // Peak flow (20–35 min)
            new SeedPoint(CodingScenarios.intensive(),          20),
            new SeedPoint(CodingScenarios.intensive(),          21),
            new SeedPoint(CodingScenarios.intensive(),          22),
            new SeedPoint(CodingScenarios.intensive(),          23),
            new SeedPoint(CodingScenarios.intensive(),          24),
            new SeedPoint(CodingScenarios.intensive(),          25),
            new SeedPoint(CodingScenarios.intensive(),          26),
            new SeedPoint(CodingScenarios.intensive(),          27),
            new SeedPoint(CodingScenarios.intensive(),          28),
            new SeedPoint(CodingScenarios.intensive(),          29),
            new SeedPoint(CodingScenarios.intensive(),          30),
            new SeedPoint(CodingScenarios.intensive(),          31),
            new SeedPoint(CodingScenarios.intensive(),          32),
            new SeedPoint(CodingScenarios.intensive(),          33),
            new SeedPoint(CodingScenarios.intensive(),          34),
        };
    }

    // ── DB helpers ────────────────────────────────────────────────────────────

    private static int seedSession(Connection conn, String label,
                                   Instant sessionStart,
                                   SeedPoint[] points) throws SQLException {
        String sessionId = UUID.randomUUID().toString();
        System.out.println("\n  Seeding: " + label + "  (session=" + sessionId.substring(0, 8) + "...)");

        String sql = """
            INSERT INTO snapshots (
                session_id, timestamp,
                keystrokes_per_minute, avg_key_interval_ms, backspace_count, keyboard_idle_ms,
                syntax_error_count, compilation_errors, time_since_last_error_ms,
                file_changes, time_in_current_file_ms, focus_loss_count,
                last_build_success, consecutive_failed_builds, time_since_last_build_ms,
                session_duration_ms,
                flow_score, stress_level, flow_state
            ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """;

        String prevState = null;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (SeedPoint pt : points) {
                FlowSimulator.DetectionResult r = FlowSimulator.detect(pt.metrics);
                Instant ts = sessionStart.plus(pt.minuteOffset, ChronoUnit.MINUTES);

                ps.setString(1, sessionId);
                ps.setString(2, ts.toString());
                ps.setDouble(3, pt.metrics.keystrokesPerMin);
                ps.setDouble(4, pt.metrics.avgKeyIntervalMs);
                ps.setInt(5, pt.metrics.backspaceCount);
                ps.setLong(6, pt.metrics.keyboardIdleMs);
                ps.setInt(7, pt.metrics.syntaxErrorCount);
                ps.setInt(8, pt.metrics.compilationErrors);
                ps.setLong(9, pt.metrics.timeSinceLastErrorMs);
                ps.setInt(10, pt.metrics.fileChangesLast5Min);
                ps.setLong(11, pt.metrics.timeInCurrentFileMs);
                ps.setInt(12, pt.metrics.focusLossCount);
                ps.setInt(13, pt.metrics.lastBuildSuccess ? 1 : 0);
                ps.setInt(14, pt.metrics.consecutiveFailedBuilds);
                ps.setLong(15, pt.metrics.timeSinceLastBuildMs);
                ps.setLong(16, pt.metrics.sessionDurationMs);
                ps.setDouble(17, r.flowTally);
                ps.setDouble(18, r.stressLevel);
                ps.setString(19, r.state);
                ps.addBatch();

                String change = (prevState != null && !prevState.equals(r.state))
                    ? "  ← " + prevState + " → " + r.state : "";
                System.out.printf("    T+%2d min  KPM=%-3d  tally=%.3f  %-15s%s%n",
                    pt.minuteOffset, pt.metrics.keystrokesPerMin, r.flowTally, r.state, change);
                prevState = r.state;
            }
            ps.executeBatch();
        }
        return points.length;
    }

    private static void createTable(Connection conn) throws SQLException {
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
        System.out.println("Table ready.");
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private record SeedPoint(MockFlowMetrics metrics, int minuteOffset) {}
}
