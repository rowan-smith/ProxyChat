package dev.rono.proxychat.bungee.command;

import java.util.HashSet;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import dev.rono.proxychat.bungee.platform.BungeePlatform;
import dev.rono.proxychat.common.ProxyChatCore;
import dev.rono.proxychat.common.channel.ChatChannel;

public final class BungeeChannelCommand extends Command implements TabExecutor {
    private final ProxyChatCore core;
    private final BungeePlatform platform;
    private final ChatChannel channel;

    public BungeeChannelCommand(ProxyChatCore core, BungeePlatform platform, ChatChannel channel) {
        super(channel.getCommandName(), channel.getPermission(), channel.getCommandAlias());

        this.core = core;
        this.platform = platform;
        this.channel = channel;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (sender instanceof ProxiedPlayer player) {
            core.getChannelService().execute(channel, platform.toPlayer(player), args);

        } else {
            core.getChannelService().execute(channel, platform.toSource(sender), args);
        }
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        var suggestions = new HashSet<String>();

        if (sender instanceof ProxiedPlayer player) {
            suggestions.addAll(core.getChannelService().tabComplete(channel, platform.toPlayer(player), args));
        }

        return suggestions;
    }
}
