package dev.rono.proxychat.velocity.platform;

import org.junit.jupiter.api.Test;

import dev.rono.proxychat.common.test.FakePlatform;
import dev.rono.proxychat.common.test.FakePlayer;

import static org.assertj.core.api.Assertions.assertThat;

class VelocitySignedChatHandlerTest {
    @Test
    void allowsInterceptOnLegacyProtocol() {

        // arrange
        VelocitySignedChatHandler handler = new VelocitySignedChatHandler(new FakePlatform());

        // act
        FakePlayer player = new FakePlayer("Alice", "lobby").withProtocolVersion(759);

        // assert
        assertThat(handler.canInterceptChat(player)).isTrue();
    }

    @Test
    void requiresSignedVelocityOnModernProtocol() {

        // arrange
        FakePlatform platform = new FakePlatform();
        VelocitySignedChatHandler handler = new VelocitySignedChatHandler(platform);
        FakePlayer player = new FakePlayer("Alice", "lobby").withProtocolVersion(767);

        // act
        boolean withoutSignedVelocity = handler.canInterceptChat(player);
        platform.installPlugin("signedvelocity");
        boolean withSignedVelocity = handler.canInterceptChat(player);

        // assert
        assertThat(withoutSignedVelocity).isFalse();
        assertThat(platform.warningLogs()).hasSize(1);
        assertThat(withSignedVelocity).isTrue();
    }
}
