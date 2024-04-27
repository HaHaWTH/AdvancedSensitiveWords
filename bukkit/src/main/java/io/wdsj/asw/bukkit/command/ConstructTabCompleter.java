package io.wdsj.asw.bukkit.command;

import io.wdsj.asw.bukkit.impl.list.AdvancedList;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

public class ConstructTabCompleter implements TabCompleter {
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String s, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> tabComplete = new AdvancedList<>();
            if (sender.hasPermission("advancedsensitivewords.reload") && args[0].startsWith("r")) {
                tabComplete.add("reload");
            } else if (sender.hasPermission("advancedsensitivewords.status") && args[0].startsWith("s")) {
                tabComplete.add("status");
            } else if (sender.hasPermission("advancedsensitivewords.test") && args[0].startsWith("t")) {
                tabComplete.add("test");
            } else if (sender.hasPermission("advancedsensitivewords.help") && args[0].startsWith("h")) {
                tabComplete.add("help");
            } else if (sender.hasPermission("advancedsensitivewords.info") && args[0].startsWith("i")) {
                tabComplete.add("info");
            } else if (sender.hasPermission("advancedsensitivewords.reload") ||
                    sender.hasPermission("advancedsensitivewords.status") || sender.hasPermission("advancedsensitivewords.test") ||
                    sender.hasPermission("advancedsensitivewords.help") || sender.hasPermission("advancedsensitivewords.info")) {
                tabComplete.add("help");
                tabComplete.add("reload");
                tabComplete.add("status");
                tabComplete.add("test");
                tabComplete.add("info");
            }
            return tabComplete;
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("info") && (sender.hasPermission("advancedsensitivewords.info") || sender instanceof ConsoleCommandSender)) {
                return sender.getServer().getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList());
            }
        }
        return new AdvancedList<>(); // Must return empty list, if null paper will supply player names
    }
}
