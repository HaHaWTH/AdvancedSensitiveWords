package io.wdsj.asw.bukkit.command;

import io.wdsj.asw.bukkit.manage.permission.PermissionsEnum;
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
            if (sender.hasPermission(PermissionsEnum.RELOAD.getPermission()) && args[0].startsWith("rel")) {
                tabComplete.add("reload");
                tabComplete.add("reloadconfig");
            } else if (sender.hasPermission(PermissionsEnum.ADD.getPermission()) && args[0].startsWith("a")) {
                tabComplete.add("add");
                tabComplete.add("addallow");
            } else if (sender.hasPermission(PermissionsEnum.REMOVE.getPermission()) && args[0].startsWith("rem")) {
                tabComplete.add("remove");
                tabComplete.add("removeallow");
            } else if (sender.hasPermission(PermissionsEnum.RESET.getPermission()) && args[0].startsWith("res")) {
                tabComplete.add("reset");
            } else if (sender.hasPermission(PermissionsEnum.STATUS.getPermission()) && args[0].startsWith("s")) {
                tabComplete.add("status");
            } else if (sender.hasPermission(PermissionsEnum.TEST.getPermission()) && args[0].startsWith("t")) {
                tabComplete.add("test");
            } else if (sender.hasPermission(PermissionsEnum.HELP.getPermission()) && args[0].startsWith("h")) {
                tabComplete.add("help");
            } else if (sender.hasPermission(PermissionsEnum.INFO.getPermission()) && args[0].startsWith("i")) {
                tabComplete.add("info");
            } else if (sender.hasPermission(PermissionsEnum.PUNISH.getPermission()) && args[0].startsWith("p")) {
                tabComplete.add("punish");
            } else if (sender.hasPermission(PermissionsEnum.RELOAD.getPermission()) ||
                    sender.hasPermission(PermissionsEnum.STATUS.getPermission()) || sender.hasPermission(PermissionsEnum.TEST.getPermission()) ||
                    sender.hasPermission(PermissionsEnum.HELP.getPermission()) || sender.hasPermission(PermissionsEnum.INFO.getPermission()) ||
                    sender.hasPermission(PermissionsEnum.PUNISH.getPermission()) || sender.hasPermission(PermissionsEnum.ADD.getPermission()) ||
                    sender.hasPermission(PermissionsEnum.REMOVE.getPermission())) {
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
            if (args[0].equalsIgnoreCase("info") && (sender.hasPermission(PermissionsEnum.INFO.getPermission()) || sender instanceof ConsoleCommandSender)) {
                return sender.getServer().getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("reset") && (sender.hasPermission(PermissionsEnum.RESET.getPermission()) || sender instanceof ConsoleCommandSender)) {
                return sender.getServer().getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("punish") && (sender.hasPermission(PermissionsEnum.PUNISH.getPermission()) || sender instanceof ConsoleCommandSender)) {
                return sender.getServer().getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList());
            }
        }
        return Collections.emptyList(); // Must return empty list, if null paper will supply player names
    }
}
