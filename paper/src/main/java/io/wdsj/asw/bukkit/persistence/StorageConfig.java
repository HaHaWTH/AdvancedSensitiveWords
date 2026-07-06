package io.wdsj.asw.bukkit.persistence;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

public record StorageConfig(
        StorageType type,
        Path sqliteFile,
        String host,
        int port,
        String database,
        String username,
        String password,
        String poolName,
        int maximumPoolSize,
        int minimumIdle,
        Duration connectionTimeout,
        boolean sqliteWal
) {
    public StorageConfig {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(poolName, "poolName");
        Objects.requireNonNull(connectionTimeout, "connectionTimeout");
        if (maximumPoolSize < 1 || minimumIdle < 0 || minimumIdle > maximumPoolSize) {
            throw new IllegalArgumentException("Invalid Hikari pool size settings");
        }
        if (connectionTimeout.isNegative() || connectionTimeout.isZero()) {
            throw new IllegalArgumentException("connectionTimeout must be positive");
        }
        if (type == StorageType.SQLITE && sqliteFile == null) {
            throw new IllegalArgumentException("sqliteFile is required for SQLITE storage");
        }
        if (type == StorageType.MYSQL) {
            if (isBlank(host) || port <= 0 || isBlank(database) || username == null || password == null) {
                throw new IllegalArgumentException("host, port, database, username and password are required for MYSQL storage");
            }
        }
    }

    public static StorageConfig sqlite(Path file, String poolName) {
        return new StorageConfig(
                StorageType.SQLITE,
                file,
                "",
                0,
                "",
                "",
                "",
                poolName,
                3,
                1,
                Duration.ofSeconds(5),
                true
        );
    }

    public static StorageConfig mysql(String host, int port, String database, String username, String password, String poolName) {
        return new StorageConfig(
                StorageType.MYSQL,
                null,
                host,
                port,
                database,
                username,
                password,
                poolName,
                10,
                2,
                Duration.ofSeconds(5),
                false
        );
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
