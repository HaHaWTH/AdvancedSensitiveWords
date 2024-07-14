package io.wdsj.asw.bukkit.command;

import io.wdsj.asw.bukkit.manage.permission.Permissions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ConstructTabCompleter implements TabCompleter {
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String s, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> tabComplete = new ArrayList<>();
            if (sender.hasPermission(Permissions.RELOAD) && args[0].startsWith("r")) {
                tabComplete.add("reload");
            } else if (sender.hasPermission(Permissions.STATUS) && args[0].startsWith("s")) {
                tabComplete.add("status");
            } else if (sender.hasPermission(Permissions.TEST) && args[0].startsWith("t")) {
                tabComplete.add("test");
            } else if (sender.hasPermission(Permissions.HELP) && args[0].startsWith("h")) {
                tabComplete.add("help");
            } else if (sender.hasPermission(Permissions.INFO) && args[0].startsWith("i")) {
                tabComplete.add("info");
            } else if (sender.hasPermission(Permissions.PUNISH) && args[0].startsWith("p")) {
                tabComplete.add("punish");
            } else if (sender.hasPermission(Permissions.RELOAD) ||
                    sender.hasPermission(Permissions.STATUS) || sender.hasPermission(Permissions.TEST) ||
                    sender.hasPermission(Permissions.HELP) || sender.hasPermission(Permissions.INFO) ||
                    sender.hasPermission(Permissions.PUNISH)) {
                tabComplete.add("help");
                tabComplete.add("reload");
                tabComplete.add("status");
                tabComplete.add("test");
                tabComplete.add("punish");
                tabComplete.add("info");
            }
            return tabComplete;
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("info") && (sender.hasPermission(Permissions.INFO) || sender instanceof ConsoleCommandSender)) {
                return sender.getServer().getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("punish") && (sender.hasPermission(Permissions.PUNISH) || sender instanceof ConsoleCommandSender)) {
                return sender.getServer().getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList());
            }
        }
        return Collections.emptyList(); // Must return empty list, if null paper will supply player names
    }
}
