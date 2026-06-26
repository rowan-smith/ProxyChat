package dev.rono.proxychat.velocity.listener;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.proxy.Player;
import dev.rono.proxychat.common.ProxyChatCore;
import dev.rono.proxychat.common.util.SignedChatPolicy;
import dev.rono.proxychat.velocity.platform.VelocityPlayer;

/**
 * Handles {@code /@prefix message} style input when clients send a prefix as a proxy command.
 */
public final class VelocityCommandInterceptListener {
    private final ProxyChatCore core;

    public VelocityCommandInterceptListener(ProxyChatCore core) {
        this.core = core;
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onCommandExecute(CommandExecuteEvent event) {
        if (!event.getResult().isAllowed() || !(event.getCommandSource() instanceof Player player)) {
            return;
        }

        VelocityPlayer proxyPlayer = new VelocityPlayer(player);
        if (!SignedChatPolicy.shouldInterceptChat(core.getConfig().getConfig(), core.getSignedChatHandler(), proxyPlayer)) {
            return;
        }

        if (core.getChannelService().tryInterceptPrefixedInput(proxyPlayer, event.getCommand())) {
            event.setResult(CommandExecuteEvent.CommandResult.denied());
            core.getSignedChatHandler().acknowledgeCancelledChat(proxyPlayer);
        }
    }
}
