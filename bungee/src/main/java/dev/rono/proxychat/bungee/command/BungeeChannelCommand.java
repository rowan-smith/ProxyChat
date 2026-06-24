package dev.rono.proxychat.bungee.command;

import dev.rono.proxychat.bungee.platform.BungeeCommandSource;
import dev.rono.proxychat.bungee.platform.BungeePlayer;
import dev.rono.proxychat.common.ProxyChatCore;
import dev.rono.proxychat.common.channel.ChatChannel;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.HashSet;
import java.util.Set;

public final class BungeeChannelCommand extends Command implements TabExecutor {
    private final ProxyChatCore core;
    private final ChatChannel channel;

    public BungeeChannelCommand(ProxyChatCore core, ChatChannel channel) {
        super(channel.getCommandName(), channel.getPermission(), channel.getCommandAlias());

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
