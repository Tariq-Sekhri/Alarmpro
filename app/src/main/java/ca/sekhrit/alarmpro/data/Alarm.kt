package ca.sekhrit.alarmpro.data

import java.time.LocalTime
import java.util.UUID

data class Alarm(
    val id: String = UUID.randomUUID().toString(),
    val time: LocalTime,
    val label: String = "",
    val isEnabled: Boolean = true,
    val repeat: RepeatSchedule = RepeatSchedule(),
    val vibrate: Boolean = true,
    val readLabelAloud: Boolean = false,
    val snoozeEnabled: Boolean = true,
    val snoozeMinutes: Int? = null,
    val deleteAfterDismiss: Boolean = false,
    val wakeCheckEnabled: Boolean = false,
    val wakeCheckDelayMinutes: Int = 10,
    val wakeCheckResponseMinutes: Int = 5,
    val skipUntilEpochDay: Long? = null,
    val soundUri: String? = null,
    val groupId: String? = null,
    val snoozedUntilEpochMillis: Long? = null,
    val createdEpochDay: Long = java.time.LocalDate.now().toEpochDay()
)
