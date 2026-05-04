package com.example.csit228capstone.app;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import com.example.csit228capstone.util.SupabaseConnectionManager;

public class DBtest {
    public static void main(String[] args) {

        try (Connection conn =
                     SupabaseConnectionManager.getInstance().getConnection()) {

            System.out.println("✅ CONNECTION SUCCESS");

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT 1");

            if (rs.next()) {
                System.out.println("DB RESPONSE: " + rs.getInt(1));
            }

        } catch (Exception e) {
            System.out.println("❌ CONNECTION FAILED");
            e.printStackTrace();
        }
    }
}