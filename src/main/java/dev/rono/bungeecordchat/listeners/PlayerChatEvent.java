package dev.rono.bungeecordchat.listeners;

import dev.rono.bungeecordchat.BungeecordChat;
import dev.rono.bungeecordchat.commands.ChatCommand;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class PlayerChatEvent implements Listener {

    @EventHandler
    public void onChat(ChatEvent e) {
        ProxiedPlayer player = (ProxiedPlayer) e.getSender();

        if (e.getMessage().startsWith("/")) {
            return;
        }

        if (!(e.getSender() instanceof ProxiedPlayer)) {
            return;
        }

        for (ChatCommand command : BungeecordChat.commands) {
            if (command.toggledPlayers.contains(player)) {
                command.execute((CommandSender) e.getSender(), e.getMessage().split(" "));
                e.setCancelled(true);
            }
        }
    }
}