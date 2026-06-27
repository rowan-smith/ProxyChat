package dev.rono.proxychat.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CooldownStateTest {
    @Test
    void reportsRemainingSeconds() {

        // arrange
        var cooldown = new CooldownState(5000, () -> {

        // act
        });

        // assert
        assertThat(cooldown.getRemainingSeconds()).matches("\\d+");
    }

    @Test
    void reportsZeroAfterExpiry() {

        // arrange
        var cooldown = new CooldownState(1, () -> {

        // act
        });

        // assert
        try {
            Thread.sleep(5L);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
        assertThat(cooldown.getRemainingSeconds()).isEqualTo("0");
    }

    @Test
    void runsCompletionCallback() {

        // arrange
        var completed = new boolean[]{false};
        var cooldown = new CooldownState(1000, () -> completed[0] = true);

        // act
        cooldown.run();

        // assert
        assertThat(completed[0]).isTrue();
    }
}
