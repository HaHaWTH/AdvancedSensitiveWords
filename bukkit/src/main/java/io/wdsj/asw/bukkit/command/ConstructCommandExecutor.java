package io.wdsj.asw.bukkit.command;

import com.github.houbb.heaven.util.util.OsUtil;
import io.wdsj.asw.bukkit.AdvancedSensitiveWords;
import io.wdsj.asw.bukkit.manage.permission.Permissions;
import io.wdsj.asw.bukkit.manage.punish.Punishment;
import io.wdsj.asw.bukkit.setting.PluginMessages;
import io.wdsj.asw.bukkit.setting.PluginSettings;
import io.wdsj.asw.bukkit.util.cache.BookCache;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;

import static com.github.houbb.heaven.util.util.OsUtil.is64;
import static com.github.houbb.heaven.util.util.OsUtil.isUnix;
import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.*;
import static io.wdsj.asw.bukkit.util.TimingUtils.*;
import static io.wdsj.asw.bukkit.util.Utils.getMinecraftVersion;
import static io.wdsj.asw.bukkit.util.Utils.messagesFilteredNum;

public class ConstructCommandExecutor implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("reload") && (sender.hasPermission(Permissions.RELOAD) || sender instanceof ConsoleCommandSender)) {
                if (!isInitialized) {
                    return true;
                }
                settingsManager.reload();
                File msgFile = new File(getInstance().getDataFolder(), "messages_" + settingsManager.getProperty(PluginSettings.PLUGIN_LANGUAGE) +
                        ".yml");
                if (!msgFile.exists()) {
                    getInstance().saveResource("messages_" + settingsManager.getProperty(PluginSettings.PLUGIN_LANGUAGE) + ".yml", true);
                }
                messagesManager.reload();
                sensitiveWordBs.destroy();
                getInstance().doInitTasks();
                if (settingsManager.getProperty(PluginSettings.BOOK_CACHE_CLEAR_ON_RELOAD) &&
                        settingsManager.getProperty(PluginSettings.BOOK_CACHE)) BookCache.invalidateAll();
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.MESSAGE_ON_COMMAND_RELOAD)));
                return true;
            }
            if (args[0].equalsIgnoreCase("reload")) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.NO_PERMISSION)));
                return true;
            }
            if (args[0].equalsIgnoreCase("status") && (sender.hasPermission(Permissions.STATUS) || sender instanceof ConsoleCommandSender)) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.MESSAGE_ON_COMMAND_STATUS).replace("%num%", String.valueOf(messagesFilteredNum.get())).replace("%mode%", isEventMode() ? "Event" : "Packet").replace("%init%", isInitialized ? "true" : "false").replace("%ms%", getProcessAverage() + "ms").replace("%version%", AdvancedSensitiveWords.getInstance().getDescription().getVersion()).replace("%mc_version%", getMinecraftVersion()).replace("%platform%", OsUtil.isWindows() ? "Windows" : (OsUtil.isMac() ? "Mac" : isUnix() ? "Linux" : "Unknown")).replace("%bit%", is64() ? "64bit" : "32bit").replace("%java_version%", getJvmVersion()).replace("%java_vendor%", getJvmVendor())));
                return true;
            }
            if (args[0].equalsIgnoreCase("status")) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.NO_PERMISSION)));
                return true;
            }
            if (args[0].equalsIgnoreCase("help") && (sender.hasPermission(Permissions.HELP) || sender instanceof ConsoleCommandSender)) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.MESSAGE_ON_COMMAND_HELP)));
                return true;
            }
            if (args[0].equalsIgnoreCase("help")) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.NO_PERMISSION)));
                return true;
            }
        }
        if (args.length >= 1) {
            if (args[0].equalsIgnoreCase("test") && (sender.hasPermission(Permissions.TEST) || sender instanceof ConsoleCommandSender)) {
                if (args.length >= 2) {
                    if (isInitialized) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 1; i <= args.length - 1; i++) {
                            sb.append(args[i]).append(" ");
                        }
                        String testArgs = sb.toString();
                        List<String> censoredWordList = sensitiveWordBs.findAll(testArgs);
                        if (!censoredWordList.isEmpty()) {
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.MESSAGE_ON_COMMAND_TEST).replace("%original_msg%", testArgs).replace("%processed_msg%", sensitiveWordBs.replace(testArgs)).replace("%censored_list%", censoredWordList.toString())));
                        } else {
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.MESSAGE_ON_COMMAND_TEST_PASS)));
                        }
                    } else {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.MESSAGE_ON_COMMAND_TEST_NOT_INIT)));
                    }
                } else {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.NOT_ENOUGH_ARGS)));
                }
                return true;
            }
            if (args[0].equalsIgnoreCase("test") && args.length == 1) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.NO_PERMISSION)));
                return true;
            }
            if (args[0].equalsIgnoreCase("info") && (sender.hasPermission(Permissions.INFO) || sender instanceof ConsoleCommandSender)) {
                if (args.length > 1) {
                    if (settingsManager.getProperty(PluginSettings.ENABLE_DATABASE)) {
                        String playerName = args[1];
                        String violations = databaseManager.getPlayerViolations(playerName);
                        if (violations != null) {
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.MESSAGE_ON_PLAYER_INFO).replace("%player%", playerName).replace("%total_vl%", violations)));
                        } else {
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.MESSAGE_ON_PLAYER_INFO_FAIL)));
                        }
                    } else {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.MESSAGE_ON_PLAYER_INFO_CLOSE)));
                    }
                } else {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.NOT_ENOUGH_ARGS)));
                }
                return true;
            }
            if (args[0].equalsIgnoreCase("info")) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.NO_PERMISSION)));
                return true;
            }
            if (args[0].equalsIgnoreCase("punish") && (sender.hasPermission(Permissions.PUNISH) || sender instanceof ConsoleCommandSender)) {
                if (args.length >= 2) {
                    String playerName = args[1];
                    Player player = Bukkit.getPlayer(playerName);
                    if (player != null) {
                        if (args.length >= 3) {
                            StringBuilder method = new StringBuilder(args[2]);
                            if (args.length >= 4) {
                                for (int i = 3; i <= args.length - 1; i++) {
                                    method.append(" ").append(args[i]);
                                }
                            }
                            try {
                                Punishment.processSinglePunish(player, method.toString().trim());
                                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.MESSAGE_ON_COMMAND_PUNISH_SUCCESS).replace("%player%", player.getName())));
                            } catch (Exception e) {
                                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.MESSAGE_ON_COMMAND_PUNISH_PARSE_ERROR)));
                            }
                        } else {
                            Punishment.punish(player);
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.MESSAGE_ON_COMMAND_PUNISH_SUCCESS).replace("%player%", player.getName())));
                        }
                    } else {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.PLAYER_NOT_FOUND)));
                    }
                } else {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.NOT_ENOUGH_ARGS)));
                }
                return true;
            }
            if (args[0].equalsIgnoreCase("punish")) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.NO_PERMISSION)));
                return true;
            }
        }
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.UNKNOWN_COMMAND)));
        return true;
    }

}
