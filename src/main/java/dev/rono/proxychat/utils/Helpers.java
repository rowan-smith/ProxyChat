package dev.rono.proxychat.utils;

import dev.rono.proxychat.ProxyChat;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class Helpers {
    public static void migrateConfigChatToFolder() {
        if (!ProxyChat.getConfig().contains("chats")) {
            return;
        }

        var chatList = ProxyChat.getConfig().getSection("chats");

        var chatsFolder = new File(ProxyChat.getInstance().getDataFolder(), "chats");
        if (!chatsFolder.exists()) {
            return;
        }

        for (var key : chatList.getKeys()) {
            var chatConfig = ProxyChat.getConfig().getSection("chats." + key);

            var newResourceFileName = chatConfig.getString("command-name") + ".yml";
            var resourceFile = new File(chatsFolder, newResourceFileName);

            if (!resourceFile.exists()) {
                try {
                    boolean fileCreated = resourceFile.createNewFile();
                    if (!fileCreated) {
                        ProxyChat.getInstance().getLogger().log(Level.WARNING, "Failed to generate " + newResourceFileName + " because the file already exists.");
                        return;
                    }
                } catch (IOException e) {
                    ProxyChat.getInstance().getLogger().log(Level.WARNING, "Failed to generate " + newResourceFileName);
                    return;
                }
            }

            try {
                ConfigurationProvider.getProvider(YamlConfiguration.class).save(chatConfig, resourceFile);
                ProxyChat.getInstance().getLogger().log(Level.INFO, "Migrated " + newResourceFileName);
            } catch (IOException e) {
                ProxyChat.getInstance().getLogger().log(Level.WARNING, "Failed to save " + newResourceFileName, e);
            }
        }
    }

    public static Configuration loadYmlFile(File ymlFile) {
        if (!ymlFile.exists()) {
            ProxyChat.getInstance().getLogger().log(Level.WARNING, ymlFile.getName() + " does not exist.");
            return null;
        }

        try {
            return ConfigurationProvider.getProvider(YamlConfiguration.class).load(ymlFile);

        } catch (IOException e) {
            ProxyChat.getInstance().getLogger().log(Level.WARNING, "Failed to load " + ymlFile.getName());
            return null;
        }
    }

}
