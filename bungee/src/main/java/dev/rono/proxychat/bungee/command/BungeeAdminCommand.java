package dev.rono.proxychat.bungee.command;

import dev.rono.proxychat.bungee.BungeeProxyChatPlugin;
import dev.rono.proxychat.bungee.platform.BungeeCommandSource;
import dev.rono.proxychat.common.ProxyChatCore;
import dev.rono.proxychat.common.command.ProxyChatAdminHelp;
import dev.rono.proxychat.common.message.MessageFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.HashSet;
import java.util.Set;

public final class BungeeAdminCommand extends Command implements TabExecutor {
    private final String SPIGOT_URL = "https://www.spigotmc.org/resources/73583/";

    private final ProxyChatCore core;

    public BungeeAdminCommand(ProxyChatCore core) {
        super("proxychat", "", "pc");
        this.core = core;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            ProxyChatAdminHelp.send(core, new BungeeCommandSource(sender));
            return;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            String permission = core.getConfig().getConfig().getString("reload-permission");

            if (sender.hasPermission(permission)) {
                core.reload();

                BungeeProxyChatPlugin.getInstance().registerCommands();

                String prefix = core.getConfig().getConfig().getString("prefix");
                String message = core.getConfig().getConfig().getString("reload-message");

                core.getPlatform().sendMessage(new BungeeCommandSource(sender), MessageFormatter.legacy(prefix + message));
            }

            return;
        }

        if (args[0].equalsIgnoreCase("version")) {
            Component link = MessageFormatter.legacy("&1" + SPIGOT_URL).clickEvent(ClickEvent.openUrl(SPIGOT_URL));
            Component message = MessageFormatter.legacy("&1Made by Rono @ ").append(link);
            core.getPlatform().sendMessage(new BungeeCommandSource(sender), message);
        }
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        Set<String> suggestions = new HashSet<>();

        if (sender.hasPermission(core.getConfig().getConfig().getString("reload-permission"))) {
            suggestions.add("reload");
        }

        suggestions.add("version");

        return suggestions;
    }
}
