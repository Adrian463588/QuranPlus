package com.quranplus.app.features.chatbot.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo

class ModelDownloadNotificationManager(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Unduhan Model AI",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notifikasi progres pengunduhan model AI dan RAG"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun createForegroundInfo(
        modelName: String,
        progressPercentage: Int = 0,
        bytesDownloaded: Long = 0L,
        totalBytes: Long = 0L,
        speedBytesPerSec: Long = 0L,
        isVerifying: Boolean = false,
        isPaused: Boolean = false,
        pauseReason: String? = null
    ): ForegroundInfo {
        val title = if (isVerifying) {
            "Memverifikasi Model: $modelName"
        } else if (isPaused) {
            "Unduhan Terjeda: $modelName"
        } else {
            "Mengunduh Model AI: $modelName"
        }

        val contentText = if (isVerifying) {
            "Memeriksa integritas SHA-256..."
        } else if (isPaused) {
            pauseReason ?: "Menunggu koneksi internet untuk melanjutkan..."
        } else {
            val downloadedMb = bytesDownloaded / (1024 * 1024)
            val totalMb = totalBytes / (1024 * 1024)
            val speedMb = "%.1f".format(speedBytesPerSec / (1024.0 * 1024.0))
            "$progressPercentage% • $downloadedMb MB / $totalMb MB ($speedMb MB/s)"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(contentText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(
                100,
                progressPercentage.coerceIn(0, 100),
                isVerifying || (totalBytes <= 0L && !isPaused)
            )
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()

        return ForegroundInfo(
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    fun showCompletionNotification(modelName: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Model AI Siap Digunakan")
            .setContentText("$modelName berhasil diunduh dan diverifikasi.")
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun showFailureNotification(modelName: String, errorMessage: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Unduhan Model Gagal")
            .setContentText("$modelName: $errorMessage")
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun dismissNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    companion object {
        const val CHANNEL_ID = "quran_model_download_channel"
        const val NOTIFICATION_ID = 2001
    }
}
