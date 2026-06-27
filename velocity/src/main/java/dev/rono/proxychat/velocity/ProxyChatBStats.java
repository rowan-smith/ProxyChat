package dev.rono.proxychat.velocity;

import org.bstats.velocity.Metrics;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ProxyChatBStats {

    /**
     * bStats plugin ID for the Velocity build.
     */
    private static final int PLUGIN_ID = 32179;

    public static void register(VelocityProxyChatPlugin plugin, Metrics.Factory metricsFactory) {
        metricsFactory.make(plugin, PLUGIN_ID);
    }
}
