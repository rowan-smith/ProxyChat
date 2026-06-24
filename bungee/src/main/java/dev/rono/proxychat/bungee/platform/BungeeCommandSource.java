package dev.rono.proxychat.bungee.platform;

import dev.rono.proxychat.bungee.BungeeProxyChatPlugin;
import dev.rono.proxychat.common.platform.ProxyCommandSource;
import dev.rono.proxychat.common.platform.ProxyPlayer;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.UUID;

public record BungeeCommandSource(CommandSender handle) implements ProxyCommandSource {
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
            return new BungeePlayer(player);
        }

        return null;
    }

    @Override
    public UUID getUniqueId() {
        if (handle instanceof ProxiedPlayer player) {
            return player.getUniqueId();
        }

        return new UUID(0, 0);
    }

    @Override
    public void sendMessage(Component message) {
        BungeeProxyChatPlugin.getInstance().getAdventure().sender(handle).sendMessage(message);
    }
}
