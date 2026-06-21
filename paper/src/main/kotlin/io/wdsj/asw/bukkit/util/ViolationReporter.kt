package io.wdsj.asw.bukkit.util

import io.wdsj.asw.bukkit.setting.PaperConfigurationService
import io.wdsj.asw.bukkit.manage.notice.Notifier
import io.wdsj.asw.bukkit.manage.punish.PunishmentService
import io.wdsj.asw.bukkit.manage.punish.ViolationCounter
import io.wdsj.asw.bukkit.proxy.velocity.VelocitySender
import io.wdsj.asw.bukkit.setting.PluginSettings
import io.wdsj.asw.bukkit.type.ModuleType
import io.wdsj.asw.bukkit.api.moderation.LlmChatModerationResult
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.Event

class ViolationReporter(private val configuration: PaperConfigurationService) {
    private val punishmentService = PunishmentService(configuration)

    fun reportLlm(
        player: Player,
        content: String,
        result: LlmChatModerationResult,
        punishmentActions: List<String>,
        notifyOperators: Boolean,
    ) {
        val categoryLabel = "LLM:${result.category().wireName()}"
        val logPrefix = "${player.name}(IP: ${Utils.getPlayerIp(player)})(AI)(LLM category=${result.category().wireName()}, severity=${result.severity().wireName()}, confidence=${result.confidence()})"
        reportWithCustomLogPrefix(
            player = player,
            moduleType = ModuleType.AI,
            content = content,
            censoredWords = listOf(categoryLabel),
            logPrefix = logPrefix,
            startTime = System.currentTimeMillis(),
            punishmentActions = punishmentActions,
            notifyOperators = notifyOperators,
        )
    }

    fun reportLlmObservation(player: Player, content: String, result: LlmChatModerationResult) {
        if (configuration.get(PluginSettings.HOOK_VELOCITY)) {
            VelocitySender.sendAiObservation(player, content, result)
        }
        if (configuration.get(PluginSettings.NOTICE_OPERATOR)) {
            Notifier.noticeAiObservation(player, content, result)
        }
    }

    fun report(
        player: Player,
        moduleType: ModuleType,
        content: String,
        censoredWords: List<String>,
        logSource: String,
        startTime: Long,
        punishmentActions: List<String>,
        logContent: String = content,
        event: Event? = null,
        notificationInteraction: Component? = null,
    ) {
        Utils.messagesFilteredNum.getAndIncrement()

        if (configuration.get(PluginSettings.LOG_VIOLATION)) {
            LoggingUtils.logViolation(
                "${player.name}(IP: ${Utils.getPlayerIp(player)})($logSource)",
                logContent + censoredWords,
            )
        }

        ViolationCounter.INSTANCE.incrementViolationCount(player, moduleType)

        if (configuration.get(PluginSettings.HOOK_VELOCITY)) {
            VelocitySender.sendNotifyMessage(player, moduleType, content, censoredWords)
        }

        TimingUtils.addProcessStatistic(System.currentTimeMillis(), startTime)

        if (configuration.get(PluginSettings.NOTICE_OPERATOR)) {
            Notifier.notice(player, moduleType, content, censoredWords, notificationInteraction)
        }

        executePunishment(player, moduleType, punishmentActions, event)
    }

    fun reportWithCustomLogPrefix(
        player: Player,
        moduleType: ModuleType,
        content: String,
        censoredWords: List<String>,
        logPrefix: String,
        startTime: Long,
        punishmentActions: List<String>,
        event: Event? = null,
        notificationInteraction: Component? = null,
        notifyOperators: Boolean = true,
    ) {
        Utils.messagesFilteredNum.getAndIncrement()

        if (configuration.get(PluginSettings.LOG_VIOLATION)) {
            LoggingUtils.logViolation(logPrefix, content + censoredWords)
        }

        ViolationCounter.INSTANCE.incrementViolationCount(player, moduleType)

        if (notifyOperators && configuration.get(PluginSettings.HOOK_VELOCITY)) {
            VelocitySender.sendNotifyMessage(player, moduleType, content, censoredWords)
        }

        TimingUtils.addProcessStatistic(System.currentTimeMillis(), startTime)

        if (notifyOperators && configuration.get(PluginSettings.NOTICE_OPERATOR)) {
            Notifier.notice(player, moduleType, content, censoredWords, notificationInteraction)
        }

        executePunishment(player, moduleType, punishmentActions, event)
    }

    private fun executePunishment(
        player: Player,
        moduleType: ModuleType,
        actions: List<String>,
        event: Event?,
    ) {
        if (actions.isEmpty()) return

        val execute = Runnable {
            punishmentService.executeForModule(player, moduleType, actions)
        }
        if (event?.isAsynchronous == true) {
            SchedulingUtils.runSyncIfEventAsync(event, execute)
            return
        }
        execute.run()
    }
}
