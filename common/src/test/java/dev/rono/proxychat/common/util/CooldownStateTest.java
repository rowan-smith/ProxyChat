package dev.rono.proxychat.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CooldownStateTest {
    @Test
    void reportsRemainingSeconds() {

        // arrange
        CooldownState cooldown = new CooldownState(5000, () -> {

        // act
        });

        // assert
        assertThat(cooldown.getRemainingSeconds()).matches("\\d+");
    }

    @Test
    void runsCompletionCallback() {

        // arrange
        boolean[] completed = {false};
        CooldownState cooldown = new CooldownState(1000, () -> completed[0] = true);

        // act
        cooldown.run();

        // assert
        assertThat(completed[0]).isTrue();
    }
}
