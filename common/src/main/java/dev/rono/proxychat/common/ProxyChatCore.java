package dev.rono.proxychat.common;

import dev.rono.proxychat.common.channel.ChatChannel;
import dev.rono.proxychat.common.channel.ChatChannelService;
import dev.rono.proxychat.common.config.ProxyChatConfig;
import dev.rono.proxychat.common.config.ProxyChatYaml;
import dev.rono.proxychat.common.platform.ProxyChatBootstrap;
import dev.rono.proxychat.common.platform.ProxyChatPlatform;
import dev.rono.proxychat.common.platform.SignedChatHandler;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ProxyChatCore implements ProxyChatBootstrap {
    private final Logger logger;
    private final ProxyChatPlatform platform;
    private final SignedChatHandler signedChatHandler;
    private final Path dataDirectory;
    private final ProxyChatConfig configManager;
    private final ChatChannelService channelService = new ChatChannelService(this);
    private final List<ChatChannel> channels = new ArrayList<>();

    public ProxyChatCore(Logger logger, ProxyChatPlatform platform, SignedChatHandler signedChatHandler, Path dataDirectory) {
        this.logger = logger;
        this.platform = platform;
        this.signedChatHandler = signedChatHandler;
        this.dataDirectory = dataDirectory;
        this.configManager = new ProxyChatConfig(logger, dataDirectory);
    }

    public void enable(InputStream defaultConfig, InputStream defaultChannel) {
        try {
            if (!Files.exists(dataDirectory)) {
                Files.createDirectories(dataDirectory);
            }

            configManager.ensureChatsDirectory();
            configManager.initialize(defaultConfig, defaultChannel);

            reloadChannels();

        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Failed to enable ProxyChat", exception);
        }
    }

    @Override
    public void reload() {
        try {
            configManager.reload();
            reloadChannels();

        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Failed to reload ProxyChat", exception);
        }
    }

    private void reloadChannels() {
        channels.clear();

        for (ProxyChatYaml channelConfig : configManager.loadChannels()) {
            channels.add(new ChatChannel(channelConfig));
        }

        logger.info(channels.size() + " chat channels loaded.");
    }

    @Override
    public ProxyChatPlatform getPlatform() {
        return platform;
    }

    @Override
    public SignedChatHandler getSignedChatHandler() {
        return signedChatHandler;
    }

    @Override
    public Path getDataDirectory() {
        return dataDirectory;
    }

    @Override
    public ProxyChatConfig getConfig() {
        return configManager;
    }

    @Override
    public List<ChatChannel> getChannels() {
        return channels;
    }

    @Override
    public ChatChannelService getChannelService() {
        return channelService;
    }
}
