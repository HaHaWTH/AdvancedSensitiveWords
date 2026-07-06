package io.wdsj.asw.bukkit.playergroup;

import io.wdsj.asw.bukkit.persistence.StorageType;
import io.wdsj.asw.bukkit.persistence.WriteBackRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class PlayerGroupStateRepository implements WriteBackRepository<UUID, PlayerGroupState> {
    private static final String TABLE = "asw_player_group_states";

    private final DataSource dataSource;
    private final StorageType storageType;

    PlayerGroupStateRepository(DataSource dataSource, StorageType storageType) {
        this.dataSource = dataSource;
        this.storageType = storageType;
    }

    void initialize() throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            if (storageType == StorageType.MYSQL) {
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS asw_player_group_states (
                          player_uuid VARCHAR(36) NOT NULL PRIMARY KEY,
                          player_name_lower VARCHAR(64) NOT NULL,
                          group_name VARCHAR(16) NOT NULL,
                          manual_override BOOLEAN NOT NULL,
                          updated_at BIGINT NOT NULL,
                          updated_by_uuid VARCHAR(36) NULL,
                          updated_by_name VARCHAR(64) NOT NULL,
                          INDEX idx_asw_group_state_player_name (player_name_lower),
                          INDEX idx_asw_group_state_group_name (group_name),
                          INDEX idx_asw_group_state_manual (manual_override)
                        )
                        """);
            } else {
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS asw_player_group_states (
                          player_uuid TEXT NOT NULL PRIMARY KEY,
                          player_name_lower TEXT NOT NULL,
                          group_name TEXT NOT NULL,
                          manual_override INTEGER NOT NULL,
                          updated_at INTEGER NOT NULL,
                          updated_by_uuid TEXT NULL,
                          updated_by_name TEXT NOT NULL
                        )
                        """);
                statement.execute("CREATE INDEX IF NOT EXISTS idx_asw_group_state_player_name ON " + TABLE + " (player_name_lower)");
                statement.execute("CREATE INDEX IF NOT EXISTS idx_asw_group_state_group_name ON " + TABLE + " (group_name)");
                statement.execute("CREATE INDEX IF NOT EXISTS idx_asw_group_state_manual ON " + TABLE + " (manual_override)");
            }
        }
    }

    @Override
    public Optional<PlayerGroupState> load(UUID key) throws Exception {
        String sql = "SELECT player_uuid, player_name_lower, group_name, manual_override, updated_at, updated_by_uuid, updated_by_name FROM "
                + TABLE + " WHERE player_uuid = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, key.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(readState(resultSet));
            }
        }
    }

    @Override
    public void save(UUID key, PlayerGroupState value) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            saveWithConnection(connection, value);
        }
    }

    @Override
    public void delete(UUID key) throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM " + TABLE + " WHERE player_uuid = ?")) {
            statement.setString(1, key.toString());
            statement.executeUpdate();
        }
    }

    @Override
    public void flush(Map<UUID, Optional<PlayerGroupState>> entries) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                for (Map.Entry<UUID, Optional<PlayerGroupState>> entry : entries.entrySet()) {
                    if (entry.getValue().isPresent()) {
                        saveWithConnection(connection, entry.getValue().get());
                    } else {
                        deleteWithConnection(connection, entry.getKey());
                    }
                }
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    private void saveWithConnection(Connection connection, PlayerGroupState value) throws SQLException {
        String sql;
        if (storageType == StorageType.MYSQL) {
            sql = value.manualOverride()
                    ? "INSERT INTO " + TABLE + " (player_uuid, player_name_lower, group_name, manual_override, updated_at, updated_by_uuid, updated_by_name) VALUES (?, ?, ?, ?, ?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE player_name_lower = VALUES(player_name_lower), group_name = VALUES(group_name), manual_override = VALUES(manual_override), updated_at = VALUES(updated_at), updated_by_uuid = VALUES(updated_by_uuid), updated_by_name = VALUES(updated_by_name)"
                    : "INSERT INTO " + TABLE + " (player_uuid, player_name_lower, group_name, manual_override, updated_at, updated_by_uuid, updated_by_name) VALUES (?, ?, ?, ?, ?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE player_name_lower = IF(manual_override, player_name_lower, VALUES(player_name_lower)), group_name = IF(manual_override, group_name, VALUES(group_name)), manual_override = IF(manual_override, manual_override, VALUES(manual_override)), updated_at = IF(manual_override, updated_at, VALUES(updated_at)), updated_by_uuid = IF(manual_override, updated_by_uuid, VALUES(updated_by_uuid)), updated_by_name = IF(manual_override, updated_by_name, VALUES(updated_by_name))";
        } else {
            sql = value.manualOverride()
                    ? "INSERT INTO " + TABLE + " (player_uuid, player_name_lower, group_name, manual_override, updated_at, updated_by_uuid, updated_by_name) VALUES (?, ?, ?, ?, ?, ?, ?) "
                    + "ON CONFLICT(player_uuid) DO UPDATE SET player_name_lower = excluded.player_name_lower, group_name = excluded.group_name, manual_override = excluded.manual_override, updated_at = excluded.updated_at, updated_by_uuid = excluded.updated_by_uuid, updated_by_name = excluded.updated_by_name"
                    : "INSERT INTO " + TABLE + " (player_uuid, player_name_lower, group_name, manual_override, updated_at, updated_by_uuid, updated_by_name) VALUES (?, ?, ?, ?, ?, ?, ?) "
                    + "ON CONFLICT(player_uuid) DO UPDATE SET player_name_lower = excluded.player_name_lower, group_name = excluded.group_name, manual_override = excluded.manual_override, updated_at = excluded.updated_at, updated_by_uuid = excluded.updated_by_uuid, updated_by_name = excluded.updated_by_name WHERE " + TABLE + ".manual_override = 0";
        }
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindSave(statement, value);
            statement.executeUpdate();
        }
    }

    private void deleteWithConnection(Connection connection, UUID key) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM " + TABLE + " WHERE player_uuid = ?")) {
            statement.setString(1, key.toString());
            statement.executeUpdate();
        }
    }

    private static void bindSave(PreparedStatement statement, PlayerGroupState value) throws SQLException {
        statement.setString(1, value.playerUuid().toString());
        statement.setString(2, value.playerNameLower());
        statement.setString(3, value.group().name());
        statement.setBoolean(4, value.manualOverride());
        statement.setLong(5, value.updatedAtMillis());
        statement.setString(6, value.updatedByUuid() == null ? null : value.updatedByUuid().toString());
        statement.setString(7, value.updatedByName());
    }

    private static PlayerGroupState readState(ResultSet resultSet) throws SQLException {
        String updatedByUuid = resultSet.getString("updated_by_uuid");
        return new PlayerGroupState(
                UUID.fromString(resultSet.getString("player_uuid")),
                resultSet.getString("player_name_lower"),
                PlayerGroup.valueOf(resultSet.getString("group_name")),
                resultSet.getBoolean("manual_override"),
                resultSet.getLong("updated_at"),
                updatedByUuid == null ? null : UUID.fromString(updatedByUuid),
                resultSet.getString("updated_by_name")
        );
    }
}
