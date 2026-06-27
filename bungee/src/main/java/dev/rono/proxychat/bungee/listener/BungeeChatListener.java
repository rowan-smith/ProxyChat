package dev.rono.proxychat.bungee.listener;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import dev.rono.proxychat.bungee.platform.BungeePlatform;
import dev.rono.proxychat.common.ProxyChatCore;
import dev.rono.proxychat.common.util.SignedChatPolicy;

public final class BungeeChatListener implements Listener {
    private final ProxyChatCore core;
    private final BungeePlatform platform;

    public BungeeChatListener(ProxyChatCore core, BungeePlatform platform) {
        this.core = core;
        this.platform = platform;
    }

    @EventHandler
    public void onChat(ChatEvent event) {
        if (event.isCancelled() || event.isCommand() || !(event.getSender() instanceof ProxiedPlayer handle)) {
            return;
        }

        var player = platform.toPlayer(handle);
        if (!SignedChatPolicy.shouldInterceptChat(core.getConfig().getConfig(), core.getSignedChatHandler(), player)) {
            return;
        }

        if (core.getChannelService().tryInterceptChat(player, event.getMessage())) {
            event.setCancelled(true);
            core.getSignedChatHandler().acknowledgeCancelledChat(player);
        }
    }
}
