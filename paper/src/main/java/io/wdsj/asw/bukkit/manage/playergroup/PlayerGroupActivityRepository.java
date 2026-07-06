package io.wdsj.asw.bukkit.manage.playergroup;

import io.wdsj.asw.bukkit.core.persistence.StorageType;
import io.wdsj.asw.bukkit.core.persistence.WriteBackRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class PlayerGroupActivityRepository implements WriteBackRepository<UUID, PlayerGroupActivityState> {
    private static final String TABLE = "asw_player_group_activity";

    private final DataSource dataSource;
    private final StorageType storageType;
    private final String serverId;

    PlayerGroupActivityRepository(DataSource dataSource, StorageType storageType, String serverId) {
        this.dataSource = dataSource;
        this.storageType = storageType;
        this.serverId = serverId;
    }

    void initialize() throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            if (storageType == StorageType.MYSQL) {
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS asw_player_group_activity (
                          player_uuid VARCHAR(36) NOT NULL,
                          server_id VARCHAR(64) NOT NULL,
                          player_name_lower VARCHAR(64) NOT NULL,
                          updated_at BIGINT NOT NULL,
                          play_time_hours DOUBLE NOT NULL,
                          mined_blocks BIGINT NOT NULL,
                          moved_blocks DOUBLE NOT NULL,
                          mob_kills BIGINT NOT NULL,
                          used_items BIGINT NOT NULL,
                          broken_items BIGINT NOT NULL,
                          crafted_items BIGINT NOT NULL,
                          damage_dealt BIGINT NOT NULL,
                          damage_taken BIGINT NOT NULL,
                          deaths BIGINT NOT NULL,
                          enchanted_items BIGINT NOT NULL,
                          fish_caught BIGINT NOT NULL,
                          villager_trades BIGINT NOT NULL,
                          PRIMARY KEY (player_uuid, server_id),
                          INDEX idx_asw_group_activity_player_name (player_name_lower),
                          INDEX idx_asw_group_activity_server (server_id)
                        )
                        """);
                return;
            }
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS asw_player_group_activity (
                      player_uuid TEXT NOT NULL,
                      server_id TEXT NOT NULL,
                      player_name_lower TEXT NOT NULL,
                      updated_at INTEGER NOT NULL,
                      play_time_hours REAL NOT NULL,
                      mined_blocks INTEGER NOT NULL,
                      moved_blocks REAL NOT NULL,
                      mob_kills INTEGER NOT NULL,
                      used_items INTEGER NOT NULL,
                      broken_items INTEGER NOT NULL,
                      crafted_items INTEGER NOT NULL,
                      damage_dealt INTEGER NOT NULL,
                      damage_taken INTEGER NOT NULL,
                      deaths INTEGER NOT NULL,
                      enchanted_items INTEGER NOT NULL,
                      fish_caught INTEGER NOT NULL,
                      villager_trades INTEGER NOT NULL,
                      PRIMARY KEY (player_uuid, server_id)
                    )
                    """);
            statement.execute("CREATE INDEX IF NOT EXISTS idx_asw_group_activity_player_name ON " + TABLE + " (player_name_lower)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_asw_group_activity_server ON " + TABLE + " (server_id)");
        }
    }

    @Override
    public Optional<PlayerGroupActivityState> load(UUID key) throws Exception {
        String sql = "SELECT * FROM " + TABLE + " WHERE player_uuid = ? AND server_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, key.toString());
            statement.setString(2, serverId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(readState(resultSet));
            }
        }
    }

    PlayerActivitySnapshot loadRemoteTotal(UUID playerId) throws SQLException {
        String sql = """
                SELECT
                  COALESCE(SUM(play_time_hours), 0) AS play_time_hours,
                  COALESCE(SUM(mined_blocks), 0) AS mined_blocks,
                  COALESCE(SUM(moved_blocks), 0) AS moved_blocks,
                  COALESCE(SUM(mob_kills), 0) AS mob_kills,
                  COALESCE(SUM(used_items), 0) AS used_items,
                  COALESCE(SUM(broken_items), 0) AS broken_items,
                  COALESCE(SUM(crafted_items), 0) AS crafted_items,
                  COALESCE(SUM(damage_dealt), 0) AS damage_dealt,
                  COALESCE(SUM(damage_taken), 0) AS damage_taken,
                  COALESCE(SUM(deaths), 0) AS deaths,
                  COALESCE(SUM(enchanted_items), 0) AS enchanted_items,
                  COALESCE(SUM(fish_caught), 0) AS fish_caught,
                  COALESCE(SUM(villager_trades), 0) AS villager_trades
                FROM %s
                WHERE player_uuid = ? AND server_id <> ?
                """.formatted(TABLE);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerId.toString());
            statement.setString(2, serverId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return PlayerActivitySnapshot.empty();
                }
                return readSnapshot(resultSet);
            }
        }
    }

    @Override
    public void save(UUID key, PlayerGroupActivityState value) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            saveWithConnection(connection, value);
        }
    }

    @Override
    public void delete(UUID key) throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM " + TABLE + " WHERE player_uuid = ? AND server_id = ?")) {
            statement.setString(1, key.toString());
            statement.setString(2, serverId);
            statement.executeUpdate();
        }
    }

    @Override
    public void flush(Map<UUID, Optional<PlayerGroupActivityState>> entries) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                for (Map.Entry<UUID, Optional<PlayerGroupActivityState>> entry : entries.entrySet()) {
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

    private void saveWithConnection(Connection connection, PlayerGroupActivityState value) throws SQLException {
        String sql = storageType == StorageType.MYSQL
                ? "INSERT INTO " + TABLE + " (player_uuid, server_id, player_name_lower, updated_at, play_time_hours, mined_blocks, moved_blocks, mob_kills, used_items, broken_items, crafted_items, damage_dealt, damage_taken, deaths, enchanted_items, fish_caught, villager_trades) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE player_name_lower = VALUES(player_name_lower), updated_at = VALUES(updated_at), play_time_hours = VALUES(play_time_hours), mined_blocks = VALUES(mined_blocks), moved_blocks = VALUES(moved_blocks), mob_kills = VALUES(mob_kills), used_items = VALUES(used_items), broken_items = VALUES(broken_items), crafted_items = VALUES(crafted_items), damage_dealt = VALUES(damage_dealt), damage_taken = VALUES(damage_taken), deaths = VALUES(deaths), enchanted_items = VALUES(enchanted_items), fish_caught = VALUES(fish_caught), villager_trades = VALUES(villager_trades)"
                : "INSERT INTO " + TABLE + " (player_uuid, server_id, player_name_lower, updated_at, play_time_hours, mined_blocks, moved_blocks, mob_kills, used_items, broken_items, crafted_items, damage_dealt, damage_taken, deaths, enchanted_items, fish_caught, villager_trades) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                + "ON CONFLICT(player_uuid, server_id) DO UPDATE SET player_name_lower = excluded.player_name_lower, updated_at = excluded.updated_at, play_time_hours = excluded.play_time_hours, mined_blocks = excluded.mined_blocks, moved_blocks = excluded.moved_blocks, mob_kills = excluded.mob_kills, used_items = excluded.used_items, broken_items = excluded.broken_items, crafted_items = excluded.crafted_items, damage_dealt = excluded.damage_dealt, damage_taken = excluded.damage_taken, deaths = excluded.deaths, enchanted_items = excluded.enchanted_items, fish_caught = excluded.fish_caught, villager_trades = excluded.villager_trades";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindSave(statement, value);
            statement.executeUpdate();
        }
    }

    private void deleteWithConnection(Connection connection, UUID key) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM " + TABLE + " WHERE player_uuid = ? AND server_id = ?")) {
            statement.setString(1, key.toString());
            statement.setString(2, serverId);
            statement.executeUpdate();
        }
    }

    private void bindSave(PreparedStatement statement, PlayerGroupActivityState value) throws SQLException {
        PlayerActivitySnapshot snapshot = value.snapshot();
        statement.setString(1, value.playerUuid().toString());
        statement.setString(2, serverId);
        statement.setString(3, value.playerNameLower());
        statement.setLong(4, value.updatedAtMillis());
        statement.setDouble(5, snapshot.playTimeHours());
        statement.setLong(6, snapshot.minedBlocks());
        statement.setDouble(7, snapshot.movedBlocks());
        statement.setLong(8, snapshot.mobKills());
        statement.setLong(9, snapshot.usedItems());
        statement.setLong(10, snapshot.brokenItems());
        statement.setLong(11, snapshot.craftedItems());
        statement.setLong(12, snapshot.damageDealt());
        statement.setLong(13, snapshot.damageTaken());
        statement.setLong(14, snapshot.deaths());
        statement.setLong(15, snapshot.enchantedItems());
        statement.setLong(16, snapshot.fishCaught());
        statement.setLong(17, snapshot.villagerTrades());
    }

    private static PlayerGroupActivityState readState(ResultSet resultSet) throws SQLException {
        return new PlayerGroupActivityState(
                UUID.fromString(resultSet.getString("player_uuid")),
                resultSet.getString("player_name_lower"),
                readSnapshot(resultSet),
                resultSet.getLong("updated_at")
        );
    }

    private static PlayerActivitySnapshot readSnapshot(ResultSet resultSet) throws SQLException {
        return new PlayerActivitySnapshot(
                0.0D,
                resultSet.getDouble("play_time_hours"),
                resultSet.getLong("mined_blocks"),
                resultSet.getDouble("moved_blocks"),
                resultSet.getLong("mob_kills"),
                resultSet.getLong("used_items"),
                resultSet.getLong("broken_items"),
                resultSet.getLong("crafted_items"),
                resultSet.getLong("damage_dealt"),
                resultSet.getLong("damage_taken"),
                resultSet.getLong("deaths"),
                resultSet.getLong("enchanted_items"),
                resultSet.getLong("fish_caught"),
                resultSet.getLong("villager_trades")
        );
    }
}
