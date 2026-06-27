package dev.rono.proxychat.common.config.migration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import lombok.experimental.UtilityClass;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Back-versioned {@code config.yml} snapshots under {@code src/test/resources}, keyed by public release.
 */
@UtilityClass
public class ConfigFixtures {
    public static final ConfigVersion LATEST = ConfigVersion.of(2, 3);
    public static final String LATEST_CONFIG = "v2/2-2/config.yml";
    public static final String LATEST_CHATS_DIR = "v2/2-2/chats";

    public static final List<String> ALL_RELEASE_CONFIGS = List.of(
            "v1/1-0/config.yml",
            "v1/1-1/config.yml",
            "v1/1-2/config.yml",
            "v1/1-3/config.yml",
            "v1/1-4/config.yml",
            "v1/1-5/config.yml",
            "v2/2-0/config.yml",
            "v2/2-1/config.yml",
            LATEST_CONFIG
    );

    public static Stream<String> releaseConfigResources() {
        return ALL_RELEASE_CONFIGS.stream();
    }

    public static void copyResource(String resourceName, Path target) throws IOException {
        try (InputStream input = ConfigFixtures.class.getClassLoader().getResourceAsStream(resourceName)) {
            assertThat(input)
                    .as("Missing test resource: %s", resourceName)
                    .isNotNull();
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
            Files.copy(input, target);
        }
    }

    public static void copyLatestHarness(Path dataDirectory) throws IOException {
        copyResource(LATEST_CONFIG, dataDirectory.resolve("config.yml"));
        var chatsDir = dataDirectory.resolve("chats");
        Files.createDirectories(chatsDir);
        copyResource(LATEST_CHATS_DIR + "/global.yml", chatsDir.resolve("global.yml"));
        copyResource(LATEST_CHATS_DIR + "/local.yml", chatsDir.resolve("local.yml"));
        copyResource(LATEST_CHATS_DIR + "/blacklisted.yml", chatsDir.resolve("blacklisted.yml"));
    }
}
