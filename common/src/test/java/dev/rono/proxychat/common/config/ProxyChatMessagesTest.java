package dev.rono.proxychat.common.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProxyChatMessagesTest {

    @Test
    void fallsBackWhenConfigKeyMissing() throws Exception {
        ProxyChatYaml config = ProxyChatYaml.fromMap(Map.of("prefix", "&aCustom » "));

        assertThat(ProxyChatMessages.resolve(config, "toggle-unsupported-message"))
                .contains("Toggle chat is unavailable");
        assertThat(ProxyChatMessages.resolve(config, "prefix")).isEqualTo("&aCustom » ");
    }

    @Test
    void prefersConfiguredValue() throws Exception {
        ProxyChatYaml config = ProxyChatYaml.fromMap(Map.of(
                "toggle-unsupported-message", "&cCustom unsupported message"
        ));

        assertThat(ProxyChatMessages.resolve(config, "toggle-unsupported-message"))
                .isEqualTo("&cCustom unsupported message");
    }
}
