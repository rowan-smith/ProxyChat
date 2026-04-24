package dev.rono.proxychat.core.utils

import dev.rono.proxychat.api.ProxyChatConfig
import dev.rono.proxychat.core.ProxyChatInstance
import java.io.File
import java.nio.file.Files
import java.util.logging.Level

/**
 * Helper utilities for ProxyChat
 */
object Helpers {
    fun saveResource(instance: ProxyChatInstance, resourcePath: String, outputFile: File) {
        if (outputFile.exists()) return

        try {
            val inputStream = Helpers::class.java.classLoader.getResourceAsStream(resourcePath)
            if (inputStream == null) {
                instance.getLogger().warning("Resource $resourcePath not found in classpath.")
                return
            }

            if (!outputFile.parentFile.exists()) {
                outputFile.parentFile.mkdirs()
            }

            inputStream.use { input ->
                Files.copy(input, outputFile.toPath())
            }
            instance.getLogger().info("Created $resourcePath.")
        } catch (e: Exception) {
            instance.getLogger().log(Level.SEVERE, "Could not save $resourcePath to $outputFile", e)
        }
    }

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
