package dev.rono.proxychat.velocity.command;

import java.util.List;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

import dev.rono.proxychat.common.ProxyChatCore;
import dev.rono.proxychat.common.channel.ChatChannel;
import dev.rono.proxychat.velocity.platform.VelocityCommandSource;
import dev.rono.proxychat.velocity.platform.VelocityPlayer;

public final class VelocityPrefixCommand implements SimpleCommand {
    private final ProxyChatCore core;
    private final ChatChannel channel;

    public VelocityPrefixCommand(ProxyChatCore core, ChatChannel channel) {
        this.core = core;
        this.channel = channel;
    }

    @Override
    public void execute(Invocation invocation) {
        var source = invocation.source();
        var args = invocation.arguments();

        if (source instanceof Player player) {
            core.getChannelService().execute(channel, new VelocityPlayer(player), args);

        } else {
            core.getChannelService().execute(channel, new VelocityCommandSource(source), args);
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) {
            return List.of();
        }

        return List.copyOf(
                core.getChannelService().tabComplete(channel, new VelocityPlayer(player), invocation.arguments())
        );
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        var permission = channel.getPermission();
        return permission == null || permission.isEmpty() || invocation.source().hasPermission(permission);
    }
}
