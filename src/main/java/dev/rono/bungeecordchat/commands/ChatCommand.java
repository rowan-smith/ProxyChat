package dev.rono.bungeecordchat.commands;

import dev.rono.bungeecordchat.BungeecordChat;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.config.Configuration;

import java.util.ArrayList;

public class ChatCommand extends Command {

    private Configuration chatConfig;
    public ArrayList<ProxiedPlayer> toggledPlayers = new ArrayList<>();
    private ArrayList<ProxiedPlayer> ignoredPlayers = new ArrayList<>();

    public ChatCommand(Configuration chatConfig, String name, String permission, String... aliases) {
        super(name, permission, aliases);
        this.chatConfig = chatConfig;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(new TextComponent(ChatColor.RED + "You cannot use this command from console!"));
            return;
        }

        ProxiedPlayer proxiedPlayer = ((ProxiedPlayer) sender);

        if (args.length < 1) {
            proxiedPlayer.sendMessage(handleText(proxiedPlayer, chatConfig.getString("invalid-args"), args));
            return;
        }

        if (chatConfig.getBoolean("toggleable")) {
            if (args[0].equalsIgnoreCase("toggle")) {
                handleToggle(proxiedPlayer, args);
                return;
            }
        }

        if (chatConfig.getBoolean("ignorable")) {
            if (args[0].equalsIgnoreCase("ignore")) {
                handleIgnore(proxiedPlayer, args);
                return;
            }
        }

        TextComponent message;
        if (BungeecordChat.config.getBoolean("use-global-layout")) {
            message = handleText(proxiedPlayer, BungeecordChat.config.getString("global-layout"), args, true);
        } else {
            message = handleText(proxiedPlayer, chatConfig.getString("format"), args, true);
        }

        if (ignoredPlayers.contains(proxiedPlayer)) {
            proxiedPlayer.sendMessage(handleText(proxiedPlayer, BungeecordChat.config.getString("chat-disabled-message"), args));
            return;
        }

        for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
            if (player.hasPermission(getPermission()) || getPermission().isEmpty()) {
                if (!ignoredPlayers.contains(player))
                    player.sendMessage(message);
            }
        }
    }

    private void handleToggle(ProxiedPlayer proxiedPlayer, String[] args) {
        if (!toggledPlayers.contains(proxiedPlayer)) {
            toggledPlayers.add(proxiedPlayer);
            proxiedPlayer.sendMessage(handleText(proxiedPlayer, BungeecordChat.config.getString("toggle-enable-message"), args));
        } else {
            toggledPlayers.remove(proxiedPlayer);
            proxiedPlayer.sendMessage(handleText(proxiedPlayer, BungeecordChat.config.getString("toggle-disable-message"), args));
        }
    }

    private void handleIgnore(ProxiedPlayer proxiedPlayer, String[] args) {
        if (!ignoredPlayers.contains(proxiedPlayer)) {
            ignoredPlayers.add(proxiedPlayer);
            proxiedPlayer.sendMessage(handleText(proxiedPlayer, BungeecordChat.config.getString("ignore-enable-message"), args));
        } else {
            ignoredPlayers.remove(proxiedPlayer);
            proxiedPlayer.sendMessage(handleText(proxiedPlayer, BungeecordChat.config.getString("ignore-disable-message"), args));
        }
    }

    private TextComponent handleText(ProxiedPlayer proxiedPlayer, String message, String[] args, Boolean ignorePrefix) {
        if (!ignorePrefix)
            message = BungeecordChat.config.getString("prefix") + message;
        return getTextComponent(proxiedPlayer, message, args);
    }

    private TextComponent handleText(ProxiedPlayer proxiedPlayer, String message, String[] args) {
        message = BungeecordChat.config.getString("prefix") + message;
        return getTextComponent(proxiedPlayer, message, args);
    }

    private TextComponent getTextComponent(ProxiedPlayer proxiedPlayer, String message, String[] args) {
        message = message
                .replace("%player%", proxiedPlayer.getName())
                .replace("%prefix%", BungeecordChat.config.getString("prefix"))
                .replace("%server%", proxiedPlayer.getServer().getInfo().getName())
                .replace("%chat-prefix%", chatConfig.getString("chat-prefix"))
                .replace("%message%", String.join(" ", args))
                .replace("%alias%", chatConfig.getString("alias").toLowerCase())
                .replace("%chat%", chatConfig.getString("command-name"));
        return new TextComponent(ChatColor.translateAlternateColorCodes('&', message));
    }
}
