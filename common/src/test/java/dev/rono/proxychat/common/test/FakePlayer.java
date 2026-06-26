package dev.rono.proxychat.common.test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import dev.rono.proxychat.common.platform.ProxyPlayer;

public final class FakePlayer implements ProxyPlayer {
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private final UUID uniqueId;
    private final String name;
    private String serverName;
    private int protocolVersion = 767;
    private final Set<String> permissions = new HashSet<>();
    private final List<String> receivedMessages = new ArrayList<>();

    public FakePlayer(String name, String serverName) {
        this.uniqueId = UUID.nameUUIDFromBytes(name.getBytes());
        this.name = name;
        this.serverName = serverName;
    }

    public FakePlayer withPermission(String permission) {
        permissions.add(permission);
        return this;
    }

    public FakePlayer withProtocolVersion(int protocolVersion) {
        this.protocolVersion = protocolVersion;
        return this;
    }

    public FakePlayer onServer(String serverName) {
        this.serverName = serverName;
        return this;
    }

    public List<String> receivedMessages() {
        return receivedMessages;
    }

    public void clearMessages() {
        receivedMessages.clear();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    @Override
    public boolean isPlayer() {
        return true;
    }

    @Override
    public UUID getUniqueId() {
        return uniqueId;
    }

    @Override
    public String getServerName() {
        return serverName;
    }

    @Override
    public int getProtocolVersion() {
        return protocolVersion;
    }

    @Override
    public void sendMessage(Component message) {
        receivedMessages.add(PLAIN.serialize(message));
    }

    @Override
    public ProxyPlayer asPlayer() {
        return this;
    }
}
