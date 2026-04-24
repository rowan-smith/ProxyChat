package dev.rono.proxychat.velocity

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.PlayerChatEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import dev.rono.proxychat.api.ProxyChatConfig
import dev.rono.proxychat.core.ProxyChatInstance
import org.slf4j.Logger
import java.io.File

@Plugin(
    id = "proxychat",
    name = "ProxyChat",
    version = "2.0.0",
    description = "A cross-proxy chat plugin for Velocity",
    authors = ["Rono"]
)
class ProxyChatVelocity @Inject constructor(
    private val proxyServer: ProxyServer,
    private val logger: Logger
) : ProxyChatInstance {

    private var dataFolder: File? = null

    override fun getDataFolder(): File {
        if (dataFolder == null) {
            dataFolder = File("plugins/ProxyChat")
            if (!dataFolder!!.exists()) {
                dataFolder!!.mkdirs()
            }
        }
        return dataFolder!!
    }

    override fun getLogger(): java.util.logging.Logger {
        return java.util.logging.Logger.getLogger("ProxyChat")
    }

    override fun getConfig(): ProxyChatConfig {
        // Placeholder implementation for now
        throw UnsupportedOperationException("Not implemented yet")
    }

    override fun loadConfig(file: File): ProxyChatConfig? {
        // Placeholder implementation for now
        return null
    }

    override fun onEnable() {
        logger.info("ProxyChat for Velocity has been enabled!")
    }

    override fun onDisable() {
        logger.info("ProxyChat for Velocity has been disabled!")
    }

    @Subscribe
    fun onPlayerChat(event: PlayerChatEvent) {
        // Chat event handler can be implemented here
    }
}
