package dev.rono.proxychat.common.config;

import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ProxyChatConfig {
    private final Logger logger;
    private final Path dataDirectory;
    @Getter private ProxyChatYaml config;

    public ProxyChatConfig(Logger logger, Path dataDirectory) {
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    public void initialize(InputStream configDefaults, InputStream channelDefaults) throws Exception {
        ProxyChatYamlDocuments.configureDefaults(configDefaults, channelDefaults);
        reload();
    }

    public void loadDefaults(InputStream configDefaults, InputStream channelDefaults) throws Exception {
        initialize(configDefaults, channelDefaults);
    }

    public void reload() throws Exception {
        config = ProxyChatYaml.wrap(ProxyChatYamlDocuments.loadMainConfig(dataDirectory, logger));
    }

    public List<ProxyChatYaml> loadChannels() {
        Path chatsDirectory = dataDirectory.resolve("chats");
        List<ProxyChatYaml> channels = new ArrayList<>();

        if (!chatsDirectory.toFile().exists()) {
            return channels;
        }

        var chatFiles = chatsDirectory.toFile().listFiles((dir, name) -> name.endsWith(".yml"));
        if (chatFiles == null) {
            return channels;
        }

        for (var chatFile : chatFiles) {
            try {
                channels.add(ProxyChatYaml.wrap(ProxyChatYamlDocuments.loadChannel(chatFile.toPath())));

            } catch (Exception exception) {
                logger.log(Level.WARNING, "Failed to load " + chatFile.getName(), exception);
            }
        }

        return channels;
    }

    public void ensureChatsDirectory() throws Exception {
        Path chatsDirectory = dataDirectory.resolve("chats");
        if (!chatsDirectory.toFile().mkdirs() && !chatsDirectory.toFile().exists()) {
            logger.warning("Could not create chats directory.");
        }

        Path globalFile = chatsDirectory.resolve("global.yml");
        if (!globalFile.toFile().exists()) {
            ProxyChatYamlDocuments.loadChannel(globalFile);
        }
    }
}
