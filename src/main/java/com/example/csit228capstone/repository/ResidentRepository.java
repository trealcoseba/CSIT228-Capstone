package com.example.csit228capstone.repository;

import com.example.csit228capstone.model.Resident;
import com.example.csit228capstone.model.vulnerability.VulnerabilityTag;
import com.example.csit228capstone.util.SupabaseConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ResidentRepository {

    private Connection getConn() throws SQLException {
        return SupabaseConnectionManager.getInstance().getConnection();
    }

    public List<Resident> findAll() {
        List<Resident> list = new ArrayList<>();
        // This query gets all resident details AND their tags in one go
        String sql = "SELECT r.*, string_agg(rv.tag::text, ',') as tags " +
                "FROM residents r " +
                "LEFT JOIN resident_vulnerabilities rv ON r.id = rv.resident_id " +
                "GROUP BY r.id " +
                "ORDER BY r.created_at ASC"; // Keeps your ID ordering intact

        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Resident r = mapRow(rs);

                String tagsString = rs.getString("tags");
                if (tagsString != null && !tagsString.isBlank()) {
                    for (String tagName : tagsString.split(",")) {
                        try {
                            // Now r.getVulnerabilities() won't be null
                            r.getVulnerabilities().add(VulnerabilityTag.valueOf(tagName.trim().toUpperCase()));
                        } catch (IllegalArgumentException e) {
                            // Ignore tags that don't match the enum
                        }
                    }
                }
                list.add(r);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not load residents", e);
        }
        return list;
    }

    public Optional<Resident> findById(UUID id) {
        String sql = "SELECT * FROM residents WHERE id = ?";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public List<Resident> findByPurok(String purok) {
        String sql = "SELECT r.* FROM residents r JOIN households h ON r.household_id = h.id WHERE h.purok = ? ORDER BY r.last_name";
        List<Resident> list = new ArrayList<>();
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, purok);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapRow(rs)); }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }


    public List<Resident> search(String query) {
        // Search by name and keep chronological order
        String sql = "SELECT * FROM residents WHERE LOWER(first_name || ' ' || last_name) LIKE ? " +
                "ORDER BY created_at ASC";
        List<Resident> list = new ArrayList<>();
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "%" + query.toLowerCase() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    public UUID insert(Resident r) {
        String sql = "INSERT INTO residents (household_id, first_name, middle_name, last_name, suffix, date_of_birth, sex, civil_status, contact_number, photo_url, is_household_head) " +
                     "VALUES (?,?,?,?,?,?,?,?,?,?,?) RETURNING id";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, r.getHouseholdId());
            ps.setString(2, r.getFirstName());
            ps.setString(3, r.getMiddleName());
            ps.setString(4, r.getLastName());
            ps.setString(5, r.getSuffix());
            ps.setDate(6, Date.valueOf(r.getDateOfBirth()));
            ps.setString(7, r.getSex());
            ps.setString(8, r.getCivilStatus());
            ps.setString(9, r.getContactNumber());
            ps.setString(10, r.getPhotoUrl());
            ps.setBoolean(11, r.isHouseholdHead());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getObject("id", UUID.class);
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public void update(Resident r) {
        String sql = "UPDATE residents SET household_id=?, first_name=?, middle_name=?, last_name=?, suffix=?, date_of_birth=?, sex=?, civil_status=?, contact_number=?, photo_url=?, is_household_head=?, updated_at=now() WHERE id=?";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, r.getHouseholdId());
            ps.setString(2, r.getFirstName());
            ps.setString(3, r.getMiddleName());
            ps.setString(4, r.getLastName());
            ps.setString(5, r.getSuffix());
            ps.setDate(6, Date.valueOf(r.getDateOfBirth()));
            ps.setString(7, r.getSex());
            ps.setString(8, r.getCivilStatus());
            ps.setString(9, r.getContactNumber());
            ps.setString(10, r.getPhotoUrl());
            ps.setBoolean(11, r.isHouseholdHead());
            ps.setObject(12, r.getId());
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public void delete(UUID id) {
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement("DELETE FROM residents WHERE id=?")) {
            ps.setObject(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public int countAll() {
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM residents");
             ResultSet rs = ps.executeQuery()) {
            rs.next(); return rs.getInt(1);
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public int countByVulnerability(VulnerabilityTag tag) {
        String sql = "SELECT COUNT(*) FROM resident_vulnerabilities WHERE tag = ?::vulnerability_tag";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tag.name().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private Resident mapRow(ResultSet rs) throws SQLException {
        Resident r = new Resident();
        r.setId(rs.getObject("id", UUID.class));
        r.setHouseholdId(rs.getObject("household_id", UUID.class));
        r.setFirstName(rs.getString("first_name"));
        r.setMiddleName(rs.getString("middle_name"));
        r.setLastName(rs.getString("last_name"));
        r.setSuffix(rs.getString("suffix"));
        r.setDateOfBirth(rs.getDate("date_of_birth").toLocalDate());
        r.setSex(rs.getString("sex"));
        r.setCivilStatus(rs.getString("civil_status"));
        r.setContactNumber(rs.getString("contact_number"));
        r.setPhotoUrl(rs.getString("photo_url"));
        r.setHouseholdHead(rs.getBoolean("is_household_head"));
        r.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        r.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return r;
    }

    public void addVulnerability(UUID residentId, VulnerabilityTag tag) {
        // We use .name() to get "SENIOR_CITIZEN" and cast it to the custom enum type
        String sql = "INSERT INTO resident_vulnerabilities (resident_id, tag) VALUES (?, ?::vulnerability_tag)";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, residentId);
            ps.setString(2, tag.name().toLowerCase()); // Matches 'senior_citizen' in DB
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error adding vulnerability: " + tag.getDisplayName(), e);
        }
    }

    public void deleteVulnerabilities(UUID residentId) {
        String sql = "DELETE FROM resident_vulnerabilities WHERE resident_id = ?";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, residentId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error clearing vulnerabilities", e);
        }
    }
}
