package dev.rono.proxychat.bungee.listener;

import dev.rono.proxychat.common.test.FakePlatform;
import dev.rono.proxychat.common.test.FakePlayer;
import dev.rono.proxychat.common.test.RecordingSignedChatHandler;
import dev.rono.proxychat.common.test.TestEnvironment;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BungeeChatListenerTest {

    @TempDir
    Path dataDirectory;

    @Test
    void cancelsAndAcknowledgesInterceptedChat() throws Exception {
        FakePlatform platform = new FakePlatform();
        RecordingSignedChatHandler handler = new RecordingSignedChatHandler().canIntercept(true);
        platform.setSignedChatHandler(handler);
        TestEnvironment.TestHarness harness = TestEnvironment.create(dataDirectory, platform);
        platform.addPlayer(new FakePlayer("Bob", "lobby").withPermission("proxychat.global"));

        ProxiedPlayer handle = mockPlayer("Alice", "lobby", "proxychat.global");
        ChatEvent event = mock(ChatEvent.class);
        when(event.isCancelled()).thenReturn(false);
        when(event.isCommand()).thenReturn(false);
        when(event.getSender()).thenReturn(handle);
        when(event.getMessage()).thenReturn("@intercepted message");

        new BungeeChatListener(harness.core()).onChat(event);

        verify(event).setCancelled(true);
        assertThat(handler.acknowledgedPlayers()).containsExactly(handle.getUniqueId());
    }

    @Test
    void ignoresChatWhenPolicyBlocksInterception() throws Exception {
        FakePlatform platform = new FakePlatform();
        RecordingSignedChatHandler handler = new RecordingSignedChatHandler().canIntercept(false);
        platform.setSignedChatHandler(handler);
        TestEnvironment.TestHarness harness = TestEnvironment.create(dataDirectory, platform);

        ProxiedPlayer handle = mockPlayer("Alice", "lobby", "proxychat.global");
        ChatEvent event = mock(ChatEvent.class);
        when(event.isCancelled()).thenReturn(false);
        when(event.isCommand()).thenReturn(false);
        when(event.getSender()).thenReturn(handle);
        when(event.getMessage()).thenReturn("@should-not-intercept");

        new BungeeChatListener(harness.core()).onChat(event);

        assertThat(handler.acknowledgedPlayers()).isEmpty();
    }

    @Test
    void ignoresCancelledAndCommandEvents() {
        BungeeChatListener listener = new BungeeChatListener(mock(dev.rono.proxychat.common.ProxyChatCore.class));

        ChatEvent cancelled = mock(ChatEvent.class);
        when(cancelled.isCancelled()).thenReturn(true);
        listener.onChat(cancelled);

        ChatEvent command = mock(ChatEvent.class);
        when(command.isCancelled()).thenReturn(false);
        when(command.isCommand()).thenReturn(true);
        listener.onChat(command);
    }

    private static ProxiedPlayer mockPlayer(String name, String server, String permission) {
        ProxiedPlayer handle = mock(ProxiedPlayer.class);
        UUID id = UUID.nameUUIDFromBytes(name.getBytes());
        when(handle.getName()).thenReturn(name);
        when(handle.getUniqueId()).thenReturn(id);
        when(handle.hasPermission(permission)).thenReturn(true);

        net.md_5.bungee.api.connection.Server connection = mock(net.md_5.bungee.api.connection.Server.class);
        net.md_5.bungee.api.config.ServerInfo info = mock(net.md_5.bungee.api.config.ServerInfo.class);
        when(info.getName()).thenReturn(server);
        when(connection.getInfo()).thenReturn(info);
        when(handle.getServer()).thenReturn(connection);

        PendingConnection pending = mock(PendingConnection.class);
        when(handle.getPendingConnection()).thenReturn(pending);
        when(pending.getVersion()).thenReturn(767);

        return handle;
    }
}
