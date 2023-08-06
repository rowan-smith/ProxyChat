package dev.rono.proxychat;

import com.google.common.io.ByteStreams;
import dev.rono.proxychat.commands.ChatCommand;
import dev.rono.proxychat.commands.ProxyChatCommand;
import dev.rono.proxychat.listeners.PlayerChatEvent;
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

    private static ProxyChat instance;
    public static Configuration config;
    public static ArrayList<ChatCommand> commands = new ArrayList<>();

    @Override
    public void onEnable() {
        instance = this;
        registerConfiguration();
        registerCommands();
        registerListeners();
        new MetricsLite(this);
    }

    public static void registerCommands() {
        Configuration chatList = config.getSection("chats");

        for (String key : chatList.getKeys()) {
            Configuration chatConfig = config.getSection("chats." + key);

            ChatCommand command = new ChatCommand(chatConfig, instance);
            instance.getProxy().getPluginManager().registerCommand(instance, command);
            commands.add(command);
        }

        instance.getLogger().info(commands.size() + " commands loaded.");
        instance.getProxy().getPluginManager().registerCommand(instance, new ProxyChatCommand());

    }

    public static void unregisterCommands() {
        for (ChatCommand command : commands) {
            instance.getProxy().getPluginManager().unregisterCommand(command);
        }

        commands.clear();

    }

    public static void registerConfiguration() {
        if (instance.getDataFolder().mkdir()) {
            instance.getLogger().info("Created ProxyChat folder.");
        }

        File resourceFile = new File(instance.getDataFolder(), "config.yml");

        try {
            if (resourceFile.createNewFile()) {
                instance.getLogger().info("Creating new config.");

                try (InputStream in = instance.getResourceAsStream("config.yml");
                     OutputStream out = Files.newOutputStream(resourceFile.toPath())) {
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

    private static void registerListeners() {
        instance.getProxy().getPluginManager().registerListener(instance, new PlayerChatEvent());
    }

    public static TextComponent getConfigTextValue(String value) {
        String prefix = config.getString("prefix");
        String message = config.getString(value);
        return new TextComponent(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', prefix + message)));
    }
}
