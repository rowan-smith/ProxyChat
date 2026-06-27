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
    void interceptsWhenPolicyAllows() {

        // arrange
        var platform = new FakePlatform();
        var handler = new RecordingSignedChatHandler().canIntercept(true);
        platform.setSignedChatHandler(handler);
        var harness = TestEnvironment.create(dataDirectory, platform);
        platform.addPlayer(new FakePlayer("Bob", "lobby").withPermission("proxychat.global"));
        var alice = new FakePlayer("Alice", "lobby").withPermission("proxychat.global").withProtocolVersion(767);

        // act
        var intercepted = harness.core().getChannelService().tryInterceptChat(alice, "@velocity message");
        handler.acknowledgeCancelledChat(alice);

        // assert
        assertThat(intercepted).isTrue();
        assertThat(handler.acknowledgedPlayers()).containsExactly(alice.getUniqueId());
    }

    @Test
    void skipsInterceptionWhenPolicyBlocks() {

        // arrange
        var platform = new FakePlatform();
        var handler = new RecordingSignedChatHandler().canIntercept(false);
        platform.setSignedChatHandler(handler);
        var harness = TestEnvironment.create(dataDirectory, platform);
        var alice = new FakePlayer("Alice", "lobby")
                .withPermission("proxychat.global")
                .withProtocolVersion(767);

        // act
        var decision =
                VelocityChatIntercept.decide(harness.core(), alice, "@should-not-intercept");

        // assert
        assertThat(decision).isEqualTo(VelocityChatIntercept.Action.PASS);
        assertThat(handler.acknowledgedPlayers()).isEmpty();
    }

    @Test
    void velocityPrefixedInterceptDeniesSignedChat() {

        // arrange
        var platform = new FakePlatform();
        platform.installPlugin("signedvelocity");
        platform.setSignedChatHandler(new RecordingSignedChatHandler().canIntercept(true));
        var harness = TestEnvironment.create(dataDirectory, platform);
        var alice = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));

        // act
        var action = VelocityChatIntercept.decide(harness.core(), alice, "@rewrite me");

        // assert
        assertThat(action).isEqualTo(VelocityChatIntercept.Action.DENY);
        assertThat(alice.receivedMessages()).anyMatch(message -> message.contains("rewrite me"));
    }
}
