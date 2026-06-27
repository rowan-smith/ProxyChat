package dev.rono.proxychat.bungee.platform;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.CommandSender;
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
                .map(this::toPlayer)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<? extends ProxyPlayer> getPlayersOnServer(ProxyPlayer player) {
        var bungeePlayer = ProxyServer.getInstance().getPlayer(player.getUniqueId());

        if (bungeePlayer == null || bungeePlayer.getServer() == null) {
            return List.of();
        }

        return bungeePlayer.getServer()
                .getInfo()
                .getPlayers()
                .stream()
                .map(this::toPlayer)
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
                TimeUnit.MILLISECONDS
        );
    }

    @Override
    public boolean isPluginPresent(String pluginId) {
        return plugin.getProxy().getPluginManager().getPlugin(pluginId) != null;
    }

    public BungeeAudiences adventure() {
        return plugin.getAdventure();
    }

    public BungeePlayer toPlayer(ProxiedPlayer player) {
        return new BungeePlayer(player, this);
    }

    public BungeeCommandSource toSource(CommandSender sender) {
        return new BungeeCommandSource(sender, this);
    }
}
