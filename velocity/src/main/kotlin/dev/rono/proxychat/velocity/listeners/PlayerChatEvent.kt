package dev.rono.proxychat.velocity.listeners

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.PlayerChatEvent
import dev.rono.proxychat.velocity.ProxyChatVelocity

class PlayerChatEvent {
    @Subscribe
    fun onChat(event: PlayerChatEvent) {
        val player = event.player
        val message = event.message

        if (message.startsWith("/")) {
            return
        }

        for (command in ProxyChatVelocity.instance.commands) {
            if (command.useCommandPrefix && message.startsWith(command.commandPrefix) && player.hasPermission(command.chatConfig.getString("permission") ?: "")) {
                val commandMessage = message.substring(command.commandPrefix.length).trim()
                command.handleChat(player, commandMessage.split(" ").toTypedArray())
                event.result = PlayerChatEvent.ChatResult.message("/proxychat")
                return
            } else if (command.toggleUtils.isToggled(player.uniqueId)) {
                command.handleChat(player, message.split(" ").toTypedArray())
                event.result = PlayerChatEvent.ChatResult.message("/proxychat")
                return
            }
        }
    }
}
