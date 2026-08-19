package com.quranplus.app.features.audio.data

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.quranplus.app.core.audio.Qari
import com.quranplus.app.features.audio.domain.AudioDownloadState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

class AudioDownloadScheduler(context: Context) {
    private val workManager = WorkManager.getInstance(context)

    fun enqueue(qari: Qari, surahNumber: Int, totalAyahs: Int) {
        require(surahNumber in 1..114) { "Nomor surah audio tidak valid" }
        require(totalAyahs > 0) { "Jumlah ayat audio tidak valid" }
        val request = OneTimeWorkRequestBuilder<AudioDownloadWorker>()
            .setInputData(
                workDataOf(
                    AudioDownloadContract.KEY_QARI_ID to qari.id,
                    AudioDownloadContract.KEY_SURAH_NUMBER to surahNumber,
                    AudioDownloadContract.KEY_TOTAL_AYAHS to totalAyahs
                )
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .addTag(workName(qari, surahNumber))
            .build()
        workManager.enqueueUniqueWork(
            workName(qari, surahNumber),
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    fun observe(
        qari: Qari,
        surahNumber: Int,
        totalAyahs: Int
    ): Flow<AudioDownloadState> = workManager.getWorkInfosForUniqueWorkFlow(
        workName(qari, surahNumber)
    ).map { infos ->
        infos.firstOrNull()?.toAudioDownloadState(totalAyahs) ?: AudioDownloadState.Idle
    }

    private fun workName(qari: Qari, surahNumber: Int): String =
        "quranplus-audio-${qari.id}-$surahNumber"

    private fun WorkInfo?.toAudioDownloadState(totalAyahs: Int): AudioDownloadState {
        if (this == null) return AudioDownloadState.Idle
        return when (state) {
            WorkInfo.State.ENQUEUED -> AudioDownloadState.Queued
            WorkInfo.State.RUNNING -> when (progress.getString(AudioDownloadContract.KEY_STAGE)) {
                "verifying" -> AudioDownloadState.Verifying(
                    currentAyah = progress.getInt(AudioDownloadContract.KEY_CURRENT_AYAH, 1),
                    totalAyahs = progress.getInt(AudioDownloadContract.KEY_TOTAL_AYAHS, totalAyahs)
                )
                "paused" -> AudioDownloadState.Paused(
                    progress.getString(AudioDownloadContract.KEY_REASON) ?: "Menunggu jaringan"
                )
                else -> AudioDownloadState.Downloading(
                    currentAyah = progress.getInt(AudioDownloadContract.KEY_CURRENT_AYAH, 1),
                    totalAyahs = progress.getInt(AudioDownloadContract.KEY_TOTAL_AYAHS, totalAyahs),
                    progressPercentage = progress.getInt(AudioDownloadContract.KEY_PROGRESS, 0),
                    bytesDownloaded = progress.getLong(AudioDownloadContract.KEY_BYTES_DOWNLOADED, 0L),
                    totalBytes = progress.getLong(AudioDownloadContract.KEY_TOTAL_BYTES, 0L)
                )
            }
            WorkInfo.State.SUCCEEDED -> AudioDownloadState.Completed
            WorkInfo.State.FAILED -> AudioDownloadState.Failed(
                outputData.getString(AudioDownloadContract.KEY_ERROR) ?: "Unduhan audio gagal"
            )
            WorkInfo.State.BLOCKED -> AudioDownloadState.Paused("Unduhan menunggu jaringan")
            WorkInfo.State.CANCELLED -> AudioDownloadState.Failed("Unduhan audio dibatalkan")
        }
    }
}
