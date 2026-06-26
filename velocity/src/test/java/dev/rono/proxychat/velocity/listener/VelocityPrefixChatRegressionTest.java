package dev.rono.proxychat.velocity.listener;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.rono.proxychat.common.channel.VelocityPrefixInterceptResult;
import dev.rono.proxychat.common.config.ProxyChatYaml;
import dev.rono.proxychat.common.test.FakePlatform;
import dev.rono.proxychat.common.test.FakePlayer;
import dev.rono.proxychat.common.test.RecordingSignedChatHandler;
import dev.rono.proxychat.common.test.TestEnvironment;
import dev.rono.proxychat.common.util.SignedChatPolicy;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression tests for Velocity {@code @prefix} plain-chat interception with SignedVelocity.
 */
class VelocityPrefixChatRegressionTest {
    @TempDir Path dataDirectory;

    @Test
    void prefixedChatDeniesSignedMessageAndBroadcastsProxyFormattedLine() {

        // arrange
        FakePlatform platform = signedVelocityPlatform();
        TestEnvironment.TestHarness harness = TestEnvironment.create(dataDirectory, platform);
        FakePlayer sender = platform.addPlayer(new FakePlayer("Rono", "lobby").withPermission("proxychat.global"));
        FakePlayer sameServer = platform.addPlayer(new FakePlayer("Bob", "lobby").withPermission("proxychat.global"));

        // act
        VelocityChatIntercept.Action action = VelocityChatIntercept.decide(harness.core(), sender, "@hello");

        // assert
        assertThat(action).isEqualTo(VelocityChatIntercept.Action.DENY);
        assertThat(sender.receivedMessages()).anyMatch(message ->
                message.contains("[G]") && message.contains("Rono") && message.contains("hello"));
        assertThat(sender.receivedMessages()).noneMatch(message -> message.contains("<Rono>"));
        assertThat(sameServer.receivedMessages()).anyMatch(message -> message.contains("hello"));
    }

    @Test
    void prefixedChatDoesNotLeakVanillaInputWhenIntercepted() {

        // arrange
        FakePlatform platform = signedVelocityPlatform();
        TestEnvironment.TestHarness harness = TestEnvironment.create(dataDirectory, platform);
        FakePlayer sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));

        // act
        Optional<VelocityPrefixInterceptResult> result =
                harness.core().getChannelService().tryVelocityPrefixedIntercept(sender, "@hello");

        assertThat(result).containsInstanceOf(VelocityPrefixInterceptResult.Delivered.class);
        assertThat(sender.receivedMessages()).noneMatch(message ->
                message.equals("@hello") || message.contains("<Alice> @hello"));
    }

    @Test
    void prefixedChatWithLeadingWhitespaceIsIntercepted() {

        // arrange
        FakePlatform platform = signedVelocityPlatform();
        TestEnvironment.TestHarness harness = TestEnvironment.create(dataDirectory, platform);
        FakePlayer sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));

        // act
        VelocityChatIntercept.Action action = VelocityChatIntercept.decide(harness.core(), sender, "  @leading");

        // assert
        assertThat(action).isEqualTo(VelocityChatIntercept.Action.DENY);
        assertThat(sender.receivedMessages()).anyMatch(message -> message.contains("leading"));
    }

    @Test
    void prefixedChatWithoutPermissionPassesThrough() {

        // arrange
        FakePlatform platform = signedVelocityPlatform();
        TestEnvironment.TestHarness harness = TestEnvironment.create(dataDirectory, platform);

        // act
        FakePlayer sender = platform.addPlayer(new FakePlayer("Alice", "lobby"));

        // assert
        assertThat(VelocityChatIntercept.decide(harness.core(), sender, "@hidden"))
                .isEqualTo(VelocityChatIntercept.Action.PASS);
        assertThat(sender.receivedMessages()).isEmpty();
    }

    @Test
    void cooldownBlocksSecondPrefixedMessageWithoutBroadcastingIt() {

        // arrange
        FakePlatform platform = signedVelocityPlatform();
        TestEnvironment.TestHarness harness = TestEnvironment.create(dataDirectory, platform);
        FakePlayer sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));
        FakePlayer recipient = platform.addPlayer(new FakePlayer("Bob", "lobby").withPermission("proxychat.global"));
        recipient.receivedMessages().clear();

        // act
        VelocityChatIntercept.decide(harness.core(), sender, "@first");
        VelocityChatIntercept.Action action = VelocityChatIntercept.decide(harness.core(), sender, "@second");

        // assert
        assertThat(action).isEqualTo(VelocityChatIntercept.Action.DENY);
        assertThat(sender.receivedMessages()).anyMatch(message -> message.contains("cooldown"));
        assertThat(recipient.receivedMessages()).noneMatch(message -> message.contains("second"));
    }

    @Test
    void normalChatStillPassesThrough() {

        // arrange
        FakePlatform platform = signedVelocityPlatform();
        TestEnvironment.TestHarness harness = TestEnvironment.create(dataDirectory, platform);

        // act
        FakePlayer sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));

        // assert
        assertThat(VelocityChatIntercept.decide(harness.core(), sender, "hello"))
                .isEqualTo(VelocityChatIntercept.Action.PASS);
        assertThat(sender.receivedMessages()).isEmpty();
    }

    @Test
    void toggledPlainChatStillDeniesSignedMessage() {

        // arrange
        FakePlatform platform = signedVelocityPlatform();
        TestEnvironment.TestHarness harness = TestEnvironment.create(dataDirectory, platform);
        FakePlayer sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));

        // act
        harness.globalChannel().getToggleUtils().toggleChat(sender.getUniqueId());

        // assert
        assertThat(VelocityChatIntercept.decide(harness.core(), sender, "plain chat"))
                .isEqualTo(VelocityChatIntercept.Action.DENY);
        assertThat(sender.receivedMessages()).anyMatch(message -> message.contains("plain chat"));
    }

    @Test
    void withoutSignedVelocityPrefixedChatPassesThrough() {

        // arrange
        FakePlatform platform = new FakePlatform();
        platform.setSignedChatHandler(new RecordingSignedChatHandler().canIntercept(false));
        TestEnvironment.TestHarness harness = TestEnvironment.create(dataDirectory, platform);

        // act
        FakePlayer sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));

        // assert
        assertThat(SignedChatPolicy.shouldInterceptChat(
                harness.config(),
                harness.core().getSignedChatHandler(),
                sender
        )).isFalse();
        assertThat(VelocityChatIntercept.decide(harness.core(), sender, "@hello"))
                .isEqualTo(VelocityChatIntercept.Action.PASS);
    }

    @Test
    void proxyPrefixCommandIsNotRegisteredWhenSignedVelocityIsPresent() {

        // arrange
        FakePlatform platform = signedVelocityPlatform();
        platform.installPlugin("signedvelocity");

        // act
        TestEnvironment.TestHarness harness = TestEnvironment.create(dataDirectory, platform);

        // assert
        assertThat(SignedChatPolicy.shouldRegisterProxyPrefixCommand(harness.config(), platform)).isFalse();
    }

    @Test
    void proxyPrefixCommandIsRegisteredWithoutSignedVelocity() {

        // arrange
        FakePlatform platform = new FakePlatform();

        // act
        TestEnvironment.TestHarness harness = TestEnvironment.create(dataDirectory, platform);

        // assert
        assertThat(SignedChatPolicy.shouldRegisterProxyPrefixCommand(harness.config(), platform)).isTrue();
    }

    @Test
    void proxyPrefixCommandIsRegisteredWhenInterceptionDisabledEvenWithSignedVelocity() {

        // arrange & act
        FakePlatform platform = signedVelocityPlatform();

        // assert
        LinkedHashMap<String, Object> root = new LinkedHashMap<>();
        root.put("signed-chat-interception", "never");
        ProxyChatYaml config;
        try {
            config = ProxyChatYaml.fromMap(root);
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
        assertThat(SignedChatPolicy.shouldRegisterProxyPrefixCommand(config, platform)).isTrue();
    }

    private static FakePlatform signedVelocityPlatform() {
        FakePlatform platform = new FakePlatform();
        platform.installPlugin("signedvelocity");
        platform.setSignedChatHandler(new RecordingSignedChatHandler().canIntercept(true));
        return platform;
    }
}
