package dev.rono.proxychat.common.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class YamlConfigTest {
    @TempDir Path tempDir;

    @Test
    void loadsAndSavesNestedValues() throws Exception {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("name", "global");
        root.put("enabled", true);
        root.put("delay", 5);
        root.put("tags", List.of("a", "b"));

        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("value", "nested");
        root.put("section", nested);

        YamlConfig original = YamlConfig.fromMap(root);
        Path file = tempDir.resolve("test.yml");
        original.save(file);

        YamlConfig loaded = YamlConfig.load(file);

        assertThat(loaded.getString("name")).isEqualTo("global");
        assertThat(loaded.getBoolean("enabled")).isTrue();
        assertThat(loaded.getInt("delay")).isEqualTo(5);
        assertThat(loaded.getStringList("tags")).containsExactly("a", "b");
        assertThat(loaded.getSection("section").getString("value")).isEqualTo("nested");
    }

    @Test
    void setsAndRemovesNestedPaths() {
        YamlConfig config = YamlConfig.fromMap(new LinkedHashMap<>());

        config.set("a.b.c", "value");

        assertThat(config.contains("a.b.c")).isTrue();
        assertThat(config.getString("a.b.c")).isEqualTo("value");

        config.set("a.b", null);

        assertThat(config.getSection("a.b")).isNull();
    }

    @Test
    void returnsEmptyConfigForMissingFile() throws Exception {
        YamlConfig config = YamlConfig.load(tempDir.resolve("missing.yml"));

        assertThat(config.getString("anything")).isNull();
        assertThat(config.getBoolean("anything")).isFalse();
        assertThat(config.getStringList("anything")).isEmpty();
    }
}
