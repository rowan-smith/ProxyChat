package dev.rono.proxychat.bungee.platform;

import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import dev.rono.proxychat.common.platform.ProxyCommandSource;
import dev.rono.proxychat.common.platform.ProxyPlayer;

public record BungeeCommandSource(CommandSender handle, BungeePlatform platform) implements ProxyCommandSource {
    @Override
    public String getName() {
        return handle.getName();
    }

    @Override
    public boolean hasPermission(String permission) {
        return handle.hasPermission(permission);
    }

    @Override
    public boolean isPlayer() {
        return handle instanceof ProxiedPlayer;
    }

    @Override
    public ProxyPlayer asPlayer() {
        if (handle instanceof ProxiedPlayer player) {
            return platform.toPlayer(player);
        }

        return null;
    }

    @Override
    public void sendMessage(Component message) {
        platform.adventure().sender(handle).sendMessage(message);
    }
}
