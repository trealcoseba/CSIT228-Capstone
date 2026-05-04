package com.example.csit228capstone.service;

import com.example.csit228capstone.model.alert.Alert;
import com.example.csit228capstone.model.AlertPriority;
import com.example.csit228capstone.util.AlertEventBus;
import com.example.csit228capstone.util.AlertEventBus.EventType;
import com.example.csit228capstone.util.SupabaseConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AlertService {

    public UUID broadcastAlert(String title, String body, AlertPriority priority, String targetPurok, UUID issuedBy) {
        String sql = "INSERT INTO alerts (title, body, priority, is_broadcast, target_purok, issued_by) " +
                     "VALUES (?,?::alert_priority,?,TRUE,?,?) RETURNING id";
        try (Connection c = SupabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, priority.name().toLowerCase());
            ps.setString(3, body);
            ps.setString(4, targetPurok);
            ps.setObject(5, issuedBy);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                UUID id = rs.getObject("id", UUID.class);
                AlertEventBus.getInstance().publish(EventType.ALERT_BROADCAST, id);
                return id;
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public List<Alert> getRecent(int limit) {
        String sql = "SELECT * FROM alerts ORDER BY created_at DESC LIMIT ?";
        List<Alert> list = new ArrayList<>();
        try (Connection c = SupabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Alert a = new Alert();
                    a.setId(rs.getObject("id", UUID.class));
                    a.setTitle(rs.getString("title"));
                    a.setBody(rs.getString("body"));
                    a.setPriority(AlertPriority.valueOf(rs.getString("priority").toUpperCase()));
                    a.setBroadcast(rs.getBoolean("is_broadcast"));
                    a.setTargetPurok(rs.getString("target_purok"));
                    a.setIssuedBy(rs.getObject("issued_by", UUID.class));
                    a.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    list.add(a);
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }
}
