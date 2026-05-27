package com.example.csit228capstone.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;


public class SupabaseConnectionManager {

    private static SupabaseConnectionManager instance;
    private final HikariDataSource dataSource;

    private SupabaseConnectionManager() {
        String host     = env("SUPABASE_DB_HOST", "aws-1-ap-northeast-1.pooler.supabase.com");
        String port     = env("SUPABASE_DB_PORT", "6543");
        String dbName   = env("SUPABASE_DB_NAME", "postgres");
        String user     = env("SUPABASE_DB_USER", "postgres.ojgnbqgazkkdimdodqqc");
        String password = env("SUPABASE_DB_PASSWORD", "S4jLma76_@kE#.Y");

        HikariConfig config = new HikariConfig();

        config.setDriverClassName("org.postgresql.Driver");
        config.setJdbcUrl(
                "jdbc:postgresql://" + host + ":" + port + "/" + dbName +
                        "?sslmode=require" +
                        "&prepareThreshold=0"

        );
        config.setUsername(user);
        config.setPassword(password);

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30_000);
        config.setIdleTimeout(600_000);
        config.setMaxLifetime(1_800_000);

        this.dataSource = new HikariDataSource(config);
    }

    public static synchronized SupabaseConnectionManager getInstance() {
        if (instance == null) {
            instance = new SupabaseConnectionManager();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    private static String env(String key, String defaultValue) {
        String val = System.getenv(key);
        return (val != null && !val.isEmpty()) ? val : defaultValue;
    }
}