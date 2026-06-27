package dev.rono.proxychat.velocity;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.bstats.velocity.Metrics;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;

import lombok.Getter;

import dev.rono.proxychat.common.ProxyChatCore;
import dev.rono.proxychat.common.channel.ChatChannel;
import dev.rono.proxychat.common.util.SignedChatPolicy;
import dev.rono.proxychat.velocity.command.VelocityAdminCommand;
import dev.rono.proxychat.velocity.command.VelocityChannelCommand;
import dev.rono.proxychat.velocity.command.VelocityPrefixCommand;
import dev.rono.proxychat.velocity.listener.VelocityChatListener;
import dev.rono.proxychat.velocity.listener.VelocityCommandInterceptListener;
import dev.rono.proxychat.velocity.listener.VelocityConnectionListener;
import dev.rono.proxychat.velocity.platform.VelocityPlatform;
import dev.rono.proxychat.velocity.platform.VelocitySignedChatHandler;

@Plugin(id = "proxychat", name = "ProxyChat", authors = {"Rono"})
public final class VelocityProxyChatPlugin {
    @Getter private final ProxyServer server;
    @Getter private final Logger slf4jLogger;
    @Getter private ProxyChatCore core;

    private final Path dataDirectory;
    private final Metrics.Factory metricsFactory;
    private final List<CommandMeta> registeredCommands = new ArrayList<>();
    private CommandMeta adminCommandMeta;

    @Inject
    public VelocityProxyChatPlugin(
            ProxyServer server,
            Logger slf4jLogger,
            @DataDirectory Path dataDirectory,
            Metrics.Factory metricsFactory
    ) {
        this.server = server;
        this.slf4jLogger = slf4jLogger;
        this.dataDirectory = dataDirectory;
        this.metricsFactory = metricsFactory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        var platform = new VelocityPlatform(this);

        core = new ProxyChatCore(
                java.util.logging.Logger.getLogger("ProxyChat"),
                platform,
                new VelocitySignedChatHandler(this),
                dataDirectory
        );
        if (!core.enable(
                getClass().getClassLoader().getResourceAsStream("config.yml"),
                getClass().getClassLoader().getResourceAsStream("global.yml")
        )) {
            slf4jLogger.error("ProxyChat failed to enable. Check the console for configuration errors.");
            return;
        }

        registerCommands();

        var interceptPriority = Short.MAX_VALUE;
        var chatListener = new VelocityChatListener(core);
        var commandInterceptListener = new VelocityCommandInterceptListener(core);
        server.getEventManager().register(
                this,
                PlayerChatEvent.class,
                interceptPriority,
                chatListener::onPlayerChat
        );
        server.getEventManager().register(
                this,
                CommandExecuteEvent.class,
                interceptPriority,
                commandInterceptListener::onCommandExecute
        );
        server.getEventManager().register(this, new VelocityConnectionListener(core, platform));

        ProxyChatBStats.register(this, metricsFactory);
    }

    public void registerCommands() {
        unregisterCommands();

        for (ChatChannel channel : core.getChannels()) {
            var meta = server.getCommandManager()
                    .metaBuilder(channel.getCommandName())
                    .aliases(channel.getCommandAlias())
                    .plugin(this)
                    .build();
            server.getCommandManager().register(meta, new VelocityChannelCommand(core, channel));
            registeredCommands.add(meta);

            registerPrefixCommand(channel);
        }

        if (adminCommandMeta == null) {
            adminCommandMeta = server.getCommandManager()
                    .metaBuilder("proxychat")
                    .aliases("pc")
                    .plugin(this)
                    .build();
            server.getCommandManager().register(
                    adminCommandMeta, new VelocityAdminCommand(core, this::registerCommands));
        }

        slf4jLogger.info("{} channel commands loaded.", core.getChannels().size());
    }

    private void registerPrefixCommand(ChatChannel channel) {
        if (!channel.isUseCommandPrefix()) {
            return;
        }

        var prefix = channel.getCommandPrefix();
        if (prefix == null || prefix.isEmpty()) {
            return;
        }

        if (!SignedChatPolicy.shouldRegisterProxyPrefixCommand(core.getConfig().getConfig(), core.getPlatform())) {
            return;
        }

        var meta = server.getCommandManager()
                .metaBuilder(prefix)
                .plugin(this)
                .build();
        server.getCommandManager().register(meta, new VelocityPrefixCommand(core, channel));
        registeredCommands.add(meta);
    }

    public void unregisterCommands() {
        for (CommandMeta meta : registeredCommands) {
            server.getCommandManager().unregister(meta);
        }

        registeredCommands.clear();
    }
}
