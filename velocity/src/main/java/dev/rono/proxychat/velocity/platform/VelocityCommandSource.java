package dev.rono.proxychat.velocity.platform;

import java.util.UUID;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;

import net.kyori.adventure.text.Component;

import dev.rono.proxychat.common.platform.ProxyCommandSource;
import dev.rono.proxychat.common.platform.ProxyPlayer;

public record VelocityCommandSource(CommandSource handle) implements ProxyCommandSource {
    @Override
    public String getName() {
        if (handle instanceof Player player) {
            return player.getUsername();
        }

        return "Console";
    }

    @Override
    public boolean hasPermission(String permission) {
        return handle.hasPermission(permission);
    }

    @Override
    public boolean isPlayer() {
        return handle instanceof Player;
    }

    @Override
    public ProxyPlayer asPlayer() {
        if (handle instanceof Player player) {
            return new VelocityPlayer(player);
        }

        return null;
    }

    @Override
    public UUID getUniqueId() {
        if (handle instanceof Player player) {
            return player.getUniqueId();
        }

        return new UUID(0, 0);
    }

    @Override
    public void sendMessage(Component message) {
        handle.sendMessage(message);
    }
}
