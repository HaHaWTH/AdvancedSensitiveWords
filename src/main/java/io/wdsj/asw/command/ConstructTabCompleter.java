package io.wdsj.asw.command;

import io.wdsj.asw.impl.list.AdvancedList;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class ConstructTabCompleter implements TabCompleter {
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String s, @NotNull String[] args) {
        Player player = (Player) sender;
        if (args.length == 1) {
            List<String> tabComplete = new AdvancedList<>();
            if (player.hasPermission("advancedsensitivewords.reload") && args[0].startsWith("r")) {
                tabComplete.add("reload");
            } else if (player.hasPermission("advancedsensitivewords.status") && args[0].startsWith("s")) {
                tabComplete.add("status");
            } else if (player.hasPermission("advancedsensitivewords.test") && args[0].startsWith("t")) {
                tabComplete.add("test");
            } else if (player.hasPermission("advancedsensitivewords.reload") ||
                    player.hasPermission("advancedsensitivewords.status") || player.hasPermission("advancedsensitivewords.test")) {
                tabComplete.add("help");
                tabComplete.add("reload");
                tabComplete.add("status");
                tabComplete.add("test");
            }
            return tabComplete;
        }
        return Collections.emptyList(); // Must return empty list, if null paper will supply player names
    }
}
