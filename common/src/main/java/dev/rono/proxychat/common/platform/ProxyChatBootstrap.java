package dev.rono.proxychat.common.platform;

import java.util.List;

import dev.rono.proxychat.common.channel.ChatChannel;
import dev.rono.proxychat.common.config.ProxyChatConfig;

public interface ProxyChatBootstrap {
    ProxyChatPlatform getPlatform();

    SignedChatHandler getSignedChatHandler();

    ProxyChatConfig getConfig();

    List<ChatChannel> getChannels();
}
