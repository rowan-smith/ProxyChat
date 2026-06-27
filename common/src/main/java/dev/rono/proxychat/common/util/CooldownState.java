package dev.rono.proxychat.common.util;

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
        var elapsed = System.currentTimeMillis() - startTime;
        var remainingMillis = Math.max(0L, delayMillis - elapsed);
        var seconds = (remainingMillis + 999L) / 1000L;
        return String.valueOf(seconds);
    }
}
