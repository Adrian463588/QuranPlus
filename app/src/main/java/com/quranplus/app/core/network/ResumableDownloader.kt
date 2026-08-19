package com.quranplus.app.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

sealed interface DownloadState {
    data object Idle : DownloadState
    data class Queued(val file: File) : DownloadState
    data class Downloading(
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val progressPercentage: Int,
        val speedBytesPerSec: Long
    ) : DownloadState
    data class Paused(val reason: String) : DownloadState
    data object Verifying : DownloadState
    data class Completed(val file: File) : DownloadState
    data class ChecksumError(val message: String) : DownloadState
    data class Failed(val message: String, val throwable: Throwable? = null) : DownloadState
}

class ResumableDownloader(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
) {

    fun isOnline(): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Downloads into a .tmp candidate, resumes only after a valid 206 response,
     * verifies the required digest, then atomically publishes the candidate.
     */
    fun downloadFile(
        url: String,
        targetDestination: File,
        expectedSha256: String
    ): Flow<DownloadState> = flow {
        emit(DownloadState.Idle)
        if (!expectedSha256.matches(SHA256_PATTERN)) {
            emit(DownloadState.Failed("Manifest SHA-256 model tidak valid atau belum tersedia"))
            return@flow
        }
        emit(DownloadState.Queued(targetDestination))

        if (!isOnline()) {
            emit(DownloadState.Paused("Tidak ada koneksi internet"))
            return@flow
        }

        val parent = targetDestination.parentFile ?: context.filesDir
        if (!parent.exists() && !parent.mkdirs()) {
            emit(DownloadState.Failed("Folder tujuan model tidak dapat dibuat"))
            return@flow
        }

        val tempFile = File(parent, "${targetDestination.name}.tmp")
        val existingBytes = tempFile.length()

        try {
            val requestBuilder = Request.Builder().url(url)
            if (existingBytes > 0L) {
                requestBuilder.header("Range", "bytes=$existingBytes-")
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                val append = existingBytes > 0L && response.code == 206
                if (!response.isSuccessful && response.code != 206) {
                    emit(DownloadState.Failed("Gagal mengunduh: HTTP ${response.code}"))
                    return@flow
                }
                if (existingBytes > 0L && response.code == 200) {
                    // The server ignored Range. Restart the candidate from byte zero.
                    tempFile.delete()
                }
                if (append) {
                    val contentRange = response.header("Content-Range")
                    if (contentRange?.startsWith("bytes $existingBytes-") != true) {
                        emit(DownloadState.Failed("Server mengirim Content-Range yang tidak sesuai"))
                        return@flow
                    }
                }

                val body = response.body ?: run {
                    emit(DownloadState.Failed("Response body kosong"))
                    return@flow
                }
                val totalBytes = if (append) existingBytes + body.contentLength() else body.contentLength()
                var downloaded = if (append) existingBytes else 0L
                var lastReport = System.currentTimeMillis()
                var bytesSinceReport = 0L
                val buffer = ByteArray(8192)

                FileOutputStream(tempFile, append).use { output ->
                    body.byteStream().use { input ->
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            bytesSinceReport += read

                            val now = System.currentTimeMillis()
                            val elapsed = now - lastReport
                            if (elapsed >= REPORT_INTERVAL_MS) {
                                val speed = (bytesSinceReport * 1000L) / elapsed.coerceAtLeast(1L)
                                val progress = if (totalBytes > 0L) {
                                    ((downloaded * 100L) / totalBytes).toInt().coerceIn(0, 100)
                                } else {
                                    0
                                }
                                emit(DownloadState.Downloading(downloaded, totalBytes, progress, speed))
                                lastReport = now
                                bytesSinceReport = 0L
                            }
                        }
                    }
                }
            }

            emit(DownloadState.Verifying)
            val actualSha256 = calculateSha256(tempFile)
            if (!actualSha256.equals(expectedSha256, ignoreCase = true)) {
                tempFile.delete()
                emit(DownloadState.ChecksumError("Verifikasi SHA-256 gagal: integritas file rusak"))
                return@flow
            }

            try {
                java.nio.file.Files.move(
                    tempFile.toPath(),
                    targetDestination.toPath(),
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
                )
            } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
                java.nio.file.Files.move(
                    tempFile.toPath(),
                    targetDestination.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
                )
            }

            if (targetDestination.isFile) {
                emit(DownloadState.Completed(targetDestination))
            } else {
                emit(DownloadState.Failed("File terverifikasi tidak dapat dipublikasikan"))
            }
        } catch (error: Exception) {
            // Keep the .tmp candidate for a later resume; it is never treated as a model.
            emit(DownloadState.Failed("Terjadi kesalahan saat mengunduh: ${error.localizedMessage}", error))
        }
    }.flowOn(Dispatchers.IO)

    private fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val REPORT_INTERVAL_MS = 300L
        val SHA256_PATTERN = Regex("[0-9a-fA-F]{64}")
    }
}
