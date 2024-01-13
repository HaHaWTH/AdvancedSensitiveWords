package io.wdsj.asw.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * This event fired when each message is processed by the plugin.
 */
public class ASWFilterEvent extends Event {
    private final Player player;
    private final String originalMessage;
    private final String filteredMessage;
    private final List<String> sensitiveWordList;
    private final EventType eventType;

    private static final HandlerList handlers = new HandlerList();

    public ASWFilterEvent(Player player, String originalMessage, String filteredMessage, List<String> sensitiveWordList, EventType eventType) {
        this.player = player;
        this.originalMessage = originalMessage;
        this.filteredMessage = filteredMessage;
        this.sensitiveWordList = sensitiveWordList;
        this.eventType = eventType;
    }

    /**
     * Returns the player who sent the message.
     *
     * @return the player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Returns the message which is not filtered.
     *
     * @return the original message
     */
    public String getOriginalMessage() {
        return originalMessage;
    }

    /**
     * Returns the message filtered
     *
     * @return the filtered message
     */
    public String getFilteredMessage() {
        return filteredMessage;
    }

    /**
     * Returns true if the message is filtered.
     * If the player has bypass permission, this will return false.
     *
     * @return true if the message is filtered, false otherwise
     * @deprecated Since version Flare, this always returns true.
     */
    @Deprecated
    public boolean isMessageFiltered() {
        return !originalMessage.equals(filteredMessage);
    }

    /**
     * Returns a list of filtered words in the message.
     *
     * @return the list of filtered words
     */
    public List<String> getFilteredWordList() {
        return sensitiveWordList;
    }

    /**
     * Returns the type of event that is being fired.
     * EventTypes: CHAT, SIGN, BOOK, ANVIL
     *
     * @return the event type
     */
    public EventType getEventType() {
        return eventType;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}
