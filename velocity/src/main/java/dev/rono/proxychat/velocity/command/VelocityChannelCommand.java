package dev.rono.proxychat.velocity.command;

import java.util.ArrayList;
import java.util.List;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

import dev.rono.proxychat.common.ProxyChatCore;
import dev.rono.proxychat.common.channel.ChatChannel;
import dev.rono.proxychat.velocity.platform.VelocityCommandSource;
import dev.rono.proxychat.velocity.platform.VelocityPlayer;

public final class VelocityChannelCommand implements SimpleCommand {
    private final ProxyChatCore core;
    private final ChatChannel channel;

    public VelocityChannelCommand(ProxyChatCore core, ChatChannel channel) {
        this.core = core;
        this.channel = channel;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

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

        return new ArrayList<>(
                core.getChannelService().tabComplete(channel, new VelocityPlayer(player), invocation.arguments())
        );
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        String permission = channel.getPermission();
        return permission == null || permission.isEmpty() || invocation.source().hasPermission(permission);
    }
}
