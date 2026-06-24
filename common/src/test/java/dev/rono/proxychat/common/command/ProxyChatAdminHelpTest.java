package dev.rono.proxychat.common.command;

import dev.rono.proxychat.common.test.FakeConsole;
import dev.rono.proxychat.common.test.FakePlatform;
import dev.rono.proxychat.common.test.FakePlayer;
import dev.rono.proxychat.common.test.RecordingSignedChatHandler;
import dev.rono.proxychat.common.test.TestEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ProxyChatAdminHelpTest {
    @TempDir Path dataDirectory;

    private TestEnvironment.TestHarness harness;
    private FakePlatform platform;

    @BeforeEach
    void setUp() throws Exception {
        platform = new FakePlatform();
        harness = TestEnvironment.create(dataDirectory, platform);
    }

    @Test
    void listsAdminAndChannelCommandsForPlayers() {
        FakePlayer player = platform.addPlayer(new FakePlayer("Alice", "lobby")
                .withPermission("proxychat.global")
                .withPermission("proxychat.reload"));

        ProxyChatAdminHelp.send(harness.core(), player);

        assertThat(player.receivedMessages()).anyMatch(message -> message.contains("reload"));
        assertThat(player.receivedMessages()).anyMatch(message -> message.contains("version"));
        assertThat(player.receivedMessages()).anyMatch(message -> message.contains("/global"));
        assertThat(player.receivedMessages()).anyMatch(message -> message.contains("Global Chat"));
        assertThat(player.receivedMessages()).noneMatch(message -> message.contains("/local"));
    }

    @Test
    void hidesReloadWithoutPermission() {
        FakePlayer player = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));

        ProxyChatAdminHelp.send(harness.core(), player);

        assertThat(player.receivedMessages()).noneMatch(message -> message.contains("reload"));
        assertThat(player.receivedMessages()).anyMatch(message -> message.contains("version"));
    }

    @Test
    void omitsToggleFromHelpWhenSignedChatInterceptionUnavailable() throws Exception {
        FakePlatform blockedPlatform = new FakePlatform();
        blockedPlatform.setSignedChatHandler(new RecordingSignedChatHandler().canIntercept(false));
        TestEnvironment.TestHarness blockedHarness = TestEnvironment.create(dataDirectory.resolve("blocked-help"), blockedPlatform);
        FakePlayer player = blockedPlatform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));

        ProxyChatAdminHelp.send(blockedHarness.core(), player);

        assertThat(player.receivedMessages()).anyMatch(message -> message.contains("/global") && message.contains("ignore"));
        assertThat(player.receivedMessages()).noneMatch(message -> message.contains("toggle"));
    }

    @Test
    void listsAllPermittedChannelsForConsole() {
        FakeConsole console = new FakeConsole().withPermission("proxychat.global").withPermission("proxychat.local");

        ProxyChatAdminHelp.send(harness.core(), console);

        assertThat(console.receivedMessages()).anyMatch(message -> message.contains("/global"));
        assertThat(console.receivedMessages()).anyMatch(message -> message.contains("/local"));
    }
}
