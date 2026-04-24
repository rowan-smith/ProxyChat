package dev.rono.proxychat.core.utils

import dev.rono.proxychat.api.ProxyChatConfig
import dev.rono.proxychat.core.ProxyChatInstance
import java.io.File
import java.util.logging.Level

/**
 * Helper utilities for ProxyChat
 */
object Helpers {
    fun migrateConfigChatToFolder(instance: ProxyChatInstance, config: ProxyChatConfig) {
        if (!config.contains("chats")) {
            return
        }

        val chatList = config.getSection("chats") ?: return
        val chatsFolder = File(instance.getDataFolder(), "chats")
        
        if (!chatsFolder.exists()) {
            return
        }

        chatList.getKeys().forEach { key: String ->
            val chatConfig = config.getSection("chats.$key") ?: return@forEach
            val newResourceFileName = chatConfig.getString("command-name") + ".yml"
            val resourceFile = File(chatsFolder, newResourceFileName)

            if (!resourceFile.exists()) {
                try {
                    val fileCreated = resourceFile.createNewFile()
                    if (!fileCreated) {
                        instance.getLogger().log(Level.WARNING, "Failed to generate $newResourceFileName because the file already exists.")
                        return@forEach
                    }
                } catch (e: Exception) {
                    instance.getLogger().log(Level.WARNING, "Failed to generate $newResourceFileName")
                    return@forEach
                }
            }

            try {
                chatConfig.save(resourceFile)
                instance.getLogger().log(Level.INFO, "Migrated $newResourceFileName")
            } catch (e: Exception) {
                instance.getLogger().log(Level.WARNING, "Failed to save $newResourceFileName", e)
            }
        }
    }

    fun loadYmlFile(instance: ProxyChatInstance, ymlFile: File): ProxyChatConfig? {
        return instance.loadConfig(ymlFile)
    }
}
