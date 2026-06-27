package dev.rono.proxychat.common.platform;

import java.util.UUID;

public interface ProxyPlayer extends ProxyCommandSource {
    @Override
    default ProxyPlayer asPlayer() {
        return this;
    }

    UUID getUniqueId();

    String getServerName();

    int getProtocolVersion();
}
