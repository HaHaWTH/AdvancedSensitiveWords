package io.wdsj.asw.bukkit.listener

import com.github.houbb.sensitive.word.api.IWordResult
import io.wdsj.asw.bukkit.AdvancedSensitiveWords
import io.wdsj.asw.bukkit.permission.PermissionsEnum
import io.wdsj.asw.bukkit.permission.option.PlayerOptionResolver
import io.wdsj.asw.bukkit.permission.option.PlayerOptionView
import io.wdsj.asw.bukkit.permission.option.PlayerOptions
import io.wdsj.asw.bukkit.setting.PaperConfigurationService
import io.wdsj.asw.bukkit.integration.packetevents.sign.SignFakeViewCompat
import io.wdsj.asw.bukkit.manage.punish.PlayerShadowController
import io.wdsj.asw.bukkit.setting.PluginMessages
import io.wdsj.asw.bukkit.setting.PluginSettings
import io.wdsj.asw.common.type.ModuleType
import io.wdsj.asw.bukkit.util.PlayerProcessingGuard
import io.wdsj.asw.bukkit.util.SensitiveFilterEvents
import io.wdsj.asw.bukkit.util.Utils
import io.wdsj.asw.bukkit.util.ViolationReporter
import io.wdsj.asw.bukkit.util.context.SignContext
import io.wdsj.asw.bukkit.util.context.SignContextEntry
import io.wdsj.asw.bukkit.util.context.SignContextTarget
import io.wdsj.asw.bukkit.util.message.MessageUtils
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig
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
        val globalEnabled = configuration.get(PluginSettings.ENABLE_SIGN_EDIT_CHECK)
        if (!globalEnabled) return
        if (event.lines().isEmpty()) return

        val player = event.player
        if (processingGuard.shouldSkipBasic(player, PermissionsEnum.BYPASS_SIGN)) return
        val options = PlayerOptionResolver.resolve(configuration, player)
        val attemptedLines = event.lines().toList()
        if (PlayerShadowController.isShadowed(player)) {
            if (SignFakeViewCompat.recordShadowEdit(event, player, attemptedLines)) {
                event.isCancelled = true
                return
            }
        }

        val startTime = System.currentTimeMillis()
        val lineScan = censorSingleLines(event, player, options)
        val multiLineViolation = if (lineScan.violation == null || !isCancelMode(options)) {
            censorMultiLine(event, options)
        } else {
            null
        }
        val contextViolation = if (
            !isCancelMode(options) || (lineScan.violation == null && multiLineViolation == null)
        ) {
            censorContext(event, player, options)
        } else {
            null
        }
        val violation = lineScan.violation
            ?: multiLineViolation
            ?: contextViolation
            ?: return

        if (isCancelMode(options) && !violation.context &&
            options.bool(PlayerOptions.SIGN_FAKE_ON_CANCEL, PluginSettings.SIGN_FAKE_ON_CANCEL)
        ) {
            SignFakeViewCompat.recordCancelledEdit(
                event,
                player,
                attemptedLines,
                violation.content,
                violation.censoredWords,
            )
        }

        if (options.bool(PlayerOptions.SIGN_SEND_MESSAGE, PluginSettings.SIGN_SEND_MESSAGE)) {
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

    private fun censorSingleLines(event: SignChangeEvent, player: Player, options: PlayerOptionView): SignLineScan {
        var violation: SignViolation? = null

        for (lineIndex in event.lines().indices) {
            val originalComponent = event.line(lineIndex) ?: continue
            val scanComponent = preprocess(originalComponent)
            val originalMessage = MessageUtils.plainText(scanComponent)
            val censoredWords = AdvancedSensitiveWords.findAllSensitive(originalMessage)
            SensitiveFilterEvents.post(event.isAsynchronous, ModuleType.SIGN, player, originalMessage, censoredWords)

            if (censoredWords.isEmpty()) {
                continue
            }

            violation = SignViolation(originalMessage, censoredWords)
            if (isCancelMode(options)) {
                event.isCancelled = true
                continue
            }
            val processedMessage = AdvancedSensitiveWords.replaceSensitive(originalMessage)
            event.line(lineIndex, replaceWholeLine(scanComponent, originalMessage, processedMessage))
        }

        return SignLineScan(violation)
    }

    private fun censorMultiLine(event: SignChangeEvent, options: PlayerOptionView): SignViolation? {
        if (!options.bool(PlayerOptions.SIGN_MULTI_LINE_CHECK, PluginSettings.SIGN_MULTI_LINE_CHECK)) return null

        val lines = event.lines().indices.mapNotNull { lineIndex ->
            val component = event.line(lineIndex) ?: return@mapNotNull null
            val scanComponent = preprocess(component)
            val content = MessageUtils.plainText(scanComponent)
            if (content.isBlank()) return@mapNotNull null
            SignLineContent(lineIndex, scanComponent, content)
        }
        if (lines.size < 2) return null

        val originalContent = lines.joinToString("") { it.content }
        val censoredWords = AdvancedSensitiveWords.findAllSensitive(originalContent)
        SensitiveFilterEvents.post(event.isAsynchronous, ModuleType.SIGN, event.player, originalContent, censoredWords)
        if (censoredWords.isEmpty()) return null

        if (isCancelMode(options)) {
            event.isCancelled = true
        } else {
            val processedLines = resolveSegmentReplacements(
                lines.map { it.content.length },
                originalContent,
            )
            lines.forEachIndexed { index, line ->
                event.line(
                    line.index,
                    replaceWholeLine(line.component, line.content, processedLines[index]),
                )
            }
        }

        return SignViolation(originalContent, censoredWords)
    }

    private fun censorContext(event: SignChangeEvent, player: Player, options: PlayerOptionView): SignViolation? {
        if (!options.bool(PlayerOptions.SIGN_CONTEXT_CHECK, PluginSettings.SIGN_CONTEXT_CHECK)) return null

        val entry = contextEntry(event)
        val contextMaxSize = options.integer(PlayerOptions.SIGN_CONTEXT_MAX_SIZE, PluginSettings.SIGN_CONTEXT_MAX_SIZE)
        val contextMaxTime = options.integer(PlayerOptions.SIGN_CONTEXT_MAX_TIME, PluginSettings.SIGN_CONTEXT_TIME_LIMIT)
        SignContext.addMessage(player, entry, contextMaxSize)
        val entries = SignContext.getHistory(player, contextMaxSize, contextMaxTime)
        val originalContext = entries.joinToString("") { it.content }
        val censoredWords = AdvancedSensitiveWords.findAllSensitive(originalContext)
        SensitiveFilterEvents.post(event.isAsynchronous, ModuleType.SIGN, player, originalContext, censoredWords)
        if (censoredWords.isEmpty()) return null

        val resolution = resolveContext(entries, originalContext)
        applyContextAction(event, resolution, options)
        SignContext.clearPlayerContext(player)
        return SignViolation(originalContext, censoredWords, true)
    }

    private fun contextEntry(event: SignChangeEvent): SignContextEntry {
        val lines = event.lines().map { MessageUtils.plainText(preprocess(it)) }
        return SignContextEntry(
            content = lines.joinToString(""),
            target = SignContextTarget(
                event.block.world.uid,
                event.block.x,
                event.block.y,
                event.block.z,
                event.side,
            ),
            lineContents = lines,
        )
    }

    private fun applyContextAction(event: SignChangeEvent, resolution: ContextResolution, options: PlayerOptionView) {
        if (isCancelMode(options)) {
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

    private fun applyEventReplacement(event: SignChangeEvent, entry: SignContextEntry, replacement: List<String>) {
        replacement.forEachIndexed { index, line ->
            val component = preprocess(event.line(index) ?: Component.empty())
            val originalLine = entry.lineContents.getOrElse(index) { "" }
            event.line(index, replaceWholeLine(component, originalLine, line))
        }
    }

    private fun scheduleSignMutation(entry: SignContextEntry, replacement: List<String>?) {
        val world = Bukkit.getWorld(entry.target.worldId) ?: return
        val location = Location(world, entry.target.x.toDouble(), entry.target.y.toDouble(), entry.target.z.toDouble())
        AdvancedSensitiveWords.getScheduler().runTaskLater(location, Runnable {
            val sign = location.block.state as? Sign ?: return@Runnable
            val signSide = sign.getSide(entry.target.side)
            val currentLines = (0 until 4).map { line -> preprocess(signSide.line(line)) }
            val currentContent = currentLines.joinToString("") { MessageUtils.plainText(it) }
            if (currentContent != entry.content) return@Runnable

            val lines = replacement ?: List(4) { "" }
            lines.forEachIndexed { index, line ->
                val component = if (replacement == null) {
                    Component.empty()
                } else {
                    replaceWholeLine(currentLines[index], entry.lineContents[index], line)
                }
                signSide.line(index, component)
            }
            sign.update(false, false)
        }, 1L)
    }

    private fun resolveSegmentReplacements(segmentLengths: List<Int>, context: String): List<String> {
        return SignTextLayout.replaceSegments(segmentLengths, context, replacementSpans(context))
    }

    private fun resolveContext(entries: List<SignContextEntry>, context: String): ContextResolution {
        val entryLengths = IntArray(entries.size) { entries[it].content.length }
        val entryStarts = segmentStarts(entryLengths)
        val lineReferences = buildList {
            entries.forEachIndexed { entryIndex, entry ->
                entry.lineContents.forEachIndexed { lineIndex, content ->
                    if (content.isNotEmpty()) {
                        add(ContextLineReference(entryIndex, lineIndex, content))
                    }
                }
            }
        }
        val spans = replacementSpans(context)
        val resolvedLines = SignTextLayout.replaceSegments(
            lineReferences.map { it.content.length },
            context,
            spans,
        )
        val replacements = entries.associateWith {
            MutableList(it.lineContents.size) { "" }
        }
        lineReferences.forEachIndexed { index, reference ->
            replacements.getValue(entries[reference.entryIndex])[reference.lineIndex] = resolvedLines[index]
        }

        val affectedEntries = linkedSetOf<SignContextEntry>()
        spans.forEach { span ->
            markAffectedEntries(affectedEntries, entries, entryStarts, span.start, span.end)
        }

        return ContextResolution(
            replacements.mapValues { it.value.toList() },
            affectedEntries,
        )
    }

    private fun replacementSpans(context: String): List<SignReplacementSpan> {
        val spans = mutableListOf<SignReplacementSpan>()
        val results = AdvancedSensitiveWords.findAllSensitiveRaw(context)
            .sortedWith(compareBy<IWordResult> { it.startIndex() }.thenByDescending { it.endIndex() })
        var cursor = 0
        for (result in results) {
            val start = result.startIndex().coerceIn(0, context.length)
            val end = result.endIndex().coerceIn(start, context.length)
            if (start < cursor || start == end) continue
            spans.add(SignReplacementSpan(start, end, replacementFor(context, start, end)))
            cursor = end
        }
        return spans
    }

    private fun segmentStarts(lengths: IntArray): IntArray {
        val starts = IntArray(lengths.size)
        for (index in 1 until lengths.size) {
            starts[index] = starts[index - 1] + lengths[index - 1]
        }
        return starts
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

    private fun replacementFor(context: String, start: Int, end: Int): String {
        val sensitiveWord = context.substring(start, end)
        configuration.get(PluginSettings.DEFINED_REPLACEMENT).forEach { definition ->
            val separator = definition.indexOf('|')
            if (separator <= 0 || definition.indexOf('|', separator + 1) >= 0) return@forEach
            if (definition.substring(0, separator) == sensitiveWord) {
                return definition.substring(separator + 1)
            }
        }
        return configuration.get(PluginSettings.REPLACEMENT).repeat(end - start)
    }

    private fun replaceWholeLine(component: Component, originalText: String, replacement: String): Component {
        if (originalText == replacement) return component

        val replaced = MessageUtils.replaceLiteral(component, originalText, replacement)
        if (MessageUtils.plainText(replaced) == replacement) return replaced
        return Component.text(replacement).style(component.style())
    }

    private fun preprocess(component: Component): Component {
        if (!configuration.get(PluginSettings.PRE_PROCESS)) return component

        val replacementConfig = TextReplacementConfig.builder()
            .match(Utils.preProcessRegex.toPattern())
            .replacement("")
            .build()
        return component.replaceText(replacementConfig)
    }

    private fun isCancelMode(options: PlayerOptionView): Boolean {
        return options.method(PlayerOptions.SIGN_METHOD, PluginSettings.SIGN_METHOD).isCancel
    }

    private data class SignLineScan(
        val violation: SignViolation?,
    )

    private data class SignLineContent(
        val index: Int,
        val component: Component,
        val content: String,
    )

    private data class ContextLineReference(
        val entryIndex: Int,
        val lineIndex: Int,
        val content: String,
    )

    private data class SignViolation(
        val content: String,
        val censoredWords: List<String>,
        val context: Boolean = false,
    )

    private data class ContextResolution(
        val replacements: Map<SignContextEntry, List<String>>,
        val affectedEntries: Set<SignContextEntry>,
    )
}
