package io.wdsj.asw.command;

import com.github.houbb.heaven.util.util.OsUtil;
import io.wdsj.asw.AdvancedSensitiveWords;
import io.wdsj.asw.setting.PluginMessages;
import io.wdsj.asw.setting.PluginSettings;
import io.wdsj.asw.util.cache.BookCache;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.github.houbb.heaven.util.util.OsUtil.is64;
import static com.github.houbb.heaven.util.util.OsUtil.isUnix;
import static io.wdsj.asw.AdvancedSensitiveWords.*;
import static io.wdsj.asw.util.TimingUtils.*;
import static io.wdsj.asw.util.Utils.*;

public class ConstructCommandExecutor implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("reload") && (sender.hasPermission("advancedsensitivewords.reload") || sender instanceof ConsoleCommandSender)) {
                if (!isInitialized) {
                    return true;
                }
                settingsManager.reload();
                messagesManager.reload();
                AdvancedSensitiveWords.getInstance().doInitTasks();
                if (settingsManager.getProperty(PluginSettings.BOOK_CACHE_CLEAR_ON_RELOAD) &&
                        settingsManager.getProperty(PluginSettings.BOOK_CACHE)) BookCache.forceClearCache();
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.MESSAGE_ON_COMMAND_RELOAD)));
                return true;
            }
            if (args[0].equalsIgnoreCase("reload")) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.NO_PERMISSION)));
                return true;
            }
            if (args[0].equalsIgnoreCase("status") && (sender.hasPermission("advancedsensitivewords.status") || sender instanceof ConsoleCommandSender)) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.MESSAGE_ON_COMMAND_STATUS).replace("%NUM%", "&a" + messagesFilteredNum.get()).replace("%MODE%", checkProtocolLib() ? "&aFast" : "&cCompatibility").replace("%INIT%", isInitialized ? "&atrue" : "&cfalse").replace("%MS%", getProcessAverage() >= 120 ? getProcessAverage() >= 300 ? "&c" + getProcessAverage() + "ms" : "&e" + getProcessAverage() + "ms" : "&a" + getProcessAverage() + "ms").replace("%VERSION%", AdvancedSensitiveWords.getInstance().getDescription().getVersion()).replace("%MC_VERSION%", getMinecraftVersion()).replace("%PLATFORM%", OsUtil.isWindows() ? "Windows" : (OsUtil.isMac() ? "Mac" : isUnix() ? "Linux" : "Unknown")).replace("%BIT%", is64() ? "64bit" : "32bit").replace("%JAVA_VERSION%", getJvmVersion()).replace("%JAVA_VENDOR%", getJvmVendor()).replace("%API_STATUS%", settingsManager.getProperty(PluginSettings.ENABLE_API) ? "&aenabled" : "&cdisabled")));
                return true;
            }
            if (args[0].equalsIgnoreCase("status")) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.NO_PERMISSION)));
                return true;
            }
            if (args[0].equalsIgnoreCase("help") && (sender.hasPermission("advancedsensitivewords.help") || sender instanceof ConsoleCommandSender)) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.MESSAGE_ON_COMMAND_HELP)));
                return true;
            }
            if (args[0].equalsIgnoreCase("help")) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.NO_PERMISSION)));
                return true;
            }
        }
        if (args.length >= 1) {
            if (args[0].equalsIgnoreCase("test") && (sender.hasPermission("advancedsensitivewords.test") || sender instanceof ConsoleCommandSender)) {
                if (args.length >= 2) {
                    if (isInitialized) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 1; i <= args.length - 1; i++) {
                            sb.append(args[i]).append(" ");
                        }
                        String testArgs = sb.toString();
                        List<String> censoredWordList = sensitiveWordBs.findAll(testArgs);
                        if (!censoredWordList.isEmpty()) {
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.MESSAGE_ON_COMMAND_TEST).replace("%ORIGINAL_MSG%", testArgs).replace("%PROCESSED_MSG%", sensitiveWordBs.replace(testArgs)).replace("%CENSORED_LIST%", censoredWordList.toString())));
                        } else {
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.MESSAGE_ON_COMMAND_TEST_PASS)));
                        }
                    } else {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.MESSAGE_ON_COMMAND_TEST_NOT_INIT)));
                    }
                } else {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.MESSAGE_ON_COMMAND_TEST_NOT_ENOUGH)));
                }
                return true;
            }
            if (args[0].equalsIgnoreCase("test") && args.length == 1) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.NO_PERMISSION)));
                return true;
            }
        }
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.UNKNOWN_COMMAND)));
        return true;
    }

}
