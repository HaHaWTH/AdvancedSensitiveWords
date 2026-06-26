package io.wdsj.asw.bukkit.listener.compatibility;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;

import java.util.HashSet;
import java.util.Set;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.LOGGER;

@SuppressWarnings("deprecation")
public class CompatibilityChecker implements Listener {
    private static final Set<String> compatibilityVerifiedPlugins = Set.of(
            "TrChat",
            "AdvancedSensitiveWords" // yes myself
    );
    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        Set<String> legacyPlugins = new HashSet<>();

        for (RegisteredListener listener : AsyncPlayerChatEvent.getHandlerList().getRegisteredListeners()) {
            Plugin plugin = listener.getPlugin();
            if (!compatibilityVerifiedPlugins.contains(plugin.getName())) {
                legacyPlugins.add(plugin.getName());
            }
        }

        if (!legacyPlugins.isEmpty()) {
            LOGGER.warn("========================================");
            LOGGER.warn("WARNING: Legacy AsyncPlayerChatEvent is being used!");
            LOGGER.warn("The following plugins are still listening to the deprecated AsyncPlayerChatEvent: ");

            for (String pluginName : legacyPlugins) {
                LOGGER.warn(" - {}", pluginName);
            }

            LOGGER.warn("Since AdvancedSensitiveWords utilizes Paper's modern AsyncChatEvent which is fired after the Spigot one,");
            LOGGER.warn("these legacy plugins might bypass the chat filter or cause conflicts.");
            LOGGER.warn("Consider updating these plugins or requesting their authors to migrate to AsyncChatEvent.");
            LOGGER.warn("You can disable this warning in the config \"{}\" if you acknowledged the risk.", "disable-compatibility-checker");
            LOGGER.warn("========================================");
        }
    }
}
