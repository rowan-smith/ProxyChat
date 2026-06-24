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
        FakePlatform platform = new FakePlatform();
        RecordingSignedChatHandler handler = new RecordingSignedChatHandler();
        platform.setSignedChatHandler(handler);
        TestEnvironment.TestHarness harness = TestEnvironment.create(dataDirectory, platform);

        FakePlayer alice = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));
        FakePlayer bob = platform.addPlayer(new FakePlayer("Bob", "survival").withPermission("proxychat.global"));

        harness.core().getChannelService().execute(harness.globalChannel(), alice, new String[]{"Hi", "Bob"});

        assertThat(bob.receivedMessages()).anyMatch(message ->
                message.contains("Alice") && message.contains("lobby") && message.contains("Hi Bob")
        );
        assertThat(platform.infoLogs()).isNotEmpty();
    }

    @Test
    void prefixInterceptAcknowledgementFlow() throws Exception {
        FakePlatform platform = new FakePlatform();
        RecordingSignedChatHandler handler = new RecordingSignedChatHandler().canIntercept(true);
        platform.setSignedChatHandler(handler);
        TestEnvironment.TestHarness harness = TestEnvironment.create(dataDirectory, platform);

        FakePlayer alice = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));
        platform.addPlayer(new FakePlayer("Bob", "lobby").withPermission("proxychat.global"));

        boolean intercepted = harness.core().getChannelService().tryInterceptChat(alice, "@hello there");

        assertThat(intercepted).isTrue();
        handler.acknowledgeCancelledChat(alice);
        assertThat(handler.acknowledgedPlayers()).contains(alice.getUniqueId());
    }

    @Test
    void toggleModePlainChatWorkflow() throws Exception {
        FakePlatform platform = new FakePlatform();
        platform.setSignedChatHandler(new RecordingSignedChatHandler());
        TestEnvironment.TestHarness harness = TestEnvironment.create(dataDirectory, platform);
        ChatChannel global = harness.globalChannel();

        FakePlayer alice = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));
        FakePlayer bob = platform.addPlayer(new FakePlayer("Bob", "lobby").withPermission("proxychat.global"));

        harness.core().getChannelService().execute(global, alice, new String[]{"toggle"});
        assertThat(harness.core().getChannelService().tryInterceptChat(alice, "plain chat works")).isTrue();
        assertThat(bob.receivedMessages()).anyMatch(message -> message.contains("plain chat works"));
    }

    @Test
    void legacyConfigMigrationAndUse() throws Exception {
        FakePlatform platform = new FakePlatform();
        platform.setSignedChatHandler(new RecordingSignedChatHandler());
        TestEnvironment.TestHarness harness = TestEnvironment.createWithLegacyConfig(dataDirectory, platform);

        assertThat(harness.dataDirectory().resolve("chats").resolve("legacy.yml")).exists();

        ChatChannel legacy = harness.core().getChannels().stream()
                .filter(channel -> channel.getCommandName().equals("legacy"))
                .findFirst()
                .orElseThrow();

        FakeConsole console = new FakeConsole();
        platform.addPlayer(new FakePlayer("Bob", "lobby").withPermission("proxychat.legacy"));

        harness.core().getChannelService().execute(legacy, console, new String[]{"migrated"});

        assertThat(platform.getOnlinePlayers().stream()
                .map(player -> ((FakePlayer) player).receivedMessages())
                .anyMatch(messages -> messages.stream().anyMatch(message -> message.contains("migrated"))))
                .isTrue();
    }

    @Test
    void reloadClearsRuntimeToggleState() throws Exception {
        FakePlatform platform = new FakePlatform();
        platform.setSignedChatHandler(new RecordingSignedChatHandler());
        TestEnvironment.TestHarness harness = TestEnvironment.create(dataDirectory, platform);

        FakePlayer alice = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));
        harness.globalChannel().getToggleUtils().toggleChat(alice.getUniqueId());

        harness.core().reload();

        ChatChannel reloaded = harness.core().getChannels().stream()
                .filter(channel -> channel.getCommandName().equals("global"))
                .findFirst()
                .orElseThrow();

        assertThat(reloaded.getToggleUtils().isToggled(alice.getUniqueId())).isFalse();
    }
}
