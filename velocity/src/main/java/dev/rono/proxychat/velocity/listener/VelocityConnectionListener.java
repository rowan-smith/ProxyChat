package dev.rono.proxychat.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;

import dev.rono.proxychat.common.ProxyChatCore;
import dev.rono.proxychat.velocity.platform.VelocityPlatform;
import dev.rono.proxychat.velocity.platform.VelocityPlayer;

public final class VelocityConnectionListener {
    private final ProxyChatCore core;
    private final VelocityPlatform platform;

    public VelocityConnectionListener(ProxyChatCore core, VelocityPlatform platform) {
        this.core = core;
        this.platform = platform;
    }

    @Subscribe
    public void onLogin(LoginEvent event) {
        core.onPlayerJoin(new VelocityPlayer(event.getPlayer()));
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        core.getSignedChatHandler().onPlayerJoin(new VelocityPlayer(event.getPlayer()));
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        var player = new VelocityPlayer(event.getPlayer());
        core.onPlayerQuit(player);
        core.getSignedChatHandler().onPlayerQuit(player);
    }
}
