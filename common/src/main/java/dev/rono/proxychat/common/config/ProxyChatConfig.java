package dev.rono.proxychat.common.config;

import lombok.Getter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ProxyChatConfig {
    private final Logger logger;
    private final Path dataDirectory;
    @Getter private YamlConfig config;

    public ProxyChatConfig(Logger logger, Path dataDirectory) {
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    public void loadDefaults(Path ignoredConfigResource, Path ignoredChannelResource) throws Exception {
        reload();
        migrateLegacyChats();
    }

    public void reload() throws Exception {
        config = YamlConfig.load(dataDirectory.resolve("config.yml"));
    }

    public List<YamlConfig> loadChannels() {
        Path chatsDirectory = dataDirectory.resolve("chats");
        List<YamlConfig> channels = new ArrayList<>();

        if (!chatsDirectory.toFile().exists()) {
            return channels;
        }

        var chatFiles = chatsDirectory.toFile().listFiles((dir, name) -> name.endsWith(".yml"));
        if (chatFiles == null) {
            return channels;
        }

        for (var chatFile : chatFiles) {
            try {
                channels.add(YamlConfig.load(chatFile.toPath()));

            } catch (Exception exception) {
                logger.log(Level.WARNING, "Failed to load " + chatFile.getName(), exception);
            }
        }

        return channels;
    }

    public void ensureChatsDirectory(Path ignored) throws Exception {
        Path chatsDirectory = dataDirectory.resolve("chats");
        if (!chatsDirectory.toFile().mkdirs() && !chatsDirectory.toFile().exists()) {
            logger.warning("Could not create chats directory.");
        }

        Path globalFile = chatsDirectory.resolve("global.yml");
        if (!globalFile.toFile().exists()) {
            // default channel is copied by ProxyChatCore
        }
    }

    private void migrateLegacyChats() {
        if (!config.contains("chats")) {
            return;
        }

        YamlConfig chatList = config.getSection("chats");
        if (chatList == null) {
            return;
        }

        Path chatsDirectory = dataDirectory.resolve("chats");
        if (!chatsDirectory.toFile().exists()) {
            return;
        }

        for (String key : chatList.getKeys()) {
            YamlConfig chatConfig = config.getSection("chats." + key);
            if (chatConfig == null) {
                continue;
            }

            String fileName = chatConfig.getString("command-name") + ".yml";
            Path target = chatsDirectory.resolve(fileName);
            if (target.toFile().exists()) {
                continue;
            }

            try {
                chatConfig.save(target);
                logger.info("Migrated " + fileName);

            } catch (Exception exception) {
                logger.log(Level.WARNING, "Failed to migrate " + fileName, exception);
            }
        }

        config.set("chats", null);
        try {
            config.save(dataDirectory.resolve("config.yml"));
        } catch (Exception exception) {
            logger.log(Level.WARNING, "Failed to remove legacy chats section from config.yml", exception);
        }
    }
}
