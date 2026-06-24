package dev.rono.proxychat.common.util;

import static java.lang.Math.abs;

import java.util.concurrent.TimeUnit;

public final class CooldownState implements Runnable {
    private final long delayMillis;
    private final Runnable onComplete;
    private final long startTime = System.currentTimeMillis();

    public CooldownState(long delayMillis, Runnable onComplete) {
        this.delayMillis = delayMillis;
        this.onComplete = onComplete;
    }

    @Override
    public void run() {
        onComplete.run();
    }

    public String getRemainingSeconds() {
        long elapsed = System.currentTimeMillis() - startTime;
        return String.valueOf(abs(TimeUnit.MILLISECONDS.toSeconds(elapsed - delayMillis)));
    }
}
