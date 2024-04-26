package io.wdsj.asw.bukkit.listener;

import io.wdsj.asw.bukkit.setting.PluginSettings;
import io.wdsj.asw.bukkit.util.context.ChatContext;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager;

public class PlayerQuitListener implements Listener {

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (!settingsManager.getProperty(PluginSettings.FLUSH_PLAYER_DATA_CACHE)) return;
        Player player = event.getPlayer();
        doFlushTask(player);
    }

    private void doFlushTask(Player player) {
        ChatContext.removePlayerContext(player);
    }
}
