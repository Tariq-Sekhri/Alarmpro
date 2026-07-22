package ca.sekhrit.alarmpro.data

import java.util.UUID

data class TimerPreset(
    val id: String = UUID.randomUUID().toString(),
    val totalSeconds: Int,
    val label: String = ""
)
