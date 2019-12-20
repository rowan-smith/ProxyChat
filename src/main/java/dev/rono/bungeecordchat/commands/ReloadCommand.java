package dev.rono.bungeecordchat.commands;

import dev.rono.bungeecordchat.BungeecordChat;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

public class ReloadCommand extends Command {

    public ReloadCommand() {
        super(BungeecordChat.config.getString("reload.command"),
                BungeecordChat.config.getString("reload.permission"));
    }

    @Override
    public void execute(CommandSender sender, String[] args) {

        BungeecordChat.registerConfiguration();
        BungeecordChat.unregisterCommands();
        BungeecordChat.registerCommands();

        sender.sendMessage(BungeecordChat.getConfigTextValue("reload.reload-message"));
    }
}
