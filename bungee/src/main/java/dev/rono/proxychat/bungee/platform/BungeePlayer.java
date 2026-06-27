package dev.rono.proxychat.bungee.platform;

import java.util.UUID;

import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import dev.rono.proxychat.common.platform.ProxyPlayer;

public record BungeePlayer(ProxiedPlayer handle, BungeePlatform platform) implements ProxyPlayer {
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
        return true;
    }

    @Override
    public UUID getUniqueId() {
        return handle.getUniqueId();
    }

    @Override
    public String getServerName() {
        if (handle.getServer() == null) {
            return "";
        }

        return handle.getServer().getInfo().getName();
    }

    @Override
    public int getProtocolVersion() {
        return handle.getPendingConnection().getVersion();
    }

    @Override
    public void sendMessage(Component message) {
        platform.adventure().player(handle).sendMessage(message);
    }
}
