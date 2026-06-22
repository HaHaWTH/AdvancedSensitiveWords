package io.wdsj.asw.bukkit.command;

import com.github.houbb.heaven.util.util.OsUtil;
import io.wdsj.asw.bukkit.AdvancedSensitiveWords;
import io.wdsj.asw.bukkit.ai.LlmChatDetectionService;
import io.wdsj.asw.bukkit.ai.LlmCategoryPolicy;
import io.wdsj.asw.bukkit.api.moderation.LlmModerationCategory;
import io.wdsj.asw.bukkit.manage.punish.PunishmentService;
import io.wdsj.asw.bukkit.manage.punish.ViolationCounter;
import io.wdsj.asw.bukkit.setting.PluginMessages;
import io.wdsj.asw.bukkit.setting.PluginSettings;
import io.wdsj.asw.bukkit.setting.PaperConfigurationService;
import io.wdsj.asw.bukkit.type.ModuleType;
import io.wdsj.asw.bukkit.util.TimingUtils;
import io.wdsj.asw.bukkit.util.Utils;
import io.wdsj.asw.bukkit.util.cache.BookCache;
import io.wdsj.asw.bukkit.util.message.MessageUtils;
import io.wdsj.asw.bukkit.util.SchedulingUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public final class AswCommandService {
    private final AdvancedSensitiveWords plugin;
    private final PaperConfigurationService configuration;
    private final PunishmentService punishmentService;

    public AswCommandService(AdvancedSensitiveWords plugin) {
        this.plugin = plugin;
        this.configuration = plugin.getConfigurationService();
        this.punishmentService = new PunishmentService(configuration);
    }

    public void reloadAll(CommandSender sender) {
        if (!AdvancedSensitiveWords.isInitialized) {
            return;
        }

        plugin.reloadPluginConfiguration();
        AdvancedSensitiveWords.sensitiveWordBs.destroy();
        plugin.doInitTasks();
        if (configuration.get(PluginSettings.BOOK_CACHE_CLEAR_ON_RELOAD)
                && configuration.get(PluginSettings.BOOK_CACHE)) {
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

    public void showAiStatus(CommandSender sender) {
        LlmChatDetectionService.LlmRuntimeStatus status = plugin.getLlmChatDetectionService().runtimeStatus();
        String message = MessageUtils.retrieveMessage(PluginMessages.MESSAGE_ON_AI_STATUS)
                .replace("%enabled%", String.valueOf(status.enabled()))
                .replace("%submitted%", String.valueOf(status.submittedRequests()))
                .replace("%dropped%", String.valueOf(status.droppedRequests()))
                .replace("%failed%", String.valueOf(status.failedRequests()))
                .replace("%invalid%", String.valueOf(status.invalidResponses()))
                .replace("%enforced%", String.valueOf(status.enforcedResponses()))
                .replace("%active%", String.valueOf(status.activeRequests()))
                .replace("%queued%", String.valueOf(status.queuedRequests()))
                .replace("%pool_size%", String.valueOf(status.poolSize()))
                .replace("%model%", status.modelName())
                .replace("%api_mode%", status.apiMode().name())
                .replace("%thresholds%", formatCategoryPolicies(status.categoryPolicy()));
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
                .replace("%violation%", String.valueOf(ViolationCounter.INSTANCE.getTotalViolationCount(player)));
        for (ModuleType moduleType : ModuleType.violationModules()) {
            String placeholder = "%" + moduleType.name().toLowerCase(Locale.ROOT) + "_violation%";
            message = message.replace(placeholder, String.valueOf(ViolationCounter.INSTANCE.getViolationCount(player, moduleType)));
        }
        MessageUtils.sendMessage(sender, message);
    }

    public void resetPlayerViolations(CommandSender sender, Player player, ModuleType moduleType) {
        if (moduleType == null) {
            ViolationCounter.INSTANCE.resetViolationCount(player);
        } else {
            ViolationCounter.INSTANCE.resetViolationCount(player, moduleType);
        }
        String message = MessageUtils.retrieveMessage(PluginMessages.MESSAGE_ON_COMMAND_RESET)
                .replace("%player%", player.getName())
                .replace("%module%", moduleType == null ? "ALL" : moduleType.name());
        MessageUtils.sendMessage(sender, message);
    }

    public void teleportToReportedLocation(CommandSender sender, UUID worldId, double x, double y, double z) {
        if (!(sender instanceof Player player)) {
            return;
        }

        World world = Bukkit.getWorld(worldId);
        if (world == null) {
            return;
        }

        Location destination = new Location(world, x + 0.5D, y + 1.0D, z + 0.5D);
        SchedulingUtils.runSyncAtEntityIfFolia(player, () -> player.teleportAsync(destination));
    }

    public void punishPlayer(CommandSender sender, Player player, String method) {
        try {
            if (method == null || method.isBlank()) {
                punishmentService.executeManual(player, configuration.get(PluginSettings.MANUAL_PUNISHMENT));
            } else {
                punishmentService.executeManualMethod(player, method);
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

    private void sendTemporaryMutationMessage(CommandSender sender, PluginMessages successMessage) {
        MessageUtils.sendMessage(sender, successMessage);
        MessageUtils.sendMessage(sender, PluginMessages.MESSAGE_ON_COMMAND_RUNTIME_ONLY);
    }

    private List<String> toWordList(String[] words) {
        return new ArrayList<>(Arrays.asList(words));
    }

    private static String formatCategoryPolicies(Map<LlmModerationCategory, LlmCategoryPolicy> policies) {
        String result = Arrays.stream(LlmModerationCategory.values())
                .map(category -> Map.entry(category, policies.get(category)))
                .filter(entry -> entry.getValue().notifyConfidence() >= 0.0D
                        || entry.getValue().punishConfidence() >= 0.0D)
                .map(entry -> entry.getKey().configurationKey() + "="
                        + entry.getValue().notifyConfidence() + "/" + entry.getValue().punishConfidence())
                .collect(Collectors.joining(", "));
        return result.isEmpty() ? "none" : result;
    }
}
