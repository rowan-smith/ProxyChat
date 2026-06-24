package dev.rono.proxychat.common.util;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ToggleUtilsTest {
    private final UUID playerId = UUID.randomUUID();

    @Test
    void togglesChatOnAndOff() {
        ToggleUtils utils = new ToggleUtils();

        assertThat(utils.toggleChat(playerId)).isTrue();
        assertThat(utils.isToggled(playerId)).isTrue();

        assertThat(utils.toggleChat(playerId)).isFalse();
        assertThat(utils.isToggled(playerId)).isFalse();
    }

    @Test
    void togglesIgnoreOnAndOff() {
        ToggleUtils utils = new ToggleUtils();

        assertThat(utils.toggleIgnore(playerId)).isTrue();
        assertThat(utils.isIgnored(playerId)).isTrue();

        assertThat(utils.toggleIgnore(playerId)).isFalse();
        assertThat(utils.isIgnored(playerId)).isFalse();
    }

    @Test
    void tracksCommandDelay() {
        ToggleUtils utils = new ToggleUtils();
        boolean[] cleared = {false};

        utils.startDelay(playerId, 250, () -> {
            utils.clearDelay(playerId);
            cleared[0] = true;
        });

        assertThat(utils.isDelayed(playerId)).isTrue();
        assertThat(utils.getDelay(playerId)).isNotNull();

        utils.getDelay(playerId).run();

        assertThat(cleared[0]).isTrue();
        assertThat(utils.isDelayed(playerId)).isFalse();
    }

    @Test
    void doesNotReplaceExistingDelay() {
        ToggleUtils utils = new ToggleUtils();
        utils.startDelay(playerId, 100, () -> utils.clearDelay(playerId));
        CooldownState first = utils.getDelay(playerId);

        utils.startDelay(playerId, 500, () -> utils.clearDelay(playerId));

        assertThat(utils.getDelay(playerId)).isSameAs(first);
    }
}
