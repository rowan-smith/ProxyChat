package dev.rono.proxychat.bungee.command;

import java.util.HashSet;
import java.util.Set;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import dev.rono.proxychat.bungee.platform.BungeeCommandSource;
import dev.rono.proxychat.bungee.platform.BungeePlayer;
import dev.rono.proxychat.common.ProxyChatCore;
import dev.rono.proxychat.common.channel.ChatChannel;

/**
 * Handles {@code /<prefix><message>} without cancelling signed chat (safe on Paper backends).
 */
public final class BungeePrefixCommand extends Command implements TabExecutor {
    private final ProxyChatCore core;
    private final ChatChannel channel;

    public BungeePrefixCommand(ProxyChatCore core, ChatChannel channel) {
        super(channel.getCommandPrefix(), channel.getPermission());

        this.core = core;
        this.channel = channel;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (sender instanceof ProxiedPlayer player) {
            core.getChannelService().execute(channel, new BungeePlayer(player), args);

        } else {
            core.getChannelService().execute(channel, new BungeeCommandSource(sender), args);
        }
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        Set<String> suggestions = new HashSet<>();

        if (sender instanceof ProxiedPlayer player) {
            suggestions.addAll(core.getChannelService().tabComplete(channel, new BungeePlayer(player), args));
        }

        return suggestions;
    }
}
