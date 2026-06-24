package dev.rono.proxychat.common.util;

import dev.rono.proxychat.common.config.ProxyChatYaml;
import dev.rono.proxychat.common.test.FakePlayer;
import dev.rono.proxychat.common.test.RecordingSignedChatHandler;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;

class SignedChatPolicyTest {
    private final FakePlayer player = new FakePlayer("Alice", "lobby").withProtocolVersion(767);
    private final RecordingSignedChatHandler handler = new RecordingSignedChatHandler().canIntercept(false);

    @Test
    void autoModeDelegatesToHandler() throws Exception {
        ProxyChatYaml config = configWithMode("auto");

        assertThat(SignedChatPolicy.shouldInterceptChat(config, handler, player)).isFalse();

        handler.canIntercept(true);
        assertThat(SignedChatPolicy.shouldInterceptChat(config, handler, player)).isTrue();
    }

    @Test
    void treatsMissingModeAsAuto() throws Exception {
        ProxyChatYaml config = ProxyChatYaml.fromMap(new LinkedHashMap<>());

        handler.canIntercept(true);
        assertThat(SignedChatPolicy.shouldInterceptChat(config, handler, player)).isTrue();
    }

    @Test
    void neverModeSkipsHandler() throws Exception {
        ProxyChatYaml config = configWithMode("never");

        handler.canIntercept(true);
        assertThat(SignedChatPolicy.shouldInterceptChat(config, handler, player)).isFalse();
    }

    @Test
    void alwaysModeIgnoresHandler() throws Exception {
        ProxyChatYaml config = configWithMode("always");

        handler.canIntercept(false);
        assertThat(SignedChatPolicy.shouldInterceptChat(config, handler, player)).isTrue();
    }

    @Test
    void acceptsSynonyms() throws Exception {
        handler.canIntercept(true);

        assertThat(SignedChatPolicy.shouldInterceptChat(configWithMode("disabled"), handler, player)).isFalse();
        assertThat(SignedChatPolicy.shouldInterceptChat(configWithMode("enabled"), handler, player)).isTrue();
        assertThat(SignedChatPolicy.shouldInterceptChat(configWithMode("true"), handler, player)).isTrue();
        assertThat(SignedChatPolicy.shouldInterceptChat(configWithMode("false"), handler, player)).isFalse();
    }

    private static ProxyChatYaml configWithMode(String mode) throws Exception {
        LinkedHashMap<String, Object> root = new LinkedHashMap<>();
        root.put("signed-chat-interception", mode);

        return ProxyChatYaml.fromMap(root);
    }
}
