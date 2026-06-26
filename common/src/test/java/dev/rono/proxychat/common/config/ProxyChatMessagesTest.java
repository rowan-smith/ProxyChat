package dev.rono.proxychat.common.config;

import org.junit.jupiter.api.Test;



import java.util.Map;



import static org.assertj.core.api.Assertions.assertThat;

class ProxyChatMessagesTest {

    @Test
    void fallsBackWhenConfigKeyMissing() throws Exception {

        // arrange & act
        ProxyChatYaml config = ProxyChatYaml.fromMap(Map.of("prefix", "&aCustom Â» "));

        // assert
        assertThat(ProxyChatMessages.resolve(config, "toggle-unsupported-message")).contains("Toggle chat is unavailable");
        assertThat(ProxyChatMessages.resolve(config, "prefix")).isEqualTo("&aCustom Â» ");
    }

    @Test
    void prefersConfiguredValue() throws Exception {

        // arrange & act
        ProxyChatYaml config = ProxyChatYaml.fromMap(Map.of("toggle-unsupported-message", "&cCustom unsupported message"));

        // assert
        assertThat(ProxyChatMessages.resolve(config, "toggle-unsupported-message")).isEqualTo("&cCustom unsupported message");
    }
}


