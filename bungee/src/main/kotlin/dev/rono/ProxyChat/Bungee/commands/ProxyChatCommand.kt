package dev.rono.proxychat.bungee.commands

import dev.rono.proxychat.bungee.ProxyChatBungee
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.plugin.Command
import net.md_5.bungee.api.plugin.TabExecutor

class ProxyChatCommand : Command("proxychat", "", "pc"), TabExecutor {

    override fun execute(sender: CommandSender, args: Array<out String>) {
        if (args.isEmpty()) {
            return
        }

        if (args[0].equals("reload", ignoreCase = true)) {
            if (sender.hasPermission(ProxyChatBungee.config.getString("reload-permission"))) {
                ProxyChatBungee.instance.registerConfiguration()
                ProxyChatBungee.instance.getChats()
                ProxyChatBungee.instance.unregisterCommands()
                ProxyChatBungee.instance.registerCommands()

                sender.sendMessage(ProxyChatBungee.instance.getConfigTextValue("reload-message"))
            }
            return
        }

        if (args[0].equals("version", ignoreCase = true)) {
            val message = TextComponent(ChatColor.DARK_BLUE.toString() + "Made by Rono @ ")
            val link = TextComponent("https://www.spigotmc.org/resources/73583/")
            link.clickEvent = ClickEvent(ClickEvent.Action.OPEN_URL, "https://www.spigotmc.org/resources/73583/")
            message.addExtra(link)
            sender.sendMessage(message)
        }
    }

    override fun onTabComplete(sender: CommandSender, args: Array<out String>): MutableIterable<String> {
        val tabCommands = mutableSetOf<String>()

        if (sender.hasPermission(ProxyChatBungee.config.getString("reload-permission"))) {
            tabCommands.add("reload")
        }

        tabCommands.add("version")

        return tabCommands
    }
}
