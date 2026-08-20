package com.quranplus.app.features.hadith.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.quranplus.app.core.database.QuranDatabase
import com.quranplus.app.core.network.DownloadState
import com.quranplus.app.core.network.ResumableDownloader
import com.quranplus.app.features.rag.data.SafAssetStore
import com.quranplus.app.features.settings.data.PreferencesManager
import kotlinx.coroutines.flow.collect
import java.io.File

/** Downloads Hadist only after an explicit user action and publishes it to SAF. */
class HadithBundleDownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val preferences = PreferencesManager(applicationContext)
        val assetStore = SafAssetStore(applicationContext, preferences)
        if (!assetStore.getStatus().isAccessible) {
            return failure("Pilih folder SAF sebelum mengunduh bundle Hadist")
        }

        val target = File(applicationContext.cacheDir, HadithBundleImporter.BUNDLE_FILENAME)
        var terminalState: DownloadState = DownloadState.Idle
        ResumableDownloader(applicationContext)
            .downloadFile(
                url = HadithBundleImporter.BUNDLE_URL,
                targetDestination = target
            )
            .collect { state ->
                terminalState = state
                when (state) {
                    is DownloadState.Transferring -> setProgress(progressData(state))
                    DownloadState.Verifying -> setProgress(
                        workDataOf(HadithBundleDownloadScheduler.KEY_STAGE to "verifying")
                    )
                    is DownloadState.Paused -> setProgress(
                        workDataOf(
                            HadithBundleDownloadScheduler.KEY_STAGE to "paused",
                            HadithBundleDownloadScheduler.KEY_ERROR to state.reason
                        )
                    )
                    else -> Unit
                }
            }

        return when (val state = terminalState) {
            is DownloadState.Completed -> runCatching {
                setProgress(workDataOf(HadithBundleDownloadScheduler.KEY_STAGE to HadithBundleDownloadScheduler.STAGE_IMPORTING))
                val database = QuranDatabase.getInstance(applicationContext)
                val importer = HadithBundleImporter(
                    assetStore = assetStore,
                    referenceImporter = HadithReferenceImporter(applicationContext, database)
                )
                val summary = importer.importArchive(state.file)
                state.file.delete()
                Result.success(
                    workDataOf(
                        KEY_RECORDS to summary.recordCount,
                        KEY_COLLECTIONS to summary.collectionCount
                    )
                )
            }.getOrElse {
                state.file.delete()
                failure(it.localizedMessage ?: "Bundle Hadist tidak dapat diimpor")
            }
            is DownloadState.Paused -> if (runAttemptCount < MAX_RETRIES) {
                Result.retry()
            } else {
                failure("Unduhan Hadist dihentikan setelah $MAX_RETRIES percobaan jaringan")
            }
            is DownloadState.ChecksumError -> failure(state.message)
            is DownloadState.Failed -> failure(state.message)
            else -> failure("Unduhan bundle Hadist berhenti tanpa status selesai")
        }
    }

    private fun progressData(state: DownloadState.Transferring): Data = workDataOf(
        HadithBundleDownloadScheduler.KEY_STAGE to "downloading",
        HadithBundleDownloadScheduler.KEY_BYTES to state.bytesDownloaded,
        HadithBundleDownloadScheduler.KEY_TOTAL_BYTES to state.totalBytes,
        HadithBundleDownloadScheduler.KEY_PROGRESS to state.progressPercentage
    )

    private fun failure(message: String): Result = Result.failure(
        workDataOf(HadithBundleDownloadScheduler.KEY_ERROR to message)
    )

    private companion object {
        const val KEY_RECORDS = "records"
        const val KEY_COLLECTIONS = "collections"
        const val MAX_RETRIES = 3
    }
}
