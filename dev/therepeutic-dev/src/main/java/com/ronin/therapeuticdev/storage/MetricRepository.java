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
import java.util.UUID;

/**
 * SQLite persistence layer for flow metrics and detection results.
 * 
 * Stores:
 * - FlowMetrics snapshots (raw data for analysis)
 * - FlowDetectionResults (calculated scores and states)
 * - Manual flow state labels (for user study validation)
 * 
 * Database location: [IDE config dir]/therapeutic-dev/metrics.db
 *
 * @see <a href="https://www.sqlite.org/lang.html">SQLite Documentation</a>
 */
public class MetricRepository {

    private static final Logger LOG = Logger.getInstance(MetricRepository.class);
    private static final String DB_NAME = "metrics.db";
    
    private final String dbPath;
    private Connection connection;

    public MetricRepository() {
        // Store in IDE's plugin config directory
        File pluginDir = new File(PathManager.getConfigPath(), "therapeutic-dev");
        if (!pluginDir.exists()) {
            pluginDir.mkdirs();
        }
        this.dbPath = new File(pluginDir, DB_NAME).getAbsolutePath();
        
        initialize();
    }

    /**
     * Initializes database connection and creates tables if needed.
     */
    private void initialize() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            // WAL mode: better concurrent read performance, no extra library needed
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
     * Creates database tables if they don't exist.
     */
    private void createTables() throws SQLException {
        String createSnapshotsTable = """
            CREATE TABLE IF NOT EXISTS snapshots (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                session_id TEXT NOT NULL,
                timestamp TEXT NOT NULL,
                
                -- Typing metrics
                keystrokes_per_minute REAL,
                avg_key_interval_ms REAL,
                backspace_count INTEGER,
                keyboard_idle_ms INTEGER,
                
                -- Error metrics
                syntax_error_count INTEGER,
                compilation_errors INTEGER,
                time_since_last_error_ms INTEGER,
                
                -- Focus metrics
                file_changes INTEGER,
                time_in_current_file_ms INTEGER,
                focus_loss_count INTEGER,
                
                -- Build metrics
                last_build_success INTEGER,
                consecutive_failed_builds INTEGER,
                time_since_last_build_ms INTEGER,
                
                -- Session
                session_duration_ms INTEGER,
                
                -- Detection results
                flow_score REAL,
                stress_level REAL,
                flow_state TEXT,
                
                -- Manual label (for user study)
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
                -- 7-item Flow State Scale adapted for software development (1–5 Likert)
                challenge_skill_balance INTEGER,
                action_awareness_merging INTEGER,
                clear_goals             INTEGER,
                unambiguous_feedback    INTEGER,
                concentration           INTEGER,
                sense_of_control        INTEGER,
                autotelic_experience    INTEGER,
                -- Composite
                composite_esm_score     REAL,
                -- Open text
                qualitative_note        TEXT,
                -- AI context
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
     * Saves a metrics snapshot with its detection result.
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
            
            // Typing
            pstmt.setDouble(3, metrics.getKeystrokesPerMin());
            pstmt.setDouble(4, metrics.getAvgKeyIntervalMs());
            pstmt.setInt(5, metrics.getBackspaceCount());
            pstmt.setLong(6, metrics.getKeyboardIdleMs());
            
            // Errors
            pstmt.setInt(7, metrics.getSyntaxErrorCount());
            pstmt.setInt(8, metrics.getCompilationErrors());
            pstmt.setLong(9, metrics.getTimeSinceLastErrorMs());
            
            // Focus
            pstmt.setInt(10, metrics.getFileChangesLast5Min());
            pstmt.setLong(11, metrics.getTimeInCurrentFileMs());
            pstmt.setInt(12, metrics.getFocusLossCount());
            
            // Builds
            pstmt.setInt(13, metrics.isLastBuildSuccess() ? 1 : 0);
            pstmt.setInt(14, metrics.getConsecutiveFailedBuilds());
            pstmt.setLong(15, metrics.getTimeSinceLastBuildMs());
            
            // Session
            pstmt.setLong(16, metrics.getSessionDurationMs());
            
            // Detection results
            pstmt.setDouble(17, result.getFlowTally());
            pstmt.setDouble(18, result.getStressLevel());
            pstmt.setString(19, result.getState().name());
            
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            LOG.error("Failed to save snapshot", e);
        }
    }

    /**
     * Saves a manual flow state label for validation.
     * Used during user study when participant indicates their perceived state.
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

    /**
     * Retrieves snapshots for a session.
     */
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

    /**
     * Retrieves all snapshots within a time range.
     */
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
     * Exports all data to CSV for analysis.
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

    /**
     * Maps a ResultSet row to a SnapshotRecord.
     */
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
     * Saves an ESM probe response, linking it to the most recent snapshot
     * for the given session.
     *
     * @return the id of the inserted row, or -1 on failure
     */
    public long saveEsmResponse(EsmResponse r) {
        // Resolve the latest snapshot id for this session
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
            ResultSet keys = pstmt.getGeneratedKeys();
            return keys.next() ? keys.getLong(1) : -1;
        } catch (SQLException e) {
            LOG.error("Failed to save ESM response", e);
            return -1;
        }
    }

    /**
     * Retrieves all ESM responses for a session, ordered chronologically.
     */
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
     * ESM probe response — 7-item Flow State Scale adapted for software development.
     * All Likert items are Integer (nullable) to allow partial responses.
     */
    public record EsmResponse(
        String  sessionId,
        Instant triggeredAt,
        Instant respondedAt,
        Integer challengeSkillBalance,   // "Challenge matches my skill level"
        Integer actionAwarenessMerging,  // "Coding just happens — not thinking about how"
        Integer clearGoals,              // "I know what I want to accomplish next"
        Integer unambiguousFeedback,     // "I can tell whether I am coding well"
        Integer concentration,           // "I am completely focused"
        Integer senseOfControl,          // "I feel in control of what I am coding"
        Integer autotelicExperience,     // "I am doing this for the enjoyment of programming"
        Double  compositeEsmScore,       // mean of non-null items, computed by dialog
        String  qualitativeNote,
        boolean usingAiTools,
        String  aiToolName
    ) {}

    /**
     * Closes the database connection.
     */
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

    /**
     * Simple record for snapshot query results.
     */
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
