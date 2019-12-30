package dev.rono.proxychat.listeners;

import dev.rono.proxychat.ProxyChat;
import dev.rono.proxychat.commands.ChatCommand;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class PlayerChatEvent implements Listener {

    @EventHandler
    public void onChat(ChatEvent e) {
        if (e.getMessage().startsWith("/")) {
            return;
        }

        for (ChatCommand command : ProxyChat.commands) {
            ProxiedPlayer player = (ProxiedPlayer) e.getSender();

            if (command.useCommandPrefix && e.getMessage().startsWith(command.commandPrefix)) {
                String message = (String) e.getMessage().subSequence(1, e.getMessage().length());
                command.execute((CommandSender) e.getSender(), message.split(" "));
                e.setCancelled(true);

            } else if (command.toggleUtils.isToggled(player.getUniqueId())) {
                command.execute((CommandSender) e.getSender(), e.getMessage().split(" "));
                e.setCancelled(true);
            }
        }
    }
}