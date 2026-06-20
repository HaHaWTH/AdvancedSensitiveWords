package io.wdsj.asw.bukkit.listener

import com.github.houbb.sensitive.word.api.IWordResult
import com.github.houbb.sensitive.word.support.result.WordResultHandlers
import io.wdsj.asw.bukkit.AdvancedSensitiveWords
import io.wdsj.asw.bukkit.AdvancedSensitiveWords.sensitiveWordBs
import io.wdsj.asw.bukkit.setting.PaperConfigurationService
import io.wdsj.asw.bukkit.integration.packetevents.sign.SignFakeViewService
import io.wdsj.asw.bukkit.setting.PluginMessages
import io.wdsj.asw.bukkit.setting.PluginSettings
import io.wdsj.asw.bukkit.type.ModuleType
import io.wdsj.asw.bukkit.util.PlayerProcessingGuard
import io.wdsj.asw.bukkit.util.Utils
import io.wdsj.asw.bukkit.util.ViolationReporter
import io.wdsj.asw.bukkit.util.context.SignContext
import io.wdsj.asw.bukkit.util.context.SignContextEntry
import io.wdsj.asw.bukkit.util.context.SignContextTarget
import io.wdsj.asw.bukkit.util.message.MessageUtils
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.Sign
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.SignChangeEvent

class SignListener(private val configuration: PaperConfigurationService) : Listener {
    private val processingGuard = PlayerProcessingGuard(configuration)
    private val violationReporter = ViolationReporter(configuration)

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onSign(event: SignChangeEvent) {
        if (!configuration.get(PluginSettings.ENABLE_SIGN_EDIT_CHECK)) return
        if (event.lines().isEmpty()) return

        val player = event.player
        if (processingGuard.shouldSkipBasic(player)) return

        val startTime = System.currentTimeMillis()
        val attemptedLines = event.lines().toList()
        val lineScan = censorSingleLines(event)
        val violation = lineScan.violation
            ?: censorMultiLine(event, lineScan)
            ?: censorContext(event, player)
            ?: return

        if (isCancelMode() && !violation.context && configuration.get(PluginSettings.SIGN_FAKE_ON_CANCEL)) {
            SignFakeViewService.recordCancelledEdit(
                event,
                player,
                attemptedLines,
                violation.content,
                violation.censoredWords,
            )
        }

        if (configuration.get(PluginSettings.SIGN_SEND_MESSAGE)) {
            MessageUtils.sendMessage(player, PluginMessages.MESSAGE_ON_SIGN)
        }

        val location = event.block.location
        val locationLog = "World: ${location.world?.name ?: "Unknown"}, X: ${location.x}, Y: ${location.y}, Z: ${location.z}"
        violationReporter.reportWithCustomLogPrefix(
            player = player,
            moduleType = ModuleType.SIGN,
            content = violation.content,
            censoredWords = violation.censoredWords,
            logPrefix = "${player.name}(IP: ${Utils.getPlayerIp(player)})(Sign)($locationLog)",
            startTime = startTime,
            punishmentActions = configuration.get(PluginSettings.SIGN_PUNISHMENT),
            event = event,
            notificationInteraction = signLocationInteraction(location),
        )
    }

    private fun signLocationInteraction(location: Location): Component {
        val world = location.world ?: return Component.empty()
        val hoverText = Component.text()
            .append(Component.text("World: ", NamedTextColor.GRAY))
            .append(Component.text(world.name, NamedTextColor.AQUA))
            .append(Component.newline())
            .append(Component.text("X: ${location.blockX}, Y: ${location.blockY}, Z: ${location.blockZ}", NamedTextColor.GRAY))
            .append(Component.newline())
            .append(Component.text("Click to teleport", NamedTextColor.GREEN))
            .build()
        return Component.empty()
            .hoverEvent(HoverEvent.showText(hoverText))
            .clickEvent(ClickEvent.runCommand(
                "/asw teleport ${world.uid} ${location.blockX} ${location.blockY} ${location.blockZ}",
            ))
    }

    private fun censorSingleLines(event: SignChangeEvent): SignLineScan {
        var violation: SignViolation? = null
        val cleanLineIndexes = mutableListOf<Int>()
        val cleanLineContent = StringBuilder()

        for (lineIndex in event.lines().indices) {
            val originalComponent = event.line(lineIndex) ?: continue
            val originalMessage = preprocess(MessageUtils.plainText(originalComponent))
            val censoredWords = sensitiveWordBs.findAll(originalMessage)

            if (censoredWords.isEmpty()) {
                if (originalMessage.trim().isNotEmpty()) {
                    cleanLineIndexes.add(lineIndex)
                    cleanLineContent.append(originalMessage)
                }
                continue
            }

            violation = SignViolation(originalMessage, censoredWords)
            if (isCancelMode()) {
                event.isCancelled = true
                continue
            }
            val processedMessage = sensitiveWordBs.replace(originalMessage)
            event.line(lineIndex, MessageUtils.replaceLiteral(originalComponent, originalMessage, processedMessage))
        }

        return SignLineScan(violation, cleanLineIndexes, cleanLineContent.toString())
    }

    private fun censorMultiLine(event: SignChangeEvent, lineScan: SignLineScan): SignViolation? {
        if (!configuration.get(PluginSettings.SIGN_MULTI_LINE_CHECK)) return null
        if (lineScan.cleanLineIndexes.isEmpty()) return null

        val censoredWords = sensitiveWordBs.findAll(lineScan.cleanLineContent)
        if (censoredWords.isEmpty()) return null

        if (isCancelMode()) {
            event.isCancelled = true
        } else {
            val processedMessage = sensitiveWordBs.replace(lineScan.cleanLineContent)
            for (lineIndex in lineScan.cleanLineIndexes) {
                event.line(lineIndex, MessageUtils.plainTextComponent(processedMessage))
            }
        }

        return SignViolation(lineScan.cleanLineContent, censoredWords)
    }

    private fun censorContext(event: SignChangeEvent, player: Player): SignViolation? {
        if (!configuration.get(PluginSettings.SIGN_CONTEXT_CHECK)) return null

        val entry = contextEntry(event)
        SignContext.addMessage(player, entry)
        val entries = SignContext.getHistory(player)
        val originalContext = entries.joinToString("") { it.content }
        val censoredWords = sensitiveWordBs.findAll(originalContext)
        if (censoredWords.isEmpty()) return null

        val resolution = resolveContext(entries, originalContext)
        applyContextAction(event, resolution)
        SignContext.clearPlayerContext(player)
        return SignViolation(originalContext, censoredWords, true)
    }

    private fun contextEntry(event: SignChangeEvent): SignContextEntry {
        val lines = event.lines().map { preprocess(MessageUtils.plainText(it)) }
        return SignContextEntry(
            content = lines.joinToString(""),
            target = SignContextTarget(
                event.block.world.uid,
                event.block.x,
                event.block.y,
                event.block.z,
                event.side,
            ),
            lineLengths = lines.map(String::length),
        )
    }

    private fun applyContextAction(event: SignChangeEvent, resolution: ContextResolution) {
        if (isCancelMode()) {
            event.isCancelled = true
            resolution.affectedEntries.forEach { scheduleSignMutation(it, null) }
            return
        }

        val currentTarget = SignContextTarget(
            event.block.world.uid,
            event.block.x,
            event.block.y,
            event.block.z,
            event.side,
        )
        resolution.affectedEntries.forEach { entry ->
            val replacement = resolution.replacements.getValue(entry)
            if (entry.target == currentTarget) {
                applyEventReplacement(event, entry, replacement)
            } else {
                scheduleSignMutation(entry, replacement)
            }
        }
    }

    private fun applyEventReplacement(event: SignChangeEvent, entry: SignContextEntry, replacement: String) {
        splitLines(entry.lineLengths, replacement).forEachIndexed { index, line ->
            event.line(index, Component.text(line))
        }
    }

    private fun scheduleSignMutation(entry: SignContextEntry, replacement: String?) {
        val world = Bukkit.getWorld(entry.target.worldId) ?: return
        val location = Location(world, entry.target.x.toDouble(), entry.target.y.toDouble(), entry.target.z.toDouble())
        AdvancedSensitiveWords.getScheduler().runTaskLater(location, Runnable {
            val sign = location.block.state as? Sign ?: return@Runnable
            val signSide = sign.getSide(entry.target.side)
            val currentContent = (0 until 4).joinToString("") { line ->
                preprocess(MessageUtils.plainText(signSide.line(line)))
            }
            if (currentContent != entry.content) return@Runnable

            val lines = replacement?.let { splitLines(entry.lineLengths, it) } ?: List(4) { "" }
            lines.forEachIndexed { index, line -> signSide.line(index, Component.text(line)) }
            sign.update(false, false)
        }, 1L)
    }

    private fun resolveContext(entries: List<SignContextEntry>, context: String): ContextResolution {
        val starts = IntArray(entries.size)
        for (index in 1 until entries.size) {
            starts[index] = starts[index - 1] + entries[index - 1].content.length
        }

        val replacements = Array(entries.size) { StringBuilder() }
        val affectedEntries = linkedSetOf<SignContextEntry>()
        val results = sensitiveWordBs.findAll(context, WordResultHandlers.raw())
            .sortedWith(compareBy<IWordResult> { it.startIndex() }.thenByDescending { it.endIndex() })
        var cursor = 0
        for (result in results) {
            val start = result.startIndex().coerceIn(0, context.length)
            val end = result.endIndex().coerceIn(start, context.length)
            if (start < cursor || start == end) continue

            appendUnchangedContext(replacements, entries, starts, context, cursor, start)
            replacements[entryIndexAt(starts, start)].append(replacementFor(context, result))
            markAffectedEntries(affectedEntries, entries, starts, start, end)
            cursor = end
        }
        appendUnchangedContext(replacements, entries, starts, context, cursor, context.length)

        return ContextResolution(
            entries.indices.associate { index -> entries[index] to replacements[index].toString() },
            affectedEntries,
        )
    }

    private fun appendUnchangedContext(
        replacements: Array<StringBuilder>,
        entries: List<SignContextEntry>,
        starts: IntArray,
        context: String,
        start: Int,
        end: Int,
    ) {
        var cursor = start
        while (cursor < end) {
            val entryIndex = entryIndexAt(starts, cursor)
            val entryEnd = starts[entryIndex] + entries[entryIndex].content.length
            val segmentEnd = minOf(end, entryEnd)
            replacements[entryIndex].append(context, cursor, segmentEnd)
            cursor = segmentEnd
        }
    }

    private fun entryIndexAt(starts: IntArray, index: Int): Int {
        for (entryIndex in starts.indices.reversed()) {
            if (index >= starts[entryIndex]) return entryIndex
        }
        return 0
    }

    private fun markAffectedEntries(
        affectedEntries: MutableSet<SignContextEntry>,
        entries: List<SignContextEntry>,
        starts: IntArray,
        start: Int,
        end: Int,
    ) {
        entries.forEachIndexed { index, entry ->
            val entryStart = starts[index]
            val entryEnd = entryStart + entry.content.length
            if (start < entryEnd && end > entryStart) {
                affectedEntries.add(entry)
            }
        }
    }

    private fun replacementFor(context: String, result: IWordResult): String {
        val sensitiveWord = context.substring(result.startIndex(), result.endIndex())
        configuration.get(PluginSettings.DEFINED_REPLACEMENT).forEach { definition ->
            val separator = definition.indexOf('|')
            if (separator <= 0 || definition.indexOf('|', separator + 1) >= 0) return@forEach
            if (definition.substring(0, separator) == sensitiveWord) {
                return definition.substring(separator + 1)
            }
        }
        return configuration.get(PluginSettings.REPLACEMENT).repeat(result.endIndex() - result.startIndex())
    }

    private fun splitLines(lineLengths: List<Int>, content: String): List<String> {
        val lines = MutableList(4) { "" }
        var offset = 0
        for (lineIndex in lines.indices) {
            val expectedLength = lineLengths.getOrElse(lineIndex) { 0 }
            val end = minOf(content.length, offset + expectedLength)
            lines[lineIndex] = content.substring(offset, end)
            offset = end
        }
        if (offset < content.length) {
            lines[3] += content.substring(offset)
        }
        return lines
    }

    private fun preprocess(text: String): String {
        if (!configuration.get(PluginSettings.PRE_PROCESS)) return text
        return text.replace(Utils.preProcessRegex.toRegex(), "")
    }

    private fun isCancelMode(): Boolean {
        return configuration.get(PluginSettings.SIGN_METHOD).isCancel
    }

    private data class SignLineScan(
        val violation: SignViolation?,
        val cleanLineIndexes: List<Int>,
        val cleanLineContent: String,
    )

    private data class SignViolation(
        val content: String,
        val censoredWords: List<String>,
        val context: Boolean = false,
    )

    private data class ContextResolution(
        val replacements: Map<SignContextEntry, String>,
        val affectedEntries: Set<SignContextEntry>,
    )
}
