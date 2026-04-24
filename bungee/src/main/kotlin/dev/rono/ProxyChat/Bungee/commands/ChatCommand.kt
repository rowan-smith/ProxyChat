package dev.rono.proxychat.bungee.commands

import dev.rono.proxychat.api.ProxyChatConfig
import dev.rono.proxychat.bungee.ProxyChatBungee
import dev.rono.proxychat.core.utils.CooldownRunnable
import dev.rono.proxychat.core.utils.ToggleUtils
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.plugin.Command
import net.md_5.bungee.api.plugin.TabExecutor
import java.util.concurrent.TimeUnit

class ChatCommand(chatConfig: ProxyChatConfig) : Command(
    chatConfig.getString("command-name"),
    chatConfig.getString("permission"),
    chatConfig.getString("command-alias")
), TabExecutor, KoinComponent {
    val useCommandPrefix = chatConfig.getBoolean("use-command-prefix")
    val commandPrefix = chatConfig.getString("command-prefix") ?: ""

    val isToggleable = chatConfig.getBoolean("toggleable")
    val isIgnorable = chatConfig.getBoolean("ignorable")
    val isLocal = chatConfig.getBoolean("local")

    val commandDelay = chatConfig.getInt("command-delay")
    val commandDelayOverridePermission = chatConfig.getString("command-delay-override-permission") ?: ""

    val chatFormat = chatConfig.getString("format") ?: ""
    val chatName = chatConfig.getString("chat-name") ?: ""
    val invalidArguments = chatConfig.getString("invalid-args") ?: ""

    val consoleChatFormat = chatConfig.getString("console-format") ?: ""
    val isConsoleAllowed = chatConfig.getBoolean("console-chat-allowed")
    val isLoggedToConsole = chatConfig.getBoolean("log-chat-to-console")

    val commandAlias = chatConfig.getString("command-alias") ?: ""
    val useColorInChatPermission = chatConfig.getString("use-color-in-chat-permission") ?: ""
    val serverBlacklist = chatConfig.getList("blacklist") ?: emptyList()

    val toggleUtils: ToggleUtils by inject()

    override fun execute(sender: CommandSender, args: Array<out String>) {
        if (sender !is ProxiedPlayer) {
            if (this.isConsoleAllowed) {
                var message = this.consoleChatFormat
                message = message
                    .replace("%message%", args.joinToString(" "))
                    .replace("%player%", sender.name)
                    .replace("%command-name%", this.name)
                    .replace("%command-alias%", this.commandAlias)
                    .replace("%command-prefix%", this.commandPrefix)
                    .replace("%chat-name%", this.chatName)
                sendAllMessage(TextComponent(*TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', message))))
            } else {
                sender.sendMessage(TextComponent(*TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', ProxyChatBungee.config.getString("console-disabled-message") ?: ""))))
            }
            return
        }

        handleChat(sender, args)
    }

    fun handleChat(sender: ProxiedPlayer, args: Array<out String>) {
        if (serverBlacklist.contains(sender.server.info.name)) {
            return
        }

        if (args.isEmpty()) {
            sender.sendMessage(handleText(sender, this.invalidArguments, args))
            return
        }

        if (this.isToggleable && args[0].equals("toggle", ignoreCase = true)) {
            if (this.toggleUtils.toggleChat(sender.uniqueId)) {
                sender.sendMessage(handleText(sender, ProxyChatBungee.config.getString("toggle-enable-message") ?: "", args))
            } else {
                sender.sendMessage(handleText(sender, ProxyChatBungee.config.getString("toggle-disable-message") ?: "", args))
            }
            return
        }

        if (this.isIgnorable && args[0].equals("ignore", ignoreCase = true)) {
            if (this.toggleUtils.toggleIgnore(sender.uniqueId)) {
                sender.sendMessage(handleText(sender, ProxyChatBungee.config.getString("ignore-enable-message") ?: "", args))
            } else {
                sender.sendMessage(handleText(sender, ProxyChatBungee.config.getString("ignore-disable-message") ?: "", args))
            }
            return
        }

        if (this.toggleUtils.isDelayed(sender.uniqueId)) {
            sender.sendMessage(handleText(sender, ProxyChatBungee.config.getString("command-cooldown-message") ?: "", args))
            return
        }

        if (this.toggleUtils.isIgnored(sender.uniqueId)) {
            sender.sendMessage(handleText(sender, ProxyChatBungee.config.getString("chat-disabled-message") ?: "", args))
            return
        }

        if (!sender.hasPermission(this.commandDelayOverridePermission)) {
            val cooldown = CooldownRunnable(sender.uniqueId, this.toggleUtils, this.commandDelay.toLong())
            val task = ProxyServer.getInstance().scheduler.schedule(ProxyChatBungee.instance, cooldown, this.commandDelay.toLong(), TimeUnit.MILLISECONDS)
            this.toggleUtils.toggleDelayOn(sender.uniqueId, cooldown)
        }

        val message = handleText(sender, this.chatFormat, args, true)

        if (this.isLocal) {
            sendLocalMessage(sender, message)
        } else {
            sendAllMessage(message)
        }
    }

    private fun sendAllMessage(message: TextComponent) {
        for (player in ProxyServer.getInstance().players) {
            if (player.hasPermission(permission) || permission.isNullOrEmpty()) {
                if (!this.toggleUtils.isIgnored(player.uniqueId)) {
                    player.sendMessage(message)
                }
            }
        }

        if (this.isLoggedToConsole) {
            ProxyChatBungee.instance.logger.info(message.toLegacyText())
        }
    }

    private fun sendLocalMessage(p: ProxiedPlayer, message: TextComponent) {
        for (player in p.server.info.players) {
            if (player.hasPermission(permission) || permission.isNullOrEmpty()) {
                if (!this.toggleUtils.isIgnored(player.uniqueId)) {
                    player.sendMessage(message)
                }
            }
        }

        if (this.isLoggedToConsole) {
            ProxyChatBungee.instance.logger.info(message.toLegacyText())
        }
    }

    private fun handleText(proxiedPlayer: ProxiedPlayer, message: String, args: Array<out String>, ignorePrefix: Boolean = false): TextComponent {
        var finalMessage = message
        if (!ignorePrefix) {
            finalMessage = (ProxyChatBungee.config.getString("prefix") ?: "") + finalMessage
        }

        return getTextComponent(proxiedPlayer, finalMessage, args)
    }

    private fun getTextComponent(proxiedPlayer: ProxiedPlayer, message: String, args: Array<out String>): TextComponent {
        var finalMessage = message
            .replace("%player%", proxiedPlayer.name)
            .replace("%prefix%", ProxyChatBungee.config.getString("prefix") ?: "")
            .replace("%server%", proxiedPlayer.server.info.name)
            .replace("%command-name%", this.name)
            .replace("%command-alias%", this.commandAlias)
            .replace("%command-prefix%", this.commandPrefix)
            .replace("%chat-name%", this.chatName)

        if (this.toggleUtils.isDelayed(proxiedPlayer.uniqueId)) {
            finalMessage = finalMessage.replace("%chat-cooldown%", this.toggleUtils.getDelayedTask(proxiedPlayer.uniqueId)?.getTime() ?: "")
        }

        if (!proxiedPlayer.hasPermission(this.useColorInChatPermission)) {
            finalMessage = finalMessage.replace("%message%", ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', args.joinToString(" "))))
        } else {
            finalMessage = finalMessage.replace("%message%", args.joinToString(" "))
        }

        return TextComponent(*TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', finalMessage)))
    }

    override fun onTabComplete(sender: CommandSender, args: Array<out String>): MutableIterable<String> {
        val tabComplete = mutableSetOf<String>()

        if (sender !is ProxiedPlayer) return tabComplete

        if (serverBlacklist.contains(sender.server.info.name)) {
            return tabComplete
        }

        if (args.size == 1) {
            if (isToggleable) {
                tabComplete.add("toggle")
            }

            if (isIgnorable) {
                tabComplete.add("ignore")
            }
        }

        return tabComplete
    }
}
