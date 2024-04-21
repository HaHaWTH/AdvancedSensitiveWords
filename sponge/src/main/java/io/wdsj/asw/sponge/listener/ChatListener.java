package io.wdsj.asw.sponge.listener;

import net.kyori.adventure.text.Component;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.message.MessageEvent;
import org.spongepowered.api.entity.living.player.Player;

public class ChatListener {

    @Listener
    public void onPlayerChat(MessageEvent event) {
        Component message = event.originalMessage();
        if (event.cause().first(Player.class).isPresent()) {
            Player player = event.cause().first(Player.class).get();
            System.out.println(player.name() + " said: " + message);
        }

    }
}
