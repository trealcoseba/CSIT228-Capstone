package com.example.csit228capstone.repository;

import com.example.csit228capstone.model.report.Report;
import com.example.csit228capstone.util.SupabaseConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ReportRepository {
    private Connection getConn() throws SQLException {
        return SupabaseConnectionManager.getInstance().getConnection();
    }

    public List<Report> findAll() {
        String query = "SELECT * FROM reports ORDER BY name ASC";
        List<Report> list = new ArrayList<>();

        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(query);
             ResultSet rs = ps.executeQuery()){
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not load reports", e);
        }
        return list;
    }

    public UUID insert(Report report) {
        String query = "INSERT INTO reports (name, type, generated_by, generated_date_time, start_date, end_date) " +
                "VALUES (?, ?, ?, ?, ?, ?) RETURNING id";

        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(query)) {
            ps.setString(1, report.getName());
            ps.setString(2, report.getType());
            ps.setString(3, report.getGeneratedBy());
            ps.setTimestamp(4, Timestamp.valueOf(report.getGeneratedDateTime()));
            ps.setObject(5, report.getStartDate() != null ? java.sql.Date.valueOf(report.getStartDate()) : null);
            ps.setObject(6, report.getEndDate() != null ? java.sql.Date.valueOf(report.getEndDate()) : null);

            try (ResultSet rs = ps.executeQuery()){
                if (rs.next()) {
                    return rs.getObject("id", UUID.class);
                }

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

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating report failed, no rows affected (ID might not exist).");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not update report name", e);
        }
    }

    public void deleteById(UUID id) {
        String query = "DELETE FROM reports WHERE id = ?";

        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(query)) {
            ps.setObject(1, id);
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                System.out.println("No report found with ID: " + id);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Could not delete report", e);
        }
    }

    private Report mapRow(ResultSet rs) throws SQLException {
        Report report = new Report();

        report.setId(rs.getObject("id", UUID.class));
        report.setName(rs.getString("name"));
        report.setType(rs.getString("type"));
        Timestamp generateDateTime = rs.getTimestamp("generated_date_time");
        if (generateDateTime != null) {
            report.setGeneratedDateTime(generateDateTime.toLocalDateTime());
        }
        report.setGeneratedBy(rs.getString("generated_by"));

        return report;
    }
}