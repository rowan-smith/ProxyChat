package dev.rono.proxychat.bungee;

import org.bstats.bungeecord.Metrics;

public final class ProxyChatBStats {

    /**
     * bStats plugin ID for the BungeeCord / Waterfall build.
     */
    private static final int PLUGIN_ID = 6156;

    public static void register(BungeeProxyChatPlugin plugin) {
        new Metrics(plugin, PLUGIN_ID);
    }
}
