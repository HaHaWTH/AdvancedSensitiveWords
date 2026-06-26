package io.wdsj.asw.bukkit.api.event;

import io.wdsj.asw.bukkit.api.moderation.LlmChatModerationResult;
import io.wdsj.asw.bukkit.type.ModuleType;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;
import java.util.UUID;

/**
 * Fired asynchronously after the LLM provider returns a response for one moderation request.
 *
 * <p>Handlers run on ASW's virtual-thread request executor. They must not directly access Bukkit world,
 * entity, or inventory APIs. Schedule Bukkit work before interacting with server state.</p>
 *
 * <p>Cancelling this event suppresses only ASW's built-in record/notify/punishment follow-up. It cannot
 * retract the already-sent chat message or cancel the completed provider request. Handlers may replace the
 * parsed result with {@link #setResult(LlmChatModerationResult)}. A {@code null} result suppresses built-in
 * enforcement without cancelling the event.</p>
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

    /**
     * Creates an asynchronous LLM moderation response event.
     *
     * @param requestId unique identifier for this provider request
     * @param source incoming content source, currently {@link ModuleType#CHAT}
     * @param playerId player UUID captured before the asynchronous request
     * @param playerName player name captured before the asynchronous request
     * @param message single message sent to the provider
     * @param entropy request-gating Shannon entropy in bits per visible code point
     * @param rawResponse provider response, capped at 8 KiB UTF-8 without parsed-model modifications
     * @param rawResponseTruncated whether {@code rawResponse} was truncated to the event exposure limit
     * @param result locally validated result, or {@code null} when the provider response was invalid JSON
     */
    @ApiStatus.Internal
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

    /**
     * @return unique identifier for this provider request
     */
    public UUID getRequestId() {
        return requestId;
    }

    /**
     * @return incoming content source; this remains {@link ModuleType#CHAT} even though an enforced LLM
     * result is recorded in ASW's separate AI violation counter
     */
    public ModuleType getSource() {
        return source;
    }

    /**
     * @return player UUID captured before dispatching the request
     */
    public UUID getPlayerId() {
        return playerId;
    }

    /**
     * @return player name captured before dispatching the request
     */
    public String getPlayerName() {
        return playerName;
    }

    /**
     * @return the one plain-text chat message sent to the provider
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return request-gating Shannon entropy in bits per visible Unicode code point
     */
    public double getEntropy() {
        return entropy;
    }

    /**
     * @return raw provider response capped at 8 KiB UTF-8; treat it as untrusted data and never execute it
     */
    public String getRawResponse() {
        return rawResponse;
    }

    /**
     * @return whether the raw response exceeded the 8 KiB event exposure limit
     */
    public boolean isRawResponseTruncated() {
        return rawResponseTruncated;
    }

    /**
     * @return locally validated classification result, or {@code null} when ASW rejected the provider output
     */
    public LlmChatModerationResult getResult() {
        return result;
    }

    /**
     * Replaces the parsed classification result used by ASW after event dispatch.
     *
     * <p>The supplied immutable result has already enforced ASW's local schema invariants. Supplying
     * {@code null} disables built-in enforcement while still allowing other handlers to observe the event.</p>
     *
     * @param result replacement validated result, or {@code null}
     */
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
