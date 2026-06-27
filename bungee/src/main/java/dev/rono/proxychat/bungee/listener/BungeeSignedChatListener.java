package dev.rono.proxychat.bungee.listener;

import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import dev.rono.proxychat.bungee.platform.BungeePlatform;
import dev.rono.proxychat.common.ProxyChatCore;

public final class BungeeSignedChatListener implements Listener {
    private final ProxyChatCore core;
    private final BungeePlatform platform;

    public BungeeSignedChatListener(ProxyChatCore core, BungeePlatform platform) {
        this.core = core;
        this.platform = platform;
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        var player = platform.toPlayer(event.getPlayer());
        core.onPlayerJoin(player);
        core.getSignedChatHandler().onPlayerJoin(player);
    }

    @EventHandler
    public void onServerConnected(ServerConnectedEvent event) {
        core.getSignedChatHandler().onPlayerJoin(platform.toPlayer(event.getPlayer()));
    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent event) {
        var player = platform.toPlayer(event.getPlayer());
        core.onPlayerQuit(player);
        core.getSignedChatHandler().onPlayerQuit(player);
    }
}
