package dev.rono.proxychat.common.channel;

/**
 * Result of matching a Velocity {@code @prefix} plain-chat message.
 */
public sealed interface VelocityPrefixInterceptResult {
    /**
     * Prefix matched and the formatted channel message was proxy-broadcast to all players.
     */
    record Delivered() implements VelocityPrefixInterceptResult { }

    /**
     * Prefix matched, but the message was rejected (cooldown, ignore, blacklist). The original
     * signed chat must still be denied so it does not leak to Paper.
     */
    record Blocked() implements VelocityPrefixInterceptResult { }
}
