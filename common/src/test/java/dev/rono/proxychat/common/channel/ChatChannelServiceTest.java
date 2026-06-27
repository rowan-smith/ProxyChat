package dev.rono.proxychat.common.channel;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.rono.proxychat.common.test.FakeConsole;
import dev.rono.proxychat.common.test.FakePlatform;
import dev.rono.proxychat.common.test.FakePlayer;
import dev.rono.proxychat.common.test.RecordingSignedChatHandler;
import dev.rono.proxychat.common.test.TestEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class ChatChannelServiceTest {
    @TempDir Path dataDirectory;

    private TestEnvironment.TestHarness harness;
    private FakePlatform platform;
    private ChatChannelService service;

    @BeforeEach
    void setUp() {
        platform = new FakePlatform();
        platform.setSignedChatHandler(new RecordingSignedChatHandler());
        harness = TestEnvironment.create(dataDirectory, platform);
        service = harness.core().getChannelService();
    }

    @Test
    void broadcastsGlobalMessageToPermittedPlayers() {

        // arrange
        var sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));
        var recipient = platform.addPlayer(new FakePlayer("Bob", "survival").withPermission("proxychat.global"));
        var denied = platform.addPlayer(new FakePlayer("Eve", "lobby"));

        // act
        service.execute(harness.globalChannel(), sender, new String[]{"Hello", "world"});

        // assert
        assertThat(recipient.receivedMessages())
                .anyMatch(message -> message.contains("Alice") && message.contains("Hello world"));
        assertThat(denied.receivedMessages()).isEmpty();
        assertThat(platform.infoLogs()).anyMatch(log -> log.contains("Alice"));
    }

    @Test
    void broadcastsLocalMessageOnlyOnSameServer() {

        // arrange
        var sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.local"));
        var sameServer = platform.addPlayer(new FakePlayer("Bob", "lobby").withPermission("proxychat.local"));
        var otherServer = platform.addPlayer(
                new FakePlayer("Carol", "survival").withPermission("proxychat.local")
        );

        // act
        service.execute(harness.localChannel(), sender, new String[]{"Local", "only"});

        // assert
        assertThat(sameServer.receivedMessages()).anyMatch(message -> message.contains("Local only"));
        assertThat(otherServer.receivedMessages()).isEmpty();
    }

    @Test
    void rejectsConsoleWhenDisabled() {

        // arrange
        var console = new FakeConsole();

        // act
        service.execute(harness.globalChannel(), console, new String[]{"Hello"});

        // assert
        assertThat(console.receivedMessages()).anyMatch(message -> message.contains("player"));
    }

    @Test
    void allowsConsoleOnEnabledChannel() {

        // arrange
        var console = new FakeConsole();
        platform.addPlayer(new FakePlayer("Bob", "lobby").withPermission("proxychat.local"));

        // act
        service.execute(harness.localChannel(), console, new String[]{"From", "console"});
        var delivered = platform.getOnlinePlayers()
                .stream()
                .map(player -> ((FakePlayer) player).receivedMessages())
                .anyMatch(messages -> messages.stream().anyMatch(message -> message.contains("From console")));

        // assert
        assertThat(console.receivedMessages()).isEmpty();
        assertThat(delivered).isTrue();
    }

    @Test
    void showsInvalidArgsWhenMessageMissing() {

        // arrange
        var sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));

        // act
        service.execute(harness.globalChannel(), sender, new String[]{});

        // assert
        assertThat(sender.receivedMessages()).anyMatch(message -> message.contains("/global"));
    }

    @Test
    void togglesChatMode() {

        // arrange
        var sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));
        service.execute(harness.globalChannel(), sender, new String[]{"toggle"});

        // act
        service.execute(harness.globalChannel(), sender, new String[]{"toggle"});

        // assert
        assertThat(sender.receivedMessages()).anyMatch(message -> message.contains("Global Chat"));
    }

    @Test
    void togglesIgnoreMode() {

        // arrange
        var sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.local"));

        // act
        service.execute(harness.localChannel(), sender, new String[]{"ignore"});
        service.execute(harness.localChannel(), sender, new String[]{"Hello"});

        // assert
        assertThat(sender.receivedMessages()).anyMatch(message -> message.contains("ignored"));
    }

    @Test
    void preventsSendingWhileIgnored() {

        // arrange
        var sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));
        harness.globalChannel().getToggleUtils().toggleIgnore(sender.getUniqueId());

        // act
        service.execute(harness.globalChannel(), sender, new String[]{"blocked"});

        // assert
        assertThat(sender.receivedMessages()).anyMatch(message -> message.contains("ignored"));
        assertThat(platform.getOnlinePlayers()
                .stream()
                .filter(player -> !player.getUniqueId().equals(sender.getUniqueId()))
                .map(player -> ((FakePlayer) player).receivedMessages())
                .allMatch(List::isEmpty)).isTrue();
    }

    @Test
    void excludesIgnoredRecipientsFromBroadcast() {

        // arrange
        var sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));
        var ignored = platform.addPlayer(new FakePlayer("Bob", "lobby").withPermission("proxychat.global"));
        harness.globalChannel().getToggleUtils().toggleIgnore(ignored.getUniqueId());

        // act
        service.execute(harness.globalChannel(), sender, new String[]{"visible"});

        // assert
        assertThat(ignored.receivedMessages()).isEmpty();
    }

    @Test
    void enforcesCooldownUnlessOverridden() {

        // arrange
        var sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));
        service.execute(harness.globalChannel(), sender, new String[]{"first"});

        // act
        service.execute(harness.globalChannel(), sender, new String[]{"second"});

        // assert
        assertThat(sender.receivedMessages()).anyMatch(message -> message.contains("cooldown"));
    }

    @Test
    void bypassesCooldownWithOverridePermission() {

        // arrange
        var sender = platform.addPlayer(
                new FakePlayer("Alice", "lobby")
                        .withPermission("proxychat.global")
                        .withPermission("proxychat.global.override")
        );
        platform.addPlayer(new FakePlayer("Bob", "lobby").withPermission("proxychat.global"));
        service.execute(harness.globalChannel(), sender, new String[]{"first"});

        // act
        service.execute(harness.globalChannel(), sender, new String[]{"second"});

        // assert
        assertThat(sender.receivedMessages()).noneMatch(message -> message.contains("cooldown"));
    }

    @Test
    void stripsColorsWithoutPermission() {

        // arrange
        var sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));
        var recipient = platform.addPlayer(new FakePlayer("Bob", "lobby").withPermission("proxychat.global"));

        // act
        service.execute(harness.globalChannel(), sender, new String[]{"&aGreen"});

        // assert
        assertThat(recipient.receivedMessages())
                .anyMatch(message -> message.contains("Green") && !message.contains("&a"));
    }

    @Test
    void keepsColorsWithPermission() {

        // arrange
        var sender = platform.addPlayer(
                new FakePlayer("Alice", "lobby")
                        .withPermission("proxychat.global")
                        .withPermission("proxychat.global.color")
        );
        var recipient = platform.addPlayer(new FakePlayer("Bob", "lobby").withPermission("proxychat.global"));

        // act
        service.execute(harness.globalChannel(), sender, new String[]{"&aGreen"});

        // assert
        assertThat(recipient.receivedMessages()).anyMatch(message -> message.contains("Green"));
    }

    @Test
    void ignoresBlacklistedServer() {

        // arrange
        var sender = platform.addPlayer(
                new FakePlayer("Alice", "blocked-server").withPermission("proxychat.staff")
        );

        // act
        service.execute(harness.staffChannel(), sender, new String[]{"secret"});

        // assert
        assertThat(sender.receivedMessages()).anyMatch(message -> message.contains("cannot use"));
    }

    @Test
    void tabCompletesToggleAndIgnore() {

        // arrange
        var player = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));
        var globalSuggestions = service.tabComplete(harness.globalChannel(), player, new String[]{""});
        var localPlayer = platform.addPlayer(new FakePlayer("Bob", "lobby").withPermission("proxychat.local"));

        // act
        var localSuggestions = service.tabComplete(harness.localChannel(), localPlayer, new String[]{""});

        // assert
        assertThat(globalSuggestions).contains("toggle", "ignore");
        assertThat(localSuggestions).containsExactly("ignore");
    }

    @Test
    void hidesToggleWhenSignedChatInterceptionUnavailable() {

        // arrange
        var blockedPlatform = new FakePlatform();
        blockedPlatform.setSignedChatHandler(new RecordingSignedChatHandler().canIntercept(false));
        var blockedHarness = TestEnvironment.create(
                dataDirectory.resolve("blocked-toggle"),
                blockedPlatform
        );
        var blockedService = blockedHarness.core().getChannelService();
        var player = blockedPlatform.addPlayer(
                new FakePlayer("Alice", "lobby").withPermission("proxychat.global")
        );

        // act
        blockedService.execute(blockedHarness.globalChannel(), player, new String[]{"toggle"});

        // assert
        assertThat(blockedService.tabComplete(blockedHarness.globalChannel(), player, new String[]{""}))
                .containsExactly("ignore");
        assertThat(player.receivedMessages()).anyMatch(message -> message.contains("unavailable"));
        assertThat(blockedHarness.globalChannel().getToggleUtils().isToggled(player.getUniqueId())).isFalse();
    }

    @Test
    void hidesToggleWhenSignedChatInterceptionDisabledInConfig() {

        // arrange
        harness.config().section().set("signed-chat-interception", "never");
        var player = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));

        // act
        service.execute(harness.globalChannel(), player, new String[]{"toggle"});

        // assert
        assertThat(service.tabComplete(harness.globalChannel(), player, new String[]{""})).containsExactly("ignore");
        assertThat(player.receivedMessages()).anyMatch(message -> message.contains("unavailable"));
        assertThat(harness.globalChannel().getToggleUtils().isToggled(player.getUniqueId())).isFalse();
    }

    @Test
    void returnsNoTabCompletionsOnBlacklistedServer() {

        // arrange & act
        var player = platform.addPlayer(
                new FakePlayer("Alice", "blocked-server").withPermission("proxychat.staff")
        );

        // assert
        assertThat(service.tabComplete(harness.staffChannel(), player, new String[]{""})).isEmpty();
    }

    @Test
    void interceptsPrefixMessages() {

        // arrange
        var sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));
        var recipient = platform.addPlayer(new FakePlayer("Bob", "lobby").withPermission("proxychat.global"));

        // act
        var intercepted = service.tryInterceptChat(sender, "@prefix message");

        // assert
        assertThat(intercepted).isTrue();
        assertThat(recipient.receivedMessages()).anyMatch(message -> message.contains("prefix message"));
    }

    @Test
    void interceptsPrefixMessagesWithLeadingWhitespace() {

        // arrange
        var sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));

        // act
        var recipient = platform.addPlayer(new FakePlayer("Bob", "lobby").withPermission("proxychat.global"));

        // assert
        assertThat(service.tryInterceptPrefixedInput(sender, "  @leading space")).isTrue();
        assertThat(recipient.receivedMessages()).anyMatch(message -> message.contains("leading space"));
    }

    @Test
    void interceptsPrefixedCommandInput() {

        // arrange
        var sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));

        // act
        var recipient = platform.addPlayer(new FakePlayer("Bob", "lobby").withPermission("proxychat.global"));

        // assert
        assertThat(service.tryInterceptPrefixedInput(sender, "@command style")).isTrue();
        assertThat(recipient.receivedMessages()).anyMatch(message -> message.contains("command style"));
    }

    @Test
    void interceptsToggledPlainChat() {

        // arrange
        var sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));
        var recipient = platform.addPlayer(new FakePlayer("Bob", "lobby").withPermission("proxychat.global"));
        harness.globalChannel().getToggleUtils().toggleChat(sender.getUniqueId());

        // act
        var intercepted = service.tryInterceptChat(sender, "plain chat");

        // assert
        assertThat(intercepted).isTrue();
        assertThat(recipient.receivedMessages()).anyMatch(message -> message.contains("plain chat"));
    }

    @Test
    void velocityPrefixedInterceptBroadcastsToAllPlayers() {

        // arrange
        var sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));
        var sameServer = platform.addPlayer(new FakePlayer("Bob", "lobby").withPermission("proxychat.global"));
        var remoteServer = platform.addPlayer(
                new FakePlayer("Carol", "survival").withPermission("proxychat.global")
        );

        // act
        var result = service.tryVelocityPrefixedIntercept(sender, "@remote only");

        // assert
        assertThat(result).containsInstanceOf(VelocityPrefixInterceptResult.Delivered.class);
        assertThat(sameServer.receivedMessages()).anyMatch(message -> message.contains("remote only"));
        assertThat(remoteServer.receivedMessages()).anyMatch(message -> message.contains("remote only"));
    }

    @Test
    void velocityPrefixedInterceptIncludesSenderOnSameServer() {

        // arrange
        var sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));

        // act
        var result = service.tryVelocityPrefixedIntercept(sender, "@hello");

        // assert
        assertThat(result).containsInstanceOf(VelocityPrefixInterceptResult.Delivered.class);
        assertThat(sender.receivedMessages())
                .anyMatch(message -> message.contains("Alice") && message.contains("hello"));
        assertThat(sender.receivedMessages()).noneMatch(message -> message.contains("<Alice>"));
    }

    @Test
    void velocityPrefixedInterceptReturnsBlockedOnCooldown() {

        // arrange
        var sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));

        // act
        service.tryVelocityPrefixedIntercept(sender, "@first");
        var blocked = service.tryVelocityPrefixedIntercept(sender, "@second");

        // assert
        assertThat(blocked).containsInstanceOf(VelocityPrefixInterceptResult.Blocked.class);
        assertThat(sender.receivedMessages()).anyMatch(message -> message.contains("cooldown"));
    }

    @Test
    void doesNotInterceptWithoutPermission() {

        // arrange & act
        var sender = platform.addPlayer(new FakePlayer("Alice", "lobby"));

        // assert
        assertThat(service.tryInterceptChat(sender, "@hidden")).isFalse();
    }
}
