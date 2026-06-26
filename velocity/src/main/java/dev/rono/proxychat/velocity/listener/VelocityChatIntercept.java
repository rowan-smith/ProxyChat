package dev.rono.proxychat.velocity.listener;

import dev.rono.proxychat.common.ProxyChatCore;
import dev.rono.proxychat.common.channel.VelocityPrefixInterceptResult;
import dev.rono.proxychat.common.platform.ProxyPlayer;
import dev.rono.proxychat.common.util.SignedChatPolicy;

import java.util.Optional;

/**
 * Chooses how Velocity should handle intercepted plain chat when SignedVelocity is present.
 */
public final class VelocityChatIntercept {
    public enum Action {
        PASS,
        DENY
    }

    private VelocityChatIntercept() {
    }

    public static Action decide(ProxyChatCore core, ProxyPlayer player, String message) {
        if (!SignedChatPolicy.shouldInterceptChat(core.getConfig().getConfig(), core.getSignedChatHandler(), player)) {
            return Action.PASS;
        }

        Optional<VelocityPrefixInterceptResult> prefixResult = core.getChannelService().tryVelocityPrefixedIntercept(player, message);
        if (prefixResult.isPresent()) {
            return Action.DENY;
        }

        if (core.getChannelService().tryInterceptToggleOnly(player, message)) {
            return Action.DENY;
        }

        return Action.PASS;
    }
}
