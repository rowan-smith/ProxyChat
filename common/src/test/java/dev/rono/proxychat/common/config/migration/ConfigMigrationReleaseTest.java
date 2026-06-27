package dev.rono.proxychat.common.config.migration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import dev.rono.proxychat.common.config.ProxyChatYaml;
import dev.rono.proxychat.common.config.ProxyChatYamlDocuments;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigMigrationReleaseTest {
    @TempDir Path dataDirectory;

    @BeforeEach
    void setUp() throws Exception {
        ProxyChatYamlDocuments.configureDefaults(null, null);
    }

    @ParameterizedTest
    @MethodSource("dev.rono.proxychat.common.config.migration.ConfigFixtures#releaseConfigResources")
    void migratesEveryPublicReleaseConfigToLatest(String releaseConfig) throws Exception {
        ConfigFixtures.copyResource(releaseConfig, dataDirectory.resolve("config.yml"));
        Files.createDirectories(dataDirectory.resolve("chats"));

        var config = ProxyChatYaml.wrap(
                ProxyChatYamlDocuments.loadMainConfig(dataDirectory, Logger.getLogger("test"))
        );

        assertThat(config.getConfigVersion()).isEqualTo(ConfigFixtures.LATEST);
        assertThat(config.getString("signed-chat-interception")).isEqualTo("auto");
        assertThat(config.getString("toggle-unsupported-message")).isNotBlank();
        assertThat(config.getString("help-header")).isNotBlank();
        assertThat(config.getString("blacklist-message")).isNotBlank();
        assertThat(config.getInt("max-message-length")).isGreaterThan(0);
        assertThat(config.contains("chats")).isFalse();
        assertThat(config.contains("reload")).isFalse();
    }

    @Test
    void migratesBungeeChatAndPreservesComments() throws Exception {

        // arrange
        ConfigFixtures.copyResource("v1/1-0/config.yml", dataDirectory.resolve("config.yml"));
        ProxyChatYamlDocuments.loadMainConfig(dataDirectory, Logger.getLogger("test"));

        // act
        var saved = Files.readString(dataDirectory.resolve("config.yml"));

        // assert
        assertThat(saved).contains("# Prefix used in front of all messages");
        assertThat(saved).contains("reload-permission: bungeechat.reload");
        assertThat(saved).contains("%chat-name%");
    }

    @Test
    void migratesInlineChatsToChatsDirectory() throws Exception {

        // arrange
        ConfigFixtures.copyResource("v1/1-5/config.yml", dataDirectory.resolve("config.yml"));
        Files.createDirectories(dataDirectory.resolve("chats"));

        // act
        ProxyChatYamlDocuments.loadMainConfig(dataDirectory, Logger.getLogger("test"));

        // assert
        assertThat(dataDirectory.resolve("chats/global.yml")).exists();
        assertThat(dataDirectory.resolve("chats/staffchat.yml")).exists();
        assertThat(Files.readString(dataDirectory.resolve("config.yml"))).doesNotContain("chats:");
    }
}
