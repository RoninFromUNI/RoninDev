package com.ronin.therapeuticdev.storage;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.ronin.therapeuticdev.detection.FlowDetectionResult;
import com.ronin.therapeuticdev.detection.FlowState;
import com.ronin.therapeuticdev.metrics.FlowMetrics;

import java.io.File;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * sqlite persistence layer — everything the plugin records ends up here.
 *
 * two tables:
 *   snapshots      — one row per persist cycle (every 60s), containing all five
 *                    sub-scores, composite tally, classified state, and raw metrics.
 *                    this is the primary data source for the dissertation analysis.
 *   esm_responses  — one row per ESM probe completion, containing the 7-item flow
 *                    state scale ratings and optional qualitative notes.
 *
 * the database lives in intellij's plugin config directory so it persists across
 * ide restarts but doesn't interfere with the user's project files. path is:
 *   [ide config]/therapeutic-dev/metrics.db
 *
 * i use WAL journal mode for better concurrent read performance — the live refresh
 * loop (every 2s) reads recent data for the sparkline while the persist cycle
 * (every 60s) writes new rows. WAL prevents these from blocking each other.
 *
 * all queries use PreparedStatement with parameterised values. even though this
 * is a local database with no untrusted input, parameterised queries are just
 * good practice and prevent any possibility of sql injection if the code is
 * ever reused in a different context.
 */
public class MetricRepository {

    private static final Logger LOG = Logger.getInstance(MetricRepository.class);
    private static final String DB_NAME = "metrics.db";

    private final String dbPath;
    private Connection connection;

    public MetricRepository() {
        File pluginDir = new File(PathManager.getConfigPath(), "therapeutic-dev");
        if (!pluginDir.exists()) {
            pluginDir.mkdirs();
        }
        this.dbPath = new File(pluginDir, DB_NAME).getAbsolutePath();

        initialize();
    }

    private void initialize() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            // WAL mode for concurrent read/write without blocking
            try (Statement s = connection.createStatement()) {
                s.execute("PRAGMA journal_mode=WAL");
            }
            createTables();
            LOG.info("MetricRepository initialized at: " + dbPath);
        } catch (Exception e) {
            LOG.error("Failed to initialize MetricRepository", e);
        }
    }

    /**
     * creates both tables and their indexes if they don't already exist.
     * the esm_responses table has a foreign key reference to snapshots(id) so
     * each self-report can be linked to the algorithmic classification that
     * triggered it — that pairing is the core of the convergent validity analysis.
     */
    private void createTables() throws SQLException {
        String createSnapshotsTable = """
            CREATE TABLE IF NOT EXISTS snapshots (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                session_id TEXT NOT NULL,
                timestamp TEXT NOT NULL,
                
                -- typing metrics
                keystrokes_per_minute REAL,
                avg_key_interval_ms REAL,
                backspace_count INTEGER,
                keyboard_idle_ms INTEGER,
                
                -- error metrics
                syntax_error_count INTEGER,
                compilation_errors INTEGER,
                time_since_last_error_ms INTEGER,
                
                -- focus metrics
                file_changes INTEGER,
                time_in_current_file_ms INTEGER,
                focus_loss_count INTEGER,
                
                -- build metrics
                last_build_success INTEGER,
                consecutive_failed_builds INTEGER,
                time_since_last_build_ms INTEGER,
                
                -- session
                session_duration_ms INTEGER,
                
                -- detection results (the algorithm's output)
                flow_score REAL,
                stress_level REAL,
                flow_state TEXT,
                
                -- manual label for validation (participant self-report)
                manual_label TEXT,
                
                created_at TEXT DEFAULT CURRENT_TIMESTAMP
            )
            """;

        String createEsmTable = """
            CREATE TABLE IF NOT EXISTS esm_responses (
                id                      INTEGER PRIMARY KEY AUTOINCREMENT,
                snapshot_id             INTEGER REFERENCES snapshots(id),
                session_id              TEXT NOT NULL,
                triggered_at            TEXT NOT NULL,
                responded_at            TEXT,
                -- 7-item flow state scale (jackson & marsh 1996), 1–5 likert each
                challenge_skill_balance INTEGER,
                action_awareness_merging INTEGER,
                clear_goals             INTEGER,
                unambiguous_feedback    INTEGER,
                concentration           INTEGER,
                sense_of_control        INTEGER,
                autotelic_experience    INTEGER,
                -- mean of non-null items
                composite_esm_score     REAL,
                -- optional free text from the participant
                qualitative_note        TEXT,
                -- ai context — was the participant using ai tools at probe time?
                using_ai_tools          INTEGER,
                ai_tool_name            TEXT,
                created_at              TEXT DEFAULT CURRENT_TIMESTAMP
            )
            """;

        String createIndexes = """
            CREATE INDEX IF NOT EXISTS idx_session ON snapshots(session_id);
            CREATE INDEX IF NOT EXISTS idx_timestamp ON snapshots(timestamp);
            CREATE INDEX IF NOT EXISTS idx_flow_state ON snapshots(flow_state);
            CREATE INDEX IF NOT EXISTS idx_esm_session ON esm_responses(session_id);
            CREATE INDEX IF NOT EXISTS idx_esm_snapshot ON esm_responses(snapshot_id);
            """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createSnapshotsTable);
            stmt.execute(createEsmTable);
            for (String index : createIndexes.split(";")) {
                if (!index.trim().isEmpty()) {
                    stmt.execute(index.trim());
                }
            }
        }
    }

    /**
     * persists a single snapshot:
     * called once per minute from SnapshotScheduler's
     * persist cycle. the session_id carries the needed participant prefix (e.g. P001_uuid)
     * so per-participant queries are pretty trivial.
     */
    public void saveSnapshot(FlowMetrics metrics, FlowDetectionResult result) {
        String sql = """
            INSERT INTO snapshots (
                session_id, timestamp,
                keystrokes_per_minute, avg_key_interval_ms, backspace_count, keyboard_idle_ms,
                syntax_error_count, compilation_errors, time_since_last_error_ms,
                file_changes, time_in_current_file_ms, focus_loss_count,
                last_build_success, consecutive_failed_builds, time_since_last_build_ms,
                session_duration_ms,
                flow_score, stress_level, flow_state
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, metrics.getSessionId());
            pstmt.setString(2, metrics.getTimestamp().toString());

            pstmt.setDouble(3, metrics.getKeystrokesPerMin());
            pstmt.setDouble(4, metrics.getAvgKeyIntervalMs());
            pstmt.setInt(5, metrics.getBackspaceCount());
            pstmt.setLong(6, metrics.getKeyboardIdleMs());

            pstmt.setInt(7, metrics.getSyntaxErrorCount());
            pstmt.setInt(8, metrics.getCompilationErrors());
            pstmt.setLong(9, metrics.getTimeSinceLastErrorMs());

            pstmt.setInt(10, metrics.getFileChangesLast5Min());
            pstmt.setLong(11, metrics.getTimeInCurrentFileMs());
            pstmt.setInt(12, metrics.getFocusLossCount());

            pstmt.setInt(13, metrics.isLastBuildSuccess() ? 1 : 0);
            pstmt.setInt(14, metrics.getConsecutiveFailedBuilds());
            pstmt.setLong(15, metrics.getTimeSinceLastBuildMs());

            pstmt.setLong(16, metrics.getSessionDurationMs());

            pstmt.setDouble(17, result.getFlowTally());
            pstmt.setDouble(18, result.getStressLevel());
            pstmt.setString(19, result.getState().name());

            pstmt.executeUpdate();

        } catch (SQLException e) {
            LOG.error("Failed to save snapshot", e);
        }
    }

    /**
     * updates the nearest snapshot with a participant's self-reported label.
     * uses JULIANDAY distance to find the closest snapshot in time to the
     * moment the participant indicated their state so its not an exact match
     * because the self-report and the snapshot won't land on the same millisecond.
     */
    public void saveManualLabel(String sessionId, Instant timestamp, String label) {
        String sql = """
            UPDATE snapshots 
            SET manual_label = ? 
            WHERE session_id = ? 
            AND timestamp = (
                SELECT timestamp FROM snapshots 
                WHERE session_id = ? 
                ORDER BY ABS(JULIANDAY(timestamp) - JULIANDAY(?)) 
                LIMIT 1
            )
            """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, label);
            pstmt.setString(2, sessionId);
            pstmt.setString(3, sessionId);
            pstmt.setString(4, timestamp.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOG.error("Failed to save manual label", e);
        }
    }

    public List<SnapshotRecord> getSessionSnapshots(String sessionId) {
        List<SnapshotRecord> records = new ArrayList<>();
        String sql = "SELECT * FROM snapshots WHERE session_id = ? ORDER BY timestamp";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, sessionId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                records.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            LOG.error("Failed to retrieve session snapshots", e);
        }

        return records;
    }

    public List<SnapshotRecord> getSnapshotsByTimeRange(Instant start, Instant end) {
        List<SnapshotRecord> records = new ArrayList<>();
        String sql = "SELECT * FROM snapshots WHERE timestamp BETWEEN ? AND ? ORDER BY timestamp";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, start.toString());
            pstmt.setString(2, end.toString());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                records.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            LOG.error("Failed to retrieve snapshots by time range", e);
        }

        return records;
    }

    /**
     * exports every snapshot as csv (which is so frickin needed) this is the study data extraction method.
     * called from the export button in TherapeuticDevConfigurable.
     * column order matches the snapshots table schema exactly.
     */
    public String exportToCsv() {
        StringBuilder csv = new StringBuilder();
        csv.append("session_id,timestamp,kpm,avg_interval,backspaces,idle_ms,");
        csv.append("syntax_errors,compilation_errors,time_since_error,");
        csv.append("file_changes,time_in_file,focus_loss,");
        csv.append("build_success,failed_builds,time_since_build,");
        csv.append("session_duration,flow_score,stress_level,flow_state,manual_label\n");

        String sql = "SELECT * FROM snapshots ORDER BY timestamp";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                csv.append(String.format("%s,%s,%.2f,%.2f,%d,%d,",
                        rs.getString("session_id"),
                        rs.getString("timestamp"),
                        rs.getDouble("keystrokes_per_minute"),
                        rs.getDouble("avg_key_interval_ms"),
                        rs.getInt("backspace_count"),
                        rs.getLong("keyboard_idle_ms")));

                csv.append(String.format("%d,%d,%d,",
                        rs.getInt("syntax_error_count"),
                        rs.getInt("compilation_errors"),
                        rs.getLong("time_since_last_error_ms")));

                csv.append(String.format("%d,%d,%d,",
                        rs.getInt("file_changes"),
                        rs.getLong("time_in_current_file_ms"),
                        rs.getInt("focus_loss_count")));

                csv.append(String.format("%d,%d,%d,",
                        rs.getInt("last_build_success"),
                        rs.getInt("consecutive_failed_builds"),
                        rs.getLong("time_since_last_build_ms")));

                csv.append(String.format("%d,%.4f,%.4f,%s,%s\n",
                        rs.getLong("session_duration_ms"),
                        rs.getDouble("flow_score"),
                        rs.getDouble("stress_level"),
                        rs.getString("flow_state"),
                        rs.getString("manual_label") != null ? rs.getString("manual_label") : ""));
            }
        } catch (SQLException e) {
            LOG.error("Failed to export to CSV", e);
        }

        return csv.toString();
    }

    public String exportEsmToCsv() {
        StringBuilder csv = new StringBuilder();
        csv.append("session_id,snapshot_id,triggered_at,responded_at,");
        csv.append("challenge_skill_balance,action_awareness_merging,clear_goals,");
        csv.append("unambiguous_feedback,concentration,sense_of_control,");
        csv.append("autotelic_experience,composite_esm_score,");
        csv.append("qualitative_note,using_ai_tools,ai_tool_name\n");

        String sql = "SELECT * FROM esm_responses ORDER BY triggered_at";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                csv.append(String.format("%s,%d,%s,%s,",
                        rs.getString("session_id"),
                        rs.getLong("snapshot_id"),
                        rs.getString("triggered_at"),
                        rs.getString("responded_at") != null ? rs.getString("responded_at") : ""));

                csv.append(String.format("%s,%s,%s,%s,%s,%s,%s,",
                        formatNullableInt(rs, "challenge_skill_balance"),
                        formatNullableInt(rs, "action_awareness_merging"),
                        formatNullableInt(rs, "clear_goals"),
                        formatNullableInt(rs, "unambiguous_feedback"),
                        formatNullableInt(rs, "concentration"),
                        formatNullableInt(rs, "sense_of_control"),
                        formatNullableInt(rs, "autotelic_experience")));

                double comp = rs.getDouble("composite_esm_score");
                csv.append(rs.wasNull() ? "," : String.format("%.2f,", comp));

                String note = rs.getString("qualitative_note");
                csv.append(note != null
                        ? "\"" + note.replace("\"", "\"\"").replace("\n", " ").replace("\r", "") + "\""
                        : "");
                csv.append(",");

                csv.append(String.format("%d,%s\n",
                        rs.getInt("using_ai_tools"),
                        rs.getString("ai_tool_name") != null ? rs.getString("ai_tool_name") : ""));
            }
        } catch (SQLException e) {
            LOG.error("Failed to export ESM responses to CSV", e);
        }

        return csv.toString();
    }

    private String formatNullableInt(ResultSet rs, String column) throws SQLException {
        int val = rs.getInt(column);
        return rs.wasNull() ? "" : String.valueOf(val);
    }

    private SnapshotRecord mapResultSet(ResultSet rs) throws SQLException {
        return new SnapshotRecord(
                rs.getInt("id"),
                rs.getString("session_id"),
                Instant.parse(rs.getString("timestamp")),
                rs.getDouble("flow_score"),
                rs.getDouble("stress_level"),
                FlowState.valueOf(rs.getString("flow_state")),
                rs.getString("manual_label")
        );
    }

    /**
     * saves an esm probe response, linking it to the most recent snapshot for
     * this session. the link lets me pair the participant's subjective rating
     * with the algorithm's classification at the same moment in time.
     */
    public long saveEsmResponse(EsmResponse r) {
        long snapshotId = -1;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT id FROM snapshots WHERE session_id = ? ORDER BY timestamp DESC LIMIT 1")) {
            ps.setString(1, r.sessionId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) snapshotId = rs.getLong("id");
        } catch (SQLException e) {
            LOG.warn("Could not resolve snapshot id for ESM response: " + e.getMessage());
        }

        String sql = """
            INSERT INTO esm_responses (
                snapshot_id, session_id, triggered_at, responded_at,
                challenge_skill_balance, action_awareness_merging, clear_goals,
                unambiguous_feedback, concentration, sense_of_control,
                autotelic_experience, composite_esm_score,
                qualitative_note, using_ai_tools, ai_tool_name
            ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """;
        try (PreparedStatement pstmt = connection.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setLong(1, snapshotId);
            pstmt.setString(2, r.sessionId());
            pstmt.setString(3, r.triggeredAt().toString());
            pstmt.setString(4, r.respondedAt() != null ? r.respondedAt().toString() : null);
            pstmt.setObject(5, r.challengeSkillBalance());
            pstmt.setObject(6, r.actionAwarenessMerging());
            pstmt.setObject(7, r.clearGoals());
            pstmt.setObject(8, r.unambiguousFeedback());
            pstmt.setObject(9, r.concentration());
            pstmt.setObject(10, r.senseOfControl());
            pstmt.setObject(11, r.autotelicExperience());
            pstmt.setObject(12, r.compositeEsmScore());
            pstmt.setString(13, r.qualitativeNote());
            pstmt.setObject(14, r.usingAiTools() ? 1 : 0);
            pstmt.setString(15, r.aiToolName());
            pstmt.executeUpdate();
//            ResultSet keys = pstmt.getGeneratedKeys();
//            return keys.next() ? keys.getLong(1) : -1;
            // yeah having errors with using this so going back to another native equivaent
            // which is last insert row id
            //this shouldnt give any implenetation error this time around.
            try (Statement rowIdStmt = connection.createStatement();
                 ResultSet keys = rowIdStmt.executeQuery("SELECT last_insert_rowid()")) {
                return keys.next() ? keys.getLong(1) : -1;
            }
        } catch (SQLException e) {
            LOG.error("Failed to save ESM response", e);
            return -1;
        }
    }

    public List<EsmResponse> getEsmResponses(String sessionId) {
        List<EsmResponse> result = new ArrayList<>();
        String sql = "SELECT * FROM esm_responses WHERE session_id = ? ORDER BY triggered_at";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, sessionId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result.add(new EsmResponse(
                    rs.getString("session_id"),
                    Instant.parse(rs.getString("triggered_at")),
                    rs.getString("responded_at") != null ? Instant.parse(rs.getString("responded_at")) : null,
                    rs.getObject("challenge_skill_balance", Integer.class),
                    rs.getObject("action_awareness_merging", Integer.class),
                    rs.getObject("clear_goals", Integer.class),
                    rs.getObject("unambiguous_feedback", Integer.class),
                    rs.getObject("concentration", Integer.class),
                    rs.getObject("sense_of_control", Integer.class),
                    rs.getObject("autotelic_experience", Integer.class),
                    rs.getObject("composite_esm_score", Double.class),
                    rs.getString("qualitative_note"),
                    rs.getInt("using_ai_tools") == 1,
                    rs.getString("ai_tool_name")
                ));
            }
        } catch (SQLException e) {
            LOG.error("Failed to retrieve ESM responses", e);
        }
        return result;
    }

    // ==================== RECORDS ====================

    /**
     * esm probe response record — 7 likert items from the adapted flow state scale,
     * plus ai usage context and optional qualitative note. all likert items are
     * nullable Integer to allow partial responses (participant can skip items).
     */
    public record EsmResponse(
        String  sessionId,
        Instant triggeredAt,
        Instant respondedAt,
        Integer challengeSkillBalance,
        Integer actionAwarenessMerging,
        Integer clearGoals,
        Integer unambiguousFeedback,
        Integer concentration,
        Integer senseOfControl,
        Integer autotelicExperience,
        Double  compositeEsmScore,
        String  qualitativeNote,
        boolean usingAiTools,
        String  aiToolName
    ) {}

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                LOG.info("MetricRepository closed");
            }
        } catch (SQLException e) {
            LOG.error("Error closing database connection", e);
        }
    }

    public record SnapshotRecord(
            int id,
            String sessionId,
            Instant timestamp,
            double flowScore,
            double stressLevel,
            FlowState state,
            String manualLabel
    ) {}
}
