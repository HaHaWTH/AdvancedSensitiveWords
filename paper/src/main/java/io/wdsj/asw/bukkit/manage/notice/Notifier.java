package io.wdsj.asw.bukkit.manage.notice;

import io.wdsj.asw.bukkit.api.moderation.LlmChatModerationResult;
import io.wdsj.asw.bukkit.manage.punish.ViolationCounter;
import io.wdsj.asw.bukkit.permission.PermissionsEnum;
import io.wdsj.asw.bukkit.permission.cache.CachingPermTool;
import io.wdsj.asw.bukkit.setting.PluginMessages;
import io.wdsj.asw.common.type.ModuleType;
import io.wdsj.asw.bukkit.util.message.MessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class Notifier {
    /**
     * Notice the operators
     * @param violatedPlayer the player who violated the rules
     * @param moduleType the detection module type
     * @param originalMessage original message sent by the player
     * @param censoredList censored list
     */
    public static void notice(Player violatedPlayer, ModuleType moduleType, String originalMessage, List<String> censoredList) {
        notice(violatedPlayer, moduleType, originalMessage, censoredList, null);
    }

    public static void notice(
            Player violatedPlayer,
            ModuleType moduleType,
            String originalMessage,
            List<String> censoredList,
            Component notificationInteraction
    ) {
        Component notification = MessageUtils.deserializeTemplate(
                MessageUtils.retrieveMessage(PluginMessages.ADMIN_REMINDER),
                Map.of(
                        "player", violatedPlayer.getName(),
                        "type", moduleType.toString(),
                        "message", stripFormatting(originalMessage),
                        "censored_list", censoredList.toString(),
                        "violation", String.valueOf(ViolationCounter.INSTANCE.getViolationCount(violatedPlayer, moduleType))
                )
        );
        if (notificationInteraction != null) {
            notification = notification
                    .hoverEvent(notificationInteraction.hoverEvent())
                    .clickEvent(notificationInteraction.clickEvent());
        }
        normalNotice(notification);
    }

    public static void normalNotice(Component message) {
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        for (Player player : players) {
            if (CachingPermTool.hasPermission(PermissionsEnum.NOTICE, player)) {
                MessageUtils.sendMessage(player, message);
            }
        }
    }

    public static void noticeAiObservation(
            Player violatedPlayer,
            String originalMessage,
            LlmChatModerationResult result
    ) {
        normalNotice(MessageUtils.deserializeTemplate(
                MessageUtils.retrieveMessage(PluginMessages.AI_OBSERVATION),
                Map.of(
                        "player", violatedPlayer.getName(),
                        "message", stripFormatting(originalMessage),
                        "category", result.category().wireName(),
                        "confidence", String.valueOf(result.confidence())
                )
        ));
    }

    /**
     * Notice Operator method used by the proxy receivers
     * @param violatedPlayer the player who violated the rules, with server name
     * @param eventType the type
     * @param originalMessage the original message sent by the player
     * @param censoredList censored list
     */
    public static void noticeFromProxy(String violatedPlayer, String serverName, String eventType, String violationCount, String originalMessage, String censoredList) {
        normalNotice(MessageUtils.deserializeTemplate(
                MessageUtils.retrieveMessage(PluginMessages.ADMIN_REMINDER_PROXY),
                Map.of(
                        "player", violatedPlayer,
                        "type", eventType,
                        "message", stripFormatting(originalMessage),
                        "censored_list", censoredList,
                        "server_name", serverName,
                        "violation", violationCount
                )
        ));
    }

    public static void noticeAiObservationFromProxy(
            String violatedPlayer,
            String serverName,
            String originalMessage,
            String category,
            String confidence
    ) {
        normalNotice(MessageUtils.deserializeTemplate(
                MessageUtils.retrieveMessage(PluginMessages.AI_OBSERVATION_PROXY),
                Map.of(
                        "player", violatedPlayer,
                        "server_name", serverName,
                        "message", stripFormatting(originalMessage),
                        "category", category,
                        "confidence", confidence
                )
        ));
    }

    private static String stripFormatting(String message) {
        return MessageUtils.plainTextFromLegacy(message);
    }
}
