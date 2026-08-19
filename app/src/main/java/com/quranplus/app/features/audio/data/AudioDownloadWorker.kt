package com.quranplus.app.features.audio.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.quranplus.app.core.audio.AudioAssetStore
import com.quranplus.app.core.audio.AudioChecksumAlgorithm
import com.quranplus.app.core.audio.EveryAyahAudioSource
import com.quranplus.app.core.audio.Qari
import com.quranplus.app.core.audio.VerifiedAudioAsset
import com.quranplus.app.core.network.DownloadState
import com.quranplus.app.core.network.ResumableDownloader
import kotlinx.coroutines.flow.collect
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class AudioDownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun doWork(): Result {
        val qari = Qari.fromId(inputData.getString(AudioDownloadContract.KEY_QARI_ID).orEmpty())
            ?: return failure("Qari audio tidak dikenal")
        val surahNumber = inputData.getInt(AudioDownloadContract.KEY_SURAH_NUMBER, -1)
        val totalAyahs = inputData.getInt(AudioDownloadContract.KEY_TOTAL_AYAHS, -1)
        if (surahNumber !in 1..114 || totalAyahs <= 0) {
            return failure("Surah audio tidak valid")
        }

        val descriptor = EveryAyahAudioSource.descriptor(qari)
        val checksums = try {
            fetchChecksums(descriptor.checksumUrl)
        } catch (error: IOException) {
            return if (runAttemptCount < MAX_RETRIES) {
                Result.retry()
            } else {
                failure("Checksum audio tidak dapat diambil: ${error.localizedMessage}")
            }
        }
        val assetStore = AudioAssetStore(applicationContext)
        val downloader = ResumableDownloader(applicationContext)

        for (ayahNumber in 1..totalAyahs) {
            val fileName = descriptor.fileName(surahNumber, ayahNumber)
            val checksum = checksums[fileName]
                ?: return failure("Checksum tidak tersedia untuk $fileName")
            if (assetStore.hasVerifiedFile(qari, surahNumber, ayahNumber)) {
                reportProgress(ayahNumber, totalAyahs, 100)
                continue
            }

            var terminalState: DownloadState = DownloadState.Idle
            downloader.downloadFile(
                url = descriptor.audioUrl(surahNumber, ayahNumber),
                targetDestination = assetStore.fileFor(qari, surahNumber, ayahNumber),
                expectedSha256 = "",
                expectedMd5 = checksum
            ).collect { state ->
                terminalState = state
                when (state) {
                    is DownloadState.Transferring -> {
                        val overallProgress = (((ayahNumber - 1) * 100) + state.progressPercentage)
                            .div(totalAyahs)
                        setProgress(
                            workDataOf(
                                AudioDownloadContract.KEY_STAGE to "transferring",
                                AudioDownloadContract.KEY_CURRENT_AYAH to ayahNumber,
                                AudioDownloadContract.KEY_TOTAL_AYAHS to totalAyahs,
                                AudioDownloadContract.KEY_PROGRESS to overallProgress,
                                AudioDownloadContract.KEY_BYTES_DOWNLOADED to state.bytesDownloaded,
                                AudioDownloadContract.KEY_TOTAL_BYTES to state.totalBytes
                            )
                        )
                    }
                    DownloadState.Verifying -> {
                        setProgress(
                            workDataOf(
                                AudioDownloadContract.KEY_STAGE to "verifying",
                                AudioDownloadContract.KEY_CURRENT_AYAH to ayahNumber,
                                AudioDownloadContract.KEY_TOTAL_AYAHS to totalAyahs
                            )
                        )
                    }
                    is DownloadState.Paused -> {
                        setProgress(
                            workDataOf(
                                AudioDownloadContract.KEY_STAGE to "paused",
                                AudioDownloadContract.KEY_REASON to state.reason
                            )
                        )
                    }
                    else -> Unit
                }
            }

            when (val completedState = terminalState) {
                is DownloadState.Completed -> {
                    try {
                        assetStore.publish(
                            VerifiedAudioAsset(
                                qariId = qari.id,
                                surahNumber = surahNumber,
                                ayahNumber = ayahNumber,
                                fileName = fileName,
                                checksum = checksum,
                                checksumAlgorithm = AudioChecksumAlgorithm.MD5,
                                sourceUrl = descriptor.sourceUrl
                            )
                        )
                    } catch (error: Exception) {
                        return failure("Audio terverifikasi gagal disimpan: ${error.localizedMessage}")
                    }
                }
                is DownloadState.Paused -> {
                    return if (runAttemptCount < MAX_RETRIES) Result.retry()
                    else failure("Unduhan dihentikan setelah $MAX_RETRIES percobaan jaringan")
                }
                is DownloadState.ChecksumError -> return failure(completedState.message)
                is DownloadState.Failed -> return failure(completedState.message)
                else -> return failure("Unduhan audio berhenti tanpa status selesai")
            }
            reportProgress(ayahNumber, totalAyahs, 100)
        }
        return Result.success()
    }

    private fun fetchChecksums(url: String): Map<String, String> {
        val request = Request.Builder().url(url).header("Accept", "text/plain").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val body = response.body?.string().orEmpty()
            val checksums = EveryAyahAudioSource.parseChecksums(body)
            if (checksums.isEmpty()) throw IOException("Manifest checksum kosong")
            return checksums
        }
    }

    private suspend fun reportProgress(currentAyah: Int, totalAyahs: Int, progress: Int) {
        setProgress(
            workDataOf(
                AudioDownloadContract.KEY_STAGE to "transferring",
                AudioDownloadContract.KEY_CURRENT_AYAH to currentAyah,
                AudioDownloadContract.KEY_TOTAL_AYAHS to totalAyahs,
                AudioDownloadContract.KEY_PROGRESS to progress.coerceIn(0, 100)
            )
        )
    }

    private fun failure(message: String): Result = Result.failure(
        workDataOf(AudioDownloadContract.KEY_ERROR to message)
    )

    companion object {
        const val MAX_RETRIES = 3
    }
}

object AudioDownloadContract {
    const val KEY_QARI_ID = "qari_id"
    const val KEY_SURAH_NUMBER = "surah_number"
    const val KEY_TOTAL_AYAHS = "total_ayahs"
    const val KEY_STAGE = "stage"
    const val KEY_REASON = "reason"
    const val KEY_CURRENT_AYAH = "current_ayah"
    const val KEY_PROGRESS = "progress"
    const val KEY_BYTES_DOWNLOADED = "bytes_downloaded"
    const val KEY_TOTAL_BYTES = "total_bytes"
    const val KEY_ERROR = "error"
}
