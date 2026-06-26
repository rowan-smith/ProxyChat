package dev.rono.proxychat.velocity.listener;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.rono.proxychat.common.test.FakePlatform;
import dev.rono.proxychat.common.test.FakePlayer;
import dev.rono.proxychat.common.test.RecordingSignedChatHandler;
import dev.rono.proxychat.common.test.TestEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class VelocityChatListenerTest {
    @TempDir Path dataDirectory;

    @Test
    void interceptsWhenPolicyAllows() throws Exception {

        // arrange
        FakePlatform platform = new FakePlatform();
        RecordingSignedChatHandler handler = new RecordingSignedChatHandler().canIntercept(true);
        platform.setSignedChatHandler(handler);
        TestEnvironment.TestHarness harness = TestEnvironment.create(dataDirectory, platform);
        platform.addPlayer(new FakePlayer("Bob", "lobby").withPermission("proxychat.global"));
        FakePlayer alice = new FakePlayer("Alice", "lobby").withPermission("proxychat.global").withProtocolVersion(767);

        // act
        boolean intercepted = harness.core().getChannelService().tryInterceptChat(alice, "@velocity message");
        handler.acknowledgeCancelledChat(alice);

        // assert
        assertThat(intercepted).isTrue();
        assertThat(handler.acknowledgedPlayers()).containsExactly(alice.getUniqueId());
    }

    @Test
    void skipsInterceptionWhenPolicyBlocks() throws Exception {

        // arrange
        FakePlatform platform = new FakePlatform();
        RecordingSignedChatHandler handler = new RecordingSignedChatHandler().canIntercept(false);
        platform.setSignedChatHandler(handler);
        TestEnvironment.TestHarness harness = TestEnvironment.create(dataDirectory, platform);
        FakePlayer alice = new FakePlayer("Alice", "lobby")
                .withPermission("proxychat.global")
                .withProtocolVersion(767);

        // act
        VelocityChatIntercept.Action decision =
                VelocityChatIntercept.decide(harness.core(), alice, "@should-not-intercept");

        // assert
        assertThat(decision).isEqualTo(VelocityChatIntercept.Action.PASS);
        assertThat(handler.acknowledgedPlayers()).isEmpty();
    }

    @Test
    void velocityPrefixedInterceptDeniesSignedChat() throws Exception {

        // arrange
        FakePlatform platform = new FakePlatform();
        platform.installPlugin("signedvelocity");
        platform.setSignedChatHandler(new RecordingSignedChatHandler().canIntercept(true));
        TestEnvironment.TestHarness harness = TestEnvironment.create(dataDirectory, platform);
        FakePlayer alice = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));

        // act
        VelocityChatIntercept.Action action = VelocityChatIntercept.decide(harness.core(), alice, "@rewrite me");

        // assert
        assertThat(action).isEqualTo(VelocityChatIntercept.Action.DENY);
        assertThat(alice.receivedMessages()).anyMatch(message -> message.contains("rewrite me"));
    }
}
