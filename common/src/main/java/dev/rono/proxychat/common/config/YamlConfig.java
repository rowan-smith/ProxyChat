package dev.rono.proxychat.common.config;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public final class YamlConfig {
    private final Map<String, Object> root;

    private YamlConfig(Map<String, Object> root) {
        this.root = root != null ? root : new LinkedHashMap<>();
    }

    public static YamlConfig load(Path file) throws IOException {
        if (!Files.exists(file)) {
            return new YamlConfig(new LinkedHashMap<>());
        }

        try (InputStream input = Files.newInputStream(file)) {
            Object loaded = new Yaml().load(input);

            if (loaded instanceof Map<?, ?> map) {
                return new YamlConfig(new LinkedHashMap<>((Map<String, Object>) map));
            }

            return new YamlConfig(new LinkedHashMap<>());
        }
    }

    public static YamlConfig fromMap(Map<String, Object> map) {
        return new YamlConfig(new LinkedHashMap<>(map));
    }

    public void save(Path file) throws IOException {
        Files.createDirectories(file.getParent());

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        try (Writer writer = Files.newBufferedWriter(file)) {
            new Yaml(options).dump(root, writer);
        }
    }

    public boolean contains(String path) {
        return getValue(path) != null;
    }

    public void set(String path, Object value) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = root;

        for (int index = 0; index < parts.length - 1; index++) {
            Object next = current.get(parts[index]);

            if (!(next instanceof Map<?, ?>)) {
                next = new LinkedHashMap<String, Object>();
                current.put(parts[index], next);
            }

            current = (Map<String, Object>) next;
        }

        current.put(parts[parts.length - 1], value);
    }

    public String getString(String path) {
        Object value = getValue(path);
        return value != null ? String.valueOf(value) : null;
    }

    public boolean getBoolean(String path) {
        Object value = getValue(path);
        return value instanceof Boolean bool && bool;
    }

    public int getInt(String path) {
        Object value = getValue(path);
        if (value instanceof Number number) {
            return number.intValue();
        }

        return 0;
    }

    public List<String> getStringList(String path) {
        Object value = getValue(path);
        if (!(value instanceof List<?> list)) {
            return Collections.emptyList();
        }

        List<String> strings = new ArrayList<>();
        for (Object entry : list) {
            if (entry != null) {
                strings.add(String.valueOf(entry));
            }
        }

        return strings;
    }

    public YamlConfig getSection(String path) {
        Object value = getValue(path);
        if (value instanceof Map<?, ?> map) {
            return fromMap((Map<String, Object>) map);
        }

        return null;
    }

    public Iterable<String> getKeys() {
        return root.keySet();
    }

    private Object getValue(String path) {
        String[] parts = path.split("\\.");
        Object current = root;

        for (String part : parts) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }

            current = map.get(part);
            if (current == null) {
                return null;
            }
        }

        return current;
    }
}
