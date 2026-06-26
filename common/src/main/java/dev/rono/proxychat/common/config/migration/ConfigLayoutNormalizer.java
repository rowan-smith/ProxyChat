package dev.rono.proxychat.common.config.migration;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import lombok.experimental.UtilityClass;

import dev.dejvokep.boostedyaml.YamlDocument;

/**
 * Re-applies bundled-default layout (spacing and section comments) while keeping user values.
 */
@UtilityClass
public class ConfigLayoutNormalizer {

    public static YamlDocument applyLayout(YamlDocument user, byte[] defaultsBytes) throws IOException {
        YamlDocument template = YamlDocument.create(new ByteArrayInputStream(defaultsBytes));
        for (String key : user.getRoutesAsStrings(false)) {
            template.set(key, user.get(key));
        }

        return template;
    }

    public static boolean layoutMatches(YamlDocument user, byte[] defaultsBytes) throws IOException {
        YamlDocument merged = applyLayout(user, defaultsBytes);
        return polishDump(merged.dump()).equals(polishDump(user.dump()));
    }

    public static String dumpPolished(YamlDocument document) {
        return polishDump(document.dump());
    }

    static String polishDump(String yaml) {
        return yaml.replaceAll("(?m)^#\\r?\\n", "");
    }
}
