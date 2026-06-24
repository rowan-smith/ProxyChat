package dev.rono.proxychat.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.rono.proxychat.common.ProxyChatCore;
import dev.rono.proxychat.common.channel.ChatChannel;
import dev.rono.proxychat.velocity.command.VelocityAdminCommand;
import dev.rono.proxychat.velocity.command.VelocityChannelCommand;
import dev.rono.proxychat.velocity.listener.VelocityChatListener;
import dev.rono.proxychat.velocity.listener.VelocityConnectionListener;
import dev.rono.proxychat.velocity.platform.VelocityPlatform;
import dev.rono.proxychat.velocity.platform.VelocitySignedChatHandler;
import lombok.Getter;
import org.bstats.velocity.Metrics;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(id = "proxychat", name = "ProxyChat", version = "2.0.0", authors = {"Rono"})
public final class VelocityProxyChatPlugin {
    @Getter private static VelocityProxyChatPlugin instance;
    @Getter private final ProxyServer server;
    @Getter private final Logger logger;
    @Getter private ProxyChatCore core;

    private final Path dataDirectory;
    private final Metrics.Factory metricsFactory;

    @Inject
    public VelocityProxyChatPlugin(
            ProxyServer server,
            Logger logger,
            @DataDirectory Path dataDirectory,
            Metrics.Factory metricsFactory
    ) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.metricsFactory = metricsFactory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        instance = this;

        VelocityPlatform platform = new VelocityPlatform(this);

        core = new ProxyChatCore(java.util.logging.Logger.getLogger("ProxyChat"), platform, new VelocitySignedChatHandler(this), dataDirectory);
        core.enable(getClass().getClassLoader().getResourceAsStream("config.yml"), getClass().getClassLoader().getResourceAsStream("global.yml"));

        registerCommands();

        server.getEventManager().register(this, new VelocityChatListener(core));
        server.getEventManager().register(this, new VelocityConnectionListener(core));

        ProxyChatBStats.register(this, metricsFactory);
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {}

    public void registerCommands() {
        for (ChatChannel channel : core.getChannels()) {
            server.getCommandManager()
                    .register(server.getCommandManager()
                                    .metaBuilder(channel.getCommandName())
                                    .aliases(channel.getCommandAlias())
                                    .plugin(this)
                                    .build(),
                            new VelocityChannelCommand(core, channel)
            );
        }

        server.getCommandManager().register(
                server.getCommandManager()
                        .metaBuilder("proxychat")
                        .aliases("pc")
                        .plugin(this)
                        .build(),
                new VelocityAdminCommand(core)
        );

        logger.info("{} channel commands loaded.", core.getChannels().size());
    }
}
