package dev.rono.proxychat.common.platform;

public interface SignedChatHandler {
    /**
     * Called when a player joins the server.
     */
    void onPlayerJoin(ProxyPlayer player);

    /**
     * Called when a player leaves the server.
     */
    void onPlayerQuit(ProxyPlayer player);

    /**
     * Called after a signed chat message was cancelled for a proxy channel.
     */
    void acknowledgeCancelledChat(ProxyPlayer player);

    /**
     * @return true when the platform can safely cancel intercepted chat on this player
     */
    default boolean canInterceptChat(ProxyPlayer player) {
        return true;
    }
}
