package dev.rono.proxychat.common.platform;

import net.kyori.adventure.text.Component;

public interface ProxyCommandSource {
    String getName();

    boolean hasPermission(String permission);

    boolean isPlayer();

    ProxyPlayer asPlayer();

    void sendMessage(Component message);
}
