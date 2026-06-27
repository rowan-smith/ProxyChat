package dev.rono.proxychat.velocity.listener;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;

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
        var platform = signedVelocityPlatform();
        var harness = TestEnvironment.create(dataDirectory, platform);
        var sender = platform.addPlayer(new FakePlayer("Rono", "lobby").withPermission("proxychat.global"));
        var sameServer = platform.addPlayer(new FakePlayer("Bob", "lobby").withPermission("proxychat.global"));

        // act
        var action = VelocityChatIntercept.decide(harness.core(), sender, "@hello");

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
        var platform = signedVelocityPlatform();
        var harness = TestEnvironment.create(dataDirectory, platform);
        var sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));

        // act
        var result =
                harness.core().getChannelService().tryVelocityPrefixedIntercept(sender, "@hello");

        assertThat(result).containsInstanceOf(VelocityPrefixInterceptResult.Delivered.class);
        assertThat(sender.receivedMessages()).noneMatch(message ->
                message.equals("@hello") || message.contains("<Alice> @hello"));
    }

    @Test
    void prefixedChatWithLeadingWhitespaceIsIntercepted() {

        // arrange
        var platform = signedVelocityPlatform();
        var harness = TestEnvironment.create(dataDirectory, platform);
        var sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));

        // act
        var action = VelocityChatIntercept.decide(harness.core(), sender, "  @leading");

        // assert
        assertThat(action).isEqualTo(VelocityChatIntercept.Action.DENY);
        assertThat(sender.receivedMessages()).anyMatch(message -> message.contains("leading"));
    }

    @Test
    void prefixedChatWithoutPermissionPassesThrough() {

        // arrange
        var platform = signedVelocityPlatform();
        var harness = TestEnvironment.create(dataDirectory, platform);

        // act
        var sender = platform.addPlayer(new FakePlayer("Alice", "lobby"));

        // assert
        assertThat(VelocityChatIntercept.decide(harness.core(), sender, "@hidden"))
                .isEqualTo(VelocityChatIntercept.Action.PASS);
        assertThat(sender.receivedMessages()).isEmpty();
    }

    @Test
    void cooldownBlocksSecondPrefixedMessageWithoutBroadcastingIt() {

        // arrange
        var platform = signedVelocityPlatform();
        var harness = TestEnvironment.create(dataDirectory, platform);
        var sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));
        var recipient = platform.addPlayer(new FakePlayer("Bob", "lobby").withPermission("proxychat.global"));
        recipient.receivedMessages().clear();

        // act
        VelocityChatIntercept.decide(harness.core(), sender, "@first");
        var action = VelocityChatIntercept.decide(harness.core(), sender, "@second");

        // assert
        assertThat(action).isEqualTo(VelocityChatIntercept.Action.DENY);
        assertThat(sender.receivedMessages()).anyMatch(message -> message.contains("cooldown"));
        assertThat(recipient.receivedMessages()).noneMatch(message -> message.contains("second"));
    }

    @Test
    void normalChatStillPassesThrough() {

        // arrange
        var platform = signedVelocityPlatform();
        var harness = TestEnvironment.create(dataDirectory, platform);

        // act
        var sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));

        // assert
        assertThat(VelocityChatIntercept.decide(harness.core(), sender, "hello"))
                .isEqualTo(VelocityChatIntercept.Action.PASS);
        assertThat(sender.receivedMessages()).isEmpty();
    }

    @Test
    void toggledPlainChatStillDeniesSignedMessage() {

        // arrange
        var platform = signedVelocityPlatform();
        var harness = TestEnvironment.create(dataDirectory, platform);
        var sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));

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
        var platform = new FakePlatform();
        platform.setSignedChatHandler(new RecordingSignedChatHandler().canIntercept(false));
        var harness = TestEnvironment.create(dataDirectory, platform);

        // act
        var sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));

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
        var platform = signedVelocityPlatform();
        platform.installPlugin("signedvelocity");

        // act
        var harness = TestEnvironment.create(dataDirectory, platform);

        // assert
        assertThat(SignedChatPolicy.shouldRegisterProxyPrefixCommand(harness.config(), platform)).isFalse();
    }

    @Test
    void proxyPrefixCommandIsRegisteredWithoutSignedVelocity() {

        // arrange
        var platform = new FakePlatform();

        // act
        var harness = TestEnvironment.create(dataDirectory, platform);

        // assert
        assertThat(SignedChatPolicy.shouldRegisterProxyPrefixCommand(harness.config(), platform)).isTrue();
    }

    @Test
    void proxyPrefixCommandIsRegisteredWhenInterceptionDisabledEvenWithSignedVelocity() {

        // arrange & act
        var platform = signedVelocityPlatform();

        // assert
        var root = new LinkedHashMap<String, Object>();
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
        var platform = new FakePlatform();
        platform.installPlugin("signedvelocity");
        platform.setSignedChatHandler(new RecordingSignedChatHandler().canIntercept(true));
        return platform;
    }
}
