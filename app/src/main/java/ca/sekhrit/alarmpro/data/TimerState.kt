package ca.sekhrit.alarmpro.data

import java.util.UUID

data class TimerState(
    val id: String = UUID.randomUUID().toString(),
    val presetId: String? = null,
    val totalSeconds: Int = 0,
    val remainingSeconds: Int = 0,
    val endTimeMillis: Long = 0L,
    val isRunning: Boolean = false,
    val label: String = ""
) {
    fun liveRemainingSeconds(nowMillis: Long = System.currentTimeMillis()): Int {
        if (endTimeMillis <= 0L) return remainingSeconds.coerceAtLeast(0)
        return ((endTimeMillis - nowMillis) / 1000L).toInt().coerceAtLeast(0)
    }

    fun isActive(nowMillis: Long = System.currentTimeMillis()): Boolean {
        return endTimeMillis > nowMillis
    }
}
