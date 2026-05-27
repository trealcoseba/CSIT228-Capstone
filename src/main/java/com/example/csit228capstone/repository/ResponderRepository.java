package com.example.csit228capstone.repository;

import com.example.csit228capstone.model.responder.Responder;
import com.example.csit228capstone.util.SupabaseConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ResponderRepository {

    private Connection getConn() throws SQLException {
        return SupabaseConnectionManager.getInstance().getConnection();
    }

    public List<Responder> findAll() throws SQLException {
        String sql = """
            SELECT
                r.id,
                r.name,
                r.contact,
                r.agency,
                r.status,
                r.created_at,
                COUNT(dr.id)                                              AS total_dispatches,
                COUNT(dr.id) FILTER (WHERE dr.status = 'dispatched')     AS active_dispatches
            FROM responders r
            LEFT JOIN dispatched_responders dr ON dr.responder_id = r.id
            GROUP BY r.id
            ORDER BY r.name
            """;

        List<Responder> list = new ArrayList<>();
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    // ─── READ: Find by ID ─────────────────────────────────────────────────────────

    public Responder findById(UUID id) throws SQLException {
        String sql = """
            SELECT
                r.id, r.name, r.contact, r.agency, r.status, r.created_at,
                COUNT(dr.id)                                             AS total_dispatches,
                COUNT(dr.id) FILTER (WHERE dr.status = 'dispatched')    AS active_dispatches
            FROM responders r
            LEFT JOIN dispatched_responders dr ON dr.responder_id = r.id
            WHERE r.id = ?
            GROUP BY r.id
            """;

        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    // ─── CREATE ───────────────────────────────────────────────────────────────────

    public Responder save(Responder r) throws SQLException {
        String sql = """
            INSERT INTO responders (name, contact, agency, status)
            VALUES (?, ?, ?, ?)
            RETURNING id, created_at
            """;

        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, r.getName());
            ps.setString(2, r.getContact());
            ps.setString(3, r.getAgency());
            ps.setString(4, r.getStatus() != null ? r.getStatus() : "available");

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    r.setId(UUID.fromString(rs.getString("id")));
                    Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null) r.setCreatedAt(ts.toLocalDateTime());
                }
            }
        }
        return r;
    }

    // ─── UPDATE ───────────────────────────────────────────────────────────────────

    public void update(Responder r) throws SQLException {
        String sql = """
            UPDATE responders
            SET name = ?, contact = ?, agency = ?, status = ?
            WHERE id = ?
            """;

        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, r.getName());
            ps.setString(2, r.getContact());
            ps.setString(3, r.getAgency());
            ps.setString(4, r.getStatus());
            ps.setObject(5, r.getId());
            ps.executeUpdate();
        }
    }

    // ─── UPDATE STATUS ONLY ───────────────────────────────────────────────────────

    public void updateStatus(UUID id, String status) throws SQLException {
        String sql = "UPDATE responders SET status = ? WHERE id = ?";
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setObject(2, id);
            ps.executeUpdate();
        }
    }

    // ─── DELETE ───────────────────────────────────────────────────────────────────

    public void delete(UUID id) throws SQLException {
        // Must delete child records first before deleting the responder
        String deleteDispatches = "DELETE FROM dispatched_responders WHERE responder_id = ?";
        String deleteResponder  = "DELETE FROM responders WHERE id = ?";

        try (Connection conn = getConn()) {
            conn.setAutoCommit(false); // wrap both in a transaction
            try (PreparedStatement ps1 = conn.prepareStatement(deleteDispatches);
                 PreparedStatement ps2 = conn.prepareStatement(deleteResponder)) {

                ps1.setObject(1, id);
                ps1.executeUpdate();

                ps2.setObject(1, id);
                ps2.executeUpdate();

                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    // ─── AGENCIES (for ComboBox) ──────────────────────────────────────────────────

    public List<String> findDistinctAgencies() throws SQLException {
        String sql = "SELECT DISTINCT agency FROM responders WHERE agency IS NOT NULL ORDER BY agency";
        List<String> list = new ArrayList<>();
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(rs.getString("agency"));
        }
        return list;
    }

    // ─── ROW MAPPER ───────────────────────────────────────────────────────────────

    private Responder mapRow(ResultSet rs) throws SQLException {
        Responder r = new Responder();
        r.setId(UUID.fromString(rs.getString("id")));
        r.setName(rs.getString("name"));
        r.setContact(rs.getString("contact"));
        r.setAgency(rs.getString("agency"));
        r.setStatus(rs.getString("status"));

        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) r.setCreatedAt(ts.toLocalDateTime());

        try {
            r.setTotalDispatches(rs.getInt("total_dispatches"));
            r.setActiveDispatches(rs.getInt("active_dispatches"));
        } catch (SQLException ignored) {
            // columns may not exist in every query variant
        }
        return r;
    }

    public void markRespondersAvailableByIncident(UUID incidentId) throws SQLException {
        String sql = """
        UPDATE responders
        SET status = 'available'
        WHERE id IN (
            SELECT responder_id FROM dispatched_responders WHERE incident_id = ?
        )
        """;
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, incidentId);
            ps.executeUpdate();
        }
    }
}