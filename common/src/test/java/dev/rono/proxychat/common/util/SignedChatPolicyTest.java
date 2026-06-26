package dev.rono.proxychat.common.util;

import java.io.IOException;
import java.util.LinkedHashMap;

import org.junit.jupiter.api.Test;

import dev.rono.proxychat.common.config.ProxyChatYaml;
import dev.rono.proxychat.common.test.FakePlatform;
import dev.rono.proxychat.common.test.FakePlayer;
import dev.rono.proxychat.common.test.RecordingSignedChatHandler;

import static org.assertj.core.api.Assertions.assertThat;

class SignedChatPolicyTest {
    private final FakePlayer player = new FakePlayer("Alice", "lobby").withProtocolVersion(767);
    private final RecordingSignedChatHandler handler = new RecordingSignedChatHandler().canIntercept(false);

    @Test
    void autoModeDelegatesToHandler() {

        // arrange
        ProxyChatYaml config = configWithMode("auto");

        // act
        boolean interceptBeforeEnable = SignedChatPolicy.shouldInterceptChat(config, handler, player);
        handler.canIntercept(true);
        boolean interceptAfterEnable = SignedChatPolicy.shouldInterceptChat(config, handler, player);

        // assert
        assertThat(interceptBeforeEnable).isFalse();
        assertThat(interceptAfterEnable).isTrue();
    }

    @Test
    void treatsMissingModeAsAuto() {

        // arrange
        ProxyChatYaml config = configFromMap(new LinkedHashMap<>());

        // act
        handler.canIntercept(true);

        // assert
        assertThat(SignedChatPolicy.shouldInterceptChat(config, handler, player)).isTrue();
    }

    @Test
    void neverModeSkipsHandler() {

        // arrange
        ProxyChatYaml config = configWithMode("never");

        // act
        handler.canIntercept(true);

        // assert
        assertThat(SignedChatPolicy.shouldInterceptChat(config, handler, player)).isFalse();
    }

    @Test
    void alwaysModeIgnoresHandler() {

        // arrange
        ProxyChatYaml config = configWithMode("always");

        // act
        handler.canIntercept(false);

        // assert
        assertThat(SignedChatPolicy.shouldInterceptChat(config, handler, player)).isTrue();
    }

    @Test
    void acceptsSynonyms() {

        // arrange & act
        handler.canIntercept(true);

        // assert
        assertThat(SignedChatPolicy.shouldInterceptChat(configWithMode("disabled"), handler, player)).isFalse();
        assertThat(SignedChatPolicy.shouldInterceptChat(configWithMode("enabled"), handler, player)).isTrue();
        assertThat(SignedChatPolicy.shouldInterceptChat(configWithMode("true"), handler, player)).isTrue();
        assertThat(SignedChatPolicy.shouldInterceptChat(configWithMode("false"), handler, player)).isFalse();
    }

    @Test
    void skipsProxyPrefixCommandWhenSignedVelocityHandlesPlainPrefix() {

        // arrange
        FakePlatform platform = new FakePlatform();

        // act
        platform.installPlugin("signedvelocity");

        // assert
        assertThat(SignedChatPolicy.shouldRegisterProxyPrefixCommand(configWithMode("auto"), platform)).isFalse();
    }

    @Test
    void registersProxyPrefixCommandWithoutSignedVelocity() {

        // arrange & act
        FakePlatform platform = new FakePlatform();

        // assert
        assertThat(SignedChatPolicy.shouldRegisterProxyPrefixCommand(configWithMode("auto"), platform)).isTrue();
    }

    @Test
    void registersProxyPrefixCommandWhenInterceptionDisabled() {

        // arrange
        FakePlatform platform = new FakePlatform();

        // act
        platform.installPlugin("signedvelocity");

        // assert
        assertThat(SignedChatPolicy.shouldRegisterProxyPrefixCommand(configWithMode("never"), platform)).isTrue();
    }

    private static ProxyChatYaml configWithMode(String mode) {
        LinkedHashMap<String, Object> root = new LinkedHashMap<>();
        root.put("signed-chat-interception", mode);
        return configFromMap(root);
    }

    private static ProxyChatYaml configFromMap(LinkedHashMap<String, Object> root) {
        try {
            return ProxyChatYaml.fromMap(root);
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
    }
}
