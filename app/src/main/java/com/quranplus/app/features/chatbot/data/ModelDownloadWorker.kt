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

/**
 * Background model transfer. It accepts only a complete, reviewed manifest;
 * an arbitrary URL can never become an active model through this worker.
 */
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
        var terminalState: DownloadState = DownloadState.Idle

        ResumableDownloader(applicationContext)
            .downloadFile(model.downloadUrl, target, model.sha256.orEmpty())
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
                failure("Model tidak dipublikasikan ke SAF: ${it.localizedMessage}")
            }
            is DownloadState.Paused -> Result.retry()
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
            sizeDescription = inputData.getString(KEY_SIZE_DESCRIPTION).orEmpty(),
            ramRequirement = inputData.getString(KEY_RAM_REQUIREMENT).orEmpty(),
            downloadUrl = inputData.getString(KEY_URL).orEmpty(),
            sha256 = inputData.getString(KEY_SHA256),
            version = inputData.getString(KEY_VERSION).orEmpty(),
            abi = inputData.getString(KEY_ABI).orEmpty(),
            licenseStatus = inputData.getString(KEY_LICENSE_STATUS).orEmpty(),
            sizeBytes = inputData.getLong(KEY_SIZE_BYTES, -1L).takeIf { it >= 0L }
        )
        return model.takeIf {
            it.id.isNotBlank() &&
                it.name.isNotBlank() &&
                it.filename == File(it.filename).name &&
                it.hasVerifiedManifest
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
        const val KEY_SIZE_DESCRIPTION = "model_size_description"
        const val KEY_RAM_REQUIREMENT = "model_ram_requirement"
        const val KEY_URL = "model_url"
        const val KEY_SHA256 = "model_sha256"
        const val KEY_VERSION = "model_version"
        const val KEY_ABI = "model_abi"
        const val KEY_LICENSE_STATUS = "model_license_status"
        const val KEY_SIZE_BYTES = "model_size_bytes"
        const val KEY_STAGE = "stage"
        const val KEY_REASON = "reason"
        const val KEY_BYTES_DOWNLOADED = "bytes_downloaded"
        const val KEY_TOTAL_BYTES = "total_bytes"
        const val KEY_PROGRESS = "progress"
        const val KEY_SPEED = "speed_bytes_per_second"
        const val KEY_ERROR = "error"
    }
}
