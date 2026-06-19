package io.wdsj.asw.bukkit.api.event;

import io.wdsj.asw.bukkit.api.moderation.LlmChatModerationResult;
import io.wdsj.asw.bukkit.type.ModuleType;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Objects;
import java.util.UUID;

/**
 * Fired asynchronously after the LLM returns a response for a moderation request.
 * Event listeners must not access Bukkit world or entity directly.
 */
public final class AsyncModerationResponseEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID requestId;
    private final ModuleType source;
    private final UUID playerId;
    private final String playerName;
    private final String message;
    private final double entropy;
    private final String rawResponse;
    private final boolean rawResponseTruncated;
    private boolean cancelled;
    private LlmChatModerationResult result;

    public AsyncModerationResponseEvent(
            UUID requestId,
            ModuleType source,
            UUID playerId,
            String playerName,
            String message,
            double entropy,
            String rawResponse,
            boolean rawResponseTruncated,
            LlmChatModerationResult result
    ) {
        super(true);
        this.requestId = Objects.requireNonNull(requestId, "requestId");
        this.source = Objects.requireNonNull(source, "source");
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.playerName = Objects.requireNonNull(playerName, "playerName");
        this.message = Objects.requireNonNull(message, "message");
        this.entropy = entropy;
        this.rawResponse = Objects.requireNonNull(rawResponse, "rawResponse");
        this.rawResponseTruncated = rawResponseTruncated;
        this.result = result;
    }

    public UUID getRequestId() {
        return requestId;
    }

    public ModuleType getSource() {
        return source;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getMessage() {
        return message;
    }

    public double getEntropy() {
        return entropy;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public boolean isRawResponseTruncated() {
        return rawResponseTruncated;
    }

    public LlmChatModerationResult getResult() {
        return result;
    }

    public void setResult(LlmChatModerationResult result) {
        this.result = result;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
