package dev.rono.proxychat.common.command;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.rono.proxychat.common.test.FakeConsole;
import dev.rono.proxychat.common.test.FakePlatform;
import dev.rono.proxychat.common.test.FakePlayer;
import dev.rono.proxychat.common.test.RecordingSignedChatHandler;
import dev.rono.proxychat.common.test.TestEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class ProxyChatAdminHelpTest {
    @TempDir Path dataDirectory;

    private TestEnvironment.TestHarness harness;
    private FakePlatform platform;

    @BeforeEach
    void setUp() {
        platform = new FakePlatform();
        harness = TestEnvironment.create(dataDirectory, platform);
    }

    @Test
    void listsAdminAndChannelCommandsForPlayers() {

        // arrange
        var player = platform.addPlayer(new FakePlayer("Alice", "lobby")
                .withPermission("proxychat.global")
                .withPermission("proxychat.reload"));
        var messages = player.receivedMessages();

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
        var player = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));

        // act
        ProxyChatAdminHelp.send(harness.core(), player);

        // assert
        assertThat(player.receivedMessages()).noneMatch(message -> message.contains("reload"));
        assertThat(player.receivedMessages()).anyMatch(message -> message.contains("version"));
    }

    @Test
    void omitsToggleFromHelpWhenSignedChatInterceptionUnavailable() {

        // arrange
        var blockedPlatform = new FakePlatform();
        blockedPlatform.setSignedChatHandler(new RecordingSignedChatHandler().canIntercept(false));
        var blockedHarness = TestEnvironment.create(
                dataDirectory.resolve("blocked-help"),
                blockedPlatform
        );
        var player = blockedPlatform.addPlayer(
                new FakePlayer("Alice", "lobby").withPermission("proxychat.global")
        );
        ProxyChatAdminHelp.send(blockedHarness.core(), player);

        // act
        var messages = player.receivedMessages();

        // assert
        assertThat(messages).anyMatch(message -> message.contains("- /global") && message.contains("ignore"));
        assertThat(messages).noneMatch(message -> message.contains("toggle"));
    }

    @Test
    void showsPlainPrefixLineWhenSignedChatInterceptionIsAvailable() {

        // arrange
        var platform = new FakePlatform();
        platform.installPlugin("signedvelocity");
        platform.setSignedChatHandler(new RecordingSignedChatHandler().canIntercept(true));
        var harness = TestEnvironment.create(dataDirectory.resolve("signed-help"), platform);
        var player = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));
        var messages = player.receivedMessages();

        // act
        ProxyChatAdminHelp.send(harness.core(), player);

        // assert
        assertThat(messages).anyMatch(message -> message.contains("- @") && message.contains("<message>"));
        assertThat(messages).noneMatch(message -> message.contains("- /@"));
    }

    @Test
    void showsProxyPrefixCommandWhenSignedChatInterceptionIsUnavailable() {

        // arrange
        var platform = new FakePlatform();
        platform.setSignedChatHandler(new RecordingSignedChatHandler().canIntercept(false));
        var harness = TestEnvironment.create(dataDirectory.resolve("unsigned-help"), platform);
        var player = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));

        // act
        ProxyChatAdminHelp.send(harness.core(), player);

        // assert
        assertThat(player.receivedMessages()).anyMatch(message -> message.contains("- /@"));
        assertThat(player.receivedMessages())
                .noneMatch(message -> message.contains("- @") && !message.contains("- /@"));
    }

    @Test
    void listsAllPermittedChannelsForConsole() {

        // arrange
        var console = new FakeConsole().withPermission("proxychat.global").withPermission("proxychat.local");
        var messages = console.receivedMessages();

        // act
        ProxyChatAdminHelp.send(harness.core(), console);

        // assert
        assertThat(messages).anyMatch(message -> message.contains("> Global"));
        assertThat(messages).anyMatch(message -> message.contains("- /global"));
        assertThat(messages).anyMatch(message -> message.contains("> Local"));
        assertThat(messages).anyMatch(message -> message.contains("- /local"));
    }

    @Test
    void omitsSubcommandsForChannelsWithoutToggleOrIgnore() {

        // arrange
        var platform = new FakePlatform();
        var harness = TestEnvironment.create(dataDirectory.resolve("staff-help"), platform);
        var player = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.staff"));
        var messages = player.receivedMessages();

        // act
        ProxyChatAdminHelp.send(harness.core(), player);

        // assert
        assertThat(messages).anyMatch(message -> message.contains("> Staff"));
        assertThat(messages).anyMatch(message -> message.contains("- /staff") && !message.contains("["));
    }
}
