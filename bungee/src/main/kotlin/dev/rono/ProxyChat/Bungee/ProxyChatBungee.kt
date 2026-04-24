package dev.rono.proxychat.bungee

import dev.rono.proxychat.core.proxyChatModule
import dev.rono.proxychat.api.ProxyChatConfig
import dev.rono.proxychat.core.ProxyChatInstance
import dev.rono.proxychat.core.config.YamlProxyChatConfig
import dev.rono.proxychat.core.utils.Helpers
import dev.rono.proxychat.bungee.commands.ChatCommand
import dev.rono.proxychat.bungee.commands.ProxyChatCommand
import dev.rono.proxychat.bungee.listeners.PlayerChatEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.plugin.Plugin
import java.io.File
import java.util.logging.Level

class ProxyChatBungee : Plugin(), ProxyChatInstance, KoinComponent {

    companion object {
        lateinit var instance: ProxyChatBungee
            private set
        lateinit var config: ProxyChatConfig
            private set
        val commands = mutableListOf<ChatCommand>()
        val chats = mutableListOf<ProxyChatConfig>()
    }

    override fun onEnable() {
        instance = this
        startKoin {
            modules(proxyChatModule, module {
                single { ProxyChatCommand() }
                single { PlayerChatEvent() }
            })
        }
        registerConfiguration()
        registerChatFolder()
        Helpers.migrateConfigChatToFolder(this, config)

        if (config.contains("chats")) {
            config.set("chats", null)
            config.save(File(getDataFolder(), "config.yml"))
        }

        getChats()
        registerListeners()
        registerCommands()
        // MetricsLite can be added here if needed
    }

    override fun getConfig(): ProxyChatConfig = config

    override fun loadConfig(file: File): ProxyChatConfig? {
        if (!file.exists()) return null
        return try {
            YamlProxyChatConfig.load(file)
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Failed to load ${file.name}", e)
            null
        }
    }

    fun registerCommands() {
        for (chat in chats) {
            val command = ChatCommand(chat)
            proxy.pluginManager.registerCommand(this, command)
            commands.add(command)
        }

        logger.info("${commands.size} commands loaded.")
        proxy.pluginManager.registerCommand(this, get<ProxyChatCommand>())
    }

    fun unregisterCommands() {
        for (command in commands) {
            proxy.pluginManager.unregisterCommand(command)
        }
        commands.clear()
    }

    fun registerConfiguration() {
        val resourceFile = File(getDataFolder(), "config.yml")
        Helpers.saveResource(this, "config.yml", resourceFile)
        config = loadConfig(resourceFile)!!
    }

    private fun registerChatFolder() {
        val chatsFolder = File(getDataFolder(), "chats")
        if (!chatsFolder.exists()) {
            chatsFolder.mkdirs()
        }
        val resourceFile = File(chatsFolder, "global.yml")
        Helpers.saveResource(this, "global.yml", resourceFile)
    }

    fun getChats() {
        chats.clear()
        val chatsFolder = File(getDataFolder(), "chats")
        val chatArray = chatsFolder.listFiles { _, name -> name.endsWith(".yml") }

        chatArray?.forEach { chatFile ->
            val chat = Helpers.loadYmlFile(this, chatFile)
            if (chat != null) {
                chats.add(chat)
            }
        }
    }

    private fun registerListeners() {
        proxy.pluginManager.registerListener(this, get<PlayerChatEvent>())
    }

    fun getConfigTextValue(value: String): TextComponent {
        val prefix = config.getString("prefix") ?: ""
        val message = config.getString(value) ?: ""
        return TextComponent(*TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', prefix + message)))
    }

    override fun onDisable() {
        stopKoin()
    }
}
