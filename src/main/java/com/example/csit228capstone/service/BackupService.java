package com.example.csit228capstone.service;

import com.example.csit228capstone.util.SupabaseConnectionManager;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class BackupService {

    public void backup(String outputPath) throws Exception {
        try (Connection conn = SupabaseConnectionManager.getInstance().getConnection();
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {

            writer.write("-- Backup generated: " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            writer.newLine();
            writer.write("-- ================================================");
            writer.newLine();
            writer.newLine();

            List<String> tables = getAllTables(conn);

            if (tables.isEmpty()) {
                writer.write("-- No tables found in the public schema.");
                writer.newLine();
                return;
            }

            for (String table : tables) {
                dumpTable(conn, writer, table);
            }

            writer.write("-- ================================================");
            writer.newLine();
            writer.write("-- Total tables backed up: " + tables.size());
            writer.newLine();
        }
    }

    private List<String> getAllTables(Connection conn) throws SQLException {
        List<String> tables = new ArrayList<>();
        DatabaseMetaData meta = conn.getMetaData();

        try (ResultSet rs = meta.getTables(null, "public", "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
            }
        }

        return tables;
    }

    private void dumpTable(Connection conn, BufferedWriter writer, String table)
            throws SQLException, IOException {

        writer.write("-- Table: " + table);
        writer.newLine();

        String query = "SELECT * FROM \"" + table + "\"";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();

            StringBuilder colNames = new StringBuilder("(");
            for (int i = 1; i <= colCount; i++) {
                colNames.append("\"").append(meta.getColumnName(i)).append("\"");
                if (i < colCount) colNames.append(", ");
            }
            colNames.append(")");

            int rowCount = 0;
            while (rs.next()) {
                StringBuilder insert = new StringBuilder();
                insert.append("INSERT INTO \"").append(table).append("\" ")
                        .append(colNames).append(" VALUES (");

                for (int i = 1; i <= colCount; i++) {
                    String value = rs.getString(i);
                    if (value == null) {
                        insert.append("NULL");
                    } else {
                        insert.append("'").append(value.replace("'", "''")).append("'");
                    }
                    if (i < colCount) insert.append(", ");
                }
                insert.append(");");

                writer.write(insert.toString());
                writer.newLine();
                rowCount++;
            }

            writer.write("-- " + rowCount + " row(s) exported from " + table);
            writer.newLine();
            writer.newLine();
        }
    }
}