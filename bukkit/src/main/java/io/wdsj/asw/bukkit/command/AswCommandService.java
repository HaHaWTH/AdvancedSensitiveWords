package io.wdsj.asw.bukkit.command;

import com.github.houbb.heaven.util.util.OsUtil;
import io.wdsj.asw.bukkit.AdvancedSensitiveWords;
import io.wdsj.asw.bukkit.manage.punish.Punishment;
import io.wdsj.asw.bukkit.manage.punish.ViolationCounter;
import io.wdsj.asw.bukkit.setting.PluginMessages;
import io.wdsj.asw.bukkit.setting.PluginSettings;
import io.wdsj.asw.bukkit.util.TimingUtils;
import io.wdsj.asw.bukkit.util.Utils;
import io.wdsj.asw.bukkit.util.cache.BookCache;
import io.wdsj.asw.bukkit.util.message.MessageUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class AswCommandService {
    private final AdvancedSensitiveWords plugin;

    public AswCommandService(AdvancedSensitiveWords plugin) {
        this.plugin = plugin;
    }

    public void reloadAll(CommandSender sender) {
        if (!AdvancedSensitiveWords.isInitialized) {
            return;
        }

        plugin.reloadPluginConfiguration();
        AdvancedSensitiveWords.sensitiveWordBs.destroy();
        plugin.doInitTasks();
        if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.BOOK_CACHE_CLEAR_ON_RELOAD)
                && AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.BOOK_CACHE)) {
            BookCache.invalidateAll();
        }
        MessageUtils.sendMessage(sender, PluginMessages.MESSAGE_ON_COMMAND_RELOAD);
    }

    public void reloadConfiguration(CommandSender sender) {
        plugin.reloadPluginConfiguration();
        MessageUtils.sendMessage(sender, PluginMessages.MESSAGE_ON_COMMAND_RELOAD);
    }

    public void showStatus(CommandSender sender) {
        String platform = OsUtil.isWindows()
                ? "Windows"
                : OsUtil.isMac() ? "Mac" : OsUtil.isUnix() ? "Linux" : "Unknown";
        String bitness = OsUtil.is64() ? "64bit" : "32bit";
        String message = MessageUtils.retrieveMessage(PluginMessages.MESSAGE_ON_COMMAND_STATUS)
                .replace("%num%", String.valueOf(Utils.messagesFilteredNum.get()))
                .replace("%mode%", "Event")
                .replace("%init%", String.valueOf(AdvancedSensitiveWords.isInitialized))
                .replace("%ms%", TimingUtils.getProcessAverage() + "ms")
                .replace("%version%", AdvancedSensitiveWords.PLUGIN_VERSION)
                .replace("%mc_version%", Utils.getMinecraftVersion())
                .replace("%platform%", platform)
                .replace("%bit%", bitness)
                .replace("%java_version%", TimingUtils.getJvmVersion())
                .replace("%java_vendor%", TimingUtils.getJvmVendor());
        MessageUtils.sendMessage(sender, message);
    }

    public void test(CommandSender sender, String text) {
        if (!isInitialized(sender)) {
            return;
        }

        List<String> censoredWords = AdvancedSensitiveWords.sensitiveWordBs.findAll(text);
        if (censoredWords.isEmpty()) {
            MessageUtils.sendMessage(sender, PluginMessages.MESSAGE_ON_COMMAND_TEST_PASS);
            return;
        }

        String message = MessageUtils.retrieveMessage(PluginMessages.MESSAGE_ON_COMMAND_TEST)
                .replace("%original_msg%", text)
                .replace("%processed_msg%", AdvancedSensitiveWords.sensitiveWordBs.replace(text))
                .replace("%censored_list%", censoredWords.toString());
        MessageUtils.sendMessage(sender, message);
    }

    public void addBlockedWords(CommandSender sender, String[] words) {
        if (!isInitialized(sender)) {
            return;
        }
        AdvancedSensitiveWords.sensitiveWordBs.addWord(toWordList(words));
        sendTemporaryMutationMessage(sender, PluginMessages.MESSAGE_ON_COMMAND_ADD_SUCCESS);
    }

    public void removeBlockedWords(CommandSender sender, String[] words) {
        if (!isInitialized(sender)) {
            return;
        }
        AdvancedSensitiveWords.sensitiveWordBs.removeWord(toWordList(words));
        sendTemporaryMutationMessage(sender, PluginMessages.MESSAGE_ON_COMMAND_REMOVE_SUCCESS);
    }

    public void addAllowedWords(CommandSender sender, String[] words) {
        if (!isInitialized(sender)) {
            return;
        }
        AdvancedSensitiveWords.sensitiveWordBs.addWordAllow(toWordList(words));
        sendTemporaryMutationMessage(sender, PluginMessages.MESSAGE_ON_COMMAND_ADD_SUCCESS);
    }

    public void removeAllowedWords(CommandSender sender, String[] words) {
        if (!isInitialized(sender)) {
            return;
        }
        AdvancedSensitiveWords.sensitiveWordBs.removeWordAllow(toWordList(words));
        sendTemporaryMutationMessage(sender, PluginMessages.MESSAGE_ON_COMMAND_REMOVE_SUCCESS);
    }

    public void showPlayerInfo(CommandSender sender, Player player) {
        String message = MessageUtils.retrieveMessage(PluginMessages.MESSAGE_ON_PLAYER_INFO)
                .replace("%player%", player.getName())
                .replace("%violation%", String.valueOf(ViolationCounter.INSTANCE.getViolationCount(player)));
        MessageUtils.sendMessage(sender, message);
    }

    public void resetPlayerViolations(CommandSender sender, Player player) {
        ViolationCounter.INSTANCE.resetViolationCount(player);
        String message = MessageUtils.retrieveMessage(PluginMessages.MESSAGE_ON_COMMAND_RESET)
                .replace("%player%", player.getName());
        MessageUtils.sendMessage(sender, message);
    }

    public void punishPlayer(CommandSender sender, Player player, String method) {
        try {
            if (method == null || method.isBlank()) {
                Punishment.punish(player);
            } else {
                Punishment.processSinglePunish(player, method);
            }
        } catch (IllegalArgumentException exception) {
            MessageUtils.sendMessage(sender, PluginMessages.MESSAGE_ON_COMMAND_PUNISH_PARSE_ERROR);
            return;
        }

        String message = MessageUtils.retrieveMessage(PluginMessages.MESSAGE_ON_COMMAND_PUNISH_SUCCESS)
                .replace("%player%", player.getName());
        MessageUtils.sendMessage(sender, message);
    }

    private boolean isInitialized(CommandSender sender) {
        if (AdvancedSensitiveWords.isInitialized) {
            return true;
        }
        MessageUtils.sendMessage(sender, PluginMessages.MESSAGE_ON_COMMAND_TEST_NOT_INIT);
        return false;
    }

    private void sendTemporaryMutationMessage(CommandSender sender, ch.jalu.configme.properties.Property<String> successMessage) {
        MessageUtils.sendMessage(sender, successMessage);
        MessageUtils.sendMessage(sender, PluginMessages.MESSAGE_ON_COMMAND_RUNTIME_ONLY);
    }

    private List<String> toWordList(String[] words) {
        return new ArrayList<>(Arrays.asList(words));
    }
}
