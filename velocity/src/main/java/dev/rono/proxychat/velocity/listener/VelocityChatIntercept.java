package dev.rono.proxychat.velocity.listener;

import java.util.Optional;

import lombok.experimental.UtilityClass;

import dev.rono.proxychat.common.ProxyChatCore;
import dev.rono.proxychat.common.channel.VelocityPrefixInterceptResult;
import dev.rono.proxychat.common.platform.ProxyPlayer;
import dev.rono.proxychat.common.util.SignedChatPolicy;

/**
 * Chooses how Velocity should handle intercepted plain chat when SignedVelocity is present.
 */
@UtilityClass
public class VelocityChatIntercept {
    public enum Action {
        PASS,
        DENY
    }

    public static Action decide(ProxyChatCore core, ProxyPlayer player, String message) {
        if (!SignedChatPolicy.shouldInterceptChat(core.getConfig().getConfig(), core.getSignedChatHandler(), player)) {
            return Action.PASS;
        }

        Optional<VelocityPrefixInterceptResult> prefixResult =
                core.getChannelService().tryVelocityPrefixedIntercept(player, message);
        if (prefixResult.isPresent()) {
            return Action.DENY;
        }

        if (core.getChannelService().tryInterceptToggleOnly(player, message)) {
            return Action.DENY;
        }

        return Action.PASS;
    }
}
