package dev.rono.proxychat.common.platform;

import net.kyori.adventure.text.Component;

import java.util.UUID;

public interface ProxyCommandSource {
    String getName();

    boolean hasPermission(String permission);

    boolean isPlayer();

    UUID getUniqueId();

    default ProxyPlayer asPlayer() {
        if (this instanceof ProxyPlayer proxyPlayer) {
            return proxyPlayer;
        }

        return null;
    }

    void sendMessage(Component message);
}
