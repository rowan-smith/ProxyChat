package dev.rono.proxychat.common.config.migration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.rono.proxychat.common.config.ProxyChatYamlDocuments;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigLayoutNormalizerTest {
    @TempDir Path dataDirectory;

    @Test
    void bundledDefaultsUseBlankLinesBetweenSections() throws Exception {

        // arrange
        var defaults = readDefaultConfigBytes();
        var document = YamlDocument.create(new ByteArrayInputStream(defaults));

        // act
        var dump = document.dump();

        // assert
        assertThat(dump).contains("\n\n# Prefix used in front of all messages");
        assertThat(dump.lines().filter(line -> line.trim().equals("#")).findAny()).isEmpty();
    }

    @Test
    void migratesV21ConfigWithNewKeysBeforeVersion() throws Exception {

        // arrange
        ProxyChatYamlDocuments.configureDefaults(null, null);
        Files.createDirectories(dataDirectory.resolve("chats"));
        copyResource("v2/2-1/config.yml", dataDirectory.resolve("config.yml"));
        ProxyChatYamlDocuments.loadMainConfig(dataDirectory, Logger.getLogger("test"));
        var saved = Files.readString(dataDirectory.resolve("config.yml"));
        var savedDocument = YamlDocument.create(
                new ByteArrayInputStream(saved.getBytes(StandardCharsets.UTF_8))
        );

        // act
        var keyOrder = new ArrayList<>(savedDocument.getRoutesAsStrings(false));

        // assert
        assertThat(keyOrder.indexOf("signed-chat-interception")).isLessThan(keyOrder.indexOf("version"));
        assertThat(keyOrder.indexOf("help-header")).isLessThan(keyOrder.indexOf("version"));
        assertThat(saved).contains("# Prefix used in front of all messages");
        assertThat(saved).contains("# Shown when running /proxychat or /pc with no arguments");
        assertThat(saved).contains("# Controls whether ProxyChat cancels plain chat for @prefix and toggle mode.");
        assertThat(saved.lines().filter(line -> line.trim().equals("#")).findAny()).isEmpty();
        assertThat(saved).contains("\n\n# This is what is shown when you toggle a command.");
    }

    @Test
    void restoresDefaultSpacingWhenKeyOrderAlreadyMatches() throws Exception {

        // arrange
        ProxyChatYamlDocuments.configureDefaults(null, null);
        Files.createDirectories(dataDirectory.resolve("chats"));
        copyResource("v2/2-2/config.yml", dataDirectory.resolve("config.yml"));
        var compacted = Files.readString(dataDirectory.resolve("config.yml")).replace("\n\n#", "\n#");
        Files.writeString(dataDirectory.resolve("config.yml"), compacted);
        ProxyChatYamlDocuments.loadMainConfig(dataDirectory, Logger.getLogger("test"));

        // act
        var saved = Files.readString(dataDirectory.resolve("config.yml"));

        // assert
        assertThat(saved).contains("\n\n# This is what is shown when you toggle a command.");
        assertThat(saved.lines().filter(line -> line.trim().equals("#")).findAny()).isEmpty();
    }

    @Test
    void rebuildsMisplacedKeysBeforeVersion() throws Exception {

        // arrange
        var yaml = """
                reload-message: ok
                version: 2.1
                help-header: test
                signed-chat-interception: auto
                """;
        var document = YamlDocument.create(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
        var defaults = readDefaultConfigBytes();

        // act
        var merged = ConfigLayoutNormalizer.applyLayout(document, defaults);
        var keyOrder = new ArrayList<>(merged.getRoutesAsStrings(false));
        var dumped = ConfigLayoutNormalizer.polishDump(merged.dump());

        // assert
        assertThat(keyOrder.indexOf("help-header")).isLessThan(keyOrder.indexOf("version"));
        assertThat(keyOrder.indexOf("signed-chat-interception")).isLessThan(keyOrder.indexOf("version"));
        assertThat(dumped).contains("signed-chat-interception: auto");
        assertThat(dumped).contains("help-header: test");
        assertThat(dumped).contains("version: 2.1");
        assertThat(dumped.lines().filter(line -> line.trim().equals("#")).findAny()).isEmpty();
        assertThat(dumped).contains("\n\n# Shown when running /proxychat or /pc with no arguments");
    }

    private static byte[] readDefaultConfigBytes() throws IOException {
        try (InputStream stream = ConfigLayoutNormalizerTest.class.getClassLoader().getResourceAsStream("config.yml")) {
            assertThat(stream).isNotNull();
            return stream.readAllBytes();
        }
    }

    private static void copyResource(String resourceName, Path target) throws IOException {
        try (InputStream stream = ConfigLayoutNormalizerTest.class.getClassLoader().getResourceAsStream(resourceName)) {
            assertThat(stream).isNotNull();
            Files.copy(stream, target);
        }
    }
}
