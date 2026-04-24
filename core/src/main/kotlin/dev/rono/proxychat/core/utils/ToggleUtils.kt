package dev.rono.proxychat.core.utils

import java.util.*

/**
 * Utility class for managing chat toggles and cooldowns
 */
class ToggleUtils {
    private val chatIgnored = HashSet<UUID>()
    private val chatToggled = HashSet<UUID>()
    private val delay = HashMap<UUID, CooldownRunnable>()

    fun toggleIgnore(player: UUID): Boolean {
        return if (isIgnored(player)) {
            chatIgnored.remove(player)
            false
        } else {
            chatIgnored.add(player)
            true
        }
    }

    fun toggleChat(player: UUID): Boolean {
        return if (isToggled(player)) {
            chatToggled.remove(player)
            false
        } else {
            chatToggled.add(player)
            true
        }
    }

    fun toggleDelayOn(player: UUID, runnable: CooldownRunnable) {
        if (!isDelayed(player)) {
            delay[player] = runnable
        }
    }

    fun toggleDelayOff(player: UUID) {
        if (isDelayed(player)) {
            delay.remove(player)
        }
    }

    fun isIgnored(player: UUID): Boolean = chatIgnored.contains(player)
    fun isToggled(player: UUID): Boolean = chatToggled.contains(player)
    fun isDelayed(player: UUID): Boolean = delay.containsKey(player)

    fun getDelayedTask(player: UUID): CooldownRunnable? = delay[player]
}
