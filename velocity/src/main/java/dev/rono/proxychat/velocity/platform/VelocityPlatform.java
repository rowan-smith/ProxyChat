package dev.rono.proxychat.velocity.platform;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import net.kyori.adventure.text.Component;

import dev.rono.proxychat.common.platform.ProxyChatPlatform;
import dev.rono.proxychat.common.platform.ProxyCommandSource;
import dev.rono.proxychat.common.platform.ProxyPlayer;
import dev.rono.proxychat.velocity.VelocityProxyChatPlugin;

public final class VelocityPlatform implements ProxyChatPlatform {
    private final VelocityProxyChatPlugin plugin;

    public VelocityPlatform(VelocityProxyChatPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Collection<? extends ProxyPlayer> getOnlinePlayers() {
        return plugin
                .getServer()
                .getAllPlayers()
                .stream()
                .map(VelocityPlayer::new)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<? extends ProxyPlayer> getPlayersOnServer(ProxyPlayer player) {
        return plugin
                .getServer()
                .getPlayer(player.getUniqueId())
                .flatMap(connected -> connected.getCurrentServer()
                        .map(server -> server.getServer().getPlayersConnected().stream()
                                .map(VelocityPlayer::new)
                                .collect(Collectors.toList())))
                .orElseGet(List::of);
    }

    @Override
    public void sendMessage(ProxyCommandSource source, Component message) {
        if (source instanceof VelocityPlayer velocityPlayer) {
            velocityPlayer.handle().sendMessage(message);

            return;
        }

        if (source instanceof VelocityCommandSource commandSource) {
            commandSource.handle().sendMessage(message);
        }
    }

    @Override
    public void logInfo(String message) {
        plugin.getLogger().info(message);
    }

    @Override
    public void logWarning(String message) {
        plugin.getLogger().warn(message);
    }

    @Override
    public void scheduleDelayedTask(Runnable task, long delayMillis) {
        plugin.getServer()
                .getScheduler()
                .buildTask(plugin, task)
                .delay(delayMillis, TimeUnit.MILLISECONDS)
                .schedule();
    }

    @Override
    public boolean isPluginPresent(String pluginId) {
        return plugin.getServer().getPluginManager().getPlugin(pluginId).isPresent();
    }
}
