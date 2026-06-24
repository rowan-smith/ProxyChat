package dev.rono.proxychat.common.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

class ProxyChatConfigTest {
    @TempDir Path dataDirectory;

    @Test
    void migratesLegacyChatsIntoSeparateFiles() throws Exception {
        copyResource("config-legacy.yml", dataDirectory.resolve("config.yml"));
        Files.createDirectories(dataDirectory.resolve("chats"));

        ProxyChatConfig config = new ProxyChatConfig(Logger.getLogger("test"), dataDirectory);
        config.loadDefaults(null, null);

        Path legacyFile = dataDirectory.resolve("chats").resolve("legacy.yml");
        assertThat(legacyFile).exists();
        assertThat(YamlConfig.load(legacyFile).getString("command-name")).isEqualTo("legacy");
        assertThat(config.getConfig().contains("chats")).isFalse();
    }

    @Test
    void loadsChannelFilesFromChatsDirectory() throws Exception {
        copyResource("config.yml", dataDirectory.resolve("config.yml"));
        Path chatsDir = dataDirectory.resolve("chats");
        Files.createDirectories(chatsDir);
        copyResource("chats/global.yml", chatsDir.resolve("global.yml"));
        copyResource("chats/local.yml", chatsDir.resolve("local.yml"));

        ProxyChatConfig config = new ProxyChatConfig(Logger.getLogger("test"), dataDirectory);
        config.reload();

        assertThat(config.loadChannels()).hasSize(2);
    }

    private void copyResource(String resourceName, Path target) throws Exception {
        try (var input = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            assertThat(input).isNotNull();
            Files.createDirectories(target.getParent() == null ? dataDirectory : target.getParent());
            Files.copy(input, target);
        }
    }
}
