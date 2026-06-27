package dev.rono.proxychat.bungee.command;

import java.util.HashSet;

import net.kyori.adventure.text.event.ClickEvent;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import dev.rono.proxychat.bungee.platform.BungeePlatform;
import dev.rono.proxychat.common.ProxyChatCore;
import dev.rono.proxychat.common.command.ProxyChatAdminHelp;
import dev.rono.proxychat.common.message.MessageFormatter;

public final class BungeeAdminCommand extends Command implements TabExecutor {
    private static final String SPIGOT_URL = "https://www.spigotmc.org/resources/73583/";

    private final ProxyChatCore core;
    private final BungeePlatform platform;
    private final Runnable reloadCommands;

    public BungeeAdminCommand(ProxyChatCore core, BungeePlatform platform, Runnable reloadCommands) {
        super("proxychat", "", "pc");
        this.core = core;
        this.platform = platform;
        this.reloadCommands = reloadCommands;
    }

    public BungeeAdminCommand(ProxyChatCore core, Runnable reloadCommands) {
        this(core, (BungeePlatform) core.getPlatform(), reloadCommands);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            ProxyChatAdminHelp.send(core, platform.toSource(sender));
            return;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            var permission = core.getConfig().getConfig().getString("reload-permission");

            if (sender.hasPermission(permission)) {
                if (!core.reload()) {
                    platform.toSource(sender).sendMessage(MessageFormatter.legacy("&cProxyChat reload failed. Check the console for details."));
                    return;
                }

                reloadCommands.run();

                var prefix = core.getConfig().getConfig().getString("prefix");
                var message = core.getConfig().getConfig().getString("reload-message");

                core.getPlatform().sendMessage(platform.toSource(sender), MessageFormatter.legacy(prefix + message)
                );
            }

            return;
        }

        if (args[0].equalsIgnoreCase("version")) {
            var link = MessageFormatter.legacy("&1" + SPIGOT_URL).clickEvent(ClickEvent.openUrl(SPIGOT_URL));
            var message = MessageFormatter.legacy("&1Made by Rono @ ").append(link);
            core.getPlatform().sendMessage(platform.toSource(sender), message);
        }
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        var suggestions = new HashSet<String>();

        if (sender.hasPermission(core.getConfig().getConfig().getString("reload-permission"))) {
            suggestions.add("reload");
        }

        suggestions.add("version");

        return suggestions;
    }
}
