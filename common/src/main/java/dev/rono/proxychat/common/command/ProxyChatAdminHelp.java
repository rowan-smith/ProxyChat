package dev.rono.proxychat.common.command;

import dev.rono.proxychat.common.channel.ChatChannel;
import dev.rono.proxychat.common.config.ProxyChatMessages;
import dev.rono.proxychat.common.config.ProxyChatYaml;
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
        ProxyChatYaml config = bootstrap.getConfig().getConfig();
        ProxyChatPlatform platform = bootstrap.getPlatform();
        String prefix = ProxyChatMessages.resolve(config, "prefix");

        sendLine(platform, sender, prefix + ProxyChatMessages.resolve(config, "help-header"));

        if (sender.hasPermission(config.getString("reload-permission"))) {
            sendLine(platform, sender, prefix + ProxyChatMessages.resolve(config, "help-reload"));
        }

        sendLine(platform, sender, prefix + ProxyChatMessages.resolve(config, "help-version"));

        List<ChatChannel> visibleChannels = bootstrap.getChannels()
                .stream()
                .filter(channel -> canSeeChannel(channel, sender))
                .toList();

        if (visibleChannels.isEmpty()) {
            return;
        }

        sendLine(platform, sender, prefix + ProxyChatMessages.resolve(config, "help-channels-header"));

        for (ChatChannel channel : visibleChannels) {
            sendLine(platform, sender, prefix + formatChannelLine(bootstrap, config, channel, sender));
        }
    }

    private static boolean canSeeChannel(ChatChannel channel, ProxyCommandSource sender) {
        String permission = channel.getPermission();
        return permission == null || permission.isEmpty() || sender.hasPermission(permission);
    }

    private static String formatChannelLine(ProxyChatBootstrap bootstrap, ProxyChatYaml config, ChatChannel channel, ProxyCommandSource sender) {
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

    private static void sendLine(ProxyChatPlatform platform, ProxyCommandSource sender, String message) {
        platform.sendMessage(sender, MessageFormatter.legacy(message));
    }
}
