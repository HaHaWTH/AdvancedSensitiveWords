package io.wdsj.asw.bukkit.command;

import io.wdsj.asw.bukkit.manage.permission.PermissionsConstant;
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
            if (sender.hasPermission(PermissionsConstant.RELOAD) && args[0].startsWith("rel")) {
                tabComplete.add("reload");
                tabComplete.add("reloadconfig");
            } else if (sender.hasPermission(PermissionsConstant.ADD) && args[0].startsWith("a")) {
                tabComplete.add("add");
                tabComplete.add("addallow");
            } else if (sender.hasPermission(PermissionsConstant.REMOVE) && args[0].startsWith("rem")) {
                tabComplete.add("remove");
                tabComplete.add("removeallow");
            } else if (sender.hasPermission(PermissionsConstant.RESET) && args[0].startsWith("res")) {
                tabComplete.add("reset");
            } else if (sender.hasPermission(PermissionsConstant.STATUS) && args[0].startsWith("s")) {
                tabComplete.add("status");
            } else if (sender.hasPermission(PermissionsConstant.TEST) && args[0].startsWith("t")) {
                tabComplete.add("test");
            } else if (sender.hasPermission(PermissionsConstant.HELP) && args[0].startsWith("h")) {
                tabComplete.add("help");
            } else if (sender.hasPermission(PermissionsConstant.INFO) && args[0].startsWith("i")) {
                tabComplete.add("info");
            } else if (sender.hasPermission(PermissionsConstant.PUNISH) && args[0].startsWith("p")) {
                tabComplete.add("punish");
            } else if (sender.hasPermission(PermissionsConstant.RELOAD) ||
                    sender.hasPermission(PermissionsConstant.STATUS) || sender.hasPermission(PermissionsConstant.TEST) ||
                    sender.hasPermission(PermissionsConstant.HELP) || sender.hasPermission(PermissionsConstant.INFO) ||
                    sender.hasPermission(PermissionsConstant.PUNISH) || sender.hasPermission(PermissionsConstant.ADD) ||
                    sender.hasPermission(PermissionsConstant.REMOVE)) {
                tabComplete.add("help");
                tabComplete.add("reload");
                tabComplete.add("reloadconfig");
                tabComplete.add("add");
                tabComplete.add("remove");
                tabComplete.add("status");
                tabComplete.add("test");
                tabComplete.add("punish");
                tabComplete.add("info");
                tabComplete.add("reset");
                tabComplete.add("addallow");
                tabComplete.add("removeallow");
            }
            return tabComplete;
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("info") && (sender.hasPermission(PermissionsConstant.INFO) || sender instanceof ConsoleCommandSender)) {
                return sender.getServer().getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("reset") && (sender.hasPermission(PermissionsConstant.RESET) || sender instanceof ConsoleCommandSender)) {
                return sender.getServer().getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("punish") && (sender.hasPermission(PermissionsConstant.PUNISH) || sender instanceof ConsoleCommandSender)) {
                return sender.getServer().getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList());
            }
        }
        return Collections.emptyList(); // Must return empty list, if null paper will supply player names
    }
}
