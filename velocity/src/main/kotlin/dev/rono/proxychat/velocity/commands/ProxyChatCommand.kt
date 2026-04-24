package dev.rono.proxychat.velocity.commands

import com.velocitypowered.api.command.SimpleCommand
import dev.rono.proxychat.velocity.ProxyChatVelocity
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor

class ProxyChatCommand(private val plugin: ProxyChatVelocity) : SimpleCommand {

    override fun execute(invocation: SimpleCommand.Invocation) {
        val sender = invocation.source()
        val args = invocation.arguments()

        if (args.isEmpty()) {
            return
        }

        if (args[0].equals("reload", ignoreCase = true)) {
            if (sender.hasPermission(plugin.pluginConfig.getString("reload-permission"))) {
                plugin.registerConfiguration()
                plugin.getChats()
                plugin.unregisterCommands()
                plugin.registerCommands()

                sender.sendMessage(plugin.getConfigTextValue("reload-message"))
            }
            return
        }

        if (args[0].equals("version", ignoreCase = true)) {
            val message = Component.text("Made by Rono @ ", NamedTextColor.DARK_BLUE)
                .append(Component.text("https://www.spigotmc.org/resources/73583/")
                    .clickEvent(ClickEvent.openUrl("https://www.spigotmc.org/resources/73583/")))
            sender.sendMessage(message)
        }
    }

    override fun suggest(invocation: SimpleCommand.Invocation): List<String> {
        val args = invocation.arguments()
        val sender = invocation.source()
        val tabCommands = mutableListOf<String>()

        if (args.size <= 1) {
            if (sender.hasPermission(plugin.pluginConfig.getString("reload-permission"))) {
                tabCommands.add("reload")
            }
            tabCommands.add("version")
        }

        return tabCommands.filter { it.startsWith(args.lastOrNull() ?: "", ignoreCase = true) }
    }
}
