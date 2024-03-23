package dev.rono.proxychat;

import com.google.common.io.ByteStreams;
import dev.rono.proxychat.commands.ChatCommand;
import dev.rono.proxychat.commands.ProxyChatCommand;
import dev.rono.proxychat.listeners.PlayerChatEvent;
import dev.rono.proxychat.utils.Helpers;
import lombok.Getter;
import lombok.var;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.logging.Level;

public final class ProxyChat extends Plugin {

    @Getter private static ProxyChat instance;
    @Getter private static Configuration config;
    @Getter private static final ArrayList<ChatCommand> commands = new ArrayList<>();
    private static final ArrayList<Configuration> chats = new ArrayList<>();

    @Override
    public void onEnable() {
        instance = this;
        registerConfiguration();
        registerChatFolder();
        Helpers.migrateConfigChatToFolder();

        if (ProxyChat.getConfig().contains("chats")) {
            ProxyChat.getConfig().set("chats", null);
            try {
                ConfigurationProvider.getProvider(YamlConfiguration.class).save(ProxyChat.getConfig(), new File(getDataFolder(), "config.yml"));
            } catch (IOException ignore) {
            }
        }

        getChats();
        registerListeners();
        registerCommands();
        new MetricsLite(this);
    }

    public static void registerCommands() {
        for (var chat : chats) {
            var command = new ChatCommand(chat);
            instance.getProxy().getPluginManager().registerCommand(instance, command);
            commands.add(command);
        }

        instance.getLogger().info(commands.size() + " commands loaded.");
        instance.getProxy().getPluginManager().registerCommand(instance, new ProxyChatCommand());
    }

    public static void unregisterCommands() {
        for (var command : commands) {
            instance.getProxy().getPluginManager().unregisterCommand(command);
        }

        commands.clear();
    }

    public static void registerConfiguration() {
        if (instance.getDataFolder().mkdir()) {
            instance.getLogger().info("Created ProxyChat folder.");
        }

        var resourceFile = new File(instance.getDataFolder(), "config.yml");

        try {
            if (resourceFile.createNewFile()) {
                instance.getLogger().info("Creating new config.");

                try (var in = instance.getResourceAsStream("config.yml");
                     var out = Files.newOutputStream(resourceFile.toPath())) {
                    ByteStreams.copy(in, out);
                }
            }
        } catch (Exception e) {
            instance.getLogger().log(Level.SEVERE, "Config file could not be created.", e);
        }

        try {
            config =  ConfigurationProvider.getProvider(YamlConfiguration.class).load(resourceFile);
        } catch (IOException e) {
            instance.getLogger().log(Level.SEVERE, "Config file not found!", e);
        }
    }

    public static void registerChatFolder() {
        var chatsFolder = new File(instance.getDataFolder(), "chats");
        if (chatsFolder.mkdir()) {
            instance.getLogger().info("Created chats folder.");

            var resourceFile = new File(chatsFolder, "global.yml");

            try {
                if (resourceFile.createNewFile()) {
                    instance.getLogger().info("Creating example chat.");

                    try (var in = instance.getResourceAsStream("global.yml");
                         var out = Files.newOutputStream(resourceFile.toPath())) {
                        ByteStreams.copy(in, out);
                    }
                }
            } catch (Exception e) {
                instance.getLogger().log(Level.SEVERE, "Example chat could not be created.", e);
            }
        }
    }

    public static void getChats() {
        var chatsFolder = new File(instance.getDataFolder(), "chats");
        var chatArray = chatsFolder.listFiles((dir, name) -> name.endsWith(".yml"));

        assert chatArray != null;
        for (var chatFile : chatArray) {
            var chat = Helpers.loadYmlFile(chatFile);
            if (chat != null) {
                chats.add(chat);
            }
        }
    }

    private static void registerListeners() {
        instance.getProxy().getPluginManager().registerListener(instance, new PlayerChatEvent());
    }

    public static TextComponent getConfigTextValue(String value) {
        var prefix = config.getString("prefix");
        var message = config.getString(value);
        return new TextComponent(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', prefix + message)));
    }
}
