package dev.rono.bungeechat.commands;

import dev.rono.bungeechat.BungeeChat;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

public class ReloadCommand extends Command {

    public ReloadCommand() {
        super(BungeeChat.config.getString("reload.command"),
                BungeeChat.config.getString("reload.permission"));
    }

    @Override
    public void execute(CommandSender sender, String[] args) {

        BungeeChat.registerConfiguration();
        BungeeChat.unregisterCommands();
        BungeeChat.registerCommands();

        sender.sendMessage(BungeeChat.getConfigTextValue("reload.reload-message"));
    }
}
