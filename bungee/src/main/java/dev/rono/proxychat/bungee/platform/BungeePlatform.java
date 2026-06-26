package dev.rono.proxychat.bungee.platform;

import java.util.Collection;
import java.util.stream.Collectors;

import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import dev.rono.proxychat.bungee.BungeeProxyChatPlugin;
import dev.rono.proxychat.common.platform.ProxyChatPlatform;
import dev.rono.proxychat.common.platform.ProxyCommandSource;
import dev.rono.proxychat.common.platform.ProxyPlayer;

public final class BungeePlatform implements ProxyChatPlatform {
    private final BungeeProxyChatPlugin plugin;

    public BungeePlatform(BungeeProxyChatPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Collection<? extends ProxyPlayer> getOnlinePlayers() {
        return ProxyServer.getInstance()
                .getPlayers()
                .stream()
                .map(BungeePlayer::new)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<? extends ProxyPlayer> getPlayersOnServer(ProxyPlayer player) {
        ProxiedPlayer bungeePlayer = ProxyServer.getInstance().getPlayer(player.getUniqueId());

        if (bungeePlayer == null || bungeePlayer.getServer() == null) {
            return getOnlinePlayers();
        }

        return bungeePlayer.getServer()
                .getInfo()
                .getPlayers()
                .stream()
                .map(BungeePlayer::new)
                .collect(Collectors.toList());
    }

    @Override
    public void sendMessage(ProxyCommandSource source, Component message) {
        if (source instanceof BungeePlayer bungeePlayer) {
            plugin.getAdventure().player(bungeePlayer.handle()).sendMessage(message);
            return;
        }

        if (source instanceof BungeeCommandSource commandSource) {
            plugin.getAdventure().sender(commandSource.handle()).sendMessage(message);
        }
    }

    @Override
    public void logInfo(String message) {
        plugin.getLogger().info(message);
    }

    @Override
    public void logWarning(String message) {
        plugin.getLogger().warning(message);
    }

    @Override
    public void scheduleDelayedTask(Runnable task, long delayMillis) {
        plugin.getProxy().getScheduler().schedule(
                plugin,
                task,
                delayMillis,
                java.util.concurrent.TimeUnit.MILLISECONDS
        );
    }

    @Override
    public boolean isPluginPresent(String pluginId) {
        return plugin.getProxy().getPluginManager().getPlugin(pluginId) != null;
    }
}
