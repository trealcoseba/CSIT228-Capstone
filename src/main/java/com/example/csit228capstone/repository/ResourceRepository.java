package com.example.csit228capstone.repository;

import com.example.csit228capstone.model.Resource;
import com.example.csit228capstone.util.SupabaseConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ResourceRepository {

    private Connection getConn() throws SQLException {
        return SupabaseConnectionManager.getInstance().getConnection();
    }

    public List<Resource> findAll() {
        String sql = "SELECT id, name, category, total_qty, available_qty, warning_level, unit, updated_at " +
                "FROM resources ORDER BY updated_at DESC NULLS LAST, name ASC";
        List<Resource> list = new ArrayList<>();

        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not load resources", e);
        }
        return list;
    }

    public UUID insert(Resource resource) {
        String sql = "INSERT INTO resources (name, category, total_qty, available_qty, warning_level, unit, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, now()) RETURNING id, updated_at";

        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, resource.getName());
            ps.setString(2, resource.getCategory());
            ps.setDouble(3, resource.getTotalQty());
            ps.setDouble(4, resource.getAvailableQty());
            ps.setDouble(5, resource.getWarningLevel());
            if (resource.getUnit() == null || resource.getUnit().isBlank()) {
                ps.setNull(6, java.sql.Types.VARCHAR);
            } else {
                ps.setString(6, resource.getUnit().trim());
            }

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                UUID id = rs.getObject("id", UUID.class);
                resource.setId(id);
                Timestamp updatedAt = rs.getTimestamp("updated_at");
                if (updatedAt != null) {
                    resource.setUpdatedAt(updatedAt.toLocalDateTime());
                }
                return id;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not insert resource", e);
        }
    }

    public void update(Resource resource) {
        String sql = "UPDATE resources SET name = ?, category = ?, total_qty = ?, available_qty = ?, " +
                "warning_level = ?, unit = ?, updated_at = now() WHERE id = ?";

        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, resource.getName());
            ps.setString(2, resource.getCategory());
            ps.setDouble(3, resource.getTotalQty());
            ps.setDouble(4, resource.getAvailableQty());
            ps.setDouble(5, resource.getWarningLevel());
            if (resource.getUnit() == null || resource.getUnit().isBlank()) {
                ps.setNull(6, java.sql.Types.VARCHAR);
            } else {
                ps.setString(6, resource.getUnit().trim());
            }
            ps.setObject(7, resource.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not update resource", e);
        }
    }

    public void deleteById(UUID id) {
        String sql = "DELETE FROM resources WHERE id = ?";
        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not delete resource", e);
        }
    }

    private Resource mapRow(ResultSet rs) throws SQLException {
        Resource resource = new Resource();
        resource.setId(rs.getObject("id", UUID.class));
        resource.setName(rs.getString("name"));
        resource.setCategory(rs.getString("category"));
        resource.setTotalQty(rs.getDouble("total_qty"));
        resource.setAvailableQty(rs.getDouble("available_qty"));
        resource.setWarningLevel(rs.getDouble("warning_level"));
        resource.setUnit(rs.getString("unit"));

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            resource.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        return resource;
    }
}
