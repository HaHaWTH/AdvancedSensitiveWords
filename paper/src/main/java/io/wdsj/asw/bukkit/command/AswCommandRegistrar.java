package io.wdsj.asw.bukkit.command;

import io.wdsj.asw.bukkit.AdvancedSensitiveWords;
import io.wdsj.asw.bukkit.setting.PluginMessages;
import io.wdsj.asw.bukkit.util.message.MessageUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.incendo.cloud.Command;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.exception.ArgumentParseException;
import org.incendo.cloud.exception.InvalidSyntaxException;
import org.incendo.cloud.exception.NoPermissionException;
import org.incendo.cloud.exception.NoSuchCommandException;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.minecraft.extras.MinecraftHelp;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.paper.util.sender.PaperSimpleSenderMapper;
import org.incendo.cloud.paper.util.sender.Source;
import org.incendo.cloud.bukkit.parser.PlayerParser;
import org.incendo.cloud.parser.standard.StringArrayParser;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.permission.PredicatePermission;

public final class AswCommandRegistrar {
    private final AdvancedSensitiveWords plugin;
    private final AswCommandService commandService;

    private PaperCommandManager<Source> commandManager;
    private MinecraftHelp<Source> minecraftHelp;

    public AswCommandRegistrar(AdvancedSensitiveWords plugin) {
        this.plugin = plugin;
        this.commandService = new AswCommandService(plugin);
    }

    public void register() {
        this.commandManager = PaperCommandManager.builder(PaperSimpleSenderMapper.simpleSenderMapper())
                .executionCoordinator(ExecutionCoordinator.simpleCoordinator())
                .buildOnEnable(plugin);
        this.minecraftHelp = MinecraftHelp.create("/asw help", commandManager, source -> source.source());

        registerExceptionHandlers();
        registerRootCommand();
        registerReloadCommands();
        registerWordCommands();
        registerPlayerCommands();
    }

    private void registerRootCommand() {
        commandManager.command(root()
                .permission(permission(CommandPermissions.HELP))
                .handler(context -> minecraftHelp.queryCommands("", context.sender())));
        commandManager.command(root()
                .literal("help", Description.of("Show command help"))
                .optional("query", StringParser.greedyStringParser())
                .permission(permission(CommandPermissions.HELP))
                .handler(context -> minecraftHelp.queryCommands(context.getOrDefault("query", ""), context.sender())));
        commandManager.command(root()
                .literal("status", Description.of("Show plugin status"))
                .permission(permission(CommandPermissions.STATUS))
                .handler(context -> commandService.showStatus(sender(context))));
        commandManager.command(root()
                .literal("test", Description.of("Test text against the word filter"))
                .required("text", StringParser.greedyStringParser())
                .permission(permission(CommandPermissions.TEST))
                .handler(context -> commandService.test(sender(context), context.get("text"))));
    }

    private void registerReloadCommands() {
        commandManager.command(root()
                .literal("reload", Description.of("Reload configuration and dictionaries"))
                .permission(permission(CommandPermissions.RELOAD_ALL))
                .handler(context -> commandService.reloadAll(sender(context))));
        commandManager.command(root()
                .literal("reload", Description.of("Reload configuration and dictionaries"))
                .literal("all", Description.of("Reload configuration and dictionaries"))
                .permission(permission(CommandPermissions.RELOAD_ALL))
                .handler(context -> commandService.reloadAll(sender(context))));
        commandManager.command(root()
                .literal("reload", Description.of("Reload configuration"))
                .literal("config", Description.of("Reload configuration only"))
                .permission(permission(CommandPermissions.RELOAD_CONFIG))
                .handler(context -> commandService.reloadConfiguration(sender(context))));
    }

    private void registerWordCommands() {
        commandManager.command(root()
                .literal("word", Description.of("Manage blocked words"))
                .literal("add", Description.of("Add temporary blocked words"))
                .required("words", StringArrayParser.stringArrayParser())
                .permission(permission(CommandPermissions.WORD_ADD))
                .handler(context -> commandService.addBlockedWords(sender(context), context.get("words"))));
        commandManager.command(root()
                .literal("word", Description.of("Manage blocked words"))
                .literal("remove", Description.of("Remove temporary blocked words"))
                .required("words", StringArrayParser.stringArrayParser())
                .permission(permission(CommandPermissions.WORD_REMOVE))
                .handler(context -> commandService.removeBlockedWords(sender(context), context.get("words"))));
        commandManager.command(root()
                .literal("allow", Description.of("Manage allowed words"))
                .literal("add", Description.of("Add temporary allowed words"))
                .required("words", StringArrayParser.stringArrayParser())
                .permission(permission(CommandPermissions.ALLOW_ADD))
                .handler(context -> commandService.addAllowedWords(sender(context), context.get("words"))));
        commandManager.command(root()
                .literal("allow", Description.of("Manage allowed words"))
                .literal("remove", Description.of("Remove temporary allowed words"))
                .required("words", StringArrayParser.stringArrayParser())
                .permission(permission(CommandPermissions.ALLOW_REMOVE))
                .handler(context -> commandService.removeAllowedWords(sender(context), context.get("words"))));
    }

    private void registerPlayerCommands() {
        commandManager.command(root()
                .literal("player", Description.of("Manage player violations"))
                .literal("info", Description.of("Show a player's violation count"))
                .required("player", PlayerParser.playerParser())
                .permission(permission(CommandPermissions.PLAYER_INFO))
                .handler(context -> commandService.showPlayerInfo(sender(context), context.get("player"))));
        commandManager.command(root()
                .literal("player", Description.of("Manage player violations"))
                .literal("reset", Description.of("Reset a player's violation count"))
                .required("player", PlayerParser.playerParser())
                .permission(permission(CommandPermissions.PLAYER_RESET))
                .handler(context -> commandService.resetPlayerViolations(sender(context), context.get("player"))));
        commandManager.command(root()
                .literal("player", Description.of("Manage player violations"))
                .literal("punish", Description.of("Apply a punishment to a player"))
                .required("player", PlayerParser.playerParser())
                .optional("method", StringParser.greedyStringParser())
                .permission(permission(CommandPermissions.PLAYER_PUNISH))
                .handler(context -> commandService.punishPlayer(
                        sender(context),
                        context.get("player"),
                        context.getOrDefault("method", null)
                )));
    }

    private void registerExceptionHandlers() {
        commandManager.exceptionController().registerHandler(NoPermissionException.class,
                context -> MessageUtils.sendMessage(sender(context.context()), PluginMessages.NO_PERMISSION));
        commandManager.exceptionController().registerHandler(NoSuchCommandException.class,
                context -> MessageUtils.sendMessage(sender(context.context()), PluginMessages.UNKNOWN_COMMAND));
        commandManager.exceptionController().registerHandler(InvalidSyntaxException.class,
                context -> MessageUtils.sendMessage(sender(context.context()), PluginMessages.NOT_ENOUGH_ARGS));
        commandManager.exceptionController().registerHandler(ArgumentParseException.class,
                context -> MessageUtils.sendMessage(sender(context.context()), PluginMessages.PLAYER_NOT_FOUND));
    }

    private Command.Builder<Source> root() {
        return commandManager.commandBuilder(
                "asw",
                Description.of("AdvancedSensitiveWords administration"),
                "advancedsensitivewords"
        );
    }

    private PredicatePermission<Source> permission(String permission) {
        return PredicatePermission.of(source -> source.source() instanceof ConsoleCommandSender
                || source.source().hasPermission(permission));
    }

    private CommandSender sender(CommandContext<Source> context) {
        return context.sender().source();
    }
}
