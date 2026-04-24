package dev.rono.proxychat.bungee

import dev.rono.proxychat.api.ProxyChatConfig
import net.md_5.bungee.config.Configuration
import net.md_5.bungee.config.ConfigurationProvider
import net.md_5.bungee.config.YamlConfiguration
import java.io.File

class BungeeConfig(val config: Configuration) : ProxyChatConfig {
    override fun getSection(path: String): ProxyChatConfig? {
        val section = config.getSection(path)
        return if (section != null) BungeeConfig(section) else null
    }

    override fun getString(path: String): String? = config.getString(path)
    override fun getBoolean(path: String): Boolean = config.getBoolean(path)
    override fun getInt(path: String): Int = config.getInt(path)
    override fun getList(path: String): List<String>? = config.getStringList(path)
    override fun getKeys(): Collection<String> = config.keys
    override fun contains(path: String): Boolean = config.contains(path)
    override fun set(path: String, value: Any?) = config.set(path, value)
    
    override fun save(file: File) {
        ConfigurationProvider.getProvider(YamlConfiguration::class.java).save(config, file)
    }
}
