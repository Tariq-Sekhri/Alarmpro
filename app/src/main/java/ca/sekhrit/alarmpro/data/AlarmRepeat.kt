package ca.sekhrit.alarmpro.data

/** Changing a schedule invalidates pending snoozes and skips from the old rule. */
fun Alarm.withRepeatSchedule(schedule: RepeatSchedule): Alarm = copy(
    repeat = schedule,
    skipUntilEpochDay = null,
    snoozedUntilEpochMillis = null,
    deleteAfterDismiss = deleteAfterDismiss && schedule.type == RepeatType.ONCE
)
