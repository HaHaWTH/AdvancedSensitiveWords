package io.wdsj.asw.bukkit.api.event;

import io.wdsj.asw.common.type.ModuleType;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Fired after ASW completes a DFA sensitive-word filtering pass.
 *
 * <p>This event is observational. It cannot change ASW's decision, replacement output, cancellation state,
 * notification, logging, or punishment. The event follows the thread of the original Bukkit/Paper event; when
 * asynchronous, handlers must not directly access Bukkit world, entity, or inventory APIs.</p>
 */
@SuppressWarnings("unused")
public final class SensitiveFilterPostProcessEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final ModuleType moduleType;
    private final UUID playerId;
    private final String playerName;
    private final String originalContent;
    private final List<String> censoredWords;
    private final boolean detected;

    @ApiStatus.Internal
    public SensitiveFilterPostProcessEvent(
            boolean asynchronous,
            ModuleType moduleType,
            @Nullable UUID playerId,
            @Nullable String playerName,
            String originalContent,
            List<String> censoredWords
    ) {
        super(asynchronous);
        this.moduleType = Objects.requireNonNull(moduleType, "moduleType");
        this.playerId = playerId;
        this.playerName = playerName;
        this.originalContent = Objects.requireNonNull(originalContent, "originalContent");
        this.censoredWords = List.copyOf(censoredWords);
        this.detected = !this.censoredWords.isEmpty();
    }

    public ModuleType getModuleType() {
        return moduleType;
    }

    /**
     * @return player UUID when the filtered content belongs to a player, or {@code null} for playerless
     * sources such as server broadcasts
     */
    @Nullable
    public UUID getPlayerId() {
        return playerId;
    }

    /**
     * @return player name when the filtered content belongs to a player, or {@code null} for playerless
     * sources such as server broadcasts
     */
    @Nullable
    public String getPlayerName() {
        return playerName;
    }

    public String getOriginalContent() {
        return originalContent;
    }

    public List<String> getCensoredWords() {
        return censoredWords;
    }

    public boolean isDetected() {
        return detected;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
