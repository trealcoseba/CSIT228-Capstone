package com.example.csit228capstone.repository;

import com.example.csit228capstone.model.Resource;
import com.example.csit228capstone.model.ResourceLog;
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

    public void useResource(UUID resourceId, UUID evacuationCenterId, String evacuationCenterName,
                            String purpose, double quantityUsed) {
        String lockSql = "SELECT available_qty FROM resources WHERE id = ? FOR UPDATE";
        String updateSql = "UPDATE resources SET available_qty = ?, updated_at = now() WHERE id = ?";
        String insertLogSql = """
                INSERT INTO resource_log
                    (resource_id, evacuation_center_id, evacuation_center_name, purpose,
                     date_used, quantity_used, quantity_available_at_time, created_at)
                VALUES (?, ?, ?, ?, now(), ?, ?, now())
                """;

        try (Connection c = getConn()) {
            boolean previousAutoCommit = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                double currentAvailable;
                try (PreparedStatement ps = c.prepareStatement(lockSql)) {
                    ps.setObject(1, resourceId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("Selected resource no longer exists.");
                        }
                        currentAvailable = rs.getDouble("available_qty");
                    }
                }

                if (quantityUsed <= 0) {
                    throw new IllegalArgumentException("Quantity used must be greater than zero.");
                }
                if (quantityUsed > currentAvailable) {
                    throw new IllegalArgumentException("Quantity used cannot be greater than available quantity.");
                }

                double remainingQty = currentAvailable - quantityUsed;
                try (PreparedStatement ps = c.prepareStatement(updateSql)) {
                    ps.setDouble(1, remainingQty);
                    ps.setObject(2, resourceId);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = c.prepareStatement(insertLogSql)) {
                    ps.setObject(1, resourceId);
                    ps.setObject(2, evacuationCenterId);
                    ps.setString(3, evacuationCenterName);
                    ps.setString(4, purpose);
                    ps.setDouble(5, quantityUsed);
                    ps.setDouble(6, remainingQty);
                    ps.executeUpdate();
                }

                c.commit();
            } catch (Exception e) {
                c.rollback();
                if (e instanceof IllegalArgumentException illegalArgumentException) {
                    throw illegalArgumentException;
                }
                throw e;
            } finally {
                c.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not save resource usage", e);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Could not save resource usage", e);
        }
    }

    public List<ResourceLog> findUsageLogs() {
        String sql = """
                SELECT rl.id, rl.resource_id, r.name AS resource_name, r.unit AS resource_unit,
                       rl.evacuation_center_id, rl.evacuation_center_name, rl.purpose,
                       rl.date_used, rl.quantity_used, rl.quantity_available_at_time, rl.created_at
                FROM resource_log rl
                LEFT JOIN resources r ON r.id = rl.resource_id
                ORDER BY rl.date_used DESC NULLS LAST, rl.created_at DESC NULLS LAST
                """;
        List<ResourceLog> list = new ArrayList<>();

        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapLogRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not load resource logs", e);
        }
        return list;
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

    private ResourceLog mapLogRow(ResultSet rs) throws SQLException {
        ResourceLog log = new ResourceLog();
        log.setId(rs.getObject("id", UUID.class));
        log.setResourceId(rs.getObject("resource_id", UUID.class));
        log.setResourceName(rs.getString("resource_name"));
        log.setResourceUnit(rs.getString("resource_unit"));
        log.setEvacuationCenterId(rs.getObject("evacuation_center_id", UUID.class));
        log.setEvacuationCenterName(rs.getString("evacuation_center_name"));
        log.setPurpose(rs.getString("purpose"));
        log.setQuantityUsed(rs.getDouble("quantity_used"));
        log.setQuantityAvailableAtTime(rs.getDouble("quantity_available_at_time"));

        Timestamp dateUsed = rs.getTimestamp("date_used");
        if (dateUsed != null) {
            log.setDateUsed(dateUsed.toLocalDateTime());
        }

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            log.setCreatedAt(createdAt.toLocalDateTime());
        }
        return log;
    }
}
