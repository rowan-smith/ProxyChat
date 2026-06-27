package dev.rono.proxychat.bungee.platform;

import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import dev.rono.proxychat.common.test.FakePlayer;

import static org.assertj.core.api.Assertions.assertThat;

class WaterfallSignedChatHandlerTest {
    @Test
    void allowsInterceptOnPreSignedChatProtocol() {

        // arrange
        var handler = new WaterfallSignedChatHandler(Logger.getLogger("test"));

        // act
        var player = new FakePlayer("Alice", "lobby").withProtocolVersion(760);

        // assert
        assertThat(handler.canInterceptChat(player)).isTrue();
    }

    @Test
    void disablesPlainChatInterceptOnModernProtocol() {

        // arrange
        var handler = new WaterfallSignedChatHandler(Logger.getLogger("test"));

        // act
        var player = new FakePlayer("Alice", "lobby").withProtocolVersion(767);

        // assert
        assertThat(handler.canInterceptChat(player)).isFalse();
    }
}
