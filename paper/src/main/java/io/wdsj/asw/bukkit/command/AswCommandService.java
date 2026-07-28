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
import io.wdsj.asw.common.type.ModuleType;
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
import java.util.LinkedHashMap;
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
        if (AdvancedSensitiveWords.networkSensitiveWordBs != null) {
            AdvancedSensitiveWords.networkSensitiveWordBs.destroy();
        }
        if (AdvancedSensitiveWords.obfuscatedUrlDetector != null) {
            AdvancedSensitiveWords.obfuscatedUrlDetector.close();
        }
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
        sendTemplate(sender, PluginMessages.MESSAGE_ON_COMMAND_STATUS, Map.of(
                "num", String.valueOf(Utils.messagesFilteredNum.get()),
                "mode", "Event",
                "init", String.valueOf(AdvancedSensitiveWords.isInitialized),
                "ms", TimingUtils.getProcessAverage() + "ms",
                "version", AdvancedSensitiveWords.PLUGIN_VERSION,
                "mc_version", Utils.getMinecraftVersion(),
                "platform", platform,
                "bit", bitness,
                "java_version", TimingUtils.getJvmVersion(),
                "java_vendor", TimingUtils.getJvmVendor()
        ));
    }

    public void showAiStatus(CommandSender sender) {
        LlmChatDetectionService.LlmRuntimeStatus status = plugin.getLlmChatDetectionService().runtimeStatus();
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("enabled", String.valueOf(status.enabled()));
        placeholders.put("submitted", String.valueOf(status.submittedRequests()));
        placeholders.put("dropped", String.valueOf(status.droppedRequests()));
        placeholders.put("failed", String.valueOf(status.failedRequests()));
        placeholders.put("invalid", String.valueOf(status.invalidResponses()));
        placeholders.put("enforced", String.valueOf(status.enforcedResponses()));
        placeholders.put("active", String.valueOf(status.activeRequests()));
        placeholders.put("queued", String.valueOf(status.queuedRequests()));
        placeholders.put("pool_size", String.valueOf(status.poolSize()));
        placeholders.put("model", status.modelName());
        placeholders.put("api_mode", status.apiMode().name());
        placeholders.put("thresholds", formatCategoryPolicies(status.categoryPolicy()));
        sendTemplate(sender, PluginMessages.MESSAGE_ON_AI_STATUS, placeholders);
    }

    public void test(CommandSender sender, String text) {
        if (!isInitialized(sender)) {
            return;
        }

        List<String> censoredWords = AdvancedSensitiveWords.findAllSensitive(text);
        if (censoredWords.isEmpty()) {
            MessageUtils.sendMessage(sender, PluginMessages.MESSAGE_ON_COMMAND_TEST_PASS);
            return;
        }

        sendTemplate(sender, PluginMessages.MESSAGE_ON_COMMAND_TEST, Map.of(
                "original_msg", text,
                "processed_msg", AdvancedSensitiveWords.replaceSensitive(text),
                "censored_list", censoredWords.toString()
        ));
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
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("player", player.getName());
        placeholders.put("violation", String.valueOf(ViolationCounter.INSTANCE.getTotalViolationCount(player)));
        for (ModuleType moduleType : ModuleType.violationModules()) {
            String placeholder = moduleType.name().toLowerCase(Locale.ROOT) + "_violation";
            placeholders.put(placeholder, String.valueOf(ViolationCounter.INSTANCE.getViolationCount(player, moduleType)));
        }
        sendTemplate(sender, PluginMessages.MESSAGE_ON_PLAYER_INFO, placeholders);
    }

    public void resetPlayerViolations(CommandSender sender, Player player, ModuleType moduleType) {
        boolean handledByProxy = plugin.getVelocitySyncClient() != null
                && plugin.getVelocitySyncClient().requestReset(player, moduleType);
        if (!handledByProxy) {
            if (moduleType == null) {
                ViolationCounter.INSTANCE.resetViolationCount(player);
            } else {
                ViolationCounter.INSTANCE.resetViolationCount(player, moduleType);
            }
        }
        sendTemplate(sender, PluginMessages.MESSAGE_ON_COMMAND_RESET, Map.of(
                "player", player.getName(),
                "module", moduleType == null ? "ALL" : moduleType.name()
        ));
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

        sendTemplate(sender, PluginMessages.MESSAGE_ON_COMMAND_PUNISH_SUCCESS, Map.of(
                "player", player.getName()
        ));
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

    private static void sendTemplate(
            CommandSender sender,
            PluginMessages template,
            Map<String, String> placeholders
    ) {
        MessageUtils.sendTemplate(sender, MessageUtils.retrieveMessage(template), placeholders);
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
