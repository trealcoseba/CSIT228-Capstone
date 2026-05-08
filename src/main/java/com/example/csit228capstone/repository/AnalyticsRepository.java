package com.example.csit228capstone.repository;

import com.example.csit228capstone.util.SupabaseConnectionManager;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Raw analytics queries.
 * Every method opens its own connection so it can be called from a background
 * thread without sharing state.
 *
 * Adjusted to match the actual `incidents` table schema:
 *   - `type`        replaces `disaster_category`
 *   - `reported_at` replaces `created_at`
 */
public class AnalyticsRepository {

    private Connection getConn() throws SQLException {
        return SupabaseConnectionManager.getInstance().getConnection();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INCIDENT COUNTS  (table: incidents)
    // ─────────────────────────────────────────────────────────────────────────

    /** Total incidents reported in the current calendar month. */
    public int countIncidentsThisMonth() {
        String sql = """
                SELECT COUNT(*) FROM incidents
                WHERE DATE_TRUNC('month', reported_at) = DATE_TRUNC('month', NOW())
                """;
        return querySingleInt(sql);
    }

    /** Total incidents reported in the previous calendar month. */
    public int countIncidentsPrevMonth() {
        String sql = """
                SELECT COUNT(*) FROM incidents
                WHERE DATE_TRUNC('month', reported_at)
                      = DATE_TRUNC('month', NOW() - INTERVAL '1 month')
                """;
        return querySingleInt(sql);
    }

    /**
     * Frequency of incidents per incident type (all time).
     * Returns a map of  type → count  ordered by count desc.
     */
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

    // ─────────────────────────────────────────────────────────────────────────
    // RESOLUTION TIME  (hours between reported_at and resolved_at)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Average resolution time (hours) for the current month.
     * Returns 0.0 when there are no resolved incidents.
     */
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

    /** Same metric for the previous month. */
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

    /**
     * Average resolution time (hours) per incident type.
     */
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

    // ─────────────────────────────────────────────────────────────────────────
    // SHELTER UTILISATION  (evacuation_centers)
    // ─────────────────────────────────────────────────────────────────────────

    /** Current overall utilisation % across all active centres. */
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

    /**
     * "Peak last month" – maximum single-day occupancy ratio recorded in the
     * previous month.  Falls back to 0 if the optional log table doesn't exist.
     *
     * Assumes an optional table: evacuation_occupancy_log(center_id, logged_at,
     * occupancy, max_capacity).
     */
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
            // Table doesn't exist – swallow and return 0
            return 0.0;
        }
    }

    /**
     * Per-center resource breakdown: used_qty and available_qty per center.
     * Returns map of centerName → [usedQty, availableQty].
     */
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
            // resource_allocations may not exist – fall through to global fallback
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

    // ─────────────────────────────────────────────────────────────────────────
    // VULNERABILITY RATIO  (resident_vulnerabilities)
    // ─────────────────────────────────────────────────────────────────────────

    /** % of residents that have at least one vulnerability tag (current). */
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

    /**
     * Vulnerability ratio for residents created last month.
     */
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

    /**
     * Demographic breakdown: vulnerability tag → distinct resident count.
     * Used by the PieChart.
     */
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

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

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