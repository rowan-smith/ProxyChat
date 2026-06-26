package dev.rono.proxychat.common.command;

import dev.rono.proxychat.common.test.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

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

        // arrange
        FakePlayer player = platform.addPlayer(new FakePlayer("Alice", "lobby")
                .withPermission("proxychat.global")
                .withPermission("proxychat.reload"));
        List<String> messages = player.receivedMessages();

        // act
        ProxyChatAdminHelp.send(harness.core(), player);

        // assert
        assertThat(messages).anyMatch(message -> message.contains("reload"));
        assertThat(messages).anyMatch(message -> message.contains("version"));
        assertThat(messages).anyMatch(message -> message.contains("Chat channels:"));
        assertThat(messages).anyMatch(message -> message.contains("> Global"));
        assertThat(messages).anyMatch(message -> message.contains("- /g") && message.contains("toggle") && message.contains("ignore"));
        assertThat(messages).anyMatch(message -> message.contains("- /global"));
        assertThat(messages).noneMatch(message -> message.contains("/local"));
    }

    @Test
    void hidesReloadWithoutPermission() {

        // arrange
        FakePlayer player = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));

        // act
        ProxyChatAdminHelp.send(harness.core(), player);

        // assert
        assertThat(player.receivedMessages()).noneMatch(message -> message.contains("reload"));
        assertThat(player.receivedMessages()).anyMatch(message -> message.contains("version"));
    }

    @Test
    void omitsToggleFromHelpWhenSignedChatInterceptionUnavailable() throws Exception {

        // arrange
        FakePlatform blockedPlatform = new FakePlatform();
        blockedPlatform.setSignedChatHandler(new RecordingSignedChatHandler().canIntercept(false));
        TestEnvironment.TestHarness blockedHarness = TestEnvironment.create(dataDirectory.resolve("blocked-help"), blockedPlatform);
        FakePlayer player = blockedPlatform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));
        ProxyChatAdminHelp.send(blockedHarness.core(), player);

        // act
        List<String> messages = player.receivedMessages();

        // assert
        assertThat(messages).anyMatch(message -> message.contains("- /global") && message.contains("ignore"));
        assertThat(messages).noneMatch(message -> message.contains("toggle"));
    }

    @Test
    void showsPlainPrefixLineWhenSignedChatInterceptionIsAvailable() throws Exception {

        // arrange
        FakePlatform platform = new FakePlatform();
        platform.installPlugin("signedvelocity");
        platform.setSignedChatHandler(new RecordingSignedChatHandler().canIntercept(true));
        TestEnvironment.TestHarness harness = TestEnvironment.create(dataDirectory.resolve("signed-help"), platform);
        FakePlayer player = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));
        List<String> messages = player.receivedMessages();

        // act
        ProxyChatAdminHelp.send(harness.core(), player);

        // assert
        assertThat(messages).anyMatch(message -> message.contains("- @") && message.contains("<message>"));
        assertThat(messages).noneMatch(message -> message.contains("- /@"));
    }

    @Test
    void showsProxyPrefixCommandWhenSignedChatInterceptionIsUnavailable() throws Exception {

        // arrange
        FakePlatform platform = new FakePlatform();
        platform.setSignedChatHandler(new RecordingSignedChatHandler().canIntercept(false));
        TestEnvironment.TestHarness harness = TestEnvironment.create(dataDirectory.resolve("unsigned-help"), platform);
        FakePlayer player = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));

        // act
        ProxyChatAdminHelp.send(harness.core(), player);

        // assert
        assertThat(player.receivedMessages()).anyMatch(message -> message.contains("- /@"));
        assertThat(player.receivedMessages()).noneMatch(message -> message.contains("- @") && !message.contains("- /@"));
    }

    @Test
    void listsAllPermittedChannelsForConsole() {

        // arrange
        FakeConsole console = new FakeConsole().withPermission("proxychat.global").withPermission("proxychat.local");
        List<String> messages = console.receivedMessages();

        // act
        ProxyChatAdminHelp.send(harness.core(), console);

        // assert
        assertThat(messages).anyMatch(message -> message.contains("> Global"));
        assertThat(messages).anyMatch(message -> message.contains("- /global"));
        assertThat(messages).anyMatch(message -> message.contains("> Local"));
        assertThat(messages).anyMatch(message -> message.contains("- /local"));
    }

    @Test
    void omitsSubcommandsForChannelsWithoutToggleOrIgnore() throws Exception {

        // arrange
        FakePlatform platform = new FakePlatform();
        TestEnvironment.TestHarness harness = TestEnvironment.create(dataDirectory.resolve("staff-help"), platform);
        FakePlayer player = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.staff"));
        List<String> messages = player.receivedMessages();

        // act
        ProxyChatAdminHelp.send(harness.core(), player);

        // assert
        assertThat(messages).anyMatch(message -> message.contains("> Staff"));
        assertThat(messages).anyMatch(message -> message.contains("- /staff") && !message.contains("["));
    }
}
