package dev.rono.proxychat.common.channel;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import net.kyori.adventure.text.Component;

import dev.rono.proxychat.common.config.ProxyChatMessages;
import dev.rono.proxychat.common.config.ProxyChatYaml;
import dev.rono.proxychat.common.message.MessageFormatter;
import dev.rono.proxychat.common.platform.ProxyChatBootstrap;
import dev.rono.proxychat.common.platform.ProxyChatPlatform;
import dev.rono.proxychat.common.platform.ProxyCommandSource;
import dev.rono.proxychat.common.platform.ProxyPlayer;
import dev.rono.proxychat.common.util.SignedChatPolicy;

public final class ChatChannelService {
    private final ProxyChatBootstrap bootstrap;

    public ChatChannelService(ProxyChatBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    public void execute(ChatChannel channel, ProxyCommandSource sender, String[] args) {
        ProxyChatPlatform platform = bootstrap.getPlatform();
        ProxyChatYaml config = bootstrap.getConfig().getConfig();

        if (!sender.isPlayer()) {
            if (channel.isConsoleChatAllowed()) {
                String message = applyPlaceholders(channel.getConsoleFormat(), sender, channel, args, config, false);
                broadcast(channel, MessageFormatter.legacy(message), null, true);

            } else {
                platform.sendMessage(
                        sender,
                        formattedConfigMessage(config, "console-disabled-message", sender, channel, args)
                );
            }

            return;
        }

        ProxyPlayer player = sender.asPlayer();
        if (player == null) {
            return;
        }

        if (channel.getServerBlacklist().contains(player.getServerName())) {
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
                player.sendMessage(formattedConfigMessage(config, "toggle-enable-message", player, channel, args));

            } else {
                player.sendMessage(formattedConfigMessage(config, "toggle-disable-message", player, channel, args));
            }

            return;
        }

        if (channel.isIgnorable() && args[0].equalsIgnoreCase("ignore")) {
            if (channel.getToggleUtils().toggleIgnore(player.getUniqueId())) {
                player.sendMessage(formattedConfigMessage(config, "ignore-enable-message", player, channel, args));

            } else {
                player.sendMessage(formattedConfigMessage(config, "ignore-disable-message", player, channel, args));
            }

            return;
        }

        if (channel.getToggleUtils().isDelayed(player.getUniqueId())) {
            player.sendMessage(formattedConfigMessage(config, "command-cooldown-message", player, channel, args));
            return;
        }

        Optional<OutgoingChatMessage> outgoing = preparePlayerChat(channel, player, args, config);
        if (outgoing.isEmpty()) {
            return;
        }

        if (channel.isLocal()) {
            broadcast(channel, outgoing.get().component(), player, false);

        } else {
            broadcast(channel, outgoing.get().component(), null, false);
        }
    }

    public Set<String> tabComplete(ChatChannel channel, ProxyPlayer player, String[] args) {
        Set<String> suggestions = new HashSet<>();
        if (channel.getServerBlacklist().contains(player.getServerName())) {
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
        String normalized = normalizeIncoming(input);
        if (normalized.isEmpty()) {
            return Optional.empty();
        }

        ProxyChatYaml config = bootstrap.getConfig().getConfig();

        for (ChatChannel channel : bootstrap.getChannels()) {
            if (!matchesPrefix(normalized, channel) || !hasChannelPermission(player, channel)) {
                continue;
            }

            String body = normalized.substring(channel.getCommandPrefix().length()).stripLeading();
            String[] args = body.isEmpty() ? new String[0] : body.split(" ");
            if (args.length < 1) {
                player.sendMessage(formatMessage(config, channel.getInvalidArgs(), player, channel, args, false));
                return Optional.of(new VelocityPrefixInterceptResult.Blocked());
            }

            Optional<OutgoingChatMessage> outgoing = preparePlayerChat(channel, player, args, config);
            if (outgoing.isEmpty()) {
                return Optional.of(new VelocityPrefixInterceptResult.Blocked());
            }

            if (channel.isLocal()) {
                broadcast(channel, outgoing.get().component(), player, false);
            } else {
                broadcast(channel, outgoing.get().component(), null, false);
            }

            return Optional.of(new VelocityPrefixInterceptResult.Delivered());
        }

        return Optional.empty();
    }

    public boolean tryInterceptToggleOnly(ProxyPlayer player, String message) {
        String normalized = normalizeIncoming(message);
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
        String normalized = normalizeIncoming(input);
        if (normalized.isEmpty()) {
            return false;
        }

        for (ChatChannel channel : bootstrap.getChannels()) {
            if (!matchesPrefix(normalized, channel) || !hasChannelPermission(player, channel)) {
                continue;
            }

            String body = normalized.substring(channel.getCommandPrefix().length()).stripLeading();
            execute(channel, player, body.isEmpty() ? new String[0] : body.split(" "));

            return true;
        }

        return false;
    }

    private static String normalizeIncoming(String input) {
        return input == null ? "" : input.stripLeading();
    }

    private static boolean matchesPrefix(String message, ChatChannel channel) {
        if (!channel.isUseCommandPrefix()) {
            return false;
        }

        String prefix = channel.getCommandPrefix();
        return prefix != null && !prefix.isEmpty() && message.startsWith(prefix);
    }

    static boolean hasChannelPermission(ProxyPlayer player, ChatChannel channel) {
        String permission = channel.getPermission();
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

    private void broadcast(ChatChannel channel, Component message, ProxyPlayer localScope, boolean consoleFormat) {
        ProxyChatPlatform platform = bootstrap.getPlatform();
        Iterable<? extends ProxyPlayer> recipients = localScope == null
                ? platform.getOnlinePlayers()
                : platform.getPlayersOnServer(localScope);

        for (ProxyPlayer recipient : recipients) {
            boolean permitted = recipient.hasPermission(channel.getPermission())
                    || channel.getPermission().isEmpty();
            if (permitted && !channel.getToggleUtils().isIgnored(recipient.getUniqueId())) {
                recipient.sendMessage(message);
            }
        }

        if (channel.isLogChatToConsole()) {
            String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                    .plainText()
                    .serialize(message);
            platform.logInfo(plain);
        }
    }

    private Optional<OutgoingChatMessage> preparePlayerChat(
            ChatChannel channel,
            ProxyPlayer player,
            String[] args,
            ProxyChatYaml config
    ) {
        if (channel.getServerBlacklist().contains(player.getServerName())) {
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

        if (!player.hasPermission(channel.getCommandDelayOverridePermission())) {
            channel.getToggleUtils().startDelay(
                    player.getUniqueId(),
                    channel.getCommandDelay(),
                    () -> channel.getToggleUtils().clearDelay(player.getUniqueId())
            );
            bootstrap.getPlatform().scheduleDelayedTask(
                    channel.getToggleUtils().getDelay(player.getUniqueId()),
                    channel.getCommandDelay()
            );
        }

        String legacyText = applyPlaceholders(channel.getFormat(), player, channel, args, config, true);
        Component component = formatMessage(config, channel.getFormat(), player, channel, args, true);

        return Optional.of(new OutgoingChatMessage(legacyText, component));
    }

    private record OutgoingChatMessage(String legacyText, Component component) {
    }

    private Component formattedConfigMessage(
            ProxyChatYaml config,
            String key,
            ProxyCommandSource source,
            ChatChannel channel,
            String[] args
    ) {
        return formatMessage(config, ProxyChatMessages.resolve(config, key), source, channel, args, false);
    }

    private Component formatMessage(
            ProxyChatYaml config,
            String template,
            ProxyCommandSource source,
            ChatChannel channel,
            String[] args,
            boolean ignorePrefix
    ) {
        if (template == null || template.isEmpty()) {
            return Component.empty();
        }

        String message = applyPlaceholders(template, source, channel, args, config, ignorePrefix);
        if (!ignorePrefix) {
            message = ProxyChatMessages.resolve(config, "prefix") + message;
        }

        return MessageFormatter.legacy(message);
    }

    private String applyPlaceholders(
            String template,
            ProxyCommandSource source,
            ChatChannel channel,
            String[] args,
            ProxyChatYaml config,
            boolean ignorePrefix
    ) {
        ProxyPlayer player = source.asPlayer();
        String prefix = nullToEmpty(ProxyChatMessages.resolve(config, "prefix"));
        String message = template
                .replace("%player%", nullToEmpty(source.getName()))
                .replace("%prefix%", prefix)
                .replace("%command-name%", nullToEmpty(channel.getCommandName()))
                .replace("%command-alias%", nullToEmpty(channel.getCommandAlias()))
                .replace("%command-prefix%", nullToEmpty(channel.getCommandPrefix()))
                .replace("%chat-name%", nullToEmpty(channel.getChatName()));

        if (player != null) {
            message = message.replace("%server%", player.getServerName());

            if (channel.getToggleUtils().isDelayed(player.getUniqueId())) {
                String remaining = channel.getToggleUtils()
                        .getDelay(player.getUniqueId())
                        .getRemainingSeconds();
                message = message.replace("%chat-cooldown%", remaining);
            }
        }

        String joinedArgs = String.join(" ", args);
        if (player != null && !player.hasPermission(channel.getUseColorInChatPermission())) {
            joinedArgs = MessageFormatter.stripLegacyColors(joinedArgs);
        }

        return message.replace("%message%", joinedArgs);
    }

    private static String nullToEmpty(String value) {
        return value != null ? value : "";
    }
}
