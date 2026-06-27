package dev.rono.proxychat.velocity.command;

import java.util.ArrayList;
import java.util.List;

import com.velocitypowered.api.command.SimpleCommand;

import net.kyori.adventure.text.event.ClickEvent;

import dev.rono.proxychat.common.ProxyChatCore;
import dev.rono.proxychat.common.command.ProxyChatAdminHelp;
import dev.rono.proxychat.common.message.MessageFormatter;
import dev.rono.proxychat.velocity.platform.VelocityCommandSource;

public final class VelocityAdminCommand implements SimpleCommand {
    private final ProxyChatCore core;
    private final Runnable reloadCommands;

    public VelocityAdminCommand(ProxyChatCore core, Runnable reloadCommands) {
        this.core = core;
        this.reloadCommands = reloadCommands;
    }

    @Override
    public void execute(Invocation invocation) {
        var source = invocation.source();
        var args = invocation.arguments();

        if (args.length < 1) {
            ProxyChatAdminHelp.send(core, new VelocityCommandSource(source));
            return;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            var permission = core.getConfig().getConfig().getString("reload-permission");

            if (source.hasPermission(permission)) {
                if (!core.reload()) {
                    core.getPlatform().sendMessage(
                            new VelocityCommandSource(source),
                            MessageFormatter.legacy("&cProxyChat reload failed. Check the console for details.")
                    );
                    return;
                }

                reloadCommands.run();

                var prefix = core.getConfig().getConfig().getString("prefix");
                var message = core.getConfig().getConfig().getString("reload-message");
                core.getPlatform().sendMessage(
                        new VelocityCommandSource(source),
                        MessageFormatter.legacy(prefix + message)
                );
            }

            return;
        }

        if (args[0].equalsIgnoreCase("version")) {
            var link = MessageFormatter.legacy("&1https://www.spigotmc.org/resources/73583/")
                    .clickEvent(ClickEvent.openUrl("https://www.spigotmc.org/resources/73583/"));
            var message = MessageFormatter.legacy("&1Made by Rono @ ").append(link);
            core.getPlatform().sendMessage(new VelocityCommandSource(source), message);
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        var suggestions = new ArrayList<String>();

        if (invocation.source().hasPermission(core.getConfig().getConfig().getString("reload-permission"))) {
            suggestions.add("reload");
        }

        suggestions.add("version");

        return suggestions;
    }
}
