package dev.rono.proxychat.common.test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import dev.rono.proxychat.common.platform.ProxyCommandSource;

public final class FakeConsole implements ProxyCommandSource {
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private final List<String> receivedMessages = new ArrayList<>();
    private final Set<String> permissions = new HashSet<>();

    public FakeConsole withPermission(String permission) {
        permissions.add(permission);
        return this;
    }

    public List<String> receivedMessages() {
        return receivedMessages;
    }

    @Override
    public String getName() {
        return "Console";
    }

    @Override
    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    @Override
    public boolean isPlayer() {
        return false;
    }

    @Override
    public UUID getUniqueId() {
        return new UUID(0, 0);
    }

    @Override
    public void sendMessage(Component message) {
        receivedMessages.add(PLAIN.serialize(message));
    }
}
