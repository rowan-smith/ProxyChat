package dev.rono.proxychat.velocity

import dev.rono.proxychat.core.proxyChatModule
import org.koin.core.component.KoinComponent
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import dev.rono.proxychat.api.ProxyChatConfig
import dev.rono.proxychat.core.ProxyChatInstance
import dev.rono.proxychat.core.config.YamlProxyChatConfig
import dev.rono.proxychat.core.utils.Helpers
import dev.rono.proxychat.velocity.commands.ChatCommand
import dev.rono.proxychat.velocity.commands.ProxyChatCommand
import dev.rono.proxychat.velocity.listeners.PlayerChatEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.slf4j.Logger
import java.io.File
import java.nio.file.Path

@Plugin(
    id = "proxychat",
    name = "ProxyChat",
    version = "2.0.0",
    description = "A cross-proxy chat plugin for Velocity",
    authors = ["Rono"]
)
class ProxyChatVelocity @com.google.inject.Inject constructor(
    private val proxyServer: ProxyServer,
    private val logger: Logger,
    @DataDirectory private val dataDirectory: Path
) : ProxyChatInstance, KoinComponent {

    companion object {
        lateinit var instance: ProxyChatVelocity
            private set
    }

    lateinit var pluginConfig: ProxyChatConfig
    val chats = mutableListOf<ProxyChatConfig>()
    val commands = mutableListOf<ChatCommand>()

    override fun getDataFolder(): File = dataDirectory.toFile()

    override fun getLogger(): java.util.logging.Logger {
        return java.util.logging.Logger.getLogger("ProxyChat")
    }

    override fun getConfig(): ProxyChatConfig = pluginConfig

    fun getServer(): ProxyServer = proxyServer

    override fun loadConfig(file: File): ProxyChatConfig? {
        return YamlProxyChatConfig.load(file)
    }

    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        onEnable()
    }

    override fun onEnable() {
        instance = this
        startKoin {
            modules(proxyChatModule)
        }
        registerConfiguration()
        registerChatFolder()
        Helpers.migrateConfigChatToFolder(this, pluginConfig)

        if (pluginConfig.contains("chats")) {
            pluginConfig.set("chats", null)
            pluginConfig.save(File(getDataFolder(), "config.yml"))
        }

        getChats()
        registerListeners()
        registerCommands()
        logger.info("ProxyChat for Velocity has been enabled!")
    }

    fun registerCommands() {
        val commandManager = proxyServer.commandManager
        for (chat in chats) {
            val command = ChatCommand(chat)
            val meta = commandManager.metaBuilder(chat.getString("command-name"))
                .aliases(*(chat.getString("command-alias") ?: "").split(",").toTypedArray())
                .build()
            commandManager.register(meta, command)
            commands.add(command)
        }

        logger.info("${commands.size} commands loaded.")

        val proxyChatMeta = commandManager.metaBuilder("proxychat")
            .aliases("pc")
            .build()
        commandManager.register(proxyChatMeta, ProxyChatCommand(this))
    }

    fun unregisterCommands() {
        val commandManager = proxyServer.commandManager
        for (command in commands) {
            commandManager.unregister(command.chatConfig.getString("command-name"))
        }
        commands.clear()
        commandManager.unregister("proxychat")
    }

    private fun registerListeners() {
        proxyServer.eventManager.register(this, PlayerChatEvent())
    }

    fun registerConfiguration() {
        val resourceFile = File(getDataFolder(), "config.yml")
        Helpers.saveResource(this, "config.yml", resourceFile)
        pluginConfig = loadConfig(resourceFile)!!
    }

    fun registerChatFolder() {
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

    override fun onDisable() {
        stopKoin()
        logger.info("ProxyChat for Velocity has been disabled!")
    }
    fun getConfigTextValue(value: String): Component {
        val prefix = pluginConfig.getString("prefix") ?: ""
        val message = pluginConfig.getString(value) ?: ""
        return LegacyComponentSerializer.legacyAmpersand().deserialize(prefix + message)
    }
}