package dev.rono.proxychat.bungee

import com.google.common.io.ByteStreams
import dev.rono.proxychat.api.ProxyChatConfig
import dev.rono.proxychat.core.ProxyChatInstance
import dev.rono.proxychat.core.utils.Helpers
import dev.rono.proxychat.bungee.commands.ChatCommand
import dev.rono.proxychat.bungee.commands.ProxyChatCommand
import dev.rono.proxychat.bungee.listeners.PlayerChatEvent
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.plugin.Plugin
import net.md_5.bungee.config.ConfigurationProvider
import net.md_5.bungee.config.YamlConfiguration
import java.io.File
import java.nio.file.Files
import java.util.logging.Level

class ProxyChatBungee : Plugin(), ProxyChatInstance {

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
        registerConfiguration()
        registerChatFolder()
        Helpers.migrateConfigChatToFolder(this, config)

        if (config.contains("chats")) {
            config.set("chats", null)
            config.save(File(dataFolder, "config.yml"))
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
            val bungeeConfig = ConfigurationProvider.getProvider(YamlConfiguration::class.java).load(file)
            BungeeConfig(bungeeConfig)
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
        proxy.pluginManager.registerCommand(this, ProxyChatCommand())
    }

    fun unregisterCommands() {
        for (command in commands) {
            proxy.pluginManager.unregisterCommand(command)
        }
        commands.clear()
    }

    fun registerConfiguration() {
        if (!dataFolder.exists()) {
            dataFolder.mkdir()
            logger.info("Created ProxyChat folder.")
        }

        val resourceFile = File(dataFolder, "config.yml")

        try {
            if (resourceFile.createNewFile()) {
                logger.info("Creating new config.")
                getResourceAsStream("config.yml").use { input ->
                    Files.newOutputStream(resourceFile.toPath()).use { output ->
                        ByteStreams.copy(input, output)
                    }
                }
            }
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Config file could not be created.", e)
        }

        config = loadConfig(resourceFile)!!
    }

    fun registerChatFolder() {
        val chatsFolder = File(dataFolder, "chats")
        if (chatsFolder.mkdir()) {
            logger.info("Created chats folder.")

            val resourceFile = File(chatsFolder, "global.yml")
            try {
                if (resourceFile.createNewFile()) {
                    logger.info("Creating example chat.")
                    getResourceAsStream("global.yml").use { input ->
                        Files.newOutputStream(resourceFile.toPath()).use { output ->
                            ByteStreams.copy(input, output)
                        }
                    }
                }
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "Example chat could not be created.", e)
            }
        }
    }

    fun getChats() {
        chats.clear()
        val chatsFolder = File(dataFolder, "chats")
        val chatArray = chatsFolder.listFiles { _, name -> name.endsWith(".yml") }

        chatArray?.forEach { chatFile ->
            val chat = Helpers.loadYmlFile(this, chatFile)
            if (chat != null) {
                chats.add(chat)
            }
        }
    }

    private fun registerListeners() {
        proxy.pluginManager.registerListener(this, PlayerChatEvent())
    }

    fun getConfigTextValue(value: String): TextComponent {
        val prefix = config.getString("prefix") ?: ""
        val message = config.getString(value) ?: ""
        return TextComponent(*TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', prefix + message)))
    }
}
