package dev.rono.proxychat.velocity.listener;

import dev.rono.proxychat.common.test.FakePlatform;
import dev.rono.proxychat.common.test.FakePlayer;
import dev.rono.proxychat.common.test.RecordingSignedChatHandler;
import dev.rono.proxychat.common.test.TestEnvironment;
import dev.rono.proxychat.common.util.SignedChatPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class VelocityChatInterceptTest {
    @TempDir Path dataDirectory;

    @Test
    void deniesPrefixedPlainChatWhenSignedVelocityIsPresent() throws Exception {
        FakePlatform platform = new FakePlatform();
        platform.installPlugin("signedvelocity");
        platform.setSignedChatHandler(new RecordingSignedChatHandler().canIntercept(true));
        TestEnvironment.TestHarness harness = TestEnvironment.create(dataDirectory, platform);
        FakePlayer sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));
        FakePlayer sameServer = platform.addPlayer(new FakePlayer("Bob", "lobby").withPermission("proxychat.global"));
        platform.addPlayer(new FakePlayer("Carol", "survival").withPermission("proxychat.global"));

        VelocityChatIntercept.Action action = VelocityChatIntercept.decide(harness.core(), sender, "@hello");

        assertThat(action).isEqualTo(VelocityChatIntercept.Action.DENY);
        assertThat(sameServer.receivedMessages()).anyMatch(message -> message.contains("Alice") && message.contains("hello"));
        assertThat(platform.getOnlinePlayers().stream()
                .filter(player -> "survival".equals(player.getServerName()))
                .flatMap(player -> ((FakePlayer) player).receivedMessages().stream())
                .anyMatch(message -> message.contains("hello"))).isTrue();
    }

    @Test
    void deniesTogglePlainChatWhenSignedVelocityIsPresent() throws Exception {
        FakePlatform platform = new FakePlatform();
        platform.installPlugin("signedvelocity");
        platform.setSignedChatHandler(new RecordingSignedChatHandler().canIntercept(true));
        TestEnvironment.TestHarness harness = TestEnvironment.create(dataDirectory, platform);
        FakePlayer sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));
        harness.globalChannel().getToggleUtils().toggleChat(sender.getUniqueId());

        VelocityChatIntercept.Action action = VelocityChatIntercept.decide(harness.core(), sender, "toggle chat");

        assertThat(action).isEqualTo(VelocityChatIntercept.Action.DENY);
    }

    @Test
    void passesThroughNormalChat() throws Exception {
        FakePlatform platform = new FakePlatform();
        platform.installPlugin("signedvelocity");
        platform.setSignedChatHandler(new RecordingSignedChatHandler().canIntercept(true));
        TestEnvironment.TestHarness harness = TestEnvironment.create(dataDirectory, platform);
        FakePlayer sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));

        VelocityChatIntercept.Action action = VelocityChatIntercept.decide(harness.core(), sender, "hello");

        assertThat(action).isEqualTo(VelocityChatIntercept.Action.PASS);
    }

    @Test
    void deniesPrefixedPlainChatOnCooldown() throws Exception {
        FakePlatform platform = new FakePlatform();
        platform.installPlugin("signedvelocity");
        platform.setSignedChatHandler(new RecordingSignedChatHandler().canIntercept(true));
        TestEnvironment.TestHarness harness = TestEnvironment.create(dataDirectory, platform);
        FakePlayer sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));

        VelocityChatIntercept.decide(harness.core(), sender, "@first");
        VelocityChatIntercept.Action action = VelocityChatIntercept.decide(harness.core(), sender, "@second");

        assertThat(action).isEqualTo(VelocityChatIntercept.Action.DENY);
        assertThat(sender.receivedMessages()).anyMatch(message -> message.contains("cooldown"));
    }

    @Test
    void passesThroughWhenSignedVelocityIsMissing() throws Exception {
        FakePlatform platform = new FakePlatform();
        platform.setSignedChatHandler(new RecordingSignedChatHandler().canIntercept(false));
        TestEnvironment.TestHarness harness = TestEnvironment.create(dataDirectory, platform);
        FakePlayer sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));

        assertThat(SignedChatPolicy.shouldInterceptChat(harness.config(), harness.core().getSignedChatHandler(), sender)).isFalse();
        assertThat(VelocityChatIntercept.decide(harness.core(), sender, "@hello")).isEqualTo(VelocityChatIntercept.Action.PASS);
    }
}
