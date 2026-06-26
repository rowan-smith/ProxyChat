package dev.rono.proxychat.velocity.platform;

import java.util.UUID;

import com.velocitypowered.api.proxy.Player;

import net.kyori.adventure.text.Component;

import dev.rono.proxychat.common.platform.ProxyPlayer;

public record VelocityPlayer(Player handle) implements ProxyPlayer {
    @Override
    public String getName() {
        return handle.getUsername();
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
        return handle.getCurrentServer()
                .map(connection -> connection.getServerInfo().getName())
                .orElse("");
    }

    @Override
    public int getProtocolVersion() {
        return handle.getProtocolVersion().getProtocol();
    }

    @Override
    public void sendMessage(Component message) {
        handle.sendMessage(message);
    }
}
