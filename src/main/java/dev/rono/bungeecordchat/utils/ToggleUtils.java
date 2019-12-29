package dev.rono.bungeecordchat.utils;

import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ToggleUtils {

    private Set<UUID> chatIgnored = new HashSet<>();
    private Set<UUID> chatToggled = new HashSet<>();
    private Set<UUID> chatDelayed = new HashSet<>();

    public Boolean toggleIgnore(ProxiedPlayer player) {
        if (this.isIgnored(player)) {
            this.chatIgnored.remove(player.getUniqueId());
            return false;
        } else {
            this.chatIgnored.add(player.getUniqueId());
            return true;
        }
    }

    public Boolean toggleChat(ProxiedPlayer player) {
        if (this.isToggled(player)) {
            this.chatToggled.remove(player.getUniqueId());
            return false;
        } else {
            this.chatToggled.add(player.getUniqueId());
            return true;
        }
    }

    public void toggleDelay(ProxiedPlayer player) {
        if (this.isDelayed(player)) {
            this.chatDelayed.remove(player.getUniqueId());
        } else {
            this.chatDelayed.add(player.getUniqueId());
        }
    }

    public Boolean isIgnored(ProxiedPlayer player) {
        return this.chatIgnored.contains(player.getUniqueId());
    }

    public Boolean isToggled(ProxiedPlayer player) {
        return this.chatToggled.contains(player.getUniqueId());
    }

    public Boolean isDelayed(ProxiedPlayer player) {
        return this.chatDelayed.contains(player.getUniqueId());
    }
}
