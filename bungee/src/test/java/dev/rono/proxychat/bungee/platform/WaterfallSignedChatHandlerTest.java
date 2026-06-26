package dev.rono.proxychat.bungee.platform;

import dev.rono.proxychat.common.test.FakePlayer;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

class WaterfallSignedChatHandlerTest {
    @Test
    void allowsInterceptOnPreSignedChatProtocol() {

        // arrange
        WaterfallSignedChatHandler handler = new WaterfallSignedChatHandler(Logger.getLogger("test"));

        // act
        FakePlayer player = new FakePlayer("Alice", "lobby").withProtocolVersion(760);

        // assert
        assertThat(handler.canInterceptChat(player)).isTrue();
    }

    @Test
    void disablesPlainChatInterceptOnModernProtocol() {

        // arrange
        WaterfallSignedChatHandler handler = new WaterfallSignedChatHandler(Logger.getLogger("test"));

        // act
        FakePlayer player = new FakePlayer("Alice", "lobby").withProtocolVersion(767);

        // assert
        assertThat(handler.canInterceptChat(player)).isFalse();
    }
}
