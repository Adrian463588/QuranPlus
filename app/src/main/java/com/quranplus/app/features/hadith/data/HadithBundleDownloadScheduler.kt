package com.quranplus.app.features.hadith.data

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

sealed interface HadithBundleWorkState {
    data object Idle : HadithBundleWorkState
    data object Queued : HadithBundleWorkState
    data class Downloading(
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val progressPercentage: Int
    ) : HadithBundleWorkState
    data object Importing : HadithBundleWorkState
    data object Completed : HadithBundleWorkState
    data class Failed(val message: String) : HadithBundleWorkState
}

class HadithBundleDownloadScheduler(
    context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    fun enqueue() = OneTimeWorkRequestBuilder<HadithBundleDownloadWorker>()
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        )
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
        .addTag(WORK_TAG)
        .build()
        .also { request ->
            workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }
        .id

    fun observe(): Flow<HadithBundleWorkState> =
        workManager.getWorkInfosForUniqueWorkFlow(WORK_NAME).map { infos ->
            infos.firstOrNull().toHadithBundleWorkState()
        }

    private fun WorkInfo?.toHadithBundleWorkState(): HadithBundleWorkState {
        if (this == null) return HadithBundleWorkState.Idle
        return when (state) {
            WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> HadithBundleWorkState.Queued
            WorkInfo.State.RUNNING -> when (progress.getString(KEY_STAGE)) {
                STAGE_IMPORTING -> HadithBundleWorkState.Importing
                else -> HadithBundleWorkState.Downloading(
                    bytesDownloaded = progress.getLong(KEY_BYTES, 0L),
                    totalBytes = progress.getLong(KEY_TOTAL_BYTES, 0L),
                    progressPercentage = progress.getInt(KEY_PROGRESS, 0)
                )
            }
            WorkInfo.State.SUCCEEDED -> HadithBundleWorkState.Completed
            WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> HadithBundleWorkState.Failed(
                outputData.getString(KEY_ERROR) ?: "Download bundle Hadist gagal"
            )
        }
    }

    companion object {
        const val WORK_NAME = "quranplus-hadith-bundle"
        const val WORK_TAG = "quranplus-hadith-bundle"
        const val KEY_STAGE = "stage"
        const val KEY_BYTES = "bytes_downloaded"
        const val KEY_TOTAL_BYTES = "total_bytes"
        const val KEY_PROGRESS = "progress"
        const val KEY_ERROR = "error"
        const val STAGE_IMPORTING = "importing"
    }
}
