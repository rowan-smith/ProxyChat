package dev.rono.proxychat.velocity.listener;

import dev.rono.proxychat.common.test.FakePlatform;
import dev.rono.proxychat.common.test.FakePlayer;
import dev.rono.proxychat.common.test.RecordingSignedChatHandler;
import dev.rono.proxychat.common.test.TestEnvironment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

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

        assertThat(VelocityChatIntercept.decide(harness.core(), alice, "@should-not-intercept"))
                .isEqualTo(VelocityChatIntercept.Action.PASS);
        assertThat(handler.acknowledgedPlayers()).isEmpty();
    }

    @Test
    void velocityPrefixedInterceptDeniesSignedChat() throws Exception {
        FakePlatform platform = new FakePlatform();
        platform.installPlugin("signedvelocity");
        platform.setSignedChatHandler(new RecordingSignedChatHandler().canIntercept(true));
        TestEnvironment.TestHarness harness = TestEnvironment.create(dataDirectory, platform);
        FakePlayer alice = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));

        VelocityChatIntercept.Action action = VelocityChatIntercept.decide(harness.core(), alice, "@rewrite me");

        assertThat(action).isEqualTo(VelocityChatIntercept.Action.DENY);
        assertThat(alice.receivedMessages()).anyMatch(message -> message.contains("rewrite me"));
    }
}
