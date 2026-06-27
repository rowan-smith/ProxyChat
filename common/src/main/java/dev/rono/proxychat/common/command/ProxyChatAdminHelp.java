package dev.rono.proxychat.common.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import lombok.experimental.UtilityClass;

import dev.rono.proxychat.common.channel.ChatChannel;
import dev.rono.proxychat.common.config.ProxyChatMessages;
import dev.rono.proxychat.common.config.ProxyChatYaml;
import dev.rono.proxychat.common.message.MessageFormatter;
import dev.rono.proxychat.common.platform.ProxyChatBootstrap;
import dev.rono.proxychat.common.platform.ProxyChatPlatform;
import dev.rono.proxychat.common.platform.ProxyCommandSource;
import dev.rono.proxychat.common.util.SignedChatPolicy;

@UtilityClass
public class ProxyChatAdminHelp {

    public static void send(ProxyChatBootstrap bootstrap, ProxyCommandSource sender) {
        var config = bootstrap.getConfig().getConfig();
        var platform = bootstrap.getPlatform();
        var prefix = ProxyChatMessages.resolve(config, "prefix");

        sendLine(platform, sender, prefix + ProxyChatMessages.resolve(config, "help-header"));

        if (sender.hasPermission(config.getString("reload-permission"))) {
            sendLine(platform, sender, prefix + ProxyChatMessages.resolve(config, "help-reload"));
        }

        sendLine(platform, sender, prefix + ProxyChatMessages.resolve(config, "help-version"));

        var visibleChannels = bootstrap.getChannels()
                .stream()
                .filter(channel -> canSeeChannel(channel, sender))
                .toList();

        if (visibleChannels.isEmpty()) {
            return;
        }

        sendLine(platform, sender, prefix + ProxyChatMessages.resolve(config, "help-channels-header"));

        for (ChatChannel channel : visibleChannels) {
            for (String line : formatChannelHelp(bootstrap, config, channel, sender)) {
                sendLine(platform, sender, prefix + line);
            }
        }
    }

    private static boolean canSeeChannel(ChatChannel channel, ProxyCommandSource sender) {
        var permission = channel.getPermission();
        return permission == null || permission.isEmpty() || sender.hasPermission(permission);
    }

    private static List<String> formatChannelHelp(ProxyChatBootstrap bootstrap, ProxyChatYaml config, ChatChannel channel, ProxyCommandSource sender) {
        var lines = new ArrayList<String>();
        lines.add(formatChannelHeading(channel));

        var optionBracket = formatSubcommandBracket(bootstrap, config, channel, sender);
        appendCommandLine(lines, channel.getCommandAlias(), optionBracket);
        appendCommandLine(lines, channel.getCommandName(), optionBracket);

        appendPrefixLines(lines, bootstrap, config, channel, sender);

        return lines;
    }

    private static String formatChannelHeading(ChatChannel channel) {
        var displayName = channel.getChatName();
        if (displayName != null && displayName.endsWith(" Chat")) {
            displayName = displayName.substring(0, displayName.length() - " Chat".length());
        }

        return "&a> &f" + (displayName == null || displayName.isEmpty() ? channel.getCommandName() : displayName);
    }

    private static void appendCommandLine(List<String> lines, String command, String optionBracket) {
        if (command == null || command.isEmpty()) {
            return;
        }

        var usage = "&7- &f/" + command.toLowerCase(Locale.ROOT);
        if (!optionBracket.isEmpty()) {
            usage += " &7[" + optionBracket + "]";
        }

        lines.add(usage + " &8<message>");
    }

    private static String formatSubcommandBracket(ProxyChatBootstrap bootstrap, ProxyChatYaml config, ChatChannel channel, ProxyCommandSource sender) {
        var player = sender.asPlayer();
        var subcommands = new ArrayList<String>();

        if (player != null
                && SignedChatPolicy.isToggleAvailable(config, bootstrap.getSignedChatHandler(), channel, player)) {
            subcommands.add("toggle");
        }

        if (channel.isIgnorable()) {
            subcommands.add("ignore");
        }

        if (subcommands.isEmpty()) {
            return "";
        }

        return String.join(" &8| &7", subcommands);
    }

    private static void appendPrefixLines(
            List<String> lines,
            ProxyChatBootstrap bootstrap,
            ProxyChatYaml config,
            ChatChannel channel,
            ProxyCommandSource sender
    ) {
        var player = sender.asPlayer();
        if (player == null) {
            return;
        }

        if (!channel.isUseCommandPrefix()) {
            return;
        }

        var commandPrefix = channel.getCommandPrefix();
        if (commandPrefix == null || commandPrefix.isEmpty()) {
            return;
        }

        if (SignedChatPolicy.shouldInterceptChat(config, bootstrap.getSignedChatHandler(), player)) {
            lines.add("&7- &f" + commandPrefix + "&8<message>");
        }

        if (SignedChatPolicy.shouldRegisterProxyPrefixCommand(config, bootstrap.getPlatform())) {
            lines.add("&7- &f/" + commandPrefix + " &8<message>");
        }
    }

    private static void sendLine(ProxyChatPlatform platform, ProxyCommandSource sender, String message) {
        platform.sendMessage(sender, MessageFormatter.legacy(message));
    }
}
