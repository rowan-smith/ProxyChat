package dev.rono.bungeecordchat;

import com.google.common.io.ByteStreams;
import dev.rono.bungeecordchat.commands.ChatCommand;
import dev.rono.bungeecordchat.commands.ReloadCommand;
import dev.rono.bungeecordchat.listeners.PlayerChatEvent;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.*;
import java.util.ArrayList;

public final class BungeecordChat extends Plugin {

    private static BungeecordChat instance;
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

            String commandName = chatConfig.getString("command-name");
            String commandPermission = chatConfig.getString("permission");
            String commandAlias = chatConfig.getString("command-alias");

            ChatCommand command = new ChatCommand(chatConfig, commandName, commandPermission, commandAlias);
            instance.getProxy().getPluginManager().registerCommand(instance, command);
            commands.add(command);
        }

        instance.getLogger().info(commands.size() + " commands loaded.");
        instance.getProxy().getPluginManager().registerCommand(instance, new ReloadCommand());

    }

    public static void unregisterCommands() {
        for (ChatCommand command : commands) {
            instance.getProxy().getPluginManager().unregisterCommand(command);
        }

        commands.clear();

    }

    public static void registerConfiguration() {
        if (!instance.getDataFolder().exists()) {
            instance.getDataFolder().mkdir();
        }

        File resourceFile = new File(instance.getDataFolder(), "config.yml");

        try {
            if (!resourceFile.exists()) {
                resourceFile.createNewFile();
                try (InputStream in = instance.getResourceAsStream("config.yml");
                     OutputStream out = new FileOutputStream(resourceFile)) {
                    ByteStreams.copy(in, out);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            config =  ConfigurationProvider.getProvider(YamlConfiguration.class).load(resourceFile);
        } catch (IOException e) {
            instance.getProxy().getLogger().severe("Config file not found!");
            e.printStackTrace();
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
