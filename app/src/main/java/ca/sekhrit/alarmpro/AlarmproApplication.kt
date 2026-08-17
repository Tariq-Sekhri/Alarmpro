package ca.sekhrit.alarmpro

import android.app.Application
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import java.util.Locale

class AlarmproApplication : Application() {
    
    companion object {
        var tts: TextToSpeech? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                tts?.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }
        }
    }
}
