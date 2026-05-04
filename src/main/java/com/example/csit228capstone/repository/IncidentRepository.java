package com.example.csit228capstone.repository;

import com.example.csit228capstone.model.incident.Incident;
import com.example.csit228capstone.model.incident.IncidentSeverity;
import com.example.csit228capstone.model.incident.IncidentStatus;
import com.example.csit228capstone.model.incident.IncidentType;
import com.example.csit228capstone.util.SupabaseConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class IncidentRepository {

    private Connection getConn() throws SQLException {
        return SupabaseConnectionManager.getInstance().getConnection();
    }

    public List<Incident> findAll() {
        List<Incident> list = new ArrayList<>();
        String sql = "SELECT * FROM incidents ORDER BY reported_at DESC";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    public List<Incident> findActive() {
        String sql = "SELECT * FROM incidents WHERE status != 'resolved' ORDER BY severity DESC, reported_at DESC";
        List<Incident> list = new ArrayList<>();
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    public Optional<Incident> findById(UUID id) {
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement("SELECT * FROM incidents WHERE id=?")) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public UUID insert(Incident i) {
        String sql = "INSERT INTO incidents (type, title, description, severity, status, location_purok, location_detail, latitude, longitude, reported_by) " +
                     "VALUES (?::incident_type,?,?,?::incident_severity,?::incident_status,?,?,?,?,?) RETURNING id";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, i.getType().name().toLowerCase());
            ps.setString(2, i.getTitle());
            ps.setString(3, i.getDescription());
            ps.setString(4, i.getSeverity().name().toLowerCase());
            ps.setString(5, i.getStatus().name().toLowerCase());
            ps.setString(6, i.getLocationPurok());
            ps.setString(7, i.getLocationDetail());
            ps.setObject(8, i.getLatitude());
            ps.setObject(9, i.getLongitude());
            ps.setObject(10, i.getReportedBy());
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getObject("id", UUID.class); }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public void updateStatus(UUID incidentId, IncidentStatus newStatus, UUID changedBy, String note) {
        try (Connection c = getConn()) {
            c.setAutoCommit(false);
            // Get old status
            IncidentStatus oldStatus;
            try (PreparedStatement ps = c.prepareStatement("SELECT status FROM incidents WHERE id=?")) {
                ps.setObject(1, incidentId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    oldStatus = IncidentStatus.valueOf(rs.getString("status").toUpperCase());
                }
            }
            // Update incident
            String upd = newStatus == IncidentStatus.RESOLVED
                    ? "UPDATE incidents SET status=?::incident_status, resolved_at=now(), updated_at=now() WHERE id=?"
                    : "UPDATE incidents SET status=?::incident_status, updated_at=now() WHERE id=?";
            try (PreparedStatement ps = c.prepareStatement(upd)) {
                ps.setString(1, newStatus.name().toLowerCase());
                ps.setObject(2, incidentId);
                ps.executeUpdate();
            }
            // Insert timeline entry
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO incident_timeline (incident_id, old_status, new_status, changed_by, note) VALUES (?,?::incident_status,?::incident_status,?,?)")) {
                ps.setObject(1, incidentId);
                ps.setString(2, oldStatus.name().toLowerCase());
                ps.setString(3, newStatus.name().toLowerCase());
                ps.setObject(4, changedBy);
                ps.setString(5, note);
                ps.executeUpdate();
            }
            c.commit();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public int countActive() {
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM incidents WHERE status != 'resolved'");
             ResultSet rs = ps.executeQuery()) {
            rs.next(); return rs.getInt(1);
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private Incident mapRow(ResultSet rs) throws SQLException {
        Incident i = new Incident();
        i.setId(rs.getObject("id", UUID.class));
        i.setType(IncidentType.valueOf(rs.getString("type").toUpperCase()));
        i.setTitle(rs.getString("title"));
        i.setDescription(rs.getString("description"));
        i.setSeverity(IncidentSeverity.valueOf(rs.getString("severity").toUpperCase()));
        i.setStatus(IncidentStatus.valueOf(rs.getString("status").toUpperCase()));
        i.setLocationPurok(rs.getString("location_purok"));
        i.setLocationDetail(rs.getString("location_detail"));
        i.setLatitude((Double) rs.getObject("latitude"));
        i.setLongitude((Double) rs.getObject("longitude"));
        i.setReportedBy(rs.getObject("reported_by", UUID.class));
        i.setReportedAt(rs.getTimestamp("reported_at").toLocalDateTime());
        Timestamp resolved = rs.getTimestamp("resolved_at");
        if (resolved != null) i.setResolvedAt(resolved.toLocalDateTime());
        i.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        i.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return i;
    }
}
