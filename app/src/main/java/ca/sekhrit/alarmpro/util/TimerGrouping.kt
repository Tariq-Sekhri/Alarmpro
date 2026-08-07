package ca.sekhrit.alarmpro.util

import ca.sekhrit.alarmpro.data.TimerPreset
import ca.sekhrit.alarmpro.data.TimerGroup
import ca.sekhrit.alarmpro.data.TimerState
import ca.sekhrit.alarmpro.data.TimerSortMode

object TimerGrouping {
    fun effectiveLabel(preset: TimerPreset, group: TimerGroup?, indexInGroup: Int?): String {
        if (preset.label.isNotBlank()) return preset.label
        if (group != null && indexInGroup != null) return "${group.label} $indexInGroup"
        return ""
    }

    fun membersOf(groupId: String, presets: List<TimerPreset>): List<TimerPreset> =
        presets.filter { it.groupId == groupId }

    fun indexInGroup(preset: TimerPreset, groupPresets: List<TimerPreset>): Int? {
        if (preset.groupId == null) return null
        val sorted = groupPresets.sortedBy { it.totalSeconds }
        val index = sorted.indexOfFirst { it.id == preset.id }
        return if (index < 0) null else index + 1
    }

    sealed interface ListEntry {
        data class GroupHeader(
            val group: TimerGroup,
            val presets: List<TimerPreset>,
            val isActive: Boolean
        ) : ListEntry

        data class PresetRow(
            val preset: TimerPreset,
            val group: TimerGroup?,
            val indexInGroup: Int?
        ) : ListEntry
    }

    private sealed interface ListSection {
        val sortKey: Long

        data class GroupSection(
            val group: TimerGroup,
            val members: List<TimerPreset>,
            override val sortKey: Long
        ) : ListSection

        data class UngroupedPreset(
            val preset: TimerPreset,
            override val sortKey: Long
        ) : ListSection
    }

    fun buildListEntries(
        presets: List<TimerPreset>,
        groups: List<TimerGroup>,
        activeTimers: Map<String, TimerState>,
        clockMillis: Long,
        sortMode: TimerSortMode,
        activeTimersFirst: Boolean
    ): List<ListEntry> {
        val groupedIds = presets.mapNotNull { it.groupId }.toSet()
        val sections = mutableListOf<ListSection>()

        groups
            .filter { it.id in groupedIds }
            .forEach { group ->
                val labelMembers = membersOf(group.id, presets)
                if (labelMembers.isEmpty()) return@forEach
                val displayMembers = sortPresets(
                    labelMembers,
                    activeTimers,
                    clockMillis,
                    sortMode,
                    activeTimersFirst
                )
                sections += ListSection.GroupSection(
                    group = group,
                    members = displayMembers,
                    sortKey = sectionSortKey(
                        labelMembers,
                        activeTimers,
                        clockMillis,
                        sortMode,
                        activeTimersFirst
                    )
                )
            }

        presets
            .filter { it.groupId == null }
            .forEach { preset ->
                sections += ListSection.UngroupedPreset(
                    preset = preset,
                    sortKey = presetSortKey(preset, activeTimers, clockMillis, sortMode, activeTimersFirst)
                )
            }

        val entries = mutableListOf<ListEntry>()
        sections.sortedBy { it.sortKey }.forEach { section ->
            when (section) {
                is ListSection.GroupSection -> {
                    val labelMembers = membersOf(section.group.id, presets)
                    entries += ListEntry.GroupHeader(
                        group = section.group,
                        presets = labelMembers,
                        isActive = labelMembers.any { activeTimers[it.id]?.isActive(clockMillis) == true }
                    )
                    if (!section.group.isCollapsed) {
                        section.members.forEach { preset ->
                            entries += ListEntry.PresetRow(
                                preset = preset,
                                group = section.group,
                                indexInGroup = indexInGroup(preset, labelMembers)
                            )
                        }
                    }
                }
                is ListSection.UngroupedPreset -> {
                    entries += ListEntry.PresetRow(
                        preset = section.preset,
                        group = null,
                        indexInGroup = null
                    )
                }
            }
        }

        return entries
    }

    private fun sortPresets(
        presets: List<TimerPreset>,
        activeTimers: Map<String, TimerState>,
        clockMillis: Long,
        sortMode: TimerSortMode,
        activeTimersFirst: Boolean
    ): List<TimerPreset> {
        val timeComparator = compareBy<TimerPreset> { preset ->
            when (sortMode) {
                TimerSortMode.TIME_ASC -> preset.totalSeconds.toLong()
                TimerSortMode.TIME_DESC -> -preset.totalSeconds.toLong()
                TimerSortMode.MANUAL -> 0L // Keep stable for manual
            }
        }
        return presets.sortedWith(
            if (activeTimersFirst) {
                compareBy<TimerPreset> { preset ->
                    !(activeTimers[preset.id]?.isActive(clockMillis) == true)
                }.then(timeComparator)
            } else {
                timeComparator
            }
        )
    }

    private fun presetSortKey(
        preset: TimerPreset,
        activeTimers: Map<String, TimerState>,
        clockMillis: Long,
        sortMode: TimerSortMode,
        activeTimersFirst: Boolean
    ): Long {
        val isActive = activeTimers[preset.id]?.isActive(clockMillis) == true
        if (activeTimersFirst && !isActive) return Long.MAX_VALUE
        return when (sortMode) {
            TimerSortMode.TIME_ASC -> preset.totalSeconds.toLong()
            TimerSortMode.TIME_DESC -> -preset.totalSeconds.toLong()
            TimerSortMode.MANUAL -> 0L
        }
    }

    private fun sectionSortKey(
        members: List<TimerPreset>,
        activeTimers: Map<String, TimerState>,
        clockMillis: Long,
        sortMode: TimerSortMode,
        activeTimersFirst: Boolean
    ): Long {
        val isActive = members.any { activeTimers[it.id]?.isActive(clockMillis) == true }
        if (activeTimersFirst && !isActive) return Long.MAX_VALUE
        
        return when (sortMode) {
            TimerSortMode.TIME_ASC -> members.minOf { it.totalSeconds.toLong() }
            TimerSortMode.TIME_DESC -> members.minOf { -it.totalSeconds.toLong() }
            TimerSortMode.MANUAL -> 0L
        }
    }
}
