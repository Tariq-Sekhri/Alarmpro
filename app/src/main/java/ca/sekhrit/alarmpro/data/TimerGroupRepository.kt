package ca.sekhrit.alarmpro.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class TimerGroupRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadGroups(): List<TimerGroup> {
        val raw = prefs.getString(KEY_GROUPS, null) ?: return emptyList()
        val array = JSONArray(raw)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    TimerGroup(
                        id = item.getString("id"),
                        label = item.getString("label"),
                        isCollapsed = item.optBoolean("collapsed", false),
                        sortOrder = item.optInt("sortOrder", 0)
                    )
                )
            }
        }
    }

    fun saveGroups(groups: List<TimerGroup>) {
        val array = JSONArray()
        groups.forEach { group ->
            array.put(
                JSONObject().apply {
                    put("id", group.id)
                    put("label", group.label)
                    put("collapsed", group.isCollapsed)
                    put("sortOrder", group.sortOrder)
                }
            )
        }
        prefs.edit().putString(KEY_GROUPS, array.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "alarmpro_prefs"
        private const val KEY_GROUPS = "timer_groups"
    }
}
