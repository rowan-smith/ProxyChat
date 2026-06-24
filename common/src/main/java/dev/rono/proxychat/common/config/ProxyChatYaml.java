package dev.rono.proxychat.common.config;

import dev.rono.proxychat.common.config.migration.ConfigVersion;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Thin read facade over a BoostedYAML {@link Section}.
 */
public final class ProxyChatYaml {
    private final Section section;

    private ProxyChatYaml(Section section) {
        this.section = section;
    }

    public static ProxyChatYaml wrap(Section section) {
        return new ProxyChatYaml(section);
    }

    public static ProxyChatYaml fromMap(Map<String, Object> values) throws IOException {
        StringBuilder yaml = new StringBuilder();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            yaml.append(entry.getKey()).append(": ").append(formatYamlValue(entry.getValue())).append('\n');
        }

        YamlDocument document = YamlDocument.create(
                new ByteArrayInputStream(yaml.toString().getBytes(StandardCharsets.UTF_8))
        );
        return wrap(document);
    }

    private static String formatYamlValue(Object value) {
        if (value instanceof String string) {
            return "\"" + string.replace("\"", "\\\"") + "\"";
        }

        return String.valueOf(value);
    }

    public Section section() {
        return section;
    }

    public boolean contains(String route) {
        return section.contains(route);
    }

    public String getString(String route) {
        return section.getString(route);
    }

    public boolean getBoolean(String route) {
        Boolean value = section.getBoolean(route);
        return value != null && value;
    }

    public int getInt(String route) {
        Integer value = section.getInt(route);
        return value != null ? value : 0;
    }

    public ConfigVersion getConfigVersion() {
        return ConfigVersion.parse(getString("version"));
    }

    public List<String> getStringList(String route) {
        List<String> values = section.getStringList(route);
        return values != null ? values : Collections.emptyList();
    }
}
