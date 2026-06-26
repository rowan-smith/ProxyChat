package dev.rono.proxychat.bungee.listener;

import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import dev.rono.proxychat.bungee.platform.BungeePlayer;
import dev.rono.proxychat.common.ProxyChatCore;

public final class BungeeSignedChatListener implements Listener {
    private final ProxyChatCore core;

    public BungeeSignedChatListener(ProxyChatCore core) {
        this.core = core;
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        core.getSignedChatHandler().onPlayerJoin(new BungeePlayer(event.getPlayer()));
    }

    @EventHandler
    public void onServerConnected(ServerConnectedEvent event) {
        core.getSignedChatHandler().onPlayerJoin(new BungeePlayer(event.getPlayer()));
    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent event) {
        core.getSignedChatHandler().onPlayerQuit(new BungeePlayer(event.getPlayer()));
    }
}
