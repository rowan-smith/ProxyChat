package dev.rono.proxychat.common.test;

import dev.rono.proxychat.common.ProxyChatCore;
import dev.rono.proxychat.common.channel.ChatChannel;
import dev.rono.proxychat.common.config.ProxyChatConfig;
import dev.rono.proxychat.common.config.YamlConfig;
import dev.rono.proxychat.common.platform.SignedChatHandler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

public final class TestEnvironment {
    public static TestHarness create(Path dataDirectory, FakePlatform platform) throws Exception {
        copyResource("config.yml", dataDirectory.resolve("config.yml"));
        Path chatsDir = dataDirectory.resolve("chats");
        Files.createDirectories(chatsDir);
        copyResource("chats/global.yml", chatsDir.resolve("global.yml"));
        copyResource("chats/local.yml", chatsDir.resolve("local.yml"));
        copyResource("chats/blacklisted.yml", chatsDir.resolve("blacklisted.yml"));

        SignedChatHandler handler = platform.signedChatHandler();
        ProxyChatCore core = new ProxyChatCore(Logger.getLogger("ProxyChatTest"), platform, handler, dataDirectory);
        core.enable(null, null);
        return new TestHarness(core, platform, dataDirectory);
    }

    public static TestHarness createWithLegacyConfig(Path dataDirectory, FakePlatform platform) throws Exception {
        copyResource("config-legacy.yml", dataDirectory.resolve("config.yml"));
        Files.createDirectories(dataDirectory.resolve("chats"));
        SignedChatHandler handler = platform.signedChatHandler();
        ProxyChatCore core = new ProxyChatCore(Logger.getLogger("ProxyChatTest"), platform, handler, dataDirectory);
        ProxyChatConfig configManager = core.getConfig();
        configManager.loadDefaults(null, null);
        core.reload();
        return new TestHarness(core, platform, dataDirectory);
    }

    private static void copyResource(String resourceName, Path target) throws IOException {
        try (InputStream input = TestEnvironment.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (input == null) {
                throw new IOException("Missing test resource: " + resourceName);
            }
            Files.createDirectories(target.getParent());
            Files.copy(input, target);
        }
    }

    public record TestHarness(ProxyChatCore core, FakePlatform platform, Path dataDirectory) {

        public ChatChannel globalChannel() {
            return findChannel("global");
        }

        public ChatChannel localChannel() {
            return findChannel("local");
        }

        public ChatChannel staffChannel() {
            return findChannel("staff");
        }

        public YamlConfig config() {
            return core.getConfig().getConfig();
        }

        private ChatChannel findChannel(String commandName) {
            return core.getChannels().stream()
                    .filter(channel -> channel.getCommandName().equals(commandName))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Missing channel: " + commandName));
        }
    }
}
