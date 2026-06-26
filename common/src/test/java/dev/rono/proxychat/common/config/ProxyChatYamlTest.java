package dev.rono.proxychat.common.config;

import dev.dejvokep.boostedyaml.YamlDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ProxyChatYamlTest {

    @Test
    void readsScalarValues() throws Exception {

        // arrange
        String yaml = """
                name: global
                enabled: true
                delay: 5
                tags:
                  - a
                  - b
                """;
        YamlDocument document = YamlDocument.create(
                new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8))
        );

        // act
        ProxyChatYaml config = ProxyChatYaml.wrap(document);

        // assert
        assertThat(config.getString("name")).isEqualTo("global");
        assertThat(config.getBoolean("enabled")).isTrue();
        assertThat(config.getInt("delay")).isEqualTo(5);
        assertThat(config.getStringList("tags")).containsExactly("a", "b");
    }

    @Test
    void returnsDefaultsForMissingValues() throws Exception {

        // arrange
        YamlDocument document = YamlDocument.create(
                new ByteArrayInputStream("prefix: test\n".getBytes(StandardCharsets.UTF_8))
        );

        // act
        ProxyChatYaml config = ProxyChatYaml.wrap(document);

        // assert
        assertThat(config.getString("missing")).isNull();
        assertThat(config.getBoolean("missing")).isFalse();
        assertThat(config.getInt("missing")).isZero();
        assertThat(config.getStringList("missing")).isEmpty();
    }
}
