package dev.rono.bungeecordchat.commands;

import dev.rono.bungeecordchat.BungeecordChat;
import dev.rono.bungeecordchat.utils.ToggleUtils;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import net.md_5.bungee.config.Configuration;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ChatCommand extends Command implements TabExecutor {

    public Boolean useCommandPrefix;
    public String commandPrefix;

    public Boolean isToggleable;

    public Boolean isIgnorable;

    private Integer commandDelay;
    private String commandDelayOverridePermission;

    public String chatFormat;
    public String chatName;
    public String invalidArguments;

    private String commandAlias;

    private String useColorInChatPermission;

    public ToggleUtils toggleUtils = new ToggleUtils();

    public ChatCommand(Configuration chatConfig) {
        super(chatConfig.getString("command-name"),
                chatConfig.getString("permission"),
                chatConfig.getString("command-alias"));

        this.useCommandPrefix = chatConfig.getBoolean("use-command-prefix");
        this.commandPrefix = chatConfig.getString("command-prefix");

        this.isToggleable = chatConfig.getBoolean("toggleable");
        this.isIgnorable = chatConfig.getBoolean("ignorable");

        this.chatName = chatConfig.getString("chat-name");
        this.chatFormat = chatConfig.getString("format");
        this.invalidArguments = chatConfig.getString("invalid-args");

        this.commandAlias = chatConfig.getString("command-alias");

        this.useColorInChatPermission = chatConfig.getString("use-color-in-chat-permission");

        this.commandDelay = chatConfig.getInt("command-delay");
        this.commandDelayOverridePermission = chatConfig.getString("command-delay-override-permission");
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

        if (this.isToggleable)
            if (args[0].equalsIgnoreCase("toggle")) {
                if (this.toggleUtils.toggleChat(proxiedPlayer))
                    proxiedPlayer.sendMessage(handleText(proxiedPlayer, BungeecordChat.config.getString("toggle-enable-message"), args));
                else
                    proxiedPlayer.sendMessage(handleText(proxiedPlayer, BungeecordChat.config.getString("toggle-disable-message"), args));
                return;
            }

        if (this.isIgnorable)
            if (args[0].equalsIgnoreCase("ignore")) {
                if (this.toggleUtils.toggleIgnore(proxiedPlayer))
                    proxiedPlayer.sendMessage(handleText(proxiedPlayer, BungeecordChat.config.getString("ignore-enable-message"), args));
                else
                    proxiedPlayer.sendMessage(handleText(proxiedPlayer, BungeecordChat.config.getString("ignore-disable-message"), args));
                return;
            }

        if (this.toggleUtils.isDelayed(proxiedPlayer)) {
            proxiedPlayer.sendMessage(handleText(proxiedPlayer, BungeecordChat.config.getString("command-cooldown-message"), args));
            return;
        }

        TextComponent message = handleText(proxiedPlayer, this.chatFormat, args, true);

        if (this.toggleUtils.isIgnored(proxiedPlayer)) {
            proxiedPlayer.sendMessage(handleText(proxiedPlayer, BungeecordChat.config.getString("chat-disabled-message"), args));
            return;
        }

        if (!proxiedPlayer.hasPermission(this.commandDelayOverridePermission)) {
            this.toggleUtils.toggleDelay(proxiedPlayer);
            BungeecordChat.instance.getProxy().getScheduler().schedule(BungeecordChat.instance, () -> this.toggleUtils.toggleDelay(proxiedPlayer), this.commandDelay, TimeUnit.MILLISECONDS);
        }

        for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
            if (player.hasPermission(getPermission()) || getPermission().isEmpty())
                if (!this.toggleUtils.isIgnored(player))
                    player.sendMessage(message);
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
