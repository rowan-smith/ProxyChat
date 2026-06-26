package dev.rono.proxychat.common.config;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.rono.proxychat.common.config.migration.ConfigFixtures;

import static org.assertj.core.api.Assertions.assertThat;

class ProxyChatConfigTest {
    @TempDir Path dataDirectory;

    @Test
    void migratesLegacyChatsIntoSeparateFiles() throws Exception {

        // arrange
        ConfigFixtures.copyResource("v1/1-5/config.yml", dataDirectory.resolve("config.yml"));
        Files.createDirectories(dataDirectory.resolve("chats"));

        // act
        ProxyChatConfig config = new ProxyChatConfig(Logger.getLogger("test"), dataDirectory);
        config.loadDefaults(null, null);

        // assert
        Path globalFile = dataDirectory.resolve("chats").resolve("global.yml");
        assertThat(globalFile).exists();
        YamlDocument globalDocument = YamlDocument.create(new ByteArrayInputStream(Files.readAllBytes(globalFile)));
        assertThat(globalDocument.getString("command-name")).isEqualTo("global");
        assertThat(config.getConfig().contains("chats")).isFalse();
    }

    @Test
    void loadsChannelFilesFromChatsDirectory() throws Exception {

        // arrange
        ConfigFixtures.copyResource(ConfigFixtures.LATEST_CONFIG, dataDirectory.resolve("config.yml"));
        Path chatsDir = dataDirectory.resolve("chats");
        Files.createDirectories(chatsDir);
        ConfigFixtures.copyResource(ConfigFixtures.LATEST_CHATS_DIR + "/global.yml", chatsDir.resolve("global.yml"));
        ConfigFixtures.copyResource(ConfigFixtures.LATEST_CHATS_DIR + "/local.yml", chatsDir.resolve("local.yml"));

        // act
        ProxyChatConfig config = new ProxyChatConfig(Logger.getLogger("test"), dataDirectory);
        config.reload();

        // assert
        assertThat(config.loadChannels()).hasSize(2);
    }
}
