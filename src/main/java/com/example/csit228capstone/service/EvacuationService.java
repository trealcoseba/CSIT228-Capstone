package com.example.csit228capstone.service;

import com.example.csit228capstone.model.EvacuationCenter;
import com.example.csit228capstone.util.SupabaseConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class EvacuationService {

    private static EvacuationService instance;

    public static EvacuationService getInstance() {
        if (instance == null) {
            instance = new EvacuationService();
        }
        return instance;
    }

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    public List<EvacuationCenter> fetchAllCenters() throws Exception {
        List<EvacuationCenter> centers = new ArrayList<>();
        String query = "SELECT * FROM evacuation_centers";

        try (Connection conn = SupabaseConnectionManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                EvacuationCenter center = new EvacuationCenter();
                center.setId(UUID.fromString(rs.getString("id")));
                center.setName(rs.getString("name"));
                center.setAddress(rs.getString("address"));
                center.setMaxCapacity(rs.getInt("max_capacity"));
                center.setCurrentOccupancy(rs.getInt("current_occupancy"));
                center.setActive(rs.getBoolean("is_active"));
                center.setManagerName(rs.getString("manager_of_center"));
                center.setLatitude(rs.getDouble("latitude"));
                center.setLongitude(rs.getDouble("longitude"));
                center.setContactNumber(rs.getString("contact_number"));
                centers.add(center);
            }
        }

        return centers;
    }

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------


    public void insertCenter(EvacuationCenter center) throws Exception {
        String sql = """
                INSERT INTO evacuation_centers
                    (name, address, latitude, longitude, max_capacity,
                     current_occupancy, status, is_active, manager_of_center, contact_number)
                VALUES (?, ?, ?, ?, ?, 0, 'available', ?, ?, ?)
                """;

        try (Connection conn = SupabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, center.getName());
            stmt.setString(2, center.getAddress());
            stmt.setDouble(3, center.getLatitude() != null ? center.getLatitude() : 0.0);
            stmt.setDouble(4, center.getLongitude() != null ? center.getLongitude() : 0.0);
            stmt.setInt(5, center.getMaxCapacity());
            stmt.setBoolean(6, center.isActive());
            stmt.setString(7, nullIfBlank(center.getManagerName()));
            stmt.setString(8, nullIfBlank(center.getContactNumber()));
            stmt.executeUpdate();
        }
    }

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------


    public void updateCenter(EvacuationCenter center) throws Exception {
        String sql = """
                UPDATE evacuation_centers
                SET name = ?, address = ?, latitude = ?, longitude = ?,
                    max_capacity = ?, is_active = ?, manager_of_center = ?, contact_number = ?
                WHERE id = ?
                """;

        try (Connection conn = SupabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, center.getName());
            stmt.setString(2, center.getAddress());
            stmt.setDouble(3, center.getLatitude() != null ? center.getLatitude() : 0.0);
            stmt.setDouble(4, center.getLongitude() != null ? center.getLongitude() : 0.0);
            stmt.setInt(5, center.getMaxCapacity());
            stmt.setBoolean(6, center.isActive());
            stmt.setString(7, nullIfBlank(center.getManagerName()));
            stmt.setString(8, nullIfBlank(center.getContactNumber()));
            stmt.setObject(9, center.getId());
            stmt.executeUpdate();
        }
    }


    public void updateOccupancy(UUID centerId, int newOccupancy) throws Exception {
        String sql = "UPDATE evacuation_centers SET current_occupancy = ? WHERE id = ?";

        try (Connection conn = SupabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, newOccupancy);
            stmt.setObject(2, centerId);
            stmt.executeUpdate();
        }
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    public void deleteCenter(UUID centerId) throws Exception {
        String sql = "DELETE FROM evacuation_centers WHERE id = ?";

        try (Connection conn = SupabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, centerId);
            stmt.executeUpdate();
        }
    }

    // -------------------------------------------------------------------------
    // STATISTICS  — usable by both EvacuationController and the Dashboard
    // -------------------------------------------------------------------------

    public int getTotalEvacuees() throws Exception {
        return fetchAllCenters().stream().mapToInt(EvacuationCenter::getCurrentOccupancy).sum();
    }

    public long getTotalActiveCenters(List<EvacuationCenter> centers) {
        return centers.stream()
                .filter(EvacuationCenter::isActive)
                .count();
    }

    public long getTotalActiveCentersFresh() {
        try {
            return getTotalActiveCenters(fetchAllCenters());
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------

    private String nullIfBlank(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}