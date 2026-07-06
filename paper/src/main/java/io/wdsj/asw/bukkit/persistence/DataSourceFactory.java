package io.wdsj.asw.bukkit.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class DataSourceFactory {
    private DataSourceFactory() {
    }

    public static HikariDataSource create(StorageConfig storage) throws SQLException {
        suppressHikariLifecycleLogs();
        HikariConfig config = new HikariConfig();
        config.setPoolName(storage.poolName());
        config.setMaximumPoolSize(storage.maximumPoolSize());
        config.setMinimumIdle(storage.minimumIdle());
        config.setConnectionTimeout(storage.connectionTimeout().toMillis());

        if (storage.type() == StorageType.SQLITE) {
            Path file = storage.sqliteFile().toAbsolutePath();
            Path parent = file.getParent();
            try {
                if (parent != null) {
                    Files.createDirectories(parent);
                }
            } catch (Exception exception) {
                throw new SQLException("Unable to create SQLite parent directory: " + parent, exception);
            }
            config.setJdbcUrl("jdbc:sqlite:" + file);
            config.setDriverClassName("org.sqlite.JDBC");
            config.setMaximumPoolSize(Math.min(storage.maximumPoolSize(), 3));
            if (storage.sqliteWal()) {
                config.setConnectionInitSql("PRAGMA synchronous=NORMAL");
            }
        } else {
            config.setJdbcUrl("jdbc:mysql://" + storage.host() + ":" + storage.port() + "/" + storage.database()
                    + "?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=UTC");
            config.setUsername(storage.username());
            config.setPassword(storage.password());
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        }

        HikariDataSource dataSource = new HikariDataSource(config);
        if (storage.type() == StorageType.SQLITE && storage.sqliteWal()) {
            try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA journal_mode=WAL");
            }
        }
        return dataSource;
    }

    private static void suppressHikariLifecycleLogs() {
        System.setProperty("org.slf4j.simpleLogger.log.com.zaxxer.hikari", "warn");
        try {
            Class<?> levelClass = Class.forName("org.apache.logging.log4j.Level");
            Object warn = levelClass.getField("WARN").get(null);
            Class<?> configuratorClass = Class.forName("org.apache.logging.log4j.core.config.Configurator");
            configuratorClass.getMethod("setLevel", String.class, levelClass)
                    .invoke(null, "com.zaxxer.hikari", warn);
        } catch (Throwable ignored) {
        }
    }
}
