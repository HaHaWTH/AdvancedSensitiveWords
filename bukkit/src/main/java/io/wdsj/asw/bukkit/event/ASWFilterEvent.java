package io.wdsj.asw.bukkit.event;

import org.apiguardian.api.API;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author HaHaWTH & HeyWTF_IS_That & 0D00_0721
 * Edited on 2024/2/6 03:58 UTC+8
 * @since Flare
 * @version Railgun
 */
public class ASWFilterEvent extends Event {
    private final Player player;
    private final String originalMessage;
    private final String filteredMessage;
    private final List<String> sensitiveWordList;
    private final EventType eventType;
    private static final HandlerList handlers = new HandlerList();

    public ASWFilterEvent(Player player, String originalMessage, String filteredMessage, List<String> sensitiveWordList, EventType eventType, boolean isAsync) {
        super(isAsync);
        this.player = player;
        this.originalMessage = originalMessage;
        this.filteredMessage = filteredMessage;
        this.sensitiveWordList = sensitiveWordList;
        this.eventType = eventType;
    }

    /**
     * Get the player who triggers the event.
     * @return The player who triggers the event.
     */
    public Player getPlayer() {
        return this.player;
    }

    /**
     * Get the original message (Which is not being processed by the plugin)
     * @return the message
     */
    public String getOriginalMessage() {
        return this.originalMessage;
    }

    /**
     * Get the filtered message (Which has been processed by the plugin)
     * @return the message
     */
    public String getFilteredMessage() {
        return this.filteredMessage;
    }

    /**
     * @deprecated Since version Flare, all events will be called only if the message is filtered
     * so this will always return true.
     */
    @API(status = API.Status.DEPRECATED, since = "Flare")
    @Deprecated
    public boolean isMessageFiltered() {
        return !this.originalMessage.equals(this.filteredMessage);
    }

    /**
     * Get the filtered word list.
     * @return a list that contains the sensitive words found in the message.
     */
    public List<String> getFilteredWordList() {
        return this.sensitiveWordList;
    }

    /**
     * Get the event type.
     * Available event types: CHAT, BOOK, NAME, SIGN, ANVIL, ITEM
     * @return the event type.
     */
    @NotNull
    public EventType getEventType() {
        return this.eventType;
    }

    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
