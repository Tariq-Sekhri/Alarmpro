package ca.sekhrit.alarmpro.data

enum class SpeechRate(val baseName: String, val wpm: Int, val value: Float) {
    VERY_SLOW("Very slow", 75, 0.5f),
    SLOW("Slow", 112, 0.75f),
    NORMAL("Normal", 150, 1.0f),
    FAST("Fast", 187, 1.25f),
    VERY_FAST("Very fast", 225, 1.5f),
    FASTEST("Fastest", 300, 2.0f);

    val label: String
        get() = "$baseName (~$wpm WPM)"

    companion object {
        fun fromName(name: String?): SpeechRate {
            return entries.find { it.name == name } ?: NORMAL
        }
    }
}
