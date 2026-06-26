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

        VelocityPlayer player = new VelocityPlayer(event.getPlayer());
        if (VelocityChatIntercept.decide(core, player, event.getMessage()) == VelocityChatIntercept.Action.DENY) {
            event.setResult(PlayerChatEvent.ChatResult.denied());
            core.getSignedChatHandler().acknowledgeCancelledChat(player);
        }
    }
}
