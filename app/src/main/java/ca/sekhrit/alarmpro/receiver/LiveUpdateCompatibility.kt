package ca.sekhrit.alarmpro.receiver

/**
 * Android 16 (API 36) added promoted ongoing notifications, which can be rendered as
 * status-bar chips. Earlier Android releases ignore this capability, so retain the
 * existing timer-notification behavior there.
 */
internal object LiveUpdateCompatibility {
    const val LIVE_UPDATES_API_LEVEL = 36

    fun shouldRequestPromotion(sdkInt: Int): Boolean = sdkInt >= LIVE_UPDATES_API_LEVEL
}
