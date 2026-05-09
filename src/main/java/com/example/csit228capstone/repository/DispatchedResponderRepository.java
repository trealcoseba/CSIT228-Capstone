package com.example.csit228capstone.repository;

import com.example.csit228capstone.model.responder.DispatchedResponder;
import com.example.csit228capstone.util.SupabaseConnectionManager;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DispatchedResponderRepository {

    // ─── Connection ──────────────────────────────────────────────────────────────

    private Connection getConn() throws SQLException {
        return SupabaseConnectionManager.getInstance().getConnection();
    }

    // ─── READ: All dispatches (joined with responders) ────────────────────────────

    /**
     * Fetches ALL dispatch records (all statuses) ordered by dispatched_at DESC.
     * Joins responders so we get the name/agency for display.
     */
    public List<DispatchedResponder> findAll() throws SQLException {
        String sql = """
            SELECT
                dr.id,
                dr.responder_id,
                dr.incident_id,
                dr.severity,
                dr.time_occurred,
                dr.dispatched_at,
                dr.latitude,
                dr.longitude,
                dr.status,
                r.name    AS responder_name,
                r.agency  AS responder_agency
            FROM dispatched_responders dr
            JOIN responders r ON r.id = dr.responder_id
            ORDER BY dr.dispatched_at DESC
            """;

        return executeQuery(sql);
    }

    // ─── READ: Filter by responder ────────────────────────────────────────────────

    public List<DispatchedResponder> findByResponderId(UUID responderId) throws SQLException {
        String sql = """
            SELECT
                dr.id, dr.responder_id, dr.incident_id,
                dr.severity, dr.time_occurred, dr.dispatched_at,
                dr.latitude, dr.longitude, dr.status,
                r.name AS responder_name, r.agency AS responder_agency
            FROM dispatched_responders dr
            JOIN responders r ON r.id = dr.responder_id
            WHERE dr.responder_id = ?
            ORDER BY dr.dispatched_at DESC
            """;

        List<DispatchedResponder> list = new ArrayList<>();
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, responderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    // ─── READ: Filter by incident ─────────────────────────────────────────────────

    public List<DispatchedResponder> findByIncidentId(UUID incidentId) throws SQLException {
        String sql = """
            SELECT
                dr.id, dr.responder_id, dr.incident_id,
                dr.severity, dr.time_occurred, dr.dispatched_at,
                dr.latitude, dr.longitude, dr.status,
                r.name AS responder_name, r.agency AS responder_agency
            FROM dispatched_responders dr
            JOIN responders r ON r.id = dr.responder_id
            WHERE dr.incident_id = ?
            ORDER BY dr.dispatched_at DESC
            """;

        List<DispatchedResponder> list = new ArrayList<>();
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, incidentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    // ─── READ: Active only ────────────────────────────────────────────────────────

    public List<DispatchedResponder> findActive() throws SQLException {
        String sql = """
            SELECT
                dr.id, dr.responder_id, dr.incident_id,
                dr.severity, dr.time_occurred, dr.dispatched_at,
                dr.latitude, dr.longitude, dr.status,
                r.name AS responder_name, r.agency AS responder_agency
            FROM dispatched_responders dr
            JOIN responders r ON r.id = dr.responder_id
            WHERE dr.status = 'dispatched'
            ORDER BY dr.dispatched_at DESC
            """;
        return executeQuery(sql);
    }

    // ─── READ: Distinct incident IDs for ComboBox ────────────────────────────────

    public List<String> findDistinctIncidentIds() throws SQLException {
        String sql = """
            SELECT DISTINCT incident_id::text
            FROM dispatched_responders
            ORDER BY incident_id::text
            """;
        List<String> list = new ArrayList<>();
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String raw = rs.getString(1);
                // Show short version in combo, full UUID as value via tag
                list.add(raw.substring(0, 8).toUpperCase());
            }
        }
        return list;
    }

    // ─── CREATE ───────────────────────────────────────────────────────────────────

    public DispatchedResponder save(DispatchedResponder dr) throws SQLException {
        String sql = """
            INSERT INTO dispatched_responders
                (responder_id, incident_id, severity, time_occurred, dispatched_at, latitude, longitude, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id, dispatched_at
            """;

        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, dr.getResponderId());
            ps.setObject(2, dr.getIncidentId());
            ps.setString(3, dr.getSeverity());

            if (dr.getTimeOccurred() != null)
                ps.setTimestamp(4, Timestamp.valueOf(dr.getTimeOccurred()));
            else
                ps.setNull(4, Types.TIMESTAMP);

            ps.setTimestamp(5, Timestamp.valueOf(
                    dr.getDispatchedAt() != null
                            ? dr.getDispatchedAt()
                            : java.time.LocalDateTime.now()));

            ps.setBigDecimal(6, dr.getLatitude());
            ps.setBigDecimal(7, dr.getLongitude());
            ps.setString(8, dr.getStatus() != null ? dr.getStatus() : "dispatched");

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    dr.setId(UUID.fromString(rs.getString("id")));
                    Timestamp ts = rs.getTimestamp("dispatched_at");
                    if (ts != null) dr.setDispatchedAt(ts.toLocalDateTime());
                }
            }
        }
        return dr;
    }

    // ─── UPDATE STATUS ────────────────────────────────────────────────────────────

    public void updateStatus(UUID id, String status) throws SQLException {
        String sql = "UPDATE dispatched_responders SET status = ? WHERE id = ?";
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setObject(2, id);
            ps.executeUpdate();
        }
    }

    // ─── DELETE ───────────────────────────────────────────────────────────────────

    public void delete(UUID id) throws SQLException {
        String sql = "DELETE FROM dispatched_responders WHERE id = ?";
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.executeUpdate();
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────────

    private List<DispatchedResponder> executeQuery(String sql) throws SQLException {
        List<DispatchedResponder> list = new ArrayList<>();
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    private DispatchedResponder mapRow(ResultSet rs) throws SQLException {
        DispatchedResponder dr = new DispatchedResponder();
        dr.setId(UUID.fromString(rs.getString("id")));
        dr.setResponderId(UUID.fromString(rs.getString("responder_id")));
        dr.setIncidentId(UUID.fromString(rs.getString("incident_id")));
        dr.setSeverity(rs.getString("severity"));
        dr.setStatus(rs.getString("status"));
        dr.setResponderName(rs.getString("responder_name"));
        dr.setResponderAgency(rs.getString("responder_agency"));

        Timestamp occurred = rs.getTimestamp("time_occurred");
        if (occurred != null) dr.setTimeOccurred(occurred.toLocalDateTime());

        Timestamp dispatched = rs.getTimestamp("dispatched_at");
        if (dispatched != null) dr.setDispatchedAt(dispatched.toLocalDateTime());

        BigDecimal lat = rs.getBigDecimal("latitude");
        BigDecimal lon = rs.getBigDecimal("longitude");
        dr.setLatitude(lat);
        dr.setLongitude(lon);

        return dr;
    }
}