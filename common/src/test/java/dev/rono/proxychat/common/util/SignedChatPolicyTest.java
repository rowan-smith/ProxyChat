package dev.rono.proxychat.common.util;

import dev.rono.proxychat.common.config.ProxyChatYaml;
import dev.rono.proxychat.common.test.FakePlatform;
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
    void treatsMissingModeAsAuto() throws Exception {

        // arrange
        ProxyChatYaml config = ProxyChatYaml.fromMap(new LinkedHashMap<>());

        // act
        handler.canIntercept(true);

        // assert
        assertThat(SignedChatPolicy.shouldInterceptChat(config, handler, player)).isTrue();
    }

    @Test
    void neverModeSkipsHandler() throws Exception {

        // arrange
        ProxyChatYaml config = configWithMode("never");

        // act
        handler.canIntercept(true);

        // assert
        assertThat(SignedChatPolicy.shouldInterceptChat(config, handler, player)).isFalse();
    }

    @Test
    void alwaysModeIgnoresHandler() throws Exception {

        // arrange
        ProxyChatYaml config = configWithMode("always");

        // act
        handler.canIntercept(false);

        // assert
        assertThat(SignedChatPolicy.shouldInterceptChat(config, handler, player)).isTrue();
    }

    @Test
    void acceptsSynonyms() throws Exception {

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
    void registersProxyPrefixCommandWithoutSignedVelocity() throws Exception {

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
        try {
            LinkedHashMap<String, Object> root = new LinkedHashMap<>();
            root.put("signed-chat-interception", mode);

            return ProxyChatYaml.fromMap(root);
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
