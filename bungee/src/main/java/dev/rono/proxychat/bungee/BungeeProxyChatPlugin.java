package dev.rono.proxychat.bungee;

import java.util.ArrayList;
import java.util.List;

import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;

import lombok.Getter;

import dev.rono.proxychat.bungee.command.BungeeAdminCommand;
import dev.rono.proxychat.bungee.command.BungeeChannelCommand;
import dev.rono.proxychat.bungee.command.BungeePrefixCommand;
import dev.rono.proxychat.bungee.listener.BungeeChatListener;
import dev.rono.proxychat.bungee.listener.BungeeSignedChatListener;
import dev.rono.proxychat.bungee.platform.BungeePlatform;
import dev.rono.proxychat.bungee.platform.WaterfallSignedChatHandler;
import dev.rono.proxychat.common.ProxyChatCore;
import dev.rono.proxychat.common.channel.ChatChannel;

public final class BungeeProxyChatPlugin extends Plugin {
    @Getter
    private ProxyChatCore core;

    @Getter
    private BungeeAudiences adventure;

    @Getter
    private BungeePlatform platform;

    private final List<Command> registeredCommands = new ArrayList<>();
    private boolean adminCommandRegistered;

    @Override
    public void onEnable() {
        var dataDirectory = getDataFolder().toPath();
        platform = new BungeePlatform(this);

        adventure = BungeeAudiences.create(this);

        core = new ProxyChatCore(getLogger(), platform, new WaterfallSignedChatHandler(this), dataDirectory);
        if (!core.enable(getResourceAsStream("config.yml"), getResourceAsStream("global.yml"))) {
            getLogger().severe("ProxyChat failed to enable. Check the console for configuration errors.");
            return;
        }

        registerListeners();
        registerCommands();

        ProxyChatBStats.register(this);
    }

    @Override
    public void onDisable() {
        if (adventure != null) {
            adventure.close();
        }
    }

    public void registerCommands() {
        unregisterCommands();

        for (ChatChannel channel : core.getChannels()) {
            var command = new BungeeChannelCommand(core, platform, channel);
            getProxy().getPluginManager().registerCommand(this, command);
            registeredCommands.add(command);
            registerPrefixCommand(channel);
        }

        if (!adminCommandRegistered) {
            getProxy().getPluginManager().registerCommand(this, new BungeeAdminCommand(core, this::registerCommands));
            adminCommandRegistered = true;
        }

        getLogger().info(registeredCommands.size() + " channel commands loaded.");
    }

    private void registerPrefixCommand(ChatChannel channel) {
        if (!channel.isUseCommandPrefix()) {
            return;
        }

        var prefix = channel.getCommandPrefix();
        if (prefix == null || prefix.isEmpty()) {
            return;
        }

        var command = new BungeePrefixCommand(core, platform, channel);
        getProxy().getPluginManager().registerCommand(this, command);
        registeredCommands.add(command);
    }

    public void unregisterCommands() {
        for (Command command : registeredCommands) {
            getProxy().getPluginManager().unregisterCommand(command);
        }

        registeredCommands.clear();
    }

    private void registerListeners() {
        getProxy().getPluginManager().registerListener(this, new BungeeChatListener(core, platform));
        getProxy().getPluginManager().registerListener(this, new BungeeSignedChatListener(core, platform));
    }
}
