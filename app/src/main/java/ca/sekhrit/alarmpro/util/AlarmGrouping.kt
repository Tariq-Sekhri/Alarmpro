package ca.sekhrit.alarmpro.util

import ca.sekhrit.alarmpro.data.Alarm
import ca.sekhrit.alarmpro.data.AlarmGroup
import java.time.LocalDateTime

object AlarmGrouping {
    fun effectiveLabel(alarm: Alarm, group: AlarmGroup?, indexInGroup: Int?): String {
        if (alarm.label.isNotBlank()) return alarm.label
        if (group != null && indexInGroup != null) return "${group.label} $indexInGroup"
        return ""
    }

    fun membersOf(groupId: String, alarms: List<Alarm>): List<Alarm> =
        alarms
            .filter { it.groupId == groupId }
            .sortedBy { it.time.toSecondOfDay() }

    fun indexInGroup(alarm: Alarm, groupAlarms: List<Alarm>): Int? {
        if (alarm.groupId == null) return null
        val sorted = groupAlarms.sortedBy { it.time.toSecondOfDay() }
        val index = sorted.indexOfFirst { it.id == alarm.id }
        return if (index < 0) null else index + 1
    }

    fun groupAllEnabled(alarms: List<Alarm>): Boolean = alarms.isNotEmpty() && alarms.all { it.isEnabled }

    fun groupAnyEnabled(alarms: List<Alarm>): Boolean = alarms.any { it.isEnabled }

    fun groupSkipSummary(alarms: List<Alarm>, now: LocalDateTime = LocalDateTime.now()): Int =
        alarms.count { RepeatCalculator.hasSkipScheduled(it, now) }

    sealed interface ListEntry {
        data class GroupHeader(
            val group: AlarmGroup,
            val alarms: List<Alarm>,
            val allEnabled: Boolean,
            val skipScheduledCount: Int
        ) : ListEntry

        data class AlarmRow(
            val alarm: Alarm,
            val group: AlarmGroup?,
            val indexInGroup: Int?
        ) : ListEntry
    }

    private sealed interface ListSection {
        val sortKey: Long

        data class GroupSection(
            val group: AlarmGroup,
            val members: List<Alarm>,
            override val sortKey: Long
        ) : ListSection

        data class UngroupedAlarm(
            val alarm: Alarm,
            override val sortKey: Long
        ) : ListSection
    }

    fun buildListEntries(
        alarms: List<Alarm>,
        groups: List<AlarmGroup>,
        now: LocalDateTime = LocalDateTime.now(),
        sortByNextTrigger: Boolean = true
    ): List<ListEntry> {
        val groupedIds = alarms.mapNotNull { it.groupId }.toSet()
        val sections = mutableListOf<ListSection>()

        groups
            .filter { it.id in groupedIds }
            .forEach { group ->
                val labelMembers = membersOf(group.id, alarms)
                if (labelMembers.isEmpty()) return@forEach
                val displayMembers = sortAlarms(labelMembers, now, sortByNextTrigger)
                sections += ListSection.GroupSection(
                    group = group,
                    members = displayMembers,
                    sortKey = sectionSortKey(labelMembers, now, sortByNextTrigger)
                )
            }

        alarms
            .filter { it.groupId == null }
            .forEach { alarm ->
                sections += ListSection.UngroupedAlarm(
                    alarm = alarm,
                    sortKey = alarmSortKey(alarm, now, sortByNextTrigger)
                )
            }

        val entries = mutableListOf<ListEntry>()
        sections.sortedBy { it.sortKey }.forEach { section ->
            when (section) {
                is ListSection.GroupSection -> {
                    val labelMembers = membersOf(section.group.id, alarms)
                    entries += ListEntry.GroupHeader(
                        group = section.group,
                        alarms = labelMembers,
                        allEnabled = groupAllEnabled(labelMembers),
                        skipScheduledCount = groupSkipSummary(labelMembers, now)
                    )
                    if (!section.group.isCollapsed) {
                        section.members.forEach { alarm ->
                            entries += ListEntry.AlarmRow(
                                alarm = alarm,
                                group = section.group,
                                indexInGroup = indexInGroup(alarm, labelMembers)
                            )
                        }
                    }
                }
                is ListSection.UngroupedAlarm -> {
                    entries += ListEntry.AlarmRow(
                        alarm = section.alarm,
                        group = null,
                        indexInGroup = null
                    )
                }
            }
        }

        return entries
    }

    private fun sortAlarms(
        alarms: List<Alarm>,
        now: LocalDateTime,
        sortByNextTrigger: Boolean
    ): List<Alarm> {
        return alarms.sortedWith(
            compareBy<Alarm> { !it.isEnabled }
                .thenBy {
                    if (sortByNextTrigger) {
                        RepeatCalculator.nextTriggerMillis(it, now)
                    } else {
                        it.time.toSecondOfDay().toLong()
                    }
                }
        )
    }

    private fun alarmSortKey(
        alarm: Alarm,
        now: LocalDateTime,
        sortByNextTrigger: Boolean
    ): Long {
        if (!alarm.isEnabled) return Long.MAX_VALUE
        return if (sortByNextTrigger) {
            RepeatCalculator.nextTriggerMillis(alarm, now)
        } else {
            alarm.time.toSecondOfDay().toLong()
        }
    }

    private fun sectionSortKey(
        members: List<Alarm>,
        now: LocalDateTime,
        sortByNextTrigger: Boolean
    ): Long {
        val enabled = members.filter { it.isEnabled }
        if (enabled.isEmpty()) return Long.MAX_VALUE
        return if (sortByNextTrigger) {
            enabled.minOf { RepeatCalculator.nextTriggerMillis(it, now) }
        } else {
            enabled.minOf { it.time.toSecondOfDay().toLong() }
        }
    }
}
