package dev.rono.proxychat.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import dev.rono.proxychat.common.ProxyChatCore;
import dev.rono.proxychat.common.util.SignedChatPolicy;
import dev.rono.proxychat.velocity.platform.VelocityPlayer;

public final class VelocityChatListener {
    private final ProxyChatCore core;

    public VelocityChatListener(ProxyChatCore core) {
        this.core = core;
    }

    @Subscribe
    public void onPlayerChat(PlayerChatEvent event) {
        VelocityPlayer player = new VelocityPlayer(event.getPlayer());

        if (!SignedChatPolicy.shouldInterceptChat(core.getConfig().getConfig(), core.getSignedChatHandler(), player)) {
            return;
        }

        if (core.getChannelService().tryInterceptChat(player, event.getMessage())) {
            event.setResult(PlayerChatEvent.ChatResult.denied());
            core.getSignedChatHandler().acknowledgeCancelledChat(player);
        }
    }
}
