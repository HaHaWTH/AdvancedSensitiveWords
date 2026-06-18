package io.wdsj.asw.bukkit.util

import io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager
import io.wdsj.asw.bukkit.manage.notice.Notifier
import io.wdsj.asw.bukkit.manage.punish.Punishment
import io.wdsj.asw.bukkit.manage.punish.ViolationCounter
import io.wdsj.asw.bukkit.proxy.velocity.VelocitySender
import io.wdsj.asw.bukkit.setting.PluginSettings
import io.wdsj.asw.bukkit.type.ModuleType
import org.bukkit.entity.Player

class ViolationReporter {
    fun report(
        player: Player,
        moduleType: ModuleType,
        content: String,
        censoredWords: List<String>,
        logSource: String,
        startTime: Long,
        punish: Boolean,
        logContent: String = content,
        punishAction: () -> Unit = { Punishment.punish(player) },
    ) {
        Utils.messagesFilteredNum.getAndIncrement()

        if (settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
            LoggingUtils.logViolation(
                "${player.name}(IP: ${Utils.getPlayerIp(player)})($logSource)",
                logContent + censoredWords,
            )
        }

        ViolationCounter.INSTANCE.incrementViolationCount(player)

        if (settingsManager.getProperty(PluginSettings.HOOK_VELOCITY)) {
            VelocitySender.sendNotifyMessage(player, moduleType, content, censoredWords)
        }

        TimingUtils.addProcessStatistic(System.currentTimeMillis(), startTime)

        if (settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) {
            Notifier.notice(player, moduleType, content, censoredWords)
        }

        if (punish) {
            punishAction()
        }
    }

    fun reportWithCustomLogPrefix(
        player: Player,
        moduleType: ModuleType,
        content: String,
        censoredWords: List<String>,
        logPrefix: String,
        startTime: Long,
        punish: Boolean,
        punishAction: () -> Unit = { Punishment.punish(player) },
    ) {
        Utils.messagesFilteredNum.getAndIncrement()

        if (settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
            LoggingUtils.logViolation(logPrefix, content + censoredWords)
        }

        ViolationCounter.INSTANCE.incrementViolationCount(player)

        if (settingsManager.getProperty(PluginSettings.HOOK_VELOCITY)) {
            VelocitySender.sendNotifyMessage(player, moduleType, content, censoredWords)
        }

        TimingUtils.addProcessStatistic(System.currentTimeMillis(), startTime)

        if (settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) {
            Notifier.notice(player, moduleType, content, censoredWords)
        }

        if (punish) {
            punishAction()
        }
    }
}
