package com.quranplus.app.core.audio

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.quranplus.app.MainActivity

class AudioNotificationManager(
    private val context: Context
) {
    companion object {
        const val CHANNEL_ID = "quran_audio_playback_channel"
        const val CHANNEL_NAME = "Pemutaran Audio Murottal"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PAUSE = "com.quranplus.app.audio.ACTION_PAUSE"
        const val ACTION_RESUME = "com.quranplus.app.audio.ACTION_RESUME"
        const val ACTION_STOP = "com.quranplus.app.audio.ACTION_STOP"
        const val ACTION_PREVIOUS = "com.quranplus.app.audio.ACTION_PREVIOUS"
        const val ACTION_NEXT = "com.quranplus.app.audio.ACTION_NEXT"
    }

    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Kontrol pemutar audio murottal Al-Qur'an"
            setShowBadge(false)
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.createNotificationChannel(channel)
    }

    fun updateNotification(track: CurrentAudioTrack, state: PlaybackState) {
        if (state is PlaybackState.Idle || state is PlaybackState.Error) {
            dismissNotification()
            return
        }

        val isPlaying = state is PlaybackState.Playing || state is PlaybackState.Buffering

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopPendingIntent = createPendingIntent(ACTION_STOP, 1)
        val togglePendingIntent = createPendingIntent(
            if (isPlaying) ACTION_PAUSE else ACTION_RESUME,
            2
        )
        val prevPendingIntent = createPendingIntent(ACTION_PREVIOUS, 3)
        val nextPendingIntent = createPendingIntent(ACTION_NEXT, 4)

        val title = if (isPlaying) {
            "QS. ${track.surahName} : Ayat ${track.ayahNumber}"
        } else {
            "QS. ${track.surahName} : Ayat ${track.ayahNumber} (Dijeda)"
        }
        val subtitle = "${track.qari.displayName} (${track.ayahNumber}/${track.totalAyahsInSurah})"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(if (isPlaying) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setContentIntent(contentPendingIntent)
            .setDeleteIntent(stopPendingIntent)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        // Previous Action
        if (track.ayahNumber > 1) {
            builder.addAction(
                android.R.drawable.ic_media_previous,
                "Sebelumnya",
                prevPendingIntent
            )
        }

        // Play/Pause Action
        if (isPlaying) {
            builder.addAction(
                android.R.drawable.ic_media_pause,
                "Jeda",
                togglePendingIntent
            )
        } else {
            builder.addAction(
                android.R.drawable.ic_media_play,
                "Lanjutkan",
                togglePendingIntent
            )
        }

        // Next Action
        if (track.ayahNumber < track.totalAyahsInSurah) {
            builder.addAction(
                android.R.drawable.ic_media_next,
                "Berikutnya",
                nextPendingIntent
            )
        }

        // Stop Action
        builder.addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Berhenti",
            stopPendingIntent
        )

        try {
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        } catch (_: SecurityException) {
            // Notifications not permitted
        }
    }

    fun dismissNotification() {
        try {
            notificationManager.cancel(NOTIFICATION_ID)
        } catch (_: Exception) {
        }
    }

    private fun createPendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, AudioNotificationReceiver::class.java).apply {
            this.action = action
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
