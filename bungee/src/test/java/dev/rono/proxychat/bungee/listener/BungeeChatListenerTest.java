package dev.rono.proxychat.bungee.listener;

import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.ChatEvent;

import dev.rono.proxychat.bungee.platform.BungeePlatform;
import dev.rono.proxychat.bungee.platform.BungeePlayer;
import dev.rono.proxychat.common.ProxyChatCore;
import dev.rono.proxychat.common.test.FakePlatform;
import dev.rono.proxychat.common.test.FakePlayer;
import dev.rono.proxychat.common.test.RecordingSignedChatHandler;
import dev.rono.proxychat.common.test.TestEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BungeeChatListenerTest {

    @TempDir
    Path dataDirectory;

    @Test
    void cancelsAndAcknowledgesInterceptedChat() {

        // arrange
        var platform = new FakePlatform();
        var handler = new RecordingSignedChatHandler().canIntercept(true);
        platform.setSignedChatHandler(handler);
        var harness = TestEnvironment.create(dataDirectory, platform);
        platform.addPlayer(new FakePlayer("Bob", "lobby").withPermission("proxychat.global"));

        // act
        var handle = mockAlice();
        var event = mock(ChatEvent.class);
        when(event.isCancelled()).thenReturn(false);
        when(event.isCommand()).thenReturn(false);
        when(event.getSender()).thenReturn(handle);
        when(event.getMessage()).thenReturn("@intercepted message");
        var bungeePlatform = mock(BungeePlatform.class);
        when(bungeePlatform.toPlayer(handle)).thenAnswer(invocation -> new BungeePlayer(invocation.getArgument(0), bungeePlatform));
        new BungeeChatListener(harness.core(), bungeePlatform).onChat(event);
        verify(event).setCancelled(true);

        // assert
        assertThat(handler.acknowledgedPlayers()).containsExactly(handle.getUniqueId());
    }

    @Test
    void ignoresChatWhenPolicyBlocksInterception() {

        // arrange
        var platform = new FakePlatform();
        var handler = new RecordingSignedChatHandler().canIntercept(false);
        platform.setSignedChatHandler(handler);
        var harness = TestEnvironment.create(dataDirectory, platform);

        // act
        var handle = mockAlice();
        var event = mock(ChatEvent.class);
        when(event.isCancelled()).thenReturn(false);
        when(event.isCommand()).thenReturn(false);
        when(event.getSender()).thenReturn(handle);
        when(event.getMessage()).thenReturn("@should-not-intercept");
        var bungeePlatform = mock(BungeePlatform.class);
        when(bungeePlatform.toPlayer(handle)).thenAnswer(invocation -> new BungeePlayer(invocation.getArgument(0), bungeePlatform));
        new BungeeChatListener(harness.core(), bungeePlatform).onChat(event);

        // assert
        assertThat(handler.acknowledgedPlayers()).isEmpty();
    }

    @Test
    void ignoresCancelledAndCommandEvents() {

        // arrange & act
        var bungeePlatform = mock(BungeePlatform.class);
        var listener = new BungeeChatListener(mock(ProxyChatCore.class), bungeePlatform);
        var cancelled = mock(ChatEvent.class);
        when(cancelled.isCancelled()).thenReturn(true);
        listener.onChat(cancelled);
        var command = mock(ChatEvent.class);
        when(command.isCancelled()).thenReturn(false);
        when(command.isCommand()).thenReturn(true);
        listener.onChat(command);

        // assert
    }

    private static ProxiedPlayer mockAlice() {
        var handle = mock(ProxiedPlayer.class);
        var id = UUID.nameUUIDFromBytes("Alice".getBytes());
        when(handle.getName()).thenReturn("Alice");
        when(handle.getUniqueId()).thenReturn(id);
        when(handle.hasPermission("proxychat.global")).thenReturn(true);

        var connection = mock(Server.class);
        var info = mock(ServerInfo.class);
        when(info.getName()).thenReturn("lobby");
        when(connection.getInfo()).thenReturn(info);
        when(handle.getServer()).thenReturn(connection);

        var pending = mock(PendingConnection.class);
        when(handle.getPendingConnection()).thenReturn(pending);
        when(pending.getVersion()).thenReturn(767);

        return handle;
    }
}
