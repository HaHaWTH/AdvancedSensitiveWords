package io.wdsj.asw.bukkit.listener;

import io.wdsj.asw.bukkit.manage.punish.PlayerShadowController;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Collection;

public class ShadowListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (PlayerShadowController.isShadowed(player)) {
            Collection<Player> recipients = event.getRecipients();
            recipients.clear();
            recipients.add(player);
        }
    }
}
