package dev.rono.proxychat.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CooldownStateTest {
    @Test
    void reportsRemainingSeconds() {
        CooldownState cooldown = new CooldownState(5000, () -> {
        });

        assertThat(cooldown.getRemainingSeconds()).matches("\\d+");
    }

    @Test
    void runsCompletionCallback() {
        boolean[] completed = {false};
        CooldownState cooldown = new CooldownState(1000, () -> completed[0] = true);

        cooldown.run();

        assertThat(completed[0]).isTrue();
    }
}
