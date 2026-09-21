package com.quranplus.app.features.chatbot.data

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.quranplus.app.core.network.DownloadState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

class ModelDownloadScheduler(
    context: Context
) {
    private val workManager = WorkManager.getInstance(context)
    private val modelDirectory = File(context.filesDir, "models")

    fun enqueue(model: ModelInfo): UUID {
        require(model.isDownloadable) {
            "Model belum dapat diunduh: ${model.downloadBlocker}"
        }
        val request = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setInputData(model.toWorkerData())
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            // Large model transfers can be stopped by Android thermal policy.
            // Linear retry prevents a transient stop from becoming an hours-long
            // stale queue while the .tmp candidate remains resumable.
            .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
            .addTag(uniqueName(model))
            .addTag(ALL_MODEL_DOWNLOADS_TAG)
            .build()
        // Only one large artifact may own the device transfer at a time.
        // ExistingWorkPolicy.REPLACE atomically replaces any previous work for this model.
        workManager.enqueueUniqueWork(
            uniqueName(model),
            ExistingWorkPolicy.REPLACE,
            request
        )
        return request.id
    }

    fun cancel(model: ModelInfo) {
        workManager.cancelUniqueWork(uniqueName(model))
    }

    suspend fun findActiveModel(models: List<ModelInfo>): ModelInfo? =
        models.firstOrNull { model ->
            getWorkInfos(model).any { !it.state.isFinished }
        }

    fun observe(id: UUID, model: ModelInfo): Flow<DownloadState> =
        // Observe the unique chain, matching the requested ID first or an active unfinished job.
        // If the new request is still being committed or only older finished work exists,
        // emit Queued rather than prematurely emitting a stale finished/cancelled state.
        workManager.getWorkInfosForUniqueWorkFlow(uniqueName(model)).map { infos ->
            val info = infos.firstOrNull { it.id == id }
                ?: infos.firstOrNull { !it.state.isFinished }
            if (info != null) {
                info.toDownloadState(model)
            } else {
                DownloadState.Queued(File(modelDirectory, model.filename))
            }
        }

    private suspend fun getWorkInfos(model: ModelInfo): List<WorkInfo> =
        workManager.getWorkInfosForUniqueWorkFlow(uniqueName(model)).first()

    private fun uniqueName(model: ModelInfo): String = "quranplus-model-${model.id}"

    private companion object {
        const val ALL_MODEL_DOWNLOADS_TAG = "quranplus-model-download"
    }

    private fun ModelInfo.toWorkerData() = workDataOf(
        "model_id" to id,
        "model_name" to name,
        "model_filename" to filename,
        "model_url" to downloadUrl,
        "model_source_url" to sourceUrl,
        "model_sha256" to sha256,
        "model_size_bytes" to (sizeBytes ?: -1L),
        "model_format" to format,
        "model_runtime" to runtime,
        "model_role" to role.name,
        "model_embedding_dimension" to (embeddingDimension ?: -1),
        "model_tokenizer_asset" to tokenizerAsset,
        "model_tokenizer_type" to tokenizerType,
        "model_tokenizer_sha256" to tokenizerSha256,
        "model_license_id" to licenseId,
        "model_license_url" to licenseUrl
    )

    private fun WorkInfo?.toDownloadState(model: ModelInfo): DownloadState {
        if (this == null) return DownloadState.Idle
        val file = File(modelDirectory, model.filename)
        return when (state) {
            WorkInfo.State.ENQUEUED -> if (runAttemptCount > 0) {
                DownloadState.Paused(
                    progress.getString("reason")
                        ?: "Menunggu percobaan ulang; file sementara akan dilanjutkan"
                )
            } else {
                DownloadState.Queued(file)
            }
            WorkInfo.State.RUNNING -> when (progress.getString("stage")) {
                "verifying" -> DownloadState.Verifying
                "paused" -> DownloadState.Paused(progress.getString("reason") ?: "Menunggu jaringan")
                else -> {
                    val bytesDownloaded = progress.getLong("bytes_downloaded", 0L)
                    val totalBytes = progress.getLong("total_bytes", model.sizeBytes ?: 0L).takeIf { it > 0L } ?: (model.sizeBytes ?: 0L)
                    val rawProgress = progress.getInt("progress", -1)
                    val progressPercentage = if (rawProgress >= 0) {
                        rawProgress
                    } else if (totalBytes > 0L && bytesDownloaded > 0L) {
                        ((bytesDownloaded * 100L) / totalBytes).toInt().coerceIn(0, 100)
                    } else {
                        0
                    }
                    DownloadState.Transferring(
                        bytesDownloaded = bytesDownloaded,
                        totalBytes = totalBytes,
                        progressPercentage = progressPercentage,
                        speedBytesPerSec = progress.getLong("speed_bytes_per_second", 0L)
                    )
                }
            }
            WorkInfo.State.SUCCEEDED -> DownloadState.Completed(file)
            WorkInfo.State.FAILED -> DownloadState.Failed(
                outputData.getString("error") ?: "Unduhan model gagal"
            )
            WorkInfo.State.BLOCKED -> DownloadState.Paused("Unduhan menunggu prasyarat WorkManager")
            WorkInfo.State.CANCELLED -> DownloadState.Failed("Unduhan model dibatalkan")
        }
    }
}
