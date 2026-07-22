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

    fun buildListEntries(
        alarms: List<Alarm>,
        groups: List<AlarmGroup>,
        now: LocalDateTime = LocalDateTime.now(),
        sortByNextTrigger: Boolean = true
    ): List<ListEntry> {
        val entries = mutableListOf<ListEntry>()
        val groupedIds = alarms.mapNotNull { it.groupId }.toSet()
        val activeGroups = groups
            .filter { it.id in groupedIds }
            .sortedWith { a, b ->
                val aMembers = membersOf(a.id, alarms)
                val bMembers = membersOf(b.id, alarms)
                val aKey = if (sortByNextTrigger) {
                    aMembers.filter { it.isEnabled }.minOfOrNull { RepeatCalculator.nextTriggerMillis(it, now) }
                        ?: Long.MAX_VALUE
                } else {
                    aMembers.minOfOrNull { it.time.toSecondOfDay().toLong() } ?: Long.MAX_VALUE
                }
                val bKey = if (sortByNextTrigger) {
                    bMembers.filter { it.isEnabled }.minOfOrNull { RepeatCalculator.nextTriggerMillis(it, now) }
                        ?: Long.MAX_VALUE
                } else {
                    bMembers.minOfOrNull { it.time.toSecondOfDay().toLong() } ?: Long.MAX_VALUE
                }
                aKey.compareTo(bKey)
            }

        activeGroups.forEach { group ->
            val members = membersOf(group.id, alarms)
            if (members.isEmpty()) return@forEach

            entries += ListEntry.GroupHeader(
                group = group,
                alarms = members,
                allEnabled = groupAllEnabled(members),
                skipScheduledCount = groupSkipSummary(members, now)
            )

            if (!group.isCollapsed) {
                members.forEach { alarm ->
                    entries += ListEntry.AlarmRow(
                        alarm = alarm,
                        group = group,
                        indexInGroup = indexInGroup(alarm, members)
                    )
                }
            }
        }

        val ungrouped = alarms
            .filter { it.groupId == null }
            .sortedWith(
                compareBy<Alarm> { !it.isEnabled }
                    .thenBy {
                        if (sortByNextTrigger) RepeatCalculator.nextTriggerMillis(it, now)
                        else it.time.toSecondOfDay().toLong()
                    }
            )

        ungrouped.forEach { alarm ->
            entries += ListEntry.AlarmRow(alarm = alarm, group = null, indexInGroup = null)
        }

        return entries
    }
}
