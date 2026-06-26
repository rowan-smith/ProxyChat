package dev.rono.proxychat.common.util;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ToggleUtilsTest {
    private final UUID playerId = UUID.randomUUID();

    @Test
    void togglesChatOnAndOff() {

        // arrange & act
        ToggleUtils utils = new ToggleUtils();

        // assert
        assertThat(utils.toggleChat(playerId)).isTrue();
        assertThat(utils.isToggled(playerId)).isTrue();
        assertThat(utils.toggleChat(playerId)).isFalse();
        assertThat(utils.isToggled(playerId)).isFalse();
    }

    @Test
    void togglesIgnoreOnAndOff() {

        // arrange & act
        ToggleUtils utils = new ToggleUtils();

        // assert
        assertThat(utils.toggleIgnore(playerId)).isTrue();
        assertThat(utils.isIgnored(playerId)).isTrue();
        assertThat(utils.toggleIgnore(playerId)).isFalse();
        assertThat(utils.isIgnored(playerId)).isFalse();
    }

    @Test
    void tracksCommandDelay() {

        // arrange
        ToggleUtils utils = new ToggleUtils();
        boolean[] cleared = {false};

        // act
        utils.startDelay(playerId, 250, () -> {
            utils.clearDelay(playerId);
            cleared[0] = true;
        });
        boolean delayedBeforeRun = utils.isDelayed(playerId);
        CooldownState delay = utils.getDelay(playerId);
        delay.run();

        // assert
        assertThat(delayedBeforeRun).isTrue();
        assertThat(delay).isNotNull();
        assertThat(cleared[0]).isTrue();
        assertThat(utils.isDelayed(playerId)).isFalse();
    }

    @Test
    void doesNotReplaceExistingDelay() {

        // arrange
        ToggleUtils utils = new ToggleUtils();

        // act
        utils.startDelay(playerId, 100, () -> utils.clearDelay(playerId));
        CooldownState first = utils.getDelay(playerId);
        utils.startDelay(playerId, 500, () -> utils.clearDelay(playerId));

        // assert
        assertThat(utils.getDelay(playerId)).isSameAs(first);
    }
}
