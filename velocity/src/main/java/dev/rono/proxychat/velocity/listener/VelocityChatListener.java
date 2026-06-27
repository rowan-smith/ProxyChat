package dev.rono.proxychat.velocity.listener;

import com.velocitypowered.api.event.player.PlayerChatEvent;

import dev.rono.proxychat.common.ProxyChatCore;
import dev.rono.proxychat.velocity.platform.VelocityPlayer;

public final class VelocityChatListener {
    private final ProxyChatCore core;

    public VelocityChatListener(ProxyChatCore core) {
        this.core = core;
    }

    public void onPlayerChat(PlayerChatEvent event) {
        if (!event.getResult().isAllowed()) {
            return;
        }

        var player = new VelocityPlayer(event.getPlayer());
        if (VelocityChatIntercept.decide(core, player, event.getMessage()) == VelocityChatIntercept.Action.DENY) {
            denySignedChat(event);
            core.getSignedChatHandler().acknowledgeCancelledChat(player);
        }
    }

    /**
     * Requires SignedVelocity on 1.19.1+ clients; {@link PlayerChatEvent.ChatResult#denied()} is deprecated but
     * remains the supported proxy-side hook when SignedVelocity synchronizes cancellation with backends.
     */
    @SuppressWarnings("deprecation")
    private static void denySignedChat(PlayerChatEvent event) {
        event.setResult(PlayerChatEvent.ChatResult.denied());
    }
}
