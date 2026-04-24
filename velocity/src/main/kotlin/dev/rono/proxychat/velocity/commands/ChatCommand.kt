package dev.rono.proxychat.velocity.commands

import com.velocitypowered.api.command.SimpleCommand
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.crypto.SignedMessage
import dev.rono.proxychat.api.ProxyChatConfig
import dev.rono.proxychat.core.utils.CooldownRunnable
import dev.rono.proxychat.core.utils.ToggleUtils
import dev.rono.proxychat.velocity.ProxyChatVelocity
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

class ChatCommand(val chatConfig: ProxyChatConfig) : SimpleCommand, KoinComponent {
    val permission = chatConfig.getString("permission") ?: ""
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

    override fun execute(invocation: SimpleCommand.Invocation) {
        val sender = invocation.source()
        val args = invocation.arguments()

        if (sender !is Player) {
            if (this.isConsoleAllowed) {
                var message = this.consoleChatFormat
                message = message
                    .replace("%message%", args.joinToString(" "))
                    .replace("%player%", "Console")
                    .replace("%command-name%", chatConfig.getString("command-name") ?: "")
                    .replace("%command-alias%", this.commandAlias)
                    .replace("%command-prefix%", this.commandPrefix)
                    .replace("%chat-name%", this.chatName)

                val component = LegacyComponentSerializer.legacyAmpersand().deserialize(message)
                broadcast(null, component)
            } else {
                sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(ProxyChatVelocity.instance.pluginConfig.getString("console-disabled-message") ?: ""))
            }
            return
        }

        if (this.permission.isNotEmpty() && !sender.hasPermission(this.permission)) {
            return
        }

        handleChat(sender, args)
    }

    fun handleChat(player: Player, args: Array<out String>) {
        if (serverBlacklist.contains(player.currentServer.map { it.server.serverInfo.name }.orElse(""))) {
            return
        }

        if (args.isEmpty()) {
            player.sendMessage(handleText(player, this.invalidArguments, args))
            return
        }

        if (this.isToggleable && args[0].equals("toggle", ignoreCase = true)) {
            if (this.toggleUtils.toggleChat(player.uniqueId)) {
                player.sendMessage(handleText(player, ProxyChatVelocity.instance.pluginConfig.getString("toggle-enable-message") ?: "", args))
            } else {
                player.sendMessage(handleText(player, ProxyChatVelocity.instance.pluginConfig.getString("toggle-disable-message") ?: "", args))
            }
            return
        }

        if (this.isIgnorable && args[0].equals("ignore", ignoreCase = true)) {
            if (this.toggleUtils.toggleIgnore(player.uniqueId)) {
                player.sendMessage(handleText(player, ProxyChatVelocity.instance.pluginConfig.getString("ignore-enable-message") ?: "", args))
            } else {
                player.sendMessage(handleText(player, ProxyChatVelocity.instance.pluginConfig.getString("ignore-disable-message") ?: "", args))
            }
            return
        }

        if (this.toggleUtils.isDelayed(player.uniqueId)) {
            player.sendMessage(handleText(player, ProxyChatVelocity.instance.pluginConfig.getString("command-cooldown-message") ?: "", args))
            return
        }

        if (this.toggleUtils.isIgnored(player.uniqueId)) {
            player.sendMessage(handleText(player, ProxyChatVelocity.instance.pluginConfig.getString("chat-disabled-message") ?: "", args))
            return
        }

        if (!player.hasPermission(this.commandDelayOverridePermission)) {
            val cooldown = CooldownRunnable(player.uniqueId, this.toggleUtils, this.commandDelay.toLong())
            ProxyChatVelocity.instance.getServer().scheduler
                .buildTask(ProxyChatVelocity.instance, cooldown)
                .delay(this.commandDelay.toLong(), TimeUnit.MILLISECONDS)
                .schedule()
            this.toggleUtils.toggleDelayOn(player.uniqueId, cooldown)
        }

        val message = handleText(player, this.chatFormat, args, true)

        if (this.isLocal) {
            broadcast(player, message)
        } else {
            broadcast(null, message)
        }
    }

    private fun broadcast(sender: Player?, message: Component) {
        val recipients: Collection<Player> = if (this.isLocal && sender != null) {
            sender.currentServer.map { it.server.playersConnected }.orElse(emptyList())
        } else {
            ProxyChatVelocity.instance.getServer().allPlayers
        }

        for (recipient in recipients) {
            if (recipient.hasPermission(this.permission) || this.permission.isEmpty()) {
                if (!this.toggleUtils.isIgnored(recipient.uniqueId)) {
                    recipient.sendMessage(message)
                }
            }
        }

        if (this.isLoggedToConsole) {
            ProxyChatVelocity.instance.getLogger().info(LegacyComponentSerializer.legacyAmpersand().serialize(message))
        }
    }

    private fun handleText(player: Player, message: String, args: Array<out String>, ignorePrefix: Boolean = false): Component {
        var finalMessage = message
        if (!ignorePrefix) {
            finalMessage = (ProxyChatVelocity.instance.pluginConfig.getString("prefix") ?: "") + finalMessage
        }

        finalMessage = finalMessage
            .replace("%player%", player.username)
            .replace("%prefix%", ProxyChatVelocity.instance.pluginConfig.getString("prefix") ?: "")
            .replace("%server%", player.currentServer.map { it.server.serverInfo.name }.orElse(""))
            .replace("%command-name%", chatConfig.getString("command-name") ?: "")
            .replace("%command-alias%", this.commandAlias)
            .replace("%command-prefix%", this.commandPrefix)
            .replace("%chat-name%", this.chatName)

        if (this.toggleUtils.isDelayed(player.uniqueId)) {
            finalMessage = finalMessage.replace("%chat-cooldown%", this.toggleUtils.getDelayedTask(player.uniqueId)?.getTime() ?: "")
        }

        val chatMessage = args.joinToString(" ")
        val processedChatMessage = if (player.hasPermission(useColorInChatPermission)) {
            chatMessage
        } else {
            chatMessage.replace(Regex("(?i)&[0-9a-fk-or]"), "")
        }

        finalMessage = finalMessage.replace("%message%", processedChatMessage)

        return LegacyComponentSerializer.legacyAmpersand().deserialize(finalMessage)
    }
}
