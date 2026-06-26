package dev.rono.proxychat.common.platform;

import java.nio.file.Path;
import java.util.List;

import dev.rono.proxychat.common.channel.ChatChannel;
import dev.rono.proxychat.common.channel.ChatChannelService;
import dev.rono.proxychat.common.config.ProxyChatConfig;

public interface ProxyChatBootstrap {
    ProxyChatPlatform getPlatform();

    SignedChatHandler getSignedChatHandler();

    Path getDataDirectory();

    ProxyChatConfig getConfig();

    List<ChatChannel> getChannels();

    ChatChannelService getChannelService();

    void reload();
}
