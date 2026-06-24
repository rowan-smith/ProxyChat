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
        VelocitySignedChatHandler handler = new VelocitySignedChatHandler(new FakePlatform(), LOGGER);
        FakePlayer player = new FakePlayer("Alice", "lobby").withProtocolVersion(759);

        assertThat(handler.canInterceptChat(player)).isTrue();
    }

    @Test
    void requiresSignedVelocityOnModernProtocol() {
        FakePlatform platform = new FakePlatform();
        VelocitySignedChatHandler handler = new VelocitySignedChatHandler(platform, LOGGER);
        FakePlayer player = new FakePlayer("Alice", "lobby").withProtocolVersion(767);

        assertThat(handler.canInterceptChat(player)).isFalse();

        platform.installPlugin("signedvelocity");
        assertThat(handler.canInterceptChat(player)).isTrue();
    }
}
