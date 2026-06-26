package dev.rono.proxychat.common.config;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProxyChatMessagesTest {

    @Test
    void fallsBackWhenConfigKeyMissing() throws Exception {

        // arrange & act
        ProxyChatYaml config = ProxyChatYaml.fromMap(Map.of("prefix", "&aCustom » "));

        // assert
        assertThat(ProxyChatMessages.resolve(config, "toggle-unsupported-message"))
                .contains("Toggle chat is unavailable");
        assertThat(ProxyChatMessages.resolve(config, "prefix")).isEqualTo("&aCustom » ");
    }

    @Test
    void prefersConfiguredValue() throws Exception {

        // arrange & act
        ProxyChatYaml config = ProxyChatYaml.fromMap(
                Map.of("toggle-unsupported-message", "&cCustom unsupported message")
        );

        // assert
        assertThat(ProxyChatMessages.resolve(config, "toggle-unsupported-message"))
                .isEqualTo("&cCustom unsupported message");
    }
}
