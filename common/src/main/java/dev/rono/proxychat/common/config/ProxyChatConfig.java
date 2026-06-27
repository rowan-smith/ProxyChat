package dev.rono.proxychat.common.config;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import lombok.Getter;

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
        var chatsDirectory = dataDirectory.resolve("chats");
        var pending = new ArrayList<ChannelConfigValidator.ValidatedChannel>();

        if (!chatsDirectory.toFile().exists()) {
            return List.of();
        }

        var chatFiles = chatsDirectory.toFile().listFiles((dir, name) -> name.endsWith(".yml"));
        if (chatFiles == null) {
            return List.of();
        }

        Arrays.sort(chatFiles, Comparator.comparing(file -> file.getName().toLowerCase()));

        var sortOrder = 0;
        for (var chatFile : chatFiles) {
            try {
                pending.add(new ChannelConfigValidator.ValidatedChannel(
                        ProxyChatYaml.wrap(ProxyChatYamlDocuments.loadChannel(chatFile.toPath())),
                        chatFile.getName(),
                        sortOrder++
                ));

            } catch (Exception exception) {
                logger.log(Level.WARNING, "Failed to load " + chatFile.getName(), exception);
            }
        }

        return ChannelConfigValidator.validateAndOrder(pending, logger)
                .stream()
                .map(ChannelConfigValidator.ValidatedChannel::config)
                .toList();
    }

    public void ensureChatsDirectory() throws Exception {
        var chatsDirectory = dataDirectory.resolve("chats");
        if (!chatsDirectory.toFile().mkdirs() && !chatsDirectory.toFile().exists()) {
            logger.warning("Could not create chats directory.");
        }

        var globalFile = chatsDirectory.resolve("global.yml");
        if (!globalFile.toFile().exists()) {
            ProxyChatYamlDocuments.loadChannel(globalFile);
        }
    }
}
