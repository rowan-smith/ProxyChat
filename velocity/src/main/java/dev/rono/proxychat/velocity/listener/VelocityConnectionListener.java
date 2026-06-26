package dev.rono.proxychat.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;

import dev.rono.proxychat.common.ProxyChatCore;
import dev.rono.proxychat.velocity.platform.VelocityPlayer;

public final class VelocityConnectionListener {
    private final ProxyChatCore core;

    public VelocityConnectionListener(ProxyChatCore core) {
        this.core = core;
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        core.getSignedChatHandler().onPlayerJoin(new VelocityPlayer(event.getPlayer()));
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        core.getSignedChatHandler().onPlayerQuit(new VelocityPlayer(event.getPlayer()));
    }
}
