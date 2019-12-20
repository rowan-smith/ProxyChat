package dev.rono.bungeechat.listeners;

import dev.rono.bungeechat.BungeeChat;
import dev.rono.bungeechat.commands.ChatCommand;
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

        for (ChatCommand command : BungeeChat.commands) {
            if (command.toggledPlayers.contains(player)) {
                command.execute((CommandSender) e.getSender(), e.getMessage().split(" "));
                e.setCancelled(true);
            }
        }
    }
}