package io.wdsj.asw.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * This event is fired when a chat message is not being determined sensitive by the plugin.
 */
public class ASWChatPassEvent extends Event {
    private final Player player;
    private final String originalMessage;
    private static final HandlerList handlers = new HandlerList();

    public ASWChatPassEvent(Player player, String originalMessage, boolean isAsync) {
        super(isAsync);
        this.player = player;
        this.originalMessage = originalMessage;
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


    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
