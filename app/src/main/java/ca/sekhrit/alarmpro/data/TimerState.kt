package ca.sekhrit.alarmpro.data

data class TimerState(
    val totalSeconds: Int = 0,
    val remainingSeconds: Int = 0,
    val endTimeMillis: Long = 0L,
    val isRunning: Boolean = false,
    val label: String = ""
)
