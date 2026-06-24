package dev.rono.proxychat.common.command;

import dev.rono.proxychat.common.channel.ChatChannel;
import dev.rono.proxychat.common.config.YamlConfig;
import dev.rono.proxychat.common.message.MessageFormatter;
import dev.rono.proxychat.common.platform.ProxyChatBootstrap;
import dev.rono.proxychat.common.platform.ProxyChatPlatform;
import dev.rono.proxychat.common.platform.ProxyCommandSource;
import dev.rono.proxychat.common.platform.ProxyPlayer;
import dev.rono.proxychat.common.util.SignedChatPolicy;

import java.util.ArrayList;
import java.util.List;

public final class ProxyChatAdminHelp {
    public static void send(ProxyChatBootstrap bootstrap, ProxyCommandSource sender) {
        YamlConfig config = bootstrap.getConfig().getConfig();
        ProxyChatPlatform platform = bootstrap.getPlatform();
        String prefix = config.getString("prefix");

        sendLine(platform, sender, prefix + configLine(config, "help-header", "&7ProxyChat commands:"));

        if (sender.hasPermission(config.getString("reload-permission"))) {
            sendLine(platform, sender, prefix + configLine(config, "help-reload", "&7/proxychat reload &8- Reload configuration and channels"));
        }

        sendLine(platform, sender, prefix + configLine(config, "help-version", "&7/proxychat version &8- Plugin information"));

        List<ChatChannel> visibleChannels = bootstrap.getChannels()
                .stream()
                .filter(channel -> canSeeChannel(channel, sender))
                .toList();

        if (visibleChannels.isEmpty()) {
            return;
        }

        sendLine(platform, sender, prefix + configLine(config, "help-channels-header", "&7Chat channels:"));

        for (ChatChannel channel : visibleChannels) {
            sendLine(platform, sender, prefix + formatChannelLine(bootstrap, config, channel, sender));
        }
    }

    private static boolean canSeeChannel(ChatChannel channel, ProxyCommandSource sender) {
        String permission = channel.getPermission();
        return permission == null || permission.isEmpty() || sender.hasPermission(permission);
    }

    private static String formatChannelLine(ProxyChatBootstrap bootstrap, YamlConfig config, ChatChannel channel, ProxyCommandSource sender) {
        StringBuilder line = new StringBuilder("&7/")
                .append(channel.getCommandName())
                .append(" <message>");

        if (channel.getCommandAlias() != null && !channel.getCommandAlias().isEmpty()) {
            line.append(" &8(/").append(channel.getCommandAlias()).append(")");
        }

        line.append(" &8- ").append(channel.getChatName());

        ProxyPlayer player = sender.asPlayer();
        List<String> subcommands = new ArrayList<>();

        if (player != null && SignedChatPolicy.isToggleAvailable(config, bootstrap.getSignedChatHandler(), channel, player)) {
            subcommands.add("toggle");
        }

        if (channel.isIgnorable()) {
            subcommands.add("ignore");
        }

        if (!subcommands.isEmpty()) {
            line.append(" &8[").append(String.join(", ", subcommands)).append("]");
        }

        if (player != null
                && channel.isUseCommandPrefix()
                && channel.getCommandPrefix() != null
                && !channel.getCommandPrefix().isEmpty()
                && SignedChatPolicy.shouldInterceptChat(config, bootstrap.getSignedChatHandler(), player)) {
            line.append(" &8prefix ").append(channel.getCommandPrefix());
        }

        return line.toString();
    }

    private static String configLine(YamlConfig config, String key, String fallback) {
        String value = config.getString(key);
        return value != null && !value.isEmpty() ? value : fallback;
    }

    private static void sendLine(ProxyChatPlatform platform, ProxyCommandSource sender, String message) {
        platform.sendMessage(sender, MessageFormatter.legacy(message));
    }
}
