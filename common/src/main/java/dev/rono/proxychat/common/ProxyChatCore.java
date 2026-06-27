package dev.rono.proxychat.common;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import lombok.Getter;

import dev.rono.proxychat.common.channel.ChatChannel;
import dev.rono.proxychat.common.channel.ChatChannelService;
import dev.rono.proxychat.common.config.PlayerPreferencesStore;
import dev.rono.proxychat.common.config.ProxyChatConfig;
import dev.rono.proxychat.common.config.ProxyChatYaml;
import dev.rono.proxychat.common.platform.ProxyChatBootstrap;
import dev.rono.proxychat.common.platform.ProxyChatPlatform;
import dev.rono.proxychat.common.platform.ProxyPlayer;
import dev.rono.proxychat.common.platform.SignedChatHandler;

public final class ProxyChatCore implements ProxyChatBootstrap {
    private final Logger logger;
    private final ProxyChatPlatform platform;
    private final SignedChatHandler signedChatHandler;
    private final Path dataDirectory;
    private final ProxyChatConfig configManager;
    private final PlayerPreferencesStore playerPreferences;
    @Getter
    private final ChatChannelService channelService = new ChatChannelService(this);
    private final List<ChatChannel> channels = new ArrayList<>();
    @Getter
    private volatile boolean enabled;

    public ProxyChatCore(
            Logger logger,
            ProxyChatPlatform platform,
            SignedChatHandler signedChatHandler,
            Path dataDirectory
    ) {
        this.logger = logger;
        this.platform = platform;
        this.signedChatHandler = signedChatHandler;
        this.dataDirectory = dataDirectory;
        this.configManager = new ProxyChatConfig(logger, dataDirectory);
        this.playerPreferences = new PlayerPreferencesStore(logger, dataDirectory);
    }

    public boolean enable(InputStream defaultConfig, InputStream defaultChannel) {
        try {
            if (!Files.exists(dataDirectory)) {
                Files.createDirectories(dataDirectory);
            }

            configManager.ensureChatsDirectory();
            configManager.initialize(defaultConfig, defaultChannel);
            loadPlayerPreferences();
            reloadChannels();

            enabled = true;
            return true;

        } catch (Exception exception) {
            enabled = false;
            logger.log(Level.SEVERE, "Failed to enable ProxyChat", exception);
            return false;
        }
    }

    public boolean reload() {
        try {
            configManager.reload();
            loadPlayerPreferences();
            reloadChannels();
            return true;

        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Failed to reload ProxyChat", exception);
            return false;
        }
    }

    private void loadPlayerPreferences() {
        if (isPlayerPreferencesPersisted()) {
            playerPreferences.load();
        }
    }

    private void reloadChannels() {
        channels.clear();

        for (ProxyChatYaml channelConfig : configManager.loadChannels()) {
            channels.add(new ChatChannel(channelConfig));
        }

        logger.info(channels.size() + " chat channels loaded.");
    }

    public void onPlayerJoin(ProxyPlayer player) {
        if (isPlayerPreferencesPersisted()) {
            playerPreferences.applyToChannels(player.getUniqueId(), channels);
        }
    }

    public void onPlayerQuit(ProxyPlayer player) {
        playerPreferences.clearRuntimeState(player.getUniqueId(), channels);
    }

    @Override
    public void onToggleChanged(ProxyPlayer player, ChatChannel channel, boolean toggled) {
        if (isPlayerPreferencesPersisted()) {
            playerPreferences.recordToggle(player.getUniqueId(), channel.getCommandName(), toggled);
        }
    }

    @Override
    public void onIgnoreChanged(ProxyPlayer player, ChatChannel channel, boolean ignored) {
        if (isPlayerPreferencesPersisted()) {
            playerPreferences.recordIgnore(player.getUniqueId(), channel.getCommandName(), ignored);
        }
    }

    @Override
    public boolean isPlayerPreferencesPersisted() {
        var config = configManager.getConfig();
        return config != null && config.getBoolean("persist-player-preferences");
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
    public ProxyChatConfig getConfig() {
        return configManager;
    }

    @Override
    public List<ChatChannel> getChannels() {
        return List.copyOf(channels);
    }
}
