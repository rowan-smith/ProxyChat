package dev.rono.proxychat.utils;

import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.lang.Math.abs;

public class CooldownRunnable implements Runnable {

    public UUID player;
    public ToggleUtils utils;
    private final Long delay;

    public Long startTime = System.currentTimeMillis();

    public CooldownRunnable(ProxiedPlayer player, ToggleUtils utils, Long delay) {
        this.player = player.getUniqueId();
        this.utils = utils;
        this.delay = delay;
    }

    @Override
    public void run() {
        this.utils.toggleDelayOff(this.player);
    }

    public String getTime() {
        return String.format("%s", abs(TimeUnit.MILLISECONDS.toSeconds((System.currentTimeMillis() - this.startTime) - delay)));
    }
}
