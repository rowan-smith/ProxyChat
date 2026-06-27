package dev.rono.proxychat.velocity.platform;

import java.util.function.Supplier;

import dev.rono.proxychat.common.platform.ProxyChatPlatform;
import dev.rono.proxychat.common.platform.ProxyPlayer;
import dev.rono.proxychat.common.platform.SignedChatHandler;
import dev.rono.proxychat.velocity.VelocityProxyChatPlugin;

/**
 * Velocity blocks cancelling signed chat unless SignedVelocity is installed on the proxy
 * and every backend server. Adventure components are used for outbound messages only.
 */
public final class VelocitySignedChatHandler implements SignedChatHandler {
    private static final int SIGNED_CHAT_PROTOCOL = 760;
    private static final String SIGNED_VELOCITY_ID = "signedvelocity";

    private final Supplier<ProxyChatPlatform> platform;
    private boolean warned;

    public VelocitySignedChatHandler(VelocityProxyChatPlugin plugin) {
        this(() -> plugin.getCore().getPlatform());
    }

    VelocitySignedChatHandler(ProxyChatPlatform platform) {
        this(() -> platform);
    }

    private VelocitySignedChatHandler(Supplier<ProxyChatPlatform> platform) {
        this.platform = platform;
    }

    @Override
    public void onPlayerJoin(ProxyPlayer player) {
        // Velocity does not expose chat-chain offsets to plugins.
    }

    @Override
    public void onPlayerQuit(ProxyPlayer player) {
        // No-op
    }

    @Override
    public void acknowledgeCancelledChat(ProxyPlayer player) {
        // SignedVelocity synchronizes denied chat with backends when installed.
    }

    @Override
    public boolean canInterceptChat(ProxyPlayer player) {
        if (player.getProtocolVersion() < SIGNED_CHAT_PROTOCOL) {
            return true;
        }

        if (platform.get().isPluginPresent(SIGNED_VELOCITY_ID)) {
            return true;
        }

        if (!warned) {
            warned = true;
            platform.get().logWarning(
                    "Signed chat interception on Velocity requires SignedVelocity on the proxy "
                            + "and all backend servers. Prefix/toggle chat will not intercept messages "
                            + "for 1.19.1+ clients until it is installed."
            );
        }

        return false;
    }
}
