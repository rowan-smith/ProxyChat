package dev.rono.proxychat.common;

import dev.rono.proxychat.common.channel.ChatChannel;
import dev.rono.proxychat.common.test.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end flows through {@link ProxyChatCore}, fake platform, and real YAML configs.
 */
class ProxyChatIT {
    @TempDir Path dataDirectory;

    @Test
    void fullGlobalChatWorkflow() throws Exception {

        // arrange
        FakePlatform platform = new FakePlatform();
        RecordingSignedChatHandler handler = new RecordingSignedChatHandler();
        platform.setSignedChatHandler(handler);
        TestEnvironment.TestHarness harness = TestEnvironment.create(dataDirectory, platform);
        FakePlayer alice = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));
        FakePlayer bob = platform.addPlayer(new FakePlayer("Bob", "survival").withPermission("proxychat.global"));

        // act
        harness.core().getChannelService().execute(harness.globalChannel(), alice, new String[]{"Hi", "Bob"});

        // assert
        assertThat(bob.receivedMessages()).anyMatch(message ->
                message.contains("Alice") && message.contains("lobby") && message.contains("Hi Bob")
        );
        assertThat(platform.infoLogs()).isNotEmpty();
    }

    @Test
    void prefixInterceptAcknowledgementFlow() throws Exception {

        // arrange
        FakePlatform platform = new FakePlatform();
        RecordingSignedChatHandler handler = new RecordingSignedChatHandler().canIntercept(true);
        platform.setSignedChatHandler(handler);
        TestEnvironment.TestHarness harness = TestEnvironment.create(dataDirectory, platform);
        FakePlayer alice = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));
        platform.addPlayer(new FakePlayer("Bob", "lobby").withPermission("proxychat.global"));

        // act
        boolean intercepted = harness.core().getChannelService().tryInterceptChat(alice, "@hello there");
        handler.acknowledgeCancelledChat(alice);

        // assert
        assertThat(intercepted).isTrue();
        assertThat(handler.acknowledgedPlayers()).contains(alice.getUniqueId());
    }

    @Test
    void toggleModePlainChatWorkflow() throws Exception {

        // arrange
        FakePlatform platform = new FakePlatform();
        platform.setSignedChatHandler(new RecordingSignedChatHandler());
        TestEnvironment.TestHarness harness = TestEnvironment.create(dataDirectory, platform);
        ChatChannel global = harness.globalChannel();
        FakePlayer alice = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));
        FakePlayer bob = platform.addPlayer(new FakePlayer("Bob", "lobby").withPermission("proxychat.global"));

        // act
        harness.core().getChannelService().execute(global, alice, new String[]{"toggle"});

        // assert
        assertThat(harness.core().getChannelService().tryInterceptChat(alice, "plain chat works")).isTrue();
        assertThat(bob.receivedMessages()).anyMatch(message -> message.contains("plain chat works"));
    }

    @Test
    void legacyConfigMigrationAndUse() throws Exception {

        // arrange
        FakePlatform platform = new FakePlatform();
        platform.setSignedChatHandler(new RecordingSignedChatHandler());
        TestEnvironment.TestHarness harness = TestEnvironment.createWithLegacyConfig(dataDirectory, platform);
        FakePlayer alice = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));
        FakePlayer bob = platform.addPlayer(new FakePlayer("Bob", "lobby").withPermission("proxychat.global"));

        // act
        ChatChannel global = harness.core().getChannels().stream()
                .filter(channel -> channel.getCommandName().equals("global"))
                .findFirst()
                .orElseThrow();
        harness.core().getChannelService().execute(global, alice, new String[]{"migrated"});

        // assert
        assertThat(harness.dataDirectory().resolve("chats").resolve("global.yml")).exists();
        assertThat(bob.receivedMessages()).anyMatch(message -> message.contains("migrated"));
    }

    @Test
    void reloadClearsRuntimeToggleState() throws Exception {

        // arrange
        FakePlatform platform = new FakePlatform();
        platform.setSignedChatHandler(new RecordingSignedChatHandler());
        TestEnvironment.TestHarness harness = TestEnvironment.create(dataDirectory, platform);
        FakePlayer alice = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));
        harness.globalChannel().getToggleUtils().toggleChat(alice.getUniqueId());

        // act
        harness.core().reload();
        ChatChannel reloaded = harness.core().getChannels().stream()
                .filter(channel -> channel.getCommandName().equals("global"))
                .findFirst()
                .orElseThrow();

        // assert
        assertThat(reloaded.getToggleUtils().isToggled(alice.getUniqueId())).isFalse();
    }
}
