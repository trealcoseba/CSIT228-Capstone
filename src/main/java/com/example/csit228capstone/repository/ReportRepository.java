package com.example.csit228capstone.repository;

import com.example.csit228capstone.model.report.Report;
import com.example.csit228capstone.util.SupabaseConnectionManager;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Updated ReportRepository.
 * Changes vs. original:
 *  - {@link #findAllFiltered} — supports search text + sort field + direction.
 *  - {@link #mapRow} — now also reads start_date / end_date from the ResultSet.
 */
public class ReportRepository {

    private Connection getConn() throws SQLException {
        return SupabaseConnectionManager.getInstance().getConnection();
    }

    // ── READ ──────────────────────────────────────────────────────────────────

    public List<Report> findAll() {
        return findAllFiltered("", "generated_date_time", false);
    }

    /**
     * Fetch reports filtered by name/generatedBy and sorted.
     *
     * @param searchText free-text filter applied to name AND generated_by (case-insensitive)
     * @param sortField  "name" | "generated_by" | "generated_date_time" | "start_date" | "end_date"
     * @param ascending  true = ASC, false = DESC
     */
    public List<Report> findAllFiltered(String searchText, String sortField, boolean ascending) {
        String safeSort;
        String field = (sortField == null) ? "" : sortField.toLowerCase();

        switch (field) {
            case "name":
                safeSort = "LOWER(name)";
                break;
            case "generated_by":
                safeSort = "LOWER(generated_by)";
                break;
            case "generated_date_time":
            default:
                safeSort = "generated_date_time";
                break;
        }

        // Standard SQL Logic:
        // ascending = true  -> ASC (A-Z, Oldest Date)
        // ascending = false -> DESC (Z-A, Newest Date)
        String direction = ascending ? "ASC" : "DESC";

        String query = "SELECT * FROM reports " +
                "WHERE (LOWER(name) LIKE ? OR LOWER(generated_by) LIKE ?) " +
                "ORDER BY " + safeSort + " " + direction;

        List<Report> list = new ArrayList<>();
        String like = "%" + (searchText == null ? "" : searchText.toLowerCase()) + "%";

        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(query)) {
            ps.setString(1, like);
            ps.setString(2, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not load reports", e);
        }
        return list;
    }

    // ── WRITE ─────────────────────────────────────────────────────────────────

    public UUID insert(Report report) {
        String query = "INSERT INTO reports (name, type, generated_by, generated_date_time, start_date, end_date) " +
                "VALUES (?, ?, ?, ?, ?, ?) RETURNING id";

        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(query)) {
            ps.setString(1, report.getName());
            ps.setString(2, report.getType());
            ps.setString(3, report.getGeneratedBy());
            ps.setTimestamp(4, Timestamp.valueOf(report.getGeneratedDateTime()));
            ps.setObject(5, report.getStartDate() != null ? Date.valueOf(report.getStartDate()) : null);
            ps.setObject(6, report.getEndDate()   != null ? Date.valueOf(report.getEndDate())   : null);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getObject("id", UUID.class);
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not insert report", e);
        }
    }

    public void update(Report report) {
        String query = "UPDATE reports SET name = ? WHERE id = ?";
        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(query)) {
            ps.setString(1, report.getName());
            ps.setObject(2, report.getId());
            int rows = ps.executeUpdate();
            if (rows == 0) throw new SQLException("No report found with ID: " + report.getId());
        } catch (SQLException e) {
            throw new RuntimeException("Could not update report", e);
        }
    }

    public void deleteById(UUID id) {
        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement("DELETE FROM reports WHERE id = ?")) {
            ps.setObject(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not delete report", e);
        }
    }

    // ── MAPPING ───────────────────────────────────────────────────────────────

    private Report mapRow(ResultSet rs) throws SQLException {
        Report report = new Report();
        report.setId(rs.getObject("id", UUID.class));
        report.setName(rs.getString("name"));
        report.setType(rs.getString("type"));
        Timestamp ts = rs.getTimestamp("generated_date_time");
        if (ts != null) report.setGeneratedDateTime(ts.toLocalDateTime());
        report.setGeneratedBy(rs.getString("generated_by"));

        Date start = rs.getDate("start_date");
        if (start != null) report.setStartDate(start.toLocalDate());
        Date end = rs.getDate("end_date");
        if (end != null) report.setEndDate(end.toLocalDate());

        return report;
    }
}