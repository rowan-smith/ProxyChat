package dev.rono.proxychat.bungee;

import dev.rono.proxychat.bungee.command.BungeeAdminCommand;
import dev.rono.proxychat.bungee.command.BungeeChannelCommand;
import dev.rono.proxychat.bungee.command.BungeePrefixCommand;
import dev.rono.proxychat.bungee.listener.BungeeChatListener;
import dev.rono.proxychat.bungee.listener.BungeeSignedChatListener;
import dev.rono.proxychat.bungee.platform.BungeePlatform;
import dev.rono.proxychat.bungee.platform.WaterfallSignedChatHandler;
import dev.rono.proxychat.common.ProxyChatCore;
import dev.rono.proxychat.common.channel.ChatChannel;
import lombok.Getter;
import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class BungeeProxyChatPlugin extends Plugin {
    @Getter
    private static BungeeProxyChatPlugin instance;

    @Getter
    private ProxyChatCore core;

    @Getter
    private BungeeAudiences adventure;

    private final List<Command> registeredCommands = new ArrayList<>();

    @Override
    public void onEnable() {
        instance = this;

        Path dataDirectory = getDataFolder().toPath();
        BungeePlatform platform = new BungeePlatform(this);

        adventure = BungeeAudiences.create(this);

        core = new ProxyChatCore(getLogger(), platform, new WaterfallSignedChatHandler(this), dataDirectory);
        core.enable(getResourceAsStream("config.yml"), getResourceAsStream("global.yml"));

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
            BungeeChannelCommand command = new BungeeChannelCommand(core, channel);
            getProxy().getPluginManager().registerCommand(this, command);
            registeredCommands.add(command);
            registerPrefixCommand(channel);
        }

        getProxy().getPluginManager().registerCommand(this, new BungeeAdminCommand(core));
        getLogger().info(registeredCommands.size() + " channel commands loaded.");
    }

    private void registerPrefixCommand(ChatChannel channel) {
        if (!channel.isUseCommandPrefix()) {
            return;
        }

        String prefix = channel.getCommandPrefix();
        if (prefix == null || prefix.isEmpty()) {
            return;
        }

        BungeePrefixCommand command = new BungeePrefixCommand(core, channel);
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
        getProxy().getPluginManager().registerListener(this, new BungeeChatListener(core));
        getProxy().getPluginManager().registerListener(this, new BungeeSignedChatListener(core));
    }
}
