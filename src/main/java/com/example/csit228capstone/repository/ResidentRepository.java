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

    // ─── READ ─────────────────────────────────────────────────────────────────

    public List<Resident> findAll() {
        List<Resident> list = new ArrayList<>();

        String sql = "SELECT r.*, rl.address, rl.latitude, rl.longitude, " +
                "string_agg(rv.tag::text, ', ') as tags " +
                "FROM residents r " +
                "LEFT JOIN resident_locations rl ON r.resident_locations_id = rl.id " +
                "LEFT JOIN resident_vulnerabilities rv ON rv.resident_id = r.id " +
                "GROUP BY r.id, rl.address, rl.latitude, rl.longitude " +
                "ORDER BY r.created_at DESC";

        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Resident r = mapRow(rs);
                r.setAddress(rs.getString("address"));

                String tagsString = rs.getString("tags");
                if (tagsString != null && !tagsString.isBlank()) {
                    for (String tagName : tagsString.split(",")) {
                        try {
                            r.getVulnerabilities().add(
                                    VulnerabilityTag.valueOf(tagName.trim().toUpperCase())
                            );
                        } catch (IllegalArgumentException ignored) {}
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
        String sql = "SELECT r.*, rl.address, rl.latitude, rl.longitude " +
                "FROM residents r " +
                "LEFT JOIN resident_locations rl ON r.resident_locations_id = rl.id " +
                "WHERE r.id = ?";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Resident r = mapRow(rs);
                    r.setAddress(rs.getString("address"));
                    return Optional.of(r);
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // ─── WRITE ────────────────────────────────────────────────────────────────

    public UUID insert(Resident r) {
        UUID locationId = null;
        if (r.getAddress() != null && !r.getAddress().isBlank()) {
            locationId = findOrInsertLocation(r.getAddress(), r.getLatitude(), r.getLongitude());
        }

        String sql = "INSERT INTO residents " +
                "(resident_locations_id, first_name, middle_name, last_name, suffix, " +
                "date_of_birth, sex, civil_status, contact_number, photo_url, is_household_head) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?) RETURNING id";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, locationId);
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
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void update(Resident r) {
        UUID existingLocationId = getLocationId(r.getId());
        String newAddress = r.getAddress();
        UUID locationId;

        if (newAddress != null && !newAddress.isBlank()) {
            if (existingLocationId != null) {
                updateLocation(existingLocationId, newAddress, r.getLatitude(), r.getLongitude());
                locationId = existingLocationId;
            } else {
                locationId = findOrInsertLocation(newAddress, r.getLatitude(), r.getLongitude());
            }
        } else {
            if (existingLocationId != null && !isLocationShared(existingLocationId, r.getId())) {
                deleteLocation(existingLocationId);
            }
            locationId = null;
        }

        // ✅ FIXED: correct UPDATE sql
        String sql = "UPDATE residents SET " +
                "resident_locations_id = ?, " +
                "first_name = ?, middle_name = ?, last_name = ?, suffix = ?, " +
                "date_of_birth = ?, sex = ?, civil_status = ?, " +
                "contact_number = ?, photo_url = ?, is_household_head = ?, updated_at = ? " +
                "WHERE id = ?";

        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, locationId);
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
            ps.setTimestamp(12, Timestamp.valueOf(r.getUpdatedAt()));
            ps.setObject(13, r.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void delete(UUID residentId) {
        UUID locationId = getLocationId(residentId);

        // 1. Delete resident first to remove the FK reference
        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement("DELETE FROM residents WHERE id=?")) {
            ps.setObject(1, residentId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        // 2. Only delete the location row if no other resident still uses it
        if (locationId != null && !isLocationShared(locationId, null)) {
            deleteLocation(locationId);
        }
    }

    // ─── LOCATION HELPERS ─────────────────────────────────────────────────────

    private UUID findOrInsertLocation(String address, double latitude, double longitude) {
        String sql = "SELECT id FROM resident_locations WHERE LOWER(address) = LOWER(?) LIMIT 1";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, address);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getObject("id", UUID.class);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return insertLocation(address, latitude, longitude);
    }

    private UUID insertLocation(String address, double latitude, double longitude) {
        String sql = "INSERT INTO resident_locations (id, address, latitude, longitude) " +
                "VALUES (gen_random_uuid(), ?, ?, ?) RETURNING id";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, address);
            ps.setDouble(2, latitude);
            ps.setDouble(3, longitude);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getObject("id", UUID.class);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not insert location", e);
        }
    }

    private void updateLocation(UUID locationId, String address, double latitude, double longitude) {
        String sql = "UPDATE resident_locations SET address=?, latitude=?, longitude=? WHERE id=?";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, address);
            ps.setDouble(2, latitude);
            ps.setDouble(3, longitude);
            ps.setObject(4, locationId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not update location", e);
        }
    }

    private void deleteLocation(UUID locationId) {
        String sql = "DELETE FROM resident_locations WHERE id = ?";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, locationId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not delete location", e);
        }
    }

    private UUID getLocationId(UUID residentId) {
        String sql = "SELECT resident_locations_id FROM residents WHERE id = ?";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, residentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getObject("resident_locations_id", UUID.class);
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not fetch location ID", e);
        }
    }

    private boolean isLocationShared(UUID locationId, UUID excludeResidentId) {
        String sql = excludeResidentId != null
                ? "SELECT COUNT(*) FROM residents WHERE resident_locations_id = ? AND id != ?"
                : "SELECT COUNT(*) FROM residents WHERE resident_locations_id = ?";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, locationId);
            if (excludeResidentId != null) ps.setObject(2, excludeResidentId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not check location usage", e);
        }
    }

    // ─── VULNERABILITIES ──────────────────────────────────────────────────────

    public void addVulnerability(UUID residentId, VulnerabilityTag tag) {
        String sql = "INSERT INTO resident_vulnerabilities (resident_id, tag) " +
                "VALUES (?, ?::vulnerability_tag)";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, residentId);
            ps.setString(2, tag.name().toLowerCase());
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

    // ─── COUNTS ───────────────────────────────────────────────────────────────

    public int countAll() {
        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM residents");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int countByVulnerability(VulnerabilityTag tag) {
        String sql = "SELECT COUNT(*) FROM resident_vulnerabilities WHERE tag = ?::vulnerability_tag";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tag.name().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // ─── MAPPING ──────────────────────────────────────────────────────────────

    private Resident mapRow(ResultSet rs) throws SQLException {
        Resident r = new Resident();
        r.setId(rs.getObject("id", UUID.class));
        r.setResidentLocationsId(rs.getObject("resident_locations_id", UUID.class));
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
        r.setLatitude(rs.getDouble("latitude"));   //
        r.setLongitude(rs.getDouble("longitude")); //
        return r;
    }
}