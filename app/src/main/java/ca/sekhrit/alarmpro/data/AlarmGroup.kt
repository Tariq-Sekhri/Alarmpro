package ca.sekhrit.alarmpro.data

import java.util.UUID

data class AlarmGroup(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val isCollapsed: Boolean = false,
    val sortOrder: Int = 0
)
