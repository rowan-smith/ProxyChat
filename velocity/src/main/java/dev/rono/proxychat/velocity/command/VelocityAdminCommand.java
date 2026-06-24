package dev.rono.proxychat.velocity.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import dev.rono.proxychat.common.ProxyChatCore;
import dev.rono.proxychat.common.command.ProxyChatAdminHelp;
import dev.rono.proxychat.common.message.MessageFormatter;
import dev.rono.proxychat.velocity.platform.VelocityCommandSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;

import java.util.ArrayList;
import java.util.List;

public final class VelocityAdminCommand implements SimpleCommand {
    private final ProxyChatCore core;

    public VelocityAdminCommand(ProxyChatCore core) {
        this.core = core;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length < 1) {
            ProxyChatAdminHelp.send(core, new VelocityCommandSource(source));
            return;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            String permission = core.getConfig().getConfig().getString("reload-permission");

            if (source.hasPermission(permission)) {
                core.reload();
                String prefix = core.getConfig().getConfig().getString("prefix");
                String message = core.getConfig().getConfig().getString("reload-message");
                core.getPlatform().sendMessage(new VelocityCommandSource(source), MessageFormatter.legacy(prefix + message));
            }

            return;
        }

        if (args[0].equalsIgnoreCase("version")) {
            Component link = MessageFormatter.legacy("&1https://www.spigotmc.org/resources/73583/")
                    .clickEvent(ClickEvent.openUrl("https://www.spigotmc.org/resources/73583/"));
            Component message = MessageFormatter.legacy("&1Made by Rono @ ").append(link);
            core.getPlatform().sendMessage(new VelocityCommandSource(source), message);
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        List<String> suggestions = new ArrayList<>();

        if (invocation.source().hasPermission(core.getConfig().getConfig().getString("reload-permission"))) {
            suggestions.add("reload");
        }

        suggestions.add("version");

        return suggestions;
    }
}
