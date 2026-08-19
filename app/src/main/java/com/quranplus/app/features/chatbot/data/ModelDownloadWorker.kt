package com.quranplus.app.features.chatbot.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.quranplus.app.core.network.DownloadState
import com.quranplus.app.core.network.ResumableDownloader
import com.quranplus.app.features.rag.data.SafAssetStore
import com.quranplus.app.features.settings.data.PreferencesManager
import kotlinx.coroutines.flow.collect
import java.io.File

/** Background transfer for a pinned chatbot or embedding asset. */
class ModelDownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val model = readManifestInput()
            ?: return failure("Manifest model tidak lengkap atau tidak terverifikasi")
        val preferences = PreferencesManager(applicationContext)
        val assetStore = SafAssetStore(applicationContext, preferences)
        val repository = ModelRepository(applicationContext, assetStore)
        val target = repository.getModelFile(model.filename)
        if (repository.isModelReady(model)) {
            return runCatching {
                repository.persistVerifiedModel(model)
                Result.success(workDataOf(KEY_FILENAME to target.name))
            }.getOrElse {
                failure("Asset siap secara lokal, tetapi belum tersimpan ke Folder SAF. Pilih folder SAF lalu coba lagi.")
            }
        }
        var terminalState: DownloadState = DownloadState.Idle

        ResumableDownloader(applicationContext)
            .downloadFile(
                url = model.downloadUrl,
                targetDestination = target,
                expectedSha256 = model.sha256.orEmpty(),
                expectedSizeBytes = model.sizeBytes
            )
            .collect { state ->
                terminalState = state
                when (state) {
                    is DownloadState.Transferring -> setProgress(progressData(state))
                    DownloadState.Verifying -> setProgress(workDataOf(KEY_STAGE to "verifying"))
                    is DownloadState.Paused -> setProgress(
                        workDataOf(KEY_STAGE to "paused", KEY_REASON to state.reason)
                    )
                    else -> Unit
                }
            }

        return when (val state = terminalState) {
            is DownloadState.Completed -> runCatching {
                repository.persistVerifiedModel(model)
                if (!repository.isModelReady(model)) {
                    error("Model gagal diverifikasi setelah publish")
                }
                Result.success(workDataOf(KEY_FILENAME to state.file.name))
            }.getOrElse {
                failure("Model tidak dipublikasikan ke SAF. Pilih folder SAF lalu coba lagi.")
            }
            is DownloadState.Paused -> {
                if (runAttemptCount < MAX_RETRIES) {
                    Result.retry()
                } else {
                    failure("Unduhan dihentikan setelah $MAX_RETRIES percobaan jaringan")
                }
            }
            is DownloadState.ChecksumError -> failure(state.message)
            is DownloadState.Failed -> failure(state.message)
            else -> failure("Unduhan model berhenti tanpa status selesai")
        }
    }

    private fun readManifestInput(): ModelInfo? {
        val model = ModelInfo(
            id = inputData.getString(KEY_ID).orEmpty(),
            name = inputData.getString(KEY_NAME).orEmpty(),
            filename = inputData.getString(KEY_FILENAME).orEmpty(),
            artifactUrl = inputData.getString(KEY_URL).orEmpty(),
            sourceUrl = inputData.getString(KEY_SOURCE_URL).orEmpty(),
            sha256 = inputData.getString(KEY_SHA256),
            sizeBytes = inputData.getLong(KEY_SIZE_BYTES, -1L).takeIf { it >= 0L },
            format = inputData.getString(KEY_FORMAT).orEmpty(),
            runtime = inputData.getString(KEY_RUNTIME).orEmpty(),
            role = inputData.getString(KEY_ROLE)?.let { role ->
                runCatching { ModelAssetRole.valueOf(role) }.getOrNull()
            } ?: ModelAssetRole.CHATBOT,
            embeddingDimension = inputData.getInt(KEY_EMBEDDING_DIMENSION, -1)
                .takeIf { it > 0 }
        )
        return model.takeIf {
            it.id.isNotBlank() &&
                it.name.isNotBlank() &&
                it.filename == File(it.filename).name &&
                it.isDownloadable
        }
    }

    private fun progressData(state: DownloadState.Transferring): Data = workDataOf(
        KEY_STAGE to "transferring",
        KEY_BYTES_DOWNLOADED to state.bytesDownloaded,
        KEY_TOTAL_BYTES to state.totalBytes,
        KEY_PROGRESS to state.progressPercentage,
        KEY_SPEED to state.speedBytesPerSec
    )

    private fun failure(message: String): Result = Result.failure(workDataOf(KEY_ERROR to message))

    private companion object {
        const val KEY_ID = "model_id"
        const val KEY_NAME = "model_name"
        const val KEY_FILENAME = "model_filename"
        const val KEY_URL = "model_url"
        const val KEY_SOURCE_URL = "model_source_url"
        const val KEY_SHA256 = "model_sha256"
        const val KEY_SIZE_BYTES = "model_size_bytes"
        const val KEY_FORMAT = "model_format"
        const val KEY_RUNTIME = "model_runtime"
        const val KEY_ROLE = "model_role"
        const val KEY_EMBEDDING_DIMENSION = "model_embedding_dimension"
        const val KEY_STAGE = "stage"
        const val KEY_REASON = "reason"
        const val KEY_BYTES_DOWNLOADED = "bytes_downloaded"
        const val KEY_TOTAL_BYTES = "total_bytes"
        const val KEY_PROGRESS = "progress"
        const val KEY_SPEED = "speed_bytes_per_second"
        const val KEY_ERROR = "error"
        const val MAX_RETRIES = 3
    }
}
