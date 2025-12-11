package com.dvsachecker.repository;

import com.dvsachecker.config.AppConfig;
import com.dvsachecker.model.Slot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StateRepository {
    private static final Logger logger = LoggerFactory.getLogger(StateRepository.class);
    private final String databaseUrl;

    public StateRepository() {
        String dbPath = AppConfig.getInstance().getDatabasePath();
        this.databaseUrl = "jdbc:sqlite:" + dbPath;
        initializeDatabase();
    }

    /**
     * Creates database schema if not exists.
     * Table: slots (hash, test_center, date_time, slot_id, test_type, timestamps)
     */
    private void initializeDatabase() {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS slots (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                hash TEXT NOT NULL UNIQUE,
                test_center TEXT NOT NULL,
                date_time TEXT NOT NULL,
                slot_id TEXT NOT NULL,
                test_type TEXT NOT NULL,
                first_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                last_notified TIMESTAMP,
                notification_count INTEGER DEFAULT 0
            );
            """;

        String createIndexSQL = "CREATE INDEX IF NOT EXISTS idx_hash ON slots(hash);";

        try (Connection conn = DriverManager.getConnection(databaseUrl);
             Statement stmt = conn.createStatement()) {

            stmt.execute(createTableSQL);
            stmt.execute(createIndexSQL);
            logger.info("Database initialized at: {}", databaseUrl);

        } catch (SQLException e) {
            logger.error("Failed to initialize database", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    /**
     * Persists new slots to database using INSERT OR IGNORE.
     * Prevents duplicate entries via unique hash constraint.
     */
    public void persistSlots(List<Slot> slots) {
        String insertSQL = "INSERT OR IGNORE INTO slots (hash, test_center, date_time, slot_id, test_type) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(databaseUrl);
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

            conn.setAutoCommit(false);

            for (Slot slot : slots) {
                pstmt.setString(1, slot.getHash());
                pstmt.setString(2, slot.getTestCenter());
                pstmt.setString(3, slot.getDateTime().toString());
                pstmt.setString(4, slot.getSlotId());
                pstmt.setString(5, slot.getTestType());
                pstmt.addBatch();
            }

            pstmt.executeBatch();
            conn.commit();
            logger.debug("Persisted {} slots to database", slots.size());

        } catch (SQLException e) {
            logger.error("Failed to persist slots", e);
        }
    }

    /**
     * Computes diff between current slots and stored slots.
     * Returns only slots that haven't been seen before.
     * Uses HashSet for O(1) lookup complexity.
     */
    public List<Slot> computeNewSlots(List<Slot> currentSlots) {
        Set<String> existingHashes = getExistingHashes();
        List<Slot> newSlots = new ArrayList<>();

        for (Slot slot : currentSlots) {
            if (!existingHashes.contains(slot.getHash())) {
                newSlots.add(slot);
            }
        }

        logger.debug("Found {} new slots out of {} current slots", newSlots.size(), currentSlots.size());
        return newSlots;
    }

    /**
     * Retrieves all existing slot hashes for efficient set-based comparison.
     */
    private Set<String> getExistingHashes() {
        Set<String> hashes = new HashSet<>();
        String querySQL = "SELECT hash FROM slots";

        try (Connection conn = DriverManager.getConnection(databaseUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(querySQL)) {

            while (rs.next()) {
                hashes.add(rs.getString("hash"));
            }

        } catch (SQLException e) {
            logger.error("Failed to retrieve existing hashes", e);
        }

        return hashes;
    }

    /**
     * Updates notification timestamp for a slot.
     * Prevents notification spam by tracking last notification time.
     */
    public void markNotified(Slot slot) {
        String updateSQL = "UPDATE slots SET last_notified = ?, notification_count = notification_count + 1 WHERE hash = ?";

        try (Connection conn = DriverManager.getConnection(databaseUrl);
             PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {

            pstmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setString(2, slot.getHash());
            pstmt.executeUpdate();

        } catch (SQLException e) {
            logger.error("Failed to mark slot as notified", e);
        }
    }

    /**
     * Retrieves all stored slots for reporting/debugging.
     */
    public List<Slot> getAllSlots() {
        List<Slot> slots = new ArrayList<>();
        String querySQL = "SELECT * FROM slots ORDER BY date_time ASC";

        try (Connection conn = DriverManager.getConnection(databaseUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(querySQL)) {

            while (rs.next()) {
                Slot slot = new Slot(
                        rs.getString("test_center"),
                        LocalDateTime.parse(rs.getString("date_time")),
                        rs.getString("slot_id"),
                        rs.getString("test_type")
                );
                slots.add(slot);
            }

        } catch (SQLException e) {
            logger.error("Failed to retrieve all slots", e);
        }

        return slots;
    }
}