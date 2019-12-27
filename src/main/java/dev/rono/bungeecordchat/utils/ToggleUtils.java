package dev.rono.bungeecordchat.utils;

import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.ArrayList;

public class ToggleUtils {

    private ArrayList<ProxiedPlayer> chatIgnored = new ArrayList<>();
    private ArrayList<ProxiedPlayer> chatToggled = new ArrayList<>();
    private ArrayList<ProxiedPlayer> chatDelayed = new ArrayList<>();

    public Boolean toggleIgnore(ProxiedPlayer player) {
        if (this.isIgnored(player)) {
            this.chatIgnored.remove(player);
            return false;
        } else {
            this.chatIgnored.add(player);
            return true;
        }
    }

    public Boolean toggleChat(ProxiedPlayer player) {
        if (this.isToggled(player)) {
            this.chatToggled.remove(player);
            return false;
        } else {
            this.chatToggled.add(player);
            return true;
        }
    }

    public void toggleDelay(ProxiedPlayer player) {
        if (this.isDelayed(player)) {
            this.chatDelayed.remove(player);
        } else {
            this.chatDelayed.add(player);
        }
    }

    public Boolean isIgnored(ProxiedPlayer player) {
        return this.chatIgnored.contains(player);
    }

    public Boolean isToggled(ProxiedPlayer player) {
        return this.chatToggled.contains(player);
    }

    public Boolean isDelayed(ProxiedPlayer player) {
        return this.chatDelayed.contains(player);
    }
}
