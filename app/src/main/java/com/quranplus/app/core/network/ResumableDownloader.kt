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
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

sealed interface DownloadState {
    data object Idle : DownloadState
    data class Downloading(
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val progressPercentage: Int,
        val speedBytesPerSec: Long
    ) : DownloadState
    data class Completed(val file: File) : DownloadState
    data class Error(val message: String, val throwable: Throwable? = null) : DownloadState
}

class ResumableDownloader(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
) {

    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNet = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNet) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Resumable download supporting HTTP Range headers, writing to .tmp before atomic rename,
     * and optional SHA-256 verification.
     */
    fun downloadFile(
        url: String,
        targetDestination: File,
        expectedSha256: String? = null
    ): Flow<DownloadState> = flow {
        emit(DownloadState.Idle)

        if (!isOnline()) {
            emit(DownloadState.Error("Tidak ada koneksi internet"))
            return@flow
        }

        val parentDir = targetDestination.parentFile ?: context.filesDir
        if (!parentDir.exists()) parentDir.mkdirs()

        val tempFile = File(parentDir, "${targetDestination.name}.tmp")
        val existingBytes = if (tempFile.exists()) tempFile.length() else 0L

        try {
            val requestBuilder = Request.Builder().url(url)
            if (existingBytes > 0) {
                requestBuilder.header("Range", "bytes=$existingBytes-")
            }

            val response = client.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful && response.code != 206) {
                emit(DownloadState.Error("Gagal mengunduh: HTTP ${response.code}"))
                return@flow
            }

            val body = response.body ?: run {
                emit(DownloadState.Error("Response body kosong"))
                return@flow
            }

            val contentLength = body.contentLength()
            val totalBytes = if (response.code == 206) existingBytes + contentLength else contentLength

            val outputStream = if (existingBytes > 0 && response.code == 206) {
                FileOutputStream(tempFile, true)
            } else {
                FileOutputStream(tempFile, false)
            }

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalDownloaded = if (response.code == 206) existingBytes else 0L
            val inputStream = body.byteStream()

            var lastReportTime = System.currentTimeMillis()
            var bytesSinceLastReport = 0L

            outputStream.use { out ->
                inputStream.use { input ->
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        out.write(buffer, 0, bytesRead)
                        totalDownloaded += bytesRead
                        bytesSinceLastReport += bytesRead

                        val now = System.currentTimeMillis()
                        val diff = now - lastReportTime
                        if (diff >= 300) { // report progress every 300ms
                            val speed = if (diff > 0) (bytesSinceLastReport * 1000) / diff else 0L
                            val progress = if (totalBytes > 0) ((totalDownloaded * 100) / totalBytes).toInt() else 0
                            emit(
                                DownloadState.Downloading(
                                    bytesDownloaded = totalDownloaded,
                                    totalBytes = totalBytes,
                                    progressPercentage = progress.coerceIn(0, 100),
                                    speedBytesPerSec = speed
                                )
                            )
                            lastReportTime = now
                            bytesSinceLastReport = 0L
                        }
                    }
                }
            }

            // Verify SHA-256 if provided
            if (expectedSha256 != null) {
                val fileSha256 = calculateSha256(tempFile)
                if (!fileSha256.equals(expectedSha256, ignoreCase = true)) {
                    tempFile.delete()
                    emit(DownloadState.Error("Verifikasi SHA-256 gagal: integritas file rusak"))
                    return@flow
                }
            }

            // Atomic rename from .tmp to target destination
            if (targetDestination.exists()) targetDestination.delete()
            val renamed = tempFile.renameTo(targetDestination)
            if (renamed || targetDestination.exists()) {
                emit(DownloadState.Completed(targetDestination))
            } else {
                emit(DownloadState.Error("Gagal memindahkan file model setelah unduhan"))
            }

        } catch (e: Exception) {
            emit(DownloadState.Error("Terjadi kesalahan saat mengunduh: ${e.localizedMessage}", e))
        }
    }.flowOn(Dispatchers.IO)

    private fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        val fis = file.inputStream()
        var read: Int
        fis.use { input ->
            while (input.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        val bytes = digest.digest()
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
