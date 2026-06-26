package dev.rono.proxychat.common.test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dev.rono.proxychat.common.platform.ProxyPlayer;
import dev.rono.proxychat.common.platform.SignedChatHandler;

public final class RecordingSignedChatHandler implements SignedChatHandler {

    private boolean canIntercept = true;
    private final List<UUID> joined = new ArrayList<>();
    private final List<UUID> quit = new ArrayList<>();
    private final List<UUID> acknowledged = new ArrayList<>();

    public RecordingSignedChatHandler canIntercept(boolean value) {
        this.canIntercept = value;
        return this;
    }

    public List<UUID> acknowledgedPlayers() {
        return acknowledged;
    }

    @Override
    public void onPlayerJoin(ProxyPlayer player) {
        joined.add(player.getUniqueId());
    }

    @Override
    public void onPlayerQuit(ProxyPlayer player) {
        quit.add(player.getUniqueId());
    }

    @Override
    public void acknowledgeCancelledChat(ProxyPlayer player) {
        acknowledged.add(player.getUniqueId());
    }

    @Override
    public boolean canInterceptChat(ProxyPlayer player) {
        return canIntercept;
    }
}
