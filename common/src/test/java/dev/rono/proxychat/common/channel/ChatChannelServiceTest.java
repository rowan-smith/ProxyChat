package dev.rono.proxychat.common.channel;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
    void setUp() throws Exception {
        platform = new FakePlatform();
        platform.setSignedChatHandler(new RecordingSignedChatHandler());
        harness = TestEnvironment.create(dataDirectory, platform);
        service = harness.core().getChannelService();
    }

    @Test
    void broadcastsGlobalMessageToPermittedPlayers() {

        // arrange
        FakePlayer sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));
        FakePlayer recipient = platform.addPlayer(new FakePlayer("Bob", "survival").withPermission("proxychat.global"));
        FakePlayer denied = platform.addPlayer(new FakePlayer("Eve", "lobby"));

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
        FakePlayer sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.local"));
        FakePlayer sameServer = platform.addPlayer(new FakePlayer("Bob", "lobby").withPermission("proxychat.local"));
        FakePlayer otherServer = platform.addPlayer(
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
        FakeConsole console = new FakeConsole();

        // act
        service.execute(harness.globalChannel(), console, new String[]{"Hello"});

        // assert
        assertThat(console.receivedMessages()).anyMatch(message -> message.contains("player"));
    }

    @Test
    void allowsConsoleOnEnabledChannel() {

        // arrange
        FakeConsole console = new FakeConsole();
        platform.addPlayer(new FakePlayer("Bob", "lobby").withPermission("proxychat.local"));

        // act
        service.execute(harness.localChannel(), console, new String[]{"From", "console"});
        boolean delivered = platform.getOnlinePlayers()
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
        FakePlayer sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));

        // act
        service.execute(harness.globalChannel(), sender, new String[]{});

        // assert
        assertThat(sender.receivedMessages()).anyMatch(message -> message.contains("/global"));
    }

    @Test
    void togglesChatMode() {

        // arrange
        FakePlayer sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));
        service.execute(harness.globalChannel(), sender, new String[]{"toggle"});

        // act
        service.execute(harness.globalChannel(), sender, new String[]{"toggle"});

        // assert
        assertThat(sender.receivedMessages()).anyMatch(message -> message.contains("Global Chat"));
    }

    @Test
    void togglesIgnoreMode() {

        // arrange
        FakePlayer sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.local"));

        // act
        service.execute(harness.localChannel(), sender, new String[]{"ignore"});
        service.execute(harness.localChannel(), sender, new String[]{"Hello"});

        // assert
        assertThat(sender.receivedMessages()).anyMatch(message -> message.contains("ignored"));
    }

    @Test
    void preventsSendingWhileIgnored() {

        // arrange
        FakePlayer sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));
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
        FakePlayer sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));
        FakePlayer ignored = platform.addPlayer(new FakePlayer("Bob", "lobby").withPermission("proxychat.global"));
        harness.globalChannel().getToggleUtils().toggleIgnore(ignored.getUniqueId());

        // act
        service.execute(harness.globalChannel(), sender, new String[]{"visible"});

        // assert
        assertThat(ignored.receivedMessages()).isEmpty();
    }

    @Test
    void enforcesCooldownUnlessOverridden() {

        // arrange
        FakePlayer sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));
        service.execute(harness.globalChannel(), sender, new String[]{"first"});

        // act
        service.execute(harness.globalChannel(), sender, new String[]{"second"});

        // assert
        assertThat(sender.receivedMessages()).anyMatch(message -> message.contains("cooldown"));
    }

    @Test
    void bypassesCooldownWithOverridePermission() {

        // arrange
        FakePlayer sender = platform.addPlayer(
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
        FakePlayer sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));
        FakePlayer recipient = platform.addPlayer(new FakePlayer("Bob", "lobby").withPermission("proxychat.global"));

        // act
        service.execute(harness.globalChannel(), sender, new String[]{"&aGreen"});

        // assert
        assertThat(recipient.receivedMessages())
                .anyMatch(message -> message.contains("Green") && !message.contains("&a"));
    }

    @Test
    void keepsColorsWithPermission() {

        // arrange
        FakePlayer sender = platform.addPlayer(
                new FakePlayer("Alice", "lobby")
                        .withPermission("proxychat.global")
                        .withPermission("proxychat.global.color")
        );
        FakePlayer recipient = platform.addPlayer(new FakePlayer("Bob", "lobby").withPermission("proxychat.global"));

        // act
        service.execute(harness.globalChannel(), sender, new String[]{"&aGreen"});

        // assert
        assertThat(recipient.receivedMessages()).anyMatch(message -> message.contains("Green"));
    }

    @Test
    void ignoresBlacklistedServer() {

        // arrange
        FakePlayer sender = platform.addPlayer(
                new FakePlayer("Alice", "blocked-server").withPermission("proxychat.staff")
        );

        // act
        service.execute(harness.staffChannel(), sender, new String[]{"secret"});

        // assert
        assertThat(sender.receivedMessages()).isEmpty();
    }

    @Test
    void tabCompletesToggleAndIgnore() {

        // arrange
        FakePlayer player = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));
        Set<String> globalSuggestions = service.tabComplete(harness.globalChannel(), player, new String[]{""});
        FakePlayer localPlayer = platform.addPlayer(new FakePlayer("Bob", "lobby").withPermission("proxychat.local"));

        // act
        Set<String> localSuggestions = service.tabComplete(harness.localChannel(), localPlayer, new String[]{""});

        // assert
        assertThat(globalSuggestions).contains("toggle", "ignore");
        assertThat(localSuggestions).containsExactly("ignore");
    }

    @Test
    void hidesToggleWhenSignedChatInterceptionUnavailable() throws Exception {

        // arrange
        FakePlatform blockedPlatform = new FakePlatform();
        blockedPlatform.setSignedChatHandler(new RecordingSignedChatHandler().canIntercept(false));
        TestEnvironment.TestHarness blockedHarness = TestEnvironment.create(
                dataDirectory.resolve("blocked-toggle"),
                blockedPlatform
        );
        ChatChannelService blockedService = blockedHarness.core().getChannelService();
        FakePlayer player = blockedPlatform.addPlayer(
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
    void hidesToggleWhenSignedChatInterceptionDisabledInConfig() throws Exception {

        // arrange
        harness.config().section().set("signed-chat-interception", "never");
        FakePlayer player = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));

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
        FakePlayer player = platform.addPlayer(
                new FakePlayer("Alice", "blocked-server").withPermission("proxychat.staff")
        );

        // assert
        assertThat(service.tabComplete(harness.staffChannel(), player, new String[]{""})).isEmpty();
    }

    @Test
    void interceptsPrefixMessages() {

        // arrange
        FakePlayer sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));
        FakePlayer recipient = platform.addPlayer(new FakePlayer("Bob", "lobby").withPermission("proxychat.global"));

        // act
        boolean intercepted = service.tryInterceptChat(sender, "@prefix message");

        // assert
        assertThat(intercepted).isTrue();
        assertThat(recipient.receivedMessages()).anyMatch(message -> message.contains("prefix message"));
    }

    @Test
    void interceptsPrefixMessagesWithLeadingWhitespace() {

        // arrange
        FakePlayer sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));

        // act
        FakePlayer recipient = platform.addPlayer(new FakePlayer("Bob", "lobby").withPermission("proxychat.global"));

        // assert
        assertThat(service.tryInterceptPrefixedInput(sender, "  @leading space")).isTrue();
        assertThat(recipient.receivedMessages()).anyMatch(message -> message.contains("leading space"));
    }

    @Test
    void interceptsPrefixedCommandInput() {

        // arrange
        FakePlayer sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));

        // act
        FakePlayer recipient = platform.addPlayer(new FakePlayer("Bob", "lobby").withPermission("proxychat.global"));

        // assert
        assertThat(service.tryInterceptPrefixedInput(sender, "@command style")).isTrue();
        assertThat(recipient.receivedMessages()).anyMatch(message -> message.contains("command style"));
    }

    @Test
    void interceptsToggledPlainChat() {

        // arrange
        FakePlayer sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));
        FakePlayer recipient = platform.addPlayer(new FakePlayer("Bob", "lobby").withPermission("proxychat.global"));
        harness.globalChannel().getToggleUtils().toggleChat(sender.getUniqueId());

        // act
        boolean intercepted = service.tryInterceptChat(sender, "plain chat");

        // assert
        assertThat(intercepted).isTrue();
        assertThat(recipient.receivedMessages()).anyMatch(message -> message.contains("plain chat"));
    }

    @Test
    void velocityPrefixedInterceptBroadcastsToAllPlayers() {

        // arrange
        FakePlayer sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));
        FakePlayer sameServer = platform.addPlayer(new FakePlayer("Bob", "lobby").withPermission("proxychat.global"));
        FakePlayer remoteServer = platform.addPlayer(
                new FakePlayer("Carol", "survival").withPermission("proxychat.global")
        );

        // act
        Optional<VelocityPrefixInterceptResult> result = service.tryVelocityPrefixedIntercept(sender, "@remote only");

        // assert
        assertThat(result).containsInstanceOf(VelocityPrefixInterceptResult.Delivered.class);
        assertThat(sameServer.receivedMessages()).anyMatch(message -> message.contains("remote only"));
        assertThat(remoteServer.receivedMessages()).anyMatch(message -> message.contains("remote only"));
    }

    @Test
    void velocityPrefixedInterceptIncludesSenderOnSameServer() {

        // arrange
        FakePlayer sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));

        // act
        Optional<VelocityPrefixInterceptResult> result = service.tryVelocityPrefixedIntercept(sender, "@hello");

        // assert
        assertThat(result).containsInstanceOf(VelocityPrefixInterceptResult.Delivered.class);
        assertThat(sender.receivedMessages())
                .anyMatch(message -> message.contains("Alice") && message.contains("hello"));
        assertThat(sender.receivedMessages()).noneMatch(message -> message.contains("<Alice>"));
    }

    @Test
    void velocityPrefixedInterceptReturnsBlockedOnCooldown() {

        // arrange
        FakePlayer sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));

        // act
        service.tryVelocityPrefixedIntercept(sender, "@first");
        Optional<VelocityPrefixInterceptResult> blocked = service.tryVelocityPrefixedIntercept(sender, "@second");

        // assert
        assertThat(blocked).containsInstanceOf(VelocityPrefixInterceptResult.Blocked.class);
        assertThat(sender.receivedMessages()).anyMatch(message -> message.contains("cooldown"));
    }

    @Test
    void doesNotInterceptWithoutPermission() {

        // arrange & act
        FakePlayer sender = platform.addPlayer(new FakePlayer("Alice", "lobby"));

        // assert
        assertThat(service.tryInterceptChat(sender, "@hidden")).isFalse();
    }
}
