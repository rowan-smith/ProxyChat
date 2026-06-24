package dev.rono.proxychat.common.util;

import dev.rono.proxychat.common.config.YamlConfig;
import dev.rono.proxychat.common.test.FakePlayer;
import dev.rono.proxychat.common.test.RecordingSignedChatHandler;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;

class SignedChatPolicyTest {
    private final FakePlayer player = new FakePlayer("Alice", "lobby").withProtocolVersion(767);
    private final RecordingSignedChatHandler handler = new RecordingSignedChatHandler().canIntercept(false);

    @Test
    void autoModeDelegatesToHandler() {
        YamlConfig config = configWithMode("auto");

        assertThat(SignedChatPolicy.shouldInterceptChat(config, handler, player)).isFalse();

        handler.canIntercept(true);
        assertThat(SignedChatPolicy.shouldInterceptChat(config, handler, player)).isTrue();
    }

    @Test
    void treatsMissingModeAsAuto() {
        YamlConfig config = YamlConfig.fromMap(new LinkedHashMap<>());

        handler.canIntercept(true);
        assertThat(SignedChatPolicy.shouldInterceptChat(config, handler, player)).isTrue();
    }

    @Test
    void neverModeSkipsHandler() {
        YamlConfig config = configWithMode("never");

        handler.canIntercept(true);
        assertThat(SignedChatPolicy.shouldInterceptChat(config, handler, player)).isFalse();
    }

    @Test
    void alwaysModeIgnoresHandler() {
        YamlConfig config = configWithMode("always");

        handler.canIntercept(false);
        assertThat(SignedChatPolicy.shouldInterceptChat(config, handler, player)).isTrue();
    }

    @Test
    void acceptsSynonyms() {
        handler.canIntercept(true);

        assertThat(SignedChatPolicy.shouldInterceptChat(configWithMode("disabled"), handler, player)).isFalse();
        assertThat(SignedChatPolicy.shouldInterceptChat(configWithMode("enabled"), handler, player)).isTrue();
        assertThat(SignedChatPolicy.shouldInterceptChat(configWithMode("true"), handler, player)).isTrue();
        assertThat(SignedChatPolicy.shouldInterceptChat(configWithMode("false"), handler, player)).isFalse();
    }

    private static YamlConfig configWithMode(String mode) {
        LinkedHashMap<String, Object> root = new LinkedHashMap<>();
        root.put("signed-chat-interception", mode);

        return YamlConfig.fromMap(root);
    }
}
