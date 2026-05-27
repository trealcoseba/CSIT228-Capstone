package com.example.csit228capstone.repository;

import com.example.csit228capstone.model.AdminProfile;
import com.example.csit228capstone.util.SupabaseConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserRepository {

    private Connection getConn() throws SQLException {
        return SupabaseConnectionManager.getInstance().getConnection();
    }

    // ── Auth ──────────────────────────────────────────────────────────────────
    public boolean authenticate(String username, String password) {
        String query = "SELECT * FROM admin WHERE username = ? AND password = ?";
        try (Connection conn = getConn();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ── Fetch profile ─────────────────────────────────────────────────────────
    public AdminProfile getAdminProfile(String username) {
        String query = "SELECT id, username, first_name, last_name FROM admin WHERE username = ?";
        try (Connection conn = getConn();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new AdminProfile(
                            rs.getString("id"),
                            rs.getString("username"),
                            rs.getString("first_name"),
                            rs.getString("last_name")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ── Update profile ────────────────────────────────────────────────────────
    public boolean updateProfile(String id, String firstName, String lastName) {
        String query = "UPDATE admin SET first_name = ?, last_name = ? WHERE id = ?::uuid";
        try (Connection conn = getConn();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, firstName);
            pstmt.setString(2, lastName);
            pstmt.setString(3, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ── Change password ───────────────────────────────────────────────────────
    public boolean changePassword(String id, String oldPassword, String newPassword) {
        String verify = "SELECT id FROM admin WHERE id = ?::uuid AND password = ?";
        try (Connection conn = getConn();
             PreparedStatement check = conn.prepareStatement(verify)) {

            check.setString(1, id);
            check.setString(2, oldPassword);
            try (ResultSet rs = check.executeQuery()) {
                if (!rs.next()) return false;
            }

            String update = "UPDATE admin SET password = ? WHERE id = ?::uuid";
            try (PreparedStatement pstmt = conn.prepareStatement(update)) {
                pstmt.setString(1, newPassword);
                pstmt.setString(2, id);
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}