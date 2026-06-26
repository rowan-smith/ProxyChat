package dev.rono.proxychat.common;

import dev.rono.proxychat.common.test.FakePlatform;
import dev.rono.proxychat.common.test.RecordingSignedChatHandler;
import dev.rono.proxychat.common.test.TestEnvironment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ProxyChatCoreTest {
    @TempDir Path dataDirectory;

    @Test
    void enableLoadsConfiguredChannels() throws Exception {

        // arrange
        FakePlatform platform = new FakePlatform();
        RecordingSignedChatHandler handler = new RecordingSignedChatHandler();
        platform.setSignedChatHandler(handler);

        // act
        TestEnvironment.TestHarness harness = TestEnvironment.create(dataDirectory, platform);

        // assert
        assertThat(harness.core().getChannels()).hasSize(3);
        assertThat(harness.globalChannel().getCommandName()).isEqualTo("global");
        assertThat(harness.core().getSignedChatHandler()).isSameAs(handler);
    }

    @Test
    void reloadRefreshesChannels() throws Exception {

        // arrange
        FakePlatform platform = new FakePlatform();
        platform.setSignedChatHandler(new RecordingSignedChatHandler());
        TestEnvironment.TestHarness harness = TestEnvironment.create(dataDirectory, platform);

        // act
        harness.core().reload();

        // assert
        assertThat(harness.core().getChannels()).hasSize(3);
    }
}
