package com.example.csit228capstone.repository;

import com.example.csit228capstone.util.SupabaseConnectionManager;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;


public class AnalyticsRepository {

    private Connection getConn() throws SQLException {
        return SupabaseConnectionManager.getInstance().getConnection();
    }

    public int countIncidentsThisMonth() {
        String sql = """
                SELECT COUNT(*) FROM incidents
                WHERE DATE_TRUNC('month', reported_at) = DATE_TRUNC('month', NOW())
                """;
        return querySingleInt(sql);
    }

    public int countIncidentsPrevMonth() {
        String sql = """
                SELECT COUNT(*) FROM incidents
                WHERE DATE_TRUNC('month', reported_at)
                      = DATE_TRUNC('month', NOW() - INTERVAL '1 month')
                """;
        return querySingleInt(sql);
    }

    public Map<String, Integer> incidentFrequencyByCategory() {
        String sql = """
                SELECT COALESCE(type::text, 'Unknown') AS category,
                       COUNT(*) AS cnt
                FROM incidents
                GROUP BY type
                ORDER BY cnt DESC
                """;
        Map<String, Integer> result = new LinkedHashMap<>();
        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString("category"), rs.getInt("cnt"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("incidentFrequencyByCategory", e);
        }
        return result;
    }


    public double avgResolutionHoursThisMonth() {
        String sql = """
                SELECT COALESCE(
                    AVG(EXTRACT(EPOCH FROM (resolved_at - reported_at)) / 3600.0), 0)
                FROM incidents
                WHERE resolved_at IS NOT NULL
                  AND DATE_TRUNC('month', reported_at) = DATE_TRUNC('month', NOW())
                """;
        return querySingleDouble(sql);
    }

    public double avgResolutionHoursPrevMonth() {
        String sql = """
                SELECT COALESCE(
                    AVG(EXTRACT(EPOCH FROM (resolved_at - reported_at)) / 3600.0), 0)
                FROM incidents
                WHERE resolved_at IS NOT NULL
                  AND DATE_TRUNC('month', reported_at)
                      = DATE_TRUNC('month', NOW() - INTERVAL '1 month')
                """;
        return querySingleDouble(sql);
    }

    public Map<String, Double> avgResolutionByCategory() {
        String sql = """
                SELECT COALESCE(type::text, 'Unknown') AS category,
                       COALESCE(
                           AVG(EXTRACT(EPOCH FROM (resolved_at - reported_at)) / 3600.0), 0)
                       AS avg_hours
                FROM incidents
                WHERE resolved_at IS NOT NULL
                GROUP BY type
                ORDER BY avg_hours DESC
                """;
        Map<String, Double> result = new LinkedHashMap<>();
        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString("category"), rs.getDouble("avg_hours"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("avgResolutionByCategory", e);
        }
        return result;
    }


    public double currentShelterUtilizationPct() {
        String sql = """
                SELECT CASE WHEN SUM(max_capacity) = 0 THEN 0
                            ELSE ROUND(100.0 * SUM(current_occupancy) / SUM(max_capacity), 1)
                       END
                FROM evacuation_centers
                WHERE is_active = TRUE
                """;
        return querySingleDouble(sql);
    }


    public double peakShelterUtilizationPrevMonth() {
        String sql = """
                SELECT COALESCE(
                    MAX(CASE WHEN max_capacity = 0 THEN 0
                             ELSE ROUND(100.0 * occupancy / max_capacity, 1) END), 0)
                FROM evacuation_occupancy_log
                WHERE DATE_TRUNC('month', logged_at)
                      = DATE_TRUNC('month', NOW() - INTERVAL '1 month')
                """;
        try {
            return querySingleDouble(sql);
        } catch (RuntimeException e) {
            return 0.0;
        }
    }


    public Map<String, double[]> resourceUsagePerCenter() {
        String sql = """
                SELECT ec.name AS center_name,
                       COALESCE(SUM(r.total_qty - r.available_qty), 0) AS used_qty,
                       COALESCE(SUM(r.available_qty), 0)               AS avail_qty
                FROM evacuation_centers ec
                LEFT JOIN resource_allocations ra ON ra.center_id = ec.id
                LEFT JOIN resources r             ON r.id          = ra.resource_id
                WHERE ec.is_active = TRUE
                GROUP BY ec.name
                ORDER BY ec.name
                """;
        Map<String, double[]> result = new LinkedHashMap<>();
        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString("center_name"),
                        new double[]{rs.getDouble("used_qty"), rs.getDouble("avail_qty")});
            }
        } catch (SQLException e) {
        }

        if (result.isEmpty()) {
            String fallback = """
                    SELECT 'All Centers' AS center_name,
                           COALESCE(SUM(total_qty - available_qty), 0) AS used_qty,
                           COALESCE(SUM(available_qty), 0)             AS avail_qty
                    FROM resources
                    """;
            try (Connection c = getConn();
                 PreparedStatement ps = c.prepareStatement(fallback);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    result.put(rs.getString("center_name"),
                            new double[]{rs.getDouble("used_qty"), rs.getDouble("avail_qty")});
                }
            } catch (SQLException ex) {
                throw new RuntimeException("resourceUsagePerCenter fallback", ex);
            }
        }
        return result;
    }

    public double vulnerabilityRatioPct() {
        String sql = """
                SELECT CASE WHEN (SELECT COUNT(*) FROM residents) = 0 THEN 0
                            ELSE ROUND(
                                100.0 * COUNT(DISTINCT resident_id)
                                      / (SELECT COUNT(*) FROM residents), 1)
                       END
                FROM resident_vulnerabilities
                """;
        return querySingleDouble(sql);
    }


    public double vulnerabilityRatioPrevMonthPct() {
        String sql = """
                SELECT CASE WHEN total = 0 THEN 0
                            ELSE ROUND(100.0 * vuln / total, 1)
                       END
                FROM (
                    SELECT
                        COUNT(DISTINCT r.id) FILTER (WHERE rv.resident_id IS NOT NULL) AS vuln,
                        COUNT(DISTINCT r.id) AS total
                    FROM residents r
                    LEFT JOIN resident_vulnerabilities rv ON rv.resident_id = r.id
                    WHERE DATE_TRUNC('month', r.created_at)
                          = DATE_TRUNC('month', NOW() - INTERVAL '1 month')
                ) sub
                """;
        return querySingleDouble(sql);
    }

    
    public Map<String, Integer> vulnerabilityTagBreakdown() {
        String sql = """
                SELECT tag::text AS tag, COUNT(DISTINCT resident_id) AS cnt
                FROM resident_vulnerabilities
                GROUP BY tag
                ORDER BY cnt DESC
                """;
        Map<String, Integer> result = new LinkedHashMap<>();
        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String raw = rs.getString("tag").replace("_", " ");
                String pretty = capitaliseWords(raw);
                result.put(pretty, rs.getInt("cnt"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("vulnerabilityTagBreakdown", e);
        }
        return result;
    }

    private int querySingleInt(String sql) {
        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException(sql, e);
        }
    }

    private double querySingleDouble(String sql) {
        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getDouble(1);
        } catch (SQLException e) {
            throw new RuntimeException(sql, e);
        }
    }

    private static String capitaliseWords(String input) {
        if (input == null || input.isBlank()) return input;
        String[] words = input.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0)))
                        .append(w.substring(1).toLowerCase())
                        .append(' ');
            }
        }
        return sb.toString().trim();
    }
}