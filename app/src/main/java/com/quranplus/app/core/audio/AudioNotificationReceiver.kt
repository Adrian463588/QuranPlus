package com.quranplus.app.core.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.koin.core.context.GlobalContext

class AudioNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val audioPlayerManager = try {
            GlobalContext.getOrNull()?.get<AudioPlayerManager>()
        } catch (_: Exception) {
            null
        } ?: return

        when (action) {
            AudioNotificationManager.ACTION_PAUSE -> audioPlayerManager.pause()
            AudioNotificationManager.ACTION_RESUME -> audioPlayerManager.resume()
            AudioNotificationManager.ACTION_STOP -> audioPlayerManager.stop()
            AudioNotificationManager.ACTION_PREVIOUS -> audioPlayerManager.previousAyah()
            AudioNotificationManager.ACTION_NEXT -> audioPlayerManager.nextAyah()
        }
    }
}
