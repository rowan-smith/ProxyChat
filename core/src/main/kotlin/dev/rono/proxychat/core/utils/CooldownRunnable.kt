package dev.rono.proxychat.core.utils

import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Runnable for managing command cooldowns
 */
class CooldownRunnable(
    private val player: UUID,
    private val utils: ToggleUtils,
    private val delay: Long
) : Runnable {
    private val startTime = System.currentTimeMillis()

    override fun run() {
        utils.toggleDelayOff(player)
    }

    fun getTime(): String {
        return String.format("%s", abs(TimeUnit.MILLISECONDS.toSeconds((System.currentTimeMillis() - startTime) - delay)))
    }
}
