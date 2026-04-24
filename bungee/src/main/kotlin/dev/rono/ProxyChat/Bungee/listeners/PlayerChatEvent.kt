package dev.rono.proxychat.bungee.listeners

import dev.rono.proxychat.bungee.ProxyChatBungee
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.event.ChatEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler

class PlayerChatEvent : Listener {
    @EventHandler
    fun onChat(e: ChatEvent) {
        if (e.isCommand || e.isProxyCommand) {
            return
        }

        val player = e.sender as? ProxiedPlayer ?: return

        for (command in ProxyChatBungee.commands) {
            if (command.useCommandPrefix && e.message.startsWith(command.commandPrefix) && player.hasPermission(command.permission)) {
                val message = e.message.substring(command.commandPrefix.length).trim()
                command.handleChat(player, message.split(" ").toTypedArray())
                e.message = "/proxychat"
                return
            } else if (command.toggleUtils.isToggled(player.uniqueId)) {
                command.handleChat(player, e.message.split(" ").toTypedArray())
                e.message = "/proxychat"
                return
            }
        }
    }
}
