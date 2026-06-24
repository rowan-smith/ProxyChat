package dev.rono.proxychat.common.test;

import dev.rono.proxychat.common.ProxyChatCore;
import dev.rono.proxychat.common.channel.ChatChannel;
import dev.rono.proxychat.common.config.ProxyChatYaml;
import dev.rono.proxychat.common.config.migration.ConfigFixtures;
import dev.rono.proxychat.common.platform.SignedChatHandler;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

public final class TestEnvironment {
    public static TestHarness create(Path dataDirectory, FakePlatform platform) throws Exception {
        ConfigFixtures.copyLatestHarness(dataDirectory);

        SignedChatHandler handler = platform.signedChatHandler();
        ProxyChatCore core = new ProxyChatCore(Logger.getLogger("ProxyChatTest"), platform, handler, dataDirectory);
        core.enable(null, null);
        return new TestHarness(core, platform, dataDirectory);
    }

    public static TestHarness createWithLegacyConfig(Path dataDirectory, FakePlatform platform) throws Exception {
        ConfigFixtures.copyResource("v1/1-5/config.yml", dataDirectory.resolve("config.yml"));
        Files.createDirectories(dataDirectory.resolve("chats"));
        SignedChatHandler handler = platform.signedChatHandler();
        ProxyChatCore core = new ProxyChatCore(Logger.getLogger("ProxyChatTest"), platform, handler, dataDirectory);
        core.getConfig().loadDefaults(null, null);
        core.reload();
        return new TestHarness(core, platform, dataDirectory);
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

        public ProxyChatYaml config() {
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
