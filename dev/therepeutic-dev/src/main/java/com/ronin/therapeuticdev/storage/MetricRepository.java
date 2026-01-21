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
 * SQLite persistence layer for flow metrics and detection results.
 * 
 * <p>Stores:
 * - FlowMetrics snapshots (raw data for analysis)
 * - FlowDetectionResults (calculated scores and states)
 * - Manual flow state labels (for user study validation)
 * 
 * <p>Database location: [IDE config dir]/therapeutic-dev/metrics.db
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
        
        String createIndexes = """
            CREATE INDEX IF NOT EXISTS idx_session ON snapshots(session_id);
            CREATE INDEX IF NOT EXISTS idx_timestamp ON snapshots(timestamp);
            CREATE INDEX IF NOT EXISTS idx_flow_state ON snapshots(flow_state);
            """;
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createSnapshotsTable);
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
            pstmt.setDouble(3, metrics.getKeystrokesPerMinute());
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
            pstmt.setLong(16, metrics.getSessionDuration());
            
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
