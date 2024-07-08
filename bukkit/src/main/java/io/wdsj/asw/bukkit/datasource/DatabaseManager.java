package io.wdsj.asw.bukkit.datasource;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.wdsj.asw.bukkit.AdvancedSensitiveWords;
import io.wdsj.asw.bukkit.setting.PluginSettings;
import io.wdsj.asw.bukkit.util.VTUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.*;
import java.util.Locale;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.LOGGER;
import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager;

public class DatabaseManager {

    private final ThreadFactory threadFactory = VTUtils.getVTThreadFactoryOrProvided(new ThreadFactoryBuilder().setDaemon(true).build());
    private final File dbFile = new File(AdvancedSensitiveWords.getInstance().getDataFolder(), settingsManager.getProperty(PluginSettings.DATABASE_NAME));
    private final Cache<String, String> dbReadCache = CacheBuilder.newBuilder()
            .expireAfterWrite(settingsManager.getProperty(PluginSettings.DATABASE_CACHE_TIME), TimeUnit.SECONDS)
            .build();

    private HikariDataSource dataSource;

    /**
     * Set up the data source.
     */
    public void setupDataSource() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.sqlite.JDBC");
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getPath());
        config.setThreadFactory(threadFactory);
        config.setMaximumPoolSize(1);
        config.setConnectionTestQuery("SELECT 1");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        dataSource = new HikariDataSource(config);
        initializeDatabase();
    }

    /**
     * Close the data source.
     */
    public void closeDataSource() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    /**
     * Get the data source.
     * @return The data source.
     */
    public HikariDataSource getDataSource() {
        return dataSource;
    }

    /**
     * Initialize the database.
     */
    private void initializeDatabase() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            String createTableSQL = "CREATE TABLE IF NOT EXISTS AdvancedSensitiveWords (" +
                    "player_name VARCHAR(255) PRIMARY KEY, " +
                    "violations BIGINT DEFAULT 0)";
            stmt.execute(createTableSQL);
            String createIndexSQL = "CREATE INDEX IF NOT EXISTS" +
                    " idx_player_name ON AdvancedSensitiveWords(player_name)";
            stmt.execute(createIndexSQL);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    /**
     * Get the player violations.
     * @param playerName The player name.
     * @return The player violations, null if not found.
     */
    @Nullable
    public String getPlayerViolations(String playerName) {
        String loweredPlayerName = playerName.toLowerCase(Locale.ROOT);
        String cachedVl = dbReadCache.getIfPresent(loweredPlayerName);
        if (cachedVl != null) {
            return cachedVl;
        }
        String query = "SELECT violations FROM AdvancedSensitiveWords WHERE player_name = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, loweredPlayerName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String vl = String.valueOf(rs.getLong("violations"));
                dbReadCache.put(loweredPlayerName, vl);
                rs.close();
                return vl;
            }
            rs.close();
            return null;
        } catch (SQLException e) {
            LOGGER.warning("Error occurred while getting player violations: " + e.getMessage());
        }
        return null;
    }

    /**
     * Get the total violations in the database.
     * @return The total violations.
     */
    public long getTotalViolations() {
        String query = "SELECT SUM(violations) AS total_violations FROM AdvancedSensitiveWords";
        long totalViolations = 0;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                totalViolations = rs.getLong("total_violations");
            }
        } catch (SQLException e) {
            LOGGER.warning("Error occurred while getting total violations: " + e.getMessage());
        }

        return totalViolations;
    }

    /**
     * Check and update the player violations.
     * @param playerName The player name.
     */
    public void checkAndUpdatePlayer(String playerName) {
        String loweredPlayerName = playerName.toLowerCase(Locale.ROOT);
        String query = "SELECT violations FROM AdvancedSensitiveWords WHERE player_name = ?";
        String insert = "INSERT INTO AdvancedSensitiveWords (player_name, violations) VALUES (?, 1)";
        String update = "UPDATE AdvancedSensitiveWords SET violations = violations + 1 WHERE player_name = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, loweredPlayerName);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                try (PreparedStatement updateStmt = conn.prepareStatement(update)) {
                    updateStmt.setString(1, loweredPlayerName);
                    updateStmt.executeUpdate();
                    String updatedVl = String.valueOf(rs.getLong("violations") + 1);
                    dbReadCache.put(loweredPlayerName, updatedVl);
                }
            } else {
                try (PreparedStatement insertStmt = conn.prepareStatement(insert)) {
                    insertStmt.setString(1, loweredPlayerName);
                    insertStmt.executeUpdate();
                    dbReadCache.put(loweredPlayerName, "1");
                }
            }
            rs.close();
        } catch (SQLException e) {
            LOGGER.warning("Error occurred while updating player VLs: " + e.getMessage());
        }
    }
}