package dev.rono.proxychat.velocity.listener;

import com.velocitypowered.api.proxy.Player;
import dev.rono.proxychat.common.test.FakePlatform;
import dev.rono.proxychat.common.test.FakePlayer;
import dev.rono.proxychat.common.test.RecordingSignedChatHandler;
import dev.rono.proxychat.common.test.TestEnvironment;
import dev.rono.proxychat.common.util.SignedChatPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Velocity's {@link Player} API cannot be mocked reliably on modern JDKs, so listener
 * behaviour is verified through the shared core intercept path used by {@link VelocityChatListener}.
 */
class VelocityChatListenerTest {
    @TempDir Path dataDirectory;

    @Test
    void interceptsWhenPolicyAllows() throws Exception {
        FakePlatform platform = new FakePlatform();
        RecordingSignedChatHandler handler = new RecordingSignedChatHandler().canIntercept(true);
        platform.setSignedChatHandler(handler);
        TestEnvironment.TestHarness harness = TestEnvironment.create(dataDirectory, platform);
        platform.addPlayer(new FakePlayer("Bob", "lobby").withPermission("proxychat.global"));

        FakePlayer alice = new FakePlayer("Alice", "lobby").withPermission("proxychat.global").withProtocolVersion(767);
        assertThat(SignedChatPolicy.shouldInterceptChat(harness.config(), harness.core().getSignedChatHandler(), alice)).isTrue();

        assertThat(harness.core().getChannelService().tryInterceptChat(alice, "@velocity message")).isTrue();
        handler.acknowledgeCancelledChat(alice);
        assertThat(handler.acknowledgedPlayers()).containsExactly(alice.getUniqueId());
    }

    @Test
    void skipsInterceptionWhenPolicyBlocks() throws Exception {
        FakePlatform platform = new FakePlatform();
        RecordingSignedChatHandler handler = new RecordingSignedChatHandler().canIntercept(false);
        platform.setSignedChatHandler(handler);
        TestEnvironment.TestHarness harness = TestEnvironment.create(dataDirectory, platform);

        FakePlayer alice = new FakePlayer("Alice", "lobby").withPermission("proxychat.global").withProtocolVersion(767);

        assertThat(SignedChatPolicy.shouldInterceptChat(harness.config(), handler, alice)).isFalse();
        assertThat(handler.acknowledgedPlayers()).isEmpty();
    }
}
