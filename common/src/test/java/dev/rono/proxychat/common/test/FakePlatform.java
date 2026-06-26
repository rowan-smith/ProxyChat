package dev.rono.proxychat.common.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.kyori.adventure.text.Component;

import lombok.Setter;

import dev.rono.proxychat.common.platform.ProxyChatPlatform;
import dev.rono.proxychat.common.platform.ProxyCommandSource;
import dev.rono.proxychat.common.platform.ProxyPlayer;
import dev.rono.proxychat.common.platform.SignedChatHandler;

public final class FakePlatform implements ProxyChatPlatform {
    private final List<FakePlayer> players = new ArrayList<>();
    private final List<String> infoLogs = new ArrayList<>();
    private final List<String> warningLogs = new ArrayList<>();
    private final List<ScheduledCall> scheduledTasks = new ArrayList<>();
    private final Map<String, Boolean> installedPlugins = new HashMap<>();
    @Setter private SignedChatHandler signedChatHandler = new NoOpSignedChatHandler();

    public FakePlayer addPlayer(FakePlayer player) {
        players.add(player);
        return player;
    }

    public void installPlugin(String pluginId) {
        installedPlugins.put(pluginId.toLowerCase(), true);
    }

    public SignedChatHandler signedChatHandler() {
        return signedChatHandler;
    }

    public List<String> infoLogs() {
        return infoLogs;
    }

    @Override
    public Collection<? extends ProxyPlayer> getOnlinePlayers() {
        return List.copyOf(players);
    }

    @Override
    public Collection<? extends ProxyPlayer> getPlayersOnServer(ProxyPlayer player) {
        return players.stream()
                .filter(p -> p.getServerName().equals(player.getServerName()))
                .collect(Collectors.toList());
    }

    @Override
    public void sendMessage(ProxyCommandSource source, Component message) {
        source.sendMessage(message);
    }

    @Override
    public void logInfo(String message) {
        infoLogs.add(message);
    }

    @Override
    public void logWarning(String message) {
        warningLogs.add(message);
    }

    @Override
    public void scheduleDelayedTask(Runnable task, long delayMillis) {
        if (delayMillis <= 0) {
            task.run();
            return;
        }

        scheduledTasks.add(new ScheduledCall(task, delayMillis));
    }

    @Override
    public boolean isPluginPresent(String pluginId) {
        return installedPlugins.getOrDefault(pluginId.toLowerCase(), false);
    }

    public record ScheduledCall(Runnable task, long delayMillis) { }

    private static final class NoOpSignedChatHandler implements SignedChatHandler {
        @Override
        public void onPlayerJoin(ProxyPlayer player) { }

        @Override
        public void onPlayerQuit(ProxyPlayer player) { }

        @Override
        public void acknowledgeCancelledChat(ProxyPlayer player) { }
    }
}
