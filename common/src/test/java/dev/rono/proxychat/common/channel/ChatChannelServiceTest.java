package dev.rono.proxychat.common.channel;

import dev.rono.proxychat.common.test.FakeConsole;
import dev.rono.proxychat.common.test.FakePlatform;
import dev.rono.proxychat.common.test.FakePlayer;
import dev.rono.proxychat.common.test.RecordingSignedChatHandler;
import dev.rono.proxychat.common.test.TestEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

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
        FakePlayer sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));
        FakePlayer recipient = platform.addPlayer(new FakePlayer("Bob", "survival").withPermission("proxychat.global"));
        FakePlayer denied = platform.addPlayer(new FakePlayer("Eve", "lobby"));

        service.execute(harness.globalChannel(), sender, new String[]{"Hello", "world"});

        assertThat(recipient.receivedMessages()).anyMatch(message -> message.contains("Alice") && message.contains("Hello world"));
        assertThat(denied.receivedMessages()).isEmpty();
        assertThat(platform.infoLogs()).anyMatch(log -> log.contains("Alice"));
    }

    @Test
    void broadcastsLocalMessageOnlyOnSameServer() {
        FakePlayer sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.local"));
        FakePlayer sameServer = platform.addPlayer(new FakePlayer("Bob", "lobby").withPermission("proxychat.local"));
        FakePlayer otherServer = platform.addPlayer(new FakePlayer("Carol", "survival").withPermission("proxychat.local"));

        service.execute(harness.localChannel(), sender, new String[]{"Local", "only"});

        assertThat(sameServer.receivedMessages()).anyMatch(message -> message.contains("Local only"));
        assertThat(otherServer.receivedMessages()).isEmpty();
    }

    @Test
    void rejectsConsoleWhenDisabled() {
        FakeConsole console = new FakeConsole();

        service.execute(harness.globalChannel(), console, new String[]{"Hello"});

        assertThat(console.receivedMessages()).anyMatch(message -> message.contains("player"));
    }

    @Test
    void allowsConsoleOnEnabledChannel() {
        FakeConsole console = new FakeConsole();
        platform.addPlayer(new FakePlayer("Bob", "lobby").withPermission("proxychat.local"));

        service.execute(harness.localChannel(), console, new String[]{"From", "console"});

        assertThat(console.receivedMessages()).isEmpty();
        boolean delivered = platform.getOnlinePlayers()
                .stream()
                .map(player -> ((FakePlayer) player).receivedMessages())
                .anyMatch(messages -> messages.stream().anyMatch(message -> message.contains("From console")));
        assertThat(delivered).isTrue();
    }

    @Test
    void showsInvalidArgsWhenMessageMissing() {
        FakePlayer sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));

        service.execute(harness.globalChannel(), sender, new String[]{});

        assertThat(sender.receivedMessages()).anyMatch(message -> message.contains("/global"));
    }

    @Test
    void togglesChatMode() {
        FakePlayer sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));

        service.execute(harness.globalChannel(), sender, new String[]{"toggle"});
        service.execute(harness.globalChannel(), sender, new String[]{"toggle"});

        assertThat(sender.receivedMessages()).anyMatch(message -> message.contains("Global Chat"));
    }

    @Test
    void togglesIgnoreMode() {
        FakePlayer sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.local"));

        service.execute(harness.localChannel(), sender, new String[]{"ignore"});
        service.execute(harness.localChannel(), sender, new String[]{"Hello"});

        assertThat(sender.receivedMessages()).anyMatch(message -> message.contains("ignored"));
    }

    @Test
    void preventsSendingWhileIgnored() {
        FakePlayer sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));
        harness.globalChannel().getToggleUtils().toggleIgnore(sender.getUniqueId());

        service.execute(harness.globalChannel(), sender, new String[]{"blocked"});

        assertThat(sender.receivedMessages()).anyMatch(message -> message.contains("ignored"));
        assertThat(platform.getOnlinePlayers()
                .stream()
                .filter(player -> !player.getUniqueId().equals(sender.getUniqueId()))
                .map(player -> ((FakePlayer) player).receivedMessages())
                .allMatch(List::isEmpty)).isTrue();
    }

    @Test
    void excludesIgnoredRecipientsFromBroadcast() {
        FakePlayer sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));
        FakePlayer ignored = platform.addPlayer(new FakePlayer("Bob", "lobby").withPermission("proxychat.global"));
        harness.globalChannel().getToggleUtils().toggleIgnore(ignored.getUniqueId());

        service.execute(harness.globalChannel(), sender, new String[]{"visible"});

        assertThat(ignored.receivedMessages()).isEmpty();
    }

    @Test
    void enforcesCooldownUnlessOverridden() {
        FakePlayer sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));

        service.execute(harness.globalChannel(), sender, new String[]{"first"});
        service.execute(harness.globalChannel(), sender, new String[]{"second"});

        assertThat(sender.receivedMessages()).anyMatch(message -> message.contains("cooldown"));
    }

    @Test
    void bypassesCooldownWithOverridePermission() {
        FakePlayer sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global").withPermission("proxychat.global.override"));
        platform.addPlayer(new FakePlayer("Bob", "lobby").withPermission("proxychat.global"));

        service.execute(harness.globalChannel(), sender, new String[]{"first"});
        service.execute(harness.globalChannel(), sender, new String[]{"second"});

        assertThat(sender.receivedMessages()).noneMatch(message -> message.contains("cooldown"));
    }

    @Test
    void stripsColorsWithoutPermission() {
        FakePlayer sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));
        FakePlayer recipient = platform.addPlayer(new FakePlayer("Bob", "lobby").withPermission("proxychat.global"));

        service.execute(harness.globalChannel(), sender, new String[]{"&aGreen"});

        assertThat(recipient.receivedMessages()).anyMatch(message -> message.contains("Green") && !message.contains("&a"));
    }

    @Test
    void keepsColorsWithPermission() {
        FakePlayer sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global").withPermission("proxychat.global.color"));
        FakePlayer recipient = platform.addPlayer(new FakePlayer("Bob", "lobby").withPermission("proxychat.global"));

        service.execute(harness.globalChannel(), sender, new String[]{"&aGreen"});

        assertThat(recipient.receivedMessages()).anyMatch(message -> message.contains("Green"));
    }

    @Test
    void ignoresBlacklistedServer() {
        FakePlayer sender = platform.addPlayer(new FakePlayer("Alice", "blocked-server").withPermission("proxychat.staff"));

        service.execute(harness.staffChannel(), sender, new String[]{"secret"});

        assertThat(sender.receivedMessages()).isEmpty();
    }

    @Test
    void tabCompletesToggleAndIgnore() {
        FakePlayer player = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));

        Set<String> globalSuggestions = service.tabComplete(harness.globalChannel(), player, new String[]{""});
        assertThat(globalSuggestions).contains("toggle", "ignore");

        FakePlayer localPlayer = platform.addPlayer(new FakePlayer("Bob", "lobby").withPermission("proxychat.local"));
        Set<String> localSuggestions = service.tabComplete(harness.localChannel(), localPlayer, new String[]{""});
        assertThat(localSuggestions).containsExactly("ignore");
    }

    @Test
    void hidesToggleWhenSignedChatInterceptionUnavailable() throws Exception {
        FakePlatform blockedPlatform = new FakePlatform();
        blockedPlatform.setSignedChatHandler(new RecordingSignedChatHandler().canIntercept(false));
        TestEnvironment.TestHarness blockedHarness = TestEnvironment.create(dataDirectory.resolve("blocked-toggle"), blockedPlatform);
        ChatChannelService blockedService = blockedHarness.core().getChannelService();
        FakePlayer player = blockedPlatform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));

        assertThat(blockedService.tabComplete(blockedHarness.globalChannel(), player, new String[]{""})).containsExactly("ignore");

        blockedService.execute(blockedHarness.globalChannel(), player, new String[]{"toggle"});

        assertThat(player.receivedMessages()).anyMatch(message -> message.contains("unavailable"));
        assertThat(blockedHarness.globalChannel().getToggleUtils().isToggled(player.getUniqueId())).isFalse();
    }

    @Test
    void hidesToggleWhenSignedChatInterceptionDisabledInConfig() throws Exception {
        harness.config().set("signed-chat-interception", "never");
        FakePlayer player = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));

        assertThat(service.tabComplete(harness.globalChannel(), player, new String[]{""})).containsExactly("ignore");

        service.execute(harness.globalChannel(), player, new String[]{"toggle"});

        assertThat(player.receivedMessages()).anyMatch(message -> message.contains("unavailable"));
        assertThat(harness.globalChannel().getToggleUtils().isToggled(player.getUniqueId())).isFalse();
    }

    @Test
    void returnsNoTabCompletionsOnBlacklistedServer() {
        FakePlayer player = platform.addPlayer(new FakePlayer("Alice", "blocked-server").withPermission("proxychat.staff"));

        assertThat(service.tabComplete(harness.staffChannel(), player, new String[]{""})).isEmpty();
    }

    @Test
    void interceptsPrefixMessages() {
        FakePlayer sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));
        FakePlayer recipient = platform.addPlayer(new FakePlayer("Bob", "lobby").withPermission("proxychat.global"));

        boolean intercepted = service.tryInterceptChat(sender, "@prefix message");

        assertThat(intercepted).isTrue();
        assertThat(recipient.receivedMessages()).anyMatch(message -> message.contains("prefix message"));
    }

    @Test
    void interceptsToggledPlainChat() {
        FakePlayer sender = platform.addPlayer(new FakePlayer("Alice", "lobby").withPermission("proxychat.global"));
        FakePlayer recipient = platform.addPlayer(new FakePlayer("Bob", "lobby").withPermission("proxychat.global"));
        harness.globalChannel().getToggleUtils().toggleChat(sender.getUniqueId());

        boolean intercepted = service.tryInterceptChat(sender, "plain chat");

        assertThat(intercepted).isTrue();
        assertThat(recipient.receivedMessages()).anyMatch(message -> message.contains("plain chat"));
    }

    @Test
    void doesNotInterceptWithoutPermission() {
        FakePlayer sender = platform.addPlayer(new FakePlayer("Alice", "lobby"));

        assertThat(service.tryInterceptChat(sender, "@hidden")).isFalse();
    }
}
