package com.example.csit228capstone.repository;

import com.example.csit228capstone.model.alert.Alert;
import com.example.csit228capstone.model.alert.AlertPriority;
import com.example.csit228capstone.util.SupabaseConnectionManager;
import org.postgresql.util.PGobject;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AlertsRepository {

    private Connection getConn() throws SQLException {
        return SupabaseConnectionManager.getInstance().getConnection();
    }

    public void save(Alert alert) throws SQLException {
        // We have 8 columns and 8 question marks here
        String sql = """
                INSERT INTO alerts
                    (id, title, body, priority, is_broadcast, expires_at, created_at, issued_by_name)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, alert.getId());
            ps.setString(2, alert.getTitle());
            ps.setString(3, alert.getBody());
            ps.setObject(4, toPGEnum(alert.getPriority()));
            ps.setBoolean(5, alert.isBroadcast());
            ps.setTimestamp(6, alert.getExpiresAt() != null
                    ? Timestamp.valueOf(alert.getExpiresAt()) : null);
            ps.setTimestamp(7, Timestamp.valueOf(
                    alert.getCreatedAt() != null ? alert.getCreatedAt() : LocalDateTime.now()));
            // Slot 8 matches 'issued_by_name'
            ps.setString(8, alert.getSentByName());

            ps.executeUpdate();
        }
    }

    public void update(Alert alert) throws SQLException {
        // 6 parameters needed (title, body, priority, expires_at, issued_by_name, and id)
        String sql = """
                UPDATE alerts
                SET title = ?, body = ?, priority = ?, expires_at = ?, issued_by_name = ?
                WHERE id = ?
                """;
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, alert.getTitle());
            ps.setString(2, alert.getBody());
            ps.setObject(3, toPGEnum(alert.getPriority()));
            ps.setTimestamp(4, alert.getExpiresAt() != null
                    ? Timestamp.valueOf(alert.getExpiresAt()) : null);
            ps.setString(5, alert.getSentByName());
            ps.setObject(6, alert.getId()); // This matches the WHERE id = ?

            ps.executeUpdate();
        }
    }

    public void delete(UUID id) throws SQLException {
        String sql = "DELETE FROM alerts WHERE id = ?";
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.executeUpdate();
        }
    }

    public List<Alert> findAll() throws SQLException {
        List<Alert> results = new ArrayList<>();
        // Make sure issued_by_name is included in the SELECT
        String sql = "SELECT * FROM alerts ORDER BY created_at DESC";
        try (Connection conn = getConn();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) results.add(mapRow(rs));
        }
        return results;
    }

    public int countBroadcasts() throws SQLException {
        return queryCount("SELECT COUNT(*) FROM alerts WHERE is_broadcast = true");
    }

    public int countScheduled() throws SQLException {
        return queryCount("SELECT COUNT(*) FROM alerts WHERE expires_at > now()");
    }

    private PGobject toPGEnum(AlertPriority priority) throws SQLException {
        PGobject pg = new PGobject();
        pg.setType("alert_priority");
        pg.setValue(priority != null ? priority.name().toLowerCase() : "medium");
        return pg;
    }

    private Alert mapRow(ResultSet rs) throws SQLException {
        Alert a = new Alert();
        a.setId(UUID.fromString(rs.getString("id")));
        a.setTitle(rs.getString("title"));
        a.setBody(rs.getString("body"));

        String dbPriority = rs.getString("priority");
        if (dbPriority != null) {
            try {
                a.setPriority(AlertPriority.valueOf(dbPriority.toUpperCase()));
            } catch (IllegalArgumentException e) {
                a.setPriority(AlertPriority.MEDIUM);
            }
        }

        a.setBroadcast(rs.getBoolean("is_broadcast"));

        // Map the name from the database column 'issued_by_name'
        a.setSentByName(rs.getString("issued_by_name"));

        Timestamp expiresAt = rs.getTimestamp("expires_at");
        if (expiresAt != null) a.setExpiresAt(expiresAt.toLocalDateTime());

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) a.setCreatedAt(createdAt.toLocalDateTime());

        return a;
    }

    private int queryCount(String sql) throws SQLException {
        try (Connection conn = getConn();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
}