package com.example.csit228capstone.repository;

import com.example.csit228capstone.model.document.DocumentRequest;
import com.example.csit228capstone.model.document.DocumentStatus;
import com.example.csit228capstone.model.document.DocumentType;
import com.example.csit228capstone.util.SupabaseConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DocumentRequestRepository {

    private Connection getConn() throws SQLException {
        return SupabaseConnectionManager.getInstance().getConnection();
    }

    public List<DocumentRequest> findAll() {
        String sql = "SELECT dr.*, r.first_name || ' ' || r.last_name AS resident_name " +
                     "FROM document_requests dr JOIN residents r ON dr.resident_id = r.id ORDER BY dr.created_at DESC";
        List<DocumentRequest> list = new ArrayList<>();
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    public List<DocumentRequest> findByStatus(DocumentStatus status) {
        String sql = "SELECT dr.*, r.first_name || ' ' || r.last_name AS resident_name " +
                     "FROM document_requests dr JOIN residents r ON dr.resident_id = r.id WHERE dr.status = ?::document_status ORDER BY dr.created_at";
        List<DocumentRequest> list = new ArrayList<>();
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.name().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapRow(rs)); }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    public UUID insert(DocumentRequest dr) {
        String sql = "INSERT INTO document_requests (resident_id, document_type, purpose) VALUES (?,?::document_type,?) RETURNING id";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, dr.getResidentId());
            ps.setString(2, dr.getDocumentType().name().toLowerCase());
            ps.setString(3, dr.getPurpose());
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getObject("id", UUID.class); }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public void updateStatus(UUID id, DocumentStatus status, UUID processedBy) {
        String sql = status == DocumentStatus.RELEASED
                ? "UPDATE document_requests SET status=?::document_status, processed_by=?, released_at=now(), updated_at=now() WHERE id=?"
                : "UPDATE document_requests SET status=?::document_status, processed_by=?, updated_at=now() WHERE id=?";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.name().toLowerCase());
            ps.setObject(2, processedBy);
            ps.setObject(3, id);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public int countByStatus(DocumentStatus status) {
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM document_requests WHERE status=?::document_status")) {
            ps.setString(1, status.name().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private DocumentRequest mapRow(ResultSet rs) throws SQLException {
        DocumentRequest d = new DocumentRequest();
        d.setId(rs.getObject("id", UUID.class));
        d.setResidentId(rs.getObject("resident_id", UUID.class));
        d.setDocumentType(DocumentType.valueOf(rs.getString("document_type").toUpperCase()));
        d.setStatus(DocumentStatus.valueOf(rs.getString("status").toUpperCase()));
        d.setPurpose(rs.getString("purpose"));
        d.setProcessedBy(rs.getObject("processed_by", UUID.class));
        Timestamp rel = rs.getTimestamp("released_at");
        if (rel != null) d.setReleasedAt(rel.toLocalDateTime());
        d.setPdfStoragePath(rs.getString("pdf_storage_path"));
        d.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        d.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        try { d.setResidentName(rs.getString("resident_name")); } catch (SQLException ignored) {}
        return d;
    }
}
