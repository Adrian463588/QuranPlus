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
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .addTag("quranplus-model-${model.id}")
            .build()
        workManager.enqueueUniqueWork(
            "quranplus-model-${model.id}",
            ExistingWorkPolicy.KEEP,
            request
        )
        return request.id
    }

    fun observe(id: UUID, model: ModelInfo): Flow<DownloadState> =
        workManager.getWorkInfoByIdFlow(id).map { info -> info.toDownloadState(model) }

    private fun ModelInfo.toWorkerData() = workDataOf(
        "model_id" to id,
        "model_name" to name,
        "model_filename" to filename,
        "model_size_description" to sizeDescription,
        "model_ram_requirement" to ramRequirement,
        "model_url" to downloadUrl,
        "model_source_url" to sourceUrl,
        "model_license_url" to licenseUrl,
        "model_sha256" to sha256,
        "model_version" to version,
        "model_revision" to revision,
        "model_abi" to abi,
        "model_license_status" to licenseStatus,
        "model_size_bytes" to (sizeBytes ?: -1L),
        "model_format" to format,
        "model_runtime" to runtime,
        "model_tokenizer_id" to tokenizerId,
        "model_tokenizer_sha256" to tokenizerSha256,
        "model_minimum_ram_mb" to (minimumRamMb ?: -1),
        "model_citation" to citation
    )

    private fun WorkInfo?.toDownloadState(model: ModelInfo): DownloadState {
        if (this == null) return DownloadState.Idle
        val file = File(modelDirectory, model.filename)
        return when (state) {
            WorkInfo.State.ENQUEUED -> DownloadState.Queued(file)
            WorkInfo.State.RUNNING -> when (progress.getString("stage")) {
                "verifying" -> DownloadState.Verifying
                "paused" -> DownloadState.Paused(progress.getString("reason") ?: "Menunggu jaringan")
                else -> DownloadState.Transferring(
                    bytesDownloaded = progress.getLong("bytes_downloaded", 0L),
                    totalBytes = progress.getLong("total_bytes", 0L),
                    progressPercentage = progress.getInt("progress", 0),
                    speedBytesPerSec = progress.getLong("speed_bytes_per_second", 0L)
                )
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
