package dev.rono.proxychat.common.platform;

import java.util.Collection;

import net.kyori.adventure.text.Component;

public interface ProxyChatPlatform {
    Collection<? extends ProxyPlayer> getOnlinePlayers();

    Collection<? extends ProxyPlayer> getPlayersOnServer(ProxyPlayer player);

    void sendMessage(ProxyCommandSource source, Component message);

    void logInfo(String message);

    void logWarning(String message);

    void scheduleDelayedTask(Runnable task, long delayMillis);

    boolean isPluginPresent(String pluginId);

    /**
     * Allows platform integrations (for example PlaceholderAPI bridges) to expand unknown placeholders.
     */
    default String replaceExternalPlaceholders(ProxyPlayer player, String template) {
        return template;
    }
}
