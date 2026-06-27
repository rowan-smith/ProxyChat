package dev.rono.proxychat.common.util;

import lombok.experimental.UtilityClass;

import dev.rono.proxychat.common.channel.ChatChannel;
import dev.rono.proxychat.common.config.ProxyChatYaml;
import dev.rono.proxychat.common.platform.ProxyChatPlatform;
import dev.rono.proxychat.common.platform.ProxyPlayer;
import dev.rono.proxychat.common.platform.SignedChatHandler;

@UtilityClass
public class SignedChatPolicy {

    /**
     * signed-chat-interception config value: auto (default), always, never
     */
    public static boolean shouldInterceptChat(ProxyChatYaml config, SignedChatHandler handler, ProxyPlayer player) {
        var mode = config.getString("signed-chat-interception");
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

    public static boolean isToggleAvailable(
            ProxyChatYaml config,
            SignedChatHandler handler,
            ChatChannel channel,
            ProxyPlayer player
    ) {
        if (!channel.isToggleable()) {
            return false;
        }

        return shouldInterceptChat(config, handler, player);
    }

    /**
     * Whether the proxy should register {@code /{prefix}} commands (e.g. {@code /@hello}).
     * When plain {@code @prefix} chat interception works (Velocity + SignedVelocity), the command is omitted.
     */
    public static boolean shouldRegisterProxyPrefixCommand(ProxyChatYaml config, ProxyChatPlatform platform) {
        if (isInterceptionDisabled(config)) {
            return true;
        }

        return !platform.isPluginPresent("signedvelocity");
    }

    private static boolean isInterceptionDisabled(ProxyChatYaml config) {
        var mode = config.getString("signed-chat-interception");
        return mode != null && (mode.equalsIgnoreCase("never")
                || mode.equalsIgnoreCase("false")
                || mode.equalsIgnoreCase("disabled"));
    }
}
