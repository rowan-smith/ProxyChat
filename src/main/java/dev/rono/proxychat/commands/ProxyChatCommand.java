package dev.rono.proxychat.commands;

import dev.rono.proxychat.ProxyChat;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.HashSet;
import java.util.Set;

public class ProxyChatCommand extends Command implements TabExecutor {

    public ProxyChatCommand() {
        super("proxychat", "", "pc");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {

        if (args.length < 1) {
            return;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (sender.hasPermission(ProxyChat.getConfig().getString("reload-permission"))) {
                ProxyChat.registerConfiguration();
                ProxyChat.unregisterCommands();
                ProxyChat.registerCommands();

                sender.sendMessage(ProxyChat.getConfigTextValue("reload-message"));
            }
            return;
        }

        if (args[0].equalsIgnoreCase("version")) {
            TextComponent message = new TextComponent(ChatColor.DARK_BLUE + "Made by Rono @ ");
            TextComponent link = new TextComponent("https://www.spigotmc.org/resources/73583/");
            link.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://www.spigotmc.org/resources/73583/"));
            message.addExtra(link);
            sender.sendMessage(message);
        }
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender commandSender, String[] strings) {

        Set<String> tabCommands = new HashSet<>();

        if (commandSender.hasPermission(ProxyChat.getConfig().getString("reload-permission"))) {
            tabCommands.add("reload");
        }

        tabCommands.add("version");

        return tabCommands;
    }
}
