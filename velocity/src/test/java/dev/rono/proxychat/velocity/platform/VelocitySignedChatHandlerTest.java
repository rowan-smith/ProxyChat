package dev.rono.proxychat.velocity.platform;

import dev.rono.proxychat.common.test.FakePlatform;
import dev.rono.proxychat.common.test.FakePlayer;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class VelocitySignedChatHandlerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(VelocitySignedChatHandlerTest.class);

    @Test
    void allowsInterceptOnLegacyProtocol() {

        // arrange
        VelocitySignedChatHandler handler = new VelocitySignedChatHandler(new FakePlatform(), LOGGER);

        // act
        FakePlayer player = new FakePlayer("Alice", "lobby").withProtocolVersion(759);

        // assert
        assertThat(handler.canInterceptChat(player)).isTrue();
    }

    @Test
    void requiresSignedVelocityOnModernProtocol() {

        // arrange
        FakePlatform platform = new FakePlatform();
        VelocitySignedChatHandler handler = new VelocitySignedChatHandler(platform, LOGGER);
        FakePlayer player = new FakePlayer("Alice", "lobby").withProtocolVersion(767);

        // act
        boolean withoutSignedVelocity = handler.canInterceptChat(player);
        platform.installPlugin("signedvelocity");
        boolean withSignedVelocity = handler.canInterceptChat(player);

        // assert
        assertThat(withoutSignedVelocity).isFalse();
        assertThat(withSignedVelocity).isTrue();
    }
}
