package ca.sekhrit.alarmpro.util

import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import ca.sekhrit.alarmpro.data.Alarm
import ca.sekhrit.alarmpro.data.AppSettings

object AlarmSoundUtils {
    fun systemDefaultUri(): Uri =
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

    fun resolvePlaybackUri(context: Context, alarm: Alarm?, settings: AppSettings): Uri {
        val explicit = alarm?.soundUri?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
            ?: settings.defaultAlarmSoundUri?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
        return explicit ?: systemDefaultUri()
    }

    fun resolveTimerPlaybackUri(settings: AppSettings): Uri {
        val explicit = settings.timerSoundUri?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
        return explicit ?: systemDefaultUri()
    }

    fun resolvePickerUri(alarm: Alarm?, settings: AppSettings): Uri {
        return alarm?.soundUri?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
            ?: settings.defaultAlarmSoundUri?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
            ?: systemDefaultUri()
    }

    fun getTitle(context: Context, uri: Uri?): String {
        if (uri == null) return "System default"
        val documentName = documentDisplayName(context, uri)
        if (!documentName.isNullOrBlank()) return readableFileName(documentName)

        val ringtoneTitle = try {
            RingtoneManager.getRingtone(context, uri)?.getTitle(context)
        } catch (_: Exception) {
            null
        }
        if (!ringtoneTitle.isNullOrBlank() && '/' !in ringtoneTitle && ':' !in ringtoneTitle) {
            return ringtoneTitle
        }

        val uriName = uri.lastPathSegment
            ?.substringAfterLast('/')
            ?.substringAfterLast(':')
            ?.takeIf { it.isNotBlank() }
        return uriName?.let(::readableFileName) ?: ringtoneTitle ?: "Custom sound"
    }

    private fun readableFileName(name: String): String {
        val withoutExtension = name.substringBeforeLast('.', name)
        return withoutExtension
            .replace(Regex("[_-]+"), " ")
            .trim()
            .split(Regex("\\s+"))
            .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
            .ifBlank { "Custom sound" }
    }

    private fun documentDisplayName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun createPickerIntent(
        context: Context,
        existingUri: Uri?,
        title: String = "Select alarm sound"
    ): Intent {
        return Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, title)
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existingUri ?: systemDefaultUri())
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
        }
    }

    fun parsePickerResult(data: Intent?): Uri? {
        if (data == null) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        }
    }

    fun uriToStorage(uri: Uri?): String? = uri?.toString()
}
