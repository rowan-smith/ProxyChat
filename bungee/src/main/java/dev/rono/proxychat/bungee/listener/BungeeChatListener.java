package dev.rono.proxychat.bungee.listener;

import dev.rono.proxychat.common.ProxyChatCore;
import dev.rono.proxychat.common.platform.ProxyPlayer;
import dev.rono.proxychat.common.util.SignedChatPolicy;
import dev.rono.proxychat.bungee.platform.BungeePlayer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public final class BungeeChatListener implements Listener {
    private final ProxyChatCore core;

    public BungeeChatListener(ProxyChatCore core) {
        this.core = core;
    }

    @EventHandler
    public void onChat(ChatEvent event) {
        if (event.isCancelled() || event.isCommand() || !(event.getSender() instanceof ProxiedPlayer handle)) {
            return;
        }

        ProxyPlayer player = new BungeePlayer(handle);
        if (!SignedChatPolicy.shouldInterceptChat(core.getConfig().getConfig(), core.getSignedChatHandler(), player)) {
            return;
        }

        if (core.getChannelService().tryInterceptChat(player, event.getMessage())) {
            event.setCancelled(true);
            core.getSignedChatHandler().acknowledgeCancelledChat(player);
        }
    }
}
