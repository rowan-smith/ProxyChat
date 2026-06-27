package dev.rono.proxychat.common.config;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProxyChatMessagesTest {

    @Test
    void fallsBackWhenConfigKeyMissing() {

        // arrange & act
        var config = yaml(Map.of("prefix", "&aCustom » "));

        // assert
        assertThat(ProxyChatMessages.resolve(config, "toggle-unsupported-message")).contains("Toggle chat is unavailable");
        assertThat(ProxyChatMessages.resolve(config, "prefix")).isEqualTo("&aCustom » ");
    }

    @Test
    void prefersConfiguredValue() {

        // arrange & act
        var config = yaml(Map.of("toggle-unsupported-message", "&cCustom unsupported message"));

        // assert
        assertThat(ProxyChatMessages.resolve(config, "toggle-unsupported-message")).isEqualTo("&cCustom unsupported message");
    }

    private static ProxyChatYaml yaml(Map<String, Object> values) {
        try {
            return ProxyChatYaml.fromMap(values);
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
    }
}
