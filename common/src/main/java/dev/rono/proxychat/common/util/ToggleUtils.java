package dev.rono.proxychat.common.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ToggleUtils {
    private final Set<UUID> chatIgnored = new HashSet<>();
    private final Set<UUID> chatToggled = new HashSet<>();
    private final Map<UUID, CooldownState> delays = new HashMap<>();

    public boolean toggleIgnore(UUID player) {
        if (isIgnored(player)) {
            chatIgnored.remove(player);
            return false;
        }

        chatIgnored.add(player);

        return true;
    }

    public boolean toggleChat(UUID player) {
        if (isToggled(player)) {
            chatToggled.remove(player);
            return false;
        }

        chatToggled.add(player);

        return true;
    }

    public void startDelay(UUID player, long delayMillis, Runnable onComplete) {
        if (!isDelayed(player)) {
            delays.put(player, new CooldownState(delayMillis, onComplete));
        }
    }

    public void clearDelay(UUID player) {
        delays.remove(player);
    }

    public boolean isIgnored(UUID player) {
        return chatIgnored.contains(player);
    }

    public boolean isToggled(UUID player) {
        return chatToggled.contains(player);
    }

    public boolean isDelayed(UUID player) {
        return delays.containsKey(player);
    }

    public CooldownState getDelay(UUID player) {
        return delays.get(player);
    }
}
