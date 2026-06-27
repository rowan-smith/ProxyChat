package dev.rono.proxychat.common.channel;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import dev.rono.proxychat.common.config.ProxyChatMessages;
import dev.rono.proxychat.common.config.ProxyChatYaml;
import dev.rono.proxychat.common.message.MessageFormatter;
import dev.rono.proxychat.common.platform.ProxyChatBootstrap;
import dev.rono.proxychat.common.platform.ProxyCommandSource;
import dev.rono.proxychat.common.platform.ProxyPlayer;
import dev.rono.proxychat.common.util.SignedChatPolicy;

public final class ChatChannelService {
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private final ProxyChatBootstrap bootstrap;

    public ChatChannelService(ProxyChatBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    public void execute(ChatChannel channel, ProxyCommandSource sender, String[] args) {
        var platform = bootstrap.getPlatform();
        var config = bootstrap.getConfig().getConfig();

        if (!sender.isPlayer()) {
            if (channel.isConsoleChatAllowed()) {
                var message = applyPlaceholders(channel.getConsoleFormat(), sender, channel, args, config);
                broadcast(channel, formatConfiguredMessage(config, message), null);

            } else {
                platform.sendMessage(
                        sender,
                        formattedConfigMessage(config, "console-disabled-message", sender, channel, args)
                );
            }

            return;
        }

        var player = sender.asPlayer();
        if (player == null) {
            return;
        }

        if (isBlacklisted(channel, player)) {
            sendBlacklistMessage(player, channel, config, args);
            return;
        }

        if (args.length < 1) {
            player.sendMessage(formatMessage(config, channel.getInvalidArgs(), player, channel, args, false));
            return;
        }

        if (channel.isToggleable() && args[0].equalsIgnoreCase("toggle")) {
            if (!isToggleAvailable(channel, player)) {
                player.sendMessage(formattedConfigMessage(config, "toggle-unsupported-message", player, channel, args));
                return;
            }

            if (channel.getToggleUtils().toggleChat(player.getUniqueId())) {
                bootstrap.onToggleChanged(player, channel, true);
                player.sendMessage(formattedConfigMessage(config, "toggle-enable-message", player, channel, args));

            } else {
                bootstrap.onToggleChanged(player, channel, false);
                player.sendMessage(formattedConfigMessage(config, "toggle-disable-message", player, channel, args));
            }

            return;
        }

        if (channel.isIgnorable() && args[0].equalsIgnoreCase("ignore")) {
            if (channel.getToggleUtils().toggleIgnore(player.getUniqueId())) {
                bootstrap.onIgnoreChanged(player, channel, true);
                player.sendMessage(formattedConfigMessage(config, "ignore-enable-message", player, channel, args));

            } else {
                bootstrap.onIgnoreChanged(player, channel, false);
                player.sendMessage(formattedConfigMessage(config, "ignore-disable-message", player, channel, args));
            }

            return;
        }

        if (channel.getToggleUtils().isDelayed(player.getUniqueId())) {
            player.sendMessage(formattedConfigMessage(config, "command-cooldown-message", player, channel, args));
            return;
        }

        var outgoing = preparePlayerChat(channel, player, args, config);
        if (outgoing.isEmpty()) {
            return;
        }

        if (channel.isLocal()) {
            broadcast(channel, outgoing.get(), player);

        } else {
            broadcast(channel, outgoing.get(), null);
        }
    }

    public Set<String> tabComplete(ChatChannel channel, ProxyPlayer player, String[] args) {
        var suggestions = new HashSet<String>();
        if (isBlacklisted(channel, player)) {
            return suggestions;
        }

        if (args.length == 1) {
            if (isToggleAvailable(channel, player)) {
                suggestions.add("toggle");
            }

            if (channel.isIgnorable()) {
                suggestions.add("ignore");
            }
        }

        return suggestions;
    }

    public boolean tryInterceptChat(ProxyPlayer player, String message) {
        if (tryInterceptPrefixedInput(player, message)) {
            return true;
        }

        return tryInterceptToggleOnly(player, message);
    }

    /**
     * Velocity signed-chat path: proxy-broadcast {@code @prefix} chat and deny the original signed
     * message so Paper does not wrap it as {@code <player> ...}.
     */
    public Optional<VelocityPrefixInterceptResult> tryVelocityPrefixedIntercept(ProxyPlayer player, String input) {
        var match = matchPrefixedChannel(player, normalizeIncoming(input));
        if (match.isEmpty()) {
            return Optional.empty();
        }

        var prefixMatch = match.get();
        var config = bootstrap.getConfig().getConfig();
        if (prefixMatch.args().length < 1) {
            player.sendMessage(formatMessage(
                    config,
                    prefixMatch.channel().getInvalidArgs(),
                    player,
                    prefixMatch.channel(),
                    prefixMatch.args(),
                    false
            ));
            return Optional.of(new VelocityPrefixInterceptResult.Blocked());
        }

        var outgoing = preparePlayerChat(prefixMatch.channel(), player, prefixMatch.args(), config);
        if (outgoing.isEmpty()) {
            return Optional.of(new VelocityPrefixInterceptResult.Blocked());
        }

        if (prefixMatch.channel().isLocal()) {
            broadcast(prefixMatch.channel(), outgoing.get(), player);
        } else {
            broadcast(prefixMatch.channel(), outgoing.get(), null);
        }

        return Optional.of(new VelocityPrefixInterceptResult.Delivered());
    }

    public boolean tryInterceptToggleOnly(ProxyPlayer player, String message) {
        var normalized = normalizeIncoming(message);
        for (ChatChannel channel : bootstrap.getChannels()) {
            if (channel.getToggleUtils().isToggled(player.getUniqueId()) && hasChannelPermission(player, channel)) {
                execute(channel, player, normalized.isEmpty() ? new String[0] : normalized.split(" "));

                return true;
            }
        }

        return false;
    }

    /**
     * Intercepts chat or proxy command input that begins with a configured channel prefix.
     */
    public boolean tryInterceptPrefixedInput(ProxyPlayer player, String input) {
        var match = matchPrefixedChannel(player, normalizeIncoming(input));
        if (match.isEmpty()) {
            return false;
        }

        var prefixMatch = match.get();
        execute(prefixMatch.channel(), player, prefixMatch.args());
        return true;
    }

    private Optional<PrefixMatch> matchPrefixedChannel(ProxyPlayer player, String normalized) {
        if (normalized.isEmpty()) {
            return Optional.empty();
        }

        for (ChatChannel channel : bootstrap.getChannels()) {
            var prefix = commandPrefix(normalized, channel);
            if (prefix.isEmpty() || !hasChannelPermission(player, channel)) {
                continue;
            }

            var body = normalized.substring(prefix.get().length()).stripLeading();
            var args = body.isEmpty() ? new String[0] : body.split(" ");
            return Optional.of(new PrefixMatch(channel, args));
        }

        return Optional.empty();
    }

    private record PrefixMatch(ChatChannel channel, String[] args) { }

    private static String normalizeIncoming(String input) {
        return input == null ? "" : input.stripLeading();
    }

    private static Optional<String> commandPrefix(String message, ChatChannel channel) {
        if (!channel.isUseCommandPrefix()) {
            return Optional.empty();
        }

        var prefix = channel.getCommandPrefix();
        if (prefix == null || prefix.isEmpty() || !message.startsWith(prefix)) {
            return Optional.empty();
        }

        return Optional.of(prefix);
    }

    static boolean hasChannelPermission(ProxyPlayer player, ChatChannel channel) {
        var permission = channel.getPermission();
        return permission == null || permission.isEmpty() || player.hasPermission(permission);
    }

    private boolean isToggleAvailable(ChatChannel channel, ProxyPlayer player) {
        return SignedChatPolicy.isToggleAvailable(
                bootstrap.getConfig().getConfig(),
                bootstrap.getSignedChatHandler(),
                channel,
                player
        );
    }

    private static boolean isBlacklisted(ChatChannel channel, ProxyPlayer player) {
        return channel.getServerBlacklist().contains(player.getServerName());
    }

    private void sendBlacklistMessage(ProxyPlayer player, ChatChannel channel, ProxyChatYaml config, String[] args) {
        player.sendMessage(formattedConfigMessage(config, "blacklist-message", player, channel, args));
    }

    private void broadcast(ChatChannel channel, Component message, ProxyPlayer localScope) {
        var platform = bootstrap.getPlatform();
        var recipients = localScope == null
                ? platform.getOnlinePlayers()
                : platform.getPlayersOnServer(localScope);

        for (ProxyPlayer recipient : recipients) {
            var permission = channel.getPermission();
            var permitted = permission == null || permission.isEmpty() || recipient.hasPermission(permission);
            if (permitted && !channel.getToggleUtils().isIgnored(recipient.getUniqueId())) {
                recipient.sendMessage(message);
            }
        }

        if (channel.isLogChatToConsole()) {
            platform.logInfo(PLAIN.serialize(message));
        }
    }

    private Optional<Component> preparePlayerChat(
            ChatChannel channel, ProxyPlayer player, String[] args, ProxyChatYaml config
    ) {
        if (isBlacklisted(channel, player)) {
            sendBlacklistMessage(player, channel, config, args);
            return Optional.empty();
        }

        if (channel.getToggleUtils().isDelayed(player.getUniqueId())) {
            player.sendMessage(formattedConfigMessage(config, "command-cooldown-message", player, channel, args));
            return Optional.empty();
        }

        if (channel.getToggleUtils().isIgnored(player.getUniqueId())) {
            player.sendMessage(formattedConfigMessage(config, "chat-disabled-message", player, channel, args));
            return Optional.empty();
        }

        var joinedArgs = String.join(" ", args);
        if (!player.hasPermission(channel.getUseColorInChatPermission())) {
            joinedArgs = MessageFormatter.stripLegacyColors(joinedArgs);
        }

        var maxLength = config.getInt("max-message-length");
        if (maxLength > 0 && joinedArgs.length() > maxLength) {
            player.sendMessage(formatMessage(
                    config,
                    ProxyChatMessages.resolve(config, "message-too-long-message")
                            .replace("%max-length%", String.valueOf(maxLength)),
                    player,
                    channel,
                    args,
                    false
            ));
            return Optional.empty();
        }

        if (!player.hasPermission(channel.getCommandDelayOverridePermission())) {
            var delay = channel.getToggleUtils().startDelay(
                    player.getUniqueId(),
                    channel.getCommandDelay(),
                    () -> channel.getToggleUtils().clearDelay(player.getUniqueId())
            );
            bootstrap.getPlatform().scheduleDelayedTask(delay, channel.getCommandDelay());
        }

        var component = formatMessage(config, channel.getFormat(), player, channel, args, true);

        return Optional.of(component);
    }

    private Component formattedConfigMessage(
            ProxyChatYaml config, String key, ProxyCommandSource source, ChatChannel channel, String[] args
    ) {
        return formatMessage(config, ProxyChatMessages.resolve(config, key), source, channel, args, false);
    }

    private Component formatMessage(
            ProxyChatYaml config, String template, ProxyCommandSource source, ChatChannel channel, String[] args,
            boolean ignorePrefix
    ) {
        if (template == null || template.isEmpty()) {
            return Component.empty();
        }

        var message = applyPlaceholders(template, source, channel, args, config);
        if (!ignorePrefix) {
            message = ProxyChatMessages.resolve(config, "prefix") + message;
        }

        return formatConfiguredMessage(config, message);
    }

    private Component formatConfiguredMessage(ProxyChatYaml config, String message) {
        var format = config.getString("message-format");
        return MessageFormatter.format(message, MessageFormatter.parseFormat(format));
    }

    private String applyPlaceholders(
            String template, ProxyCommandSource source, ChatChannel channel, String[] args, ProxyChatYaml config
    ) {
        var player = source.asPlayer();
        var prefix = nullToEmpty(ProxyChatMessages.resolve(config, "prefix"));
        var message = template
                .replace("%player%", nullToEmpty(source.getName()))
                .replace("%prefix%", prefix)
                .replace("%command-name%", nullToEmpty(channel.getCommandName()))
                .replace("%command-alias%", nullToEmpty(channel.getCommandAlias()))
                .replace("%command-prefix%", nullToEmpty(channel.getCommandPrefix()))
                .replace("%chat-name%", nullToEmpty(channel.getChatName()));

        if (player != null) {
            message = message.replace("%server%", player.getServerName());

            var delay = channel.getToggleUtils().getDelay(player.getUniqueId());
            if (delay != null) {
                message = message.replace("%chat-cooldown%", delay.getRemainingSeconds());
            }
        }

        var joinedArgs = String.join(" ", args);
        if (player != null && !player.hasPermission(channel.getUseColorInChatPermission())) {
            joinedArgs = MessageFormatter.stripLegacyColors(joinedArgs);
        }

        message = message.replace("%message%", joinedArgs);
        if (player != null) {
            message = bootstrap.getPlatform().replaceExternalPlaceholders(player, message);
        }

        return message;
    }

    private static String nullToEmpty(String value) {
        return value != null ? value : "";
    }
}
