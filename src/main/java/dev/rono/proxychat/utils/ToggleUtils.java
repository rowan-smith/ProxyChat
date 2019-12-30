package dev.rono.proxychat.utils;

import net.md_5.bungee.api.scheduler.ScheduledTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ToggleUtils {

    private Set<UUID> chatIgnored = new HashSet<>();
    private Set<UUID> chatToggled = new HashSet<>();
    private HashMap<UUID, ScheduledTask> delay = new HashMap<>();

    public Boolean toggleIgnore(UUID player) {
        if (this.isIgnored(player)) {
            this.chatIgnored.remove(player);
            return false;
        } else {
            this.chatIgnored.add(player);
            return true;
        }
    }

    public Boolean toggleChat(UUID player) {
        if (this.isToggled(player)) {
            this.chatToggled.remove(player);
            return false;
        } else {
            this.chatToggled.add(player);
            return true;
        }
    }

    public void toggleDelayOn(UUID player, ScheduledTask task) {
        if (!this.isDelayed(player)) {
            this.delay.put(player, task);
        }
    }

    public void toggleDelayOff(UUID player) {
        if (this.isDelayed(player)) {
            this.delay.remove(player);
        }
    }

    public Boolean isIgnored(UUID player) {
        return this.chatIgnored.contains(player);
    }

    public Boolean isToggled(UUID player) {
        return this.chatToggled.contains(player);
    }

    public Boolean isDelayed(UUID player) {
        return delay.containsKey(player);
    }

    public CooldownRunnable getDelayedTask(UUID player) {
        return (CooldownRunnable) this.delay.get(player).getTask();
    }
}
