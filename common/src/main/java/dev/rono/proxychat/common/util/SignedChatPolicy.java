package dev.rono.proxychat.common.util;

import dev.rono.proxychat.common.channel.ChatChannel;
import dev.rono.proxychat.common.config.YamlConfig;
import dev.rono.proxychat.common.platform.ProxyPlayer;
import dev.rono.proxychat.common.platform.SignedChatHandler;

public final class SignedChatPolicy {

    /**
     * signed-chat-interception config value: auto (default), always, never
     */
    public static boolean shouldInterceptChat(YamlConfig config, SignedChatHandler handler, ProxyPlayer player) {
        String mode = config.getString("signed-chat-interception");
        if (mode == null || mode.isEmpty() || mode.equalsIgnoreCase("auto")) {
            return handler.canInterceptChat(player);
        }

        if (mode.equalsIgnoreCase("never") || mode.equalsIgnoreCase("false") || mode.equalsIgnoreCase("disabled")) {
            return false;
        }

        if (mode.equalsIgnoreCase("always") || mode.equalsIgnoreCase("true") || mode.equalsIgnoreCase("enabled")) {
            return true;
        }

        return handler.canInterceptChat(player);
    }

    public static boolean isToggleAvailable(YamlConfig config, SignedChatHandler handler, ChatChannel channel, ProxyPlayer player) {
        if (!channel.isToggleable()) {
            return false;
        }

        return shouldInterceptChat(config, handler, player);
    }
}
