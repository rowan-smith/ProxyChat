package dev.rono.proxychat.commands;

import dev.rono.proxychat.ProxyChat;
import dev.rono.proxychat.utils.CooldownRunnable;
import dev.rono.proxychat.utils.ToggleUtils;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.config.Configuration;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ChatCommand extends Command implements TabExecutor {

    private final ProxyChat instance;

    public Boolean useCommandPrefix;
    public String commandPrefix;

    public Boolean isToggleable;

    public Boolean isIgnorable;
    public Boolean isLocal;

    private final Integer commandDelay;
    private final String commandDelayOverridePermission;

    public String chatFormat;
    public String chatName;
    public String invalidArguments;

    public String consoleChatFormat;
    public Boolean isConsoleAllowed;
    private final Boolean isLoggedToConsole;

    private final String commandAlias;

    private final String useColorInChatPermission;

    public ToggleUtils toggleUtils = new ToggleUtils();

    public ChatCommand(Configuration chatConfig, ProxyChat plugin) {
        super(chatConfig.getString("command-name"),
                chatConfig.getString("permission"),
                chatConfig.getString("command-alias"));

        this.useCommandPrefix = chatConfig.getBoolean("use-command-prefix");
        this.commandPrefix = chatConfig.getString("command-prefix");

        this.isToggleable = chatConfig.getBoolean("toggleable");
        this.isIgnorable = chatConfig.getBoolean("ignorable");
        this.isLocal = chatConfig.getBoolean("local");

        this.chatName = chatConfig.getString("chat-name");
        this.chatFormat = chatConfig.getString("format");
        this.invalidArguments = chatConfig.getString("invalid-args");

        this.commandAlias = chatConfig.getString("command-alias");

        this.useColorInChatPermission = chatConfig.getString("use-color-in-chat-permission");

        this.commandDelay = chatConfig.getInt("command-delay");
        this.commandDelayOverridePermission = chatConfig.getString("command-delay-override-permission");

        this.consoleChatFormat = chatConfig.getString("console-format");
        this.isConsoleAllowed = chatConfig.getBoolean("console-chat-allowed");
        this.isLoggedToConsole = chatConfig.getBoolean("log-chat-to-console");

        this.instance = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            if (this.isConsoleAllowed) {
                String message = this.consoleChatFormat;
                message = message
                        .replace("%message%", String.join(" ", args))
                        .replace("%player%", sender.getName())
                        .replace("%command-name%", this.getName())
                        .replace("%command-alias%", this.commandAlias)
                        .replace("%command-prefix%", this.commandPrefix)
                        .replace("%chat-name%", this.chatName);
                sendAllMessage(new TextComponent(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', message))));
            } else {
                sender.sendMessage(new TextComponent(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', ProxyChat.getConfig().getString("console-disabled-message")))));
            }
            return;
        }

        ProxiedPlayer proxiedPlayer = ((ProxiedPlayer) sender);

        if (args.length < 1) {
            proxiedPlayer.sendMessage(handleText(proxiedPlayer, this.invalidArguments, args));
            return;
        }

        if (this.isToggleable)
            if (args[0].equalsIgnoreCase("toggle")) {
                if (this.toggleUtils.toggleChat(proxiedPlayer.getUniqueId()))
                    proxiedPlayer.sendMessage(handleText(proxiedPlayer, ProxyChat.getConfig().getString("toggle-enable-message"), args));
                else
                    proxiedPlayer.sendMessage(handleText(proxiedPlayer, ProxyChat.getConfig().getString("toggle-disable-message"), args));
                return;
            }

        if (this.isIgnorable)
            if (args[0].equalsIgnoreCase("ignore")) {
                if (this.toggleUtils.toggleIgnore(proxiedPlayer.getUniqueId()))
                    proxiedPlayer.sendMessage(handleText(proxiedPlayer, ProxyChat.getConfig().getString("ignore-enable-message"), args));
                else
                    proxiedPlayer.sendMessage(handleText(proxiedPlayer, ProxyChat.getConfig().getString("ignore-disable-message"), args));
                return;
            }

        if (this.toggleUtils.isDelayed(proxiedPlayer.getUniqueId())) {
            proxiedPlayer.sendMessage(handleText(proxiedPlayer, ProxyChat.getConfig().getString("command-cooldown-message"), args));
            return;
        }

        TextComponent message = handleText(proxiedPlayer, this.chatFormat, args, true);

        if (this.toggleUtils.isIgnored(proxiedPlayer.getUniqueId())) {
            proxiedPlayer.sendMessage(handleText(proxiedPlayer, ProxyChat.getConfig().getString("chat-disabled-message"), args));
            return;
        }

        if (!proxiedPlayer.hasPermission(this.commandDelayOverridePermission)) {
            ScheduledTask task = this.instance.getProxy().getScheduler().schedule(this.instance, (
                    new CooldownRunnable(proxiedPlayer, this.toggleUtils, Long.valueOf(this.commandDelay))
                    ), this.commandDelay, TimeUnit.MILLISECONDS);
            this.toggleUtils.toggleDelayOn(proxiedPlayer.getUniqueId(), task);
        }

        if (this.isLocal)
        	sendLocalMessage(proxiedPlayer, message);
        else
        	sendAllMessage(message);
    }

    private void sendAllMessage(TextComponent message) {
        for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
            if (player.hasPermission(getPermission()) || getPermission().isEmpty())
                if (!this.toggleUtils.isIgnored(player.getUniqueId())) {
                    player.sendMessage(message);
                }
        }
        if (this.isLoggedToConsole) {
            this.instance.getLogger().info(message.toLegacyText());
        }
    }
    
    private void sendLocalMessage(ProxiedPlayer p, TextComponent message) {
    	for (ProxiedPlayer player : p.getServer().getInfo().getPlayers())
    		if (player.hasPermission(getPermission()) || getPermission().isEmpty())
    			if (!this.toggleUtils.isIgnored(player.getUniqueId()))
    				player.sendMessage(message);
    	
    	if (this.isLoggedToConsole)
    		this.instance.getLogger().info(message.toLegacyText());
    }

    private TextComponent handleText(ProxiedPlayer proxiedPlayer, String message, String[] args, Boolean ignorePrefix) {
        if (!ignorePrefix)
            message = ProxyChat.getConfig().getString("prefix") + message;
        return getTextComponent(proxiedPlayer, message, args);
    }

    private TextComponent handleText(ProxiedPlayer proxiedPlayer, String message, String[] args) {
        message = ProxyChat.getConfig().getString("prefix") + message;
        return getTextComponent(proxiedPlayer, message, args);
    }

    private TextComponent getTextComponent(ProxiedPlayer proxiedPlayer, String message, String[] args) {
        message = message
                .replace("%player%", proxiedPlayer.getName())
                .replace("%prefix%", ProxyChat.getConfig().getString("prefix"))
                .replace("%server%", proxiedPlayer.getServer().getInfo().getName())
                .replace("%command-name%", this.getName())
                .replace("%command-alias%", this.commandAlias)
                .replace("%command-prefix%", this.commandPrefix)
                .replace("%chat-name%", this.chatName);

        if (this.toggleUtils.isDelayed(proxiedPlayer.getUniqueId())) {
            message = message.replace("%chat-cooldown%", this.toggleUtils.getDelayedTask(proxiedPlayer.getUniqueId()).getTime());
        }

        if (!proxiedPlayer.hasPermission(this.useColorInChatPermission))
            message = message.replace("%message%", ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', String.join(" ", args))));
        else
            message = message.replace("%message%", String.join(" ", args));

        return new TextComponent(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', message)));
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender commandSender, String[] strings) {
        Set<String> tabComplete = new HashSet<>();

        if (strings.length == 1) {
            if (isToggleable) {
                tabComplete.add("toggle");
            }

            if (isIgnorable) {
                tabComplete.add("ignore");
            }
        }

        return tabComplete;
    }
}
