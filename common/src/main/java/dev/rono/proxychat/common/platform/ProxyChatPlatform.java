package dev.rono.proxychat.common.platform;

import net.kyori.adventure.text.Component;

import java.util.Collection;
import java.util.UUID;

public interface ProxyChatPlatform {
    Collection<? extends ProxyPlayer> getOnlinePlayers();

    Collection<? extends ProxyPlayer> getPlayersOnServer(ProxyPlayer player);

    void sendMessage(ProxyCommandSource source, Component message);

    void logInfo(String message);

    void logWarning(String message);

    void scheduleDelayedTask(Runnable task, long delayMillis);

    boolean isPluginPresent(String pluginId);
}
