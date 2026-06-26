package dev.rono.proxychat.bungee.platform;

import dev.rono.proxychat.common.test.FakePlayer;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

class WaterfallSignedChatHandlerTest {
    @Test
    void allowsInterceptOnPreSignedChatProtocol() {
        WaterfallSignedChatHandler handler = new WaterfallSignedChatHandler(Logger.getLogger("test"));
        FakePlayer player = new FakePlayer("Alice", "lobby").withProtocolVersion(760);

        assertThat(handler.canInterceptChat(player)).isTrue();
    }

    @Test
    void disablesPlainChatInterceptOnModernProtocol() {
        WaterfallSignedChatHandler handler = new WaterfallSignedChatHandler(Logger.getLogger("test"));
        FakePlayer player = new FakePlayer("Alice", "lobby").withProtocolVersion(767);

        assertThat(handler.canInterceptChat(player)).isFalse();
    }
}
