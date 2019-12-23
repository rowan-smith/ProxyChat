package dev.rono.bungeecordchat.commands;

import dev.rono.bungeecordchat.BungeecordChat;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import net.md_5.bungee.config.Configuration;

import java.util.ArrayList;

public class ChatCommand extends Command implements TabExecutor {

    public Boolean useCommandPrefix;
    public String commandPrefix;

    public Boolean toggleable;
    public ArrayList<ProxiedPlayer> toggledPlayers = new ArrayList<>();

    public Boolean ignorable;
    private ArrayList<ProxiedPlayer> ignoredPlayers = new ArrayList<>();

    public  String chatFormat;
    public String chatName;
    public String invalidArguments;

    private String commandAlias;

    private String useColorInChatPermission;

    public ChatCommand(Configuration chatConfig) {
        super(chatConfig.getString("command-name"),
                chatConfig.getString("permission"),
                chatConfig.getString("command-alias"));

        this.useCommandPrefix = chatConfig.getBoolean("use-command-prefix");
        this.commandPrefix = chatConfig.getString("command-prefix");

        this.toggleable = chatConfig.getBoolean("toggleable");
        this.ignorable = chatConfig.getBoolean("ignorable");

        this.chatName = chatConfig.getString("chat-name");
        this.chatFormat = chatConfig.getString("format");
        this.invalidArguments = chatConfig.getString("invalid-args");

        this.commandAlias = chatConfig.getString("command-alias");

        this.useColorInChatPermission = chatConfig.getString("use-color-in-chat-permission");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(new TextComponent(ChatColor.RED + "You cannot use this command from console!"));
            return;
        }

        ProxiedPlayer proxiedPlayer = ((ProxiedPlayer) sender);

        if (args.length < 1) {
            proxiedPlayer.sendMessage(handleText(proxiedPlayer, this.invalidArguments, args));
            return;
        }

        if (this.toggleable) {
            if (args[0].equalsIgnoreCase("toggle")) {
                handleToggle(proxiedPlayer, args);
                return;
            }
        }

        if (this.ignorable) {
            if (args[0].equalsIgnoreCase("ignore")) {
                handleIgnore(proxiedPlayer, args);
                return;
            }
        }

        TextComponent message;
        if (BungeecordChat.config.getBoolean("use-global-layout")) {
            message = handleText(proxiedPlayer, BungeecordChat.config.getString("global-layout"), args, true);
        } else {
            message = handleText(proxiedPlayer, this.chatFormat, args, true);
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
                .replace("%command-name%", this.getName())
                .replace("%command-alias%", this.commandAlias)
                .replace("%command-prefix%", this.commandPrefix)
                .replace("%chat-name%", this.chatName);

        if (!proxiedPlayer.hasPermission(this.useColorInChatPermission))
            message = message.replace("%message%", ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', String.join(" ", args))));
        else
            message = message.replace("%message%", String.join(" ", args));

        return new TextComponent(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', message)));
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender commandSender, String[] strings) {
        ArrayList<String> tabComplete = new ArrayList<>();

        if (strings.length == 1) {
            if (toggleable) {
                tabComplete.add("toggle");
            }

            if (ignorable) {
                tabComplete.add("ignore");
            }
        }

        return tabComplete;
    }
}
