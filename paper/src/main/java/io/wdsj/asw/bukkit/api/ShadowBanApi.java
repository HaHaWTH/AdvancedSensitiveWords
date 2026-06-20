package io.wdsj.asw.bukkit.api;

import io.wdsj.asw.bukkit.manage.punish.PlayerShadowController;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * API for ASW shadowban state.
 */
@SuppressWarnings("unused")
public final class ShadowBanApi {
    ShadowBanApi() {
    }

    /**
     * Shadowban a player for a duration.
     * All messages sent by the player will not be sent to other players.
     * Non-positive durations remove the current shadowban.
     *
     * @param player player to shadowban
     * @param duration duration to keep the player shadowbanned
     */
    public void shadow(Player player, Duration duration) {
        PlayerShadowController.shadowPlayer(player, duration);
    }

    /**
     * Shadowban a player by uuid for a duration.
     * All messages sent by the player will not be sent to other players.
     * Non-positive durations remove the current shadowban.
     *
     * @param uuid player uuid
     * @param duration duration to keep the player shadowbanned
     */
    public void shadow(UUID uuid, Duration duration) {
        PlayerShadowController.shadowPlayer(uuid, duration);
    }

    /**
     * Remove a player's shadowban.
     *
     * @param player player to unshadowban
     */
    public void unshadow(Player player) {
        PlayerShadowController.unshadowPlayer(player);
    }

    /**
     * Remove a player's shadowban.
     *
     * @param uuid player uuid
     */
    public void unshadow(UUID uuid) {
        PlayerShadowController.unshadowPlayer(uuid);
    }

    /**
     * Check whether a player is shadowbanned.
     *
     * @param player player to check
     * @return true if the player is currently shadowbanned
     */
    public boolean isShadowed(Player player) {
        return PlayerShadowController.isShadowed(player);
    }

    /**
     * Check whether a player is shadowbanned.
     *
     * @param uuid player uuid
     * @return true if the player is currently shadowbanned
     */
    public boolean isShadowed(UUID uuid) {
        return PlayerShadowController.isShadowed(uuid);
    }

    /**
     * Get the remaining shadowban duration.
     *
     * @param player player to check
     * @return remaining duration, or empty if the player is not shadowbanned
     */
    public Optional<Duration> remainingDuration(Player player) {
        return PlayerShadowController.getRemainingDuration(player);
    }

    /**
     * Get the remaining shadowban duration.
     *
     * @param uuid player uuid
     * @return remaining duration, or empty if the player is not shadowbanned
     */
    public Optional<Duration> remainingDuration(UUID uuid) {
        return PlayerShadowController.getRemainingDuration(uuid);
    }
}
