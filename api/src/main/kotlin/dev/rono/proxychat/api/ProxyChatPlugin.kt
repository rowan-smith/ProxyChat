package dev.rono.proxychat.api

import java.io.File
import java.util.logging.Logger

/**
 * Base interface for ProxyChat plugin implementations
 */
interface ProxyChatPlugin {
    fun onEnable()
    fun onDisable()
    fun getConfig(): ProxyChatConfig
    fun loadConfig(file: File): ProxyChatConfig?
    fun getDataFolder(): File
    fun getLogger(): Logger
}
