package com.quranplus.app.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.io.EOFException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed interface DownloadState {
    data object Idle : DownloadState
    data class Queued(val file: File) : DownloadState
    data class Transferring(
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
        .callTimeout(90, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()
) {

    private val redirectSafeClient = client.newBuilder()
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    fun isOnline(): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Downloads into a .tmp candidate, resumes only after a valid 206 response,
     * verifies the required digest, then atomically publishes the candidate.
     */
    fun downloadFile(
        url: String,
        targetDestination: File,
        expectedSha256: String? = null,
        expectedSizeBytes: Long? = null,
        expectedMd5: String? = null
    ): Flow<DownloadState> = flow {
        emit(DownloadState.Idle)
        val digestAlgorithm = when {
            expectedSha256?.matches(SHA256_PATTERN) == true -> "SHA-256"
            expectedMd5?.matches(MD5_PATTERN) == true -> "MD5"
            else -> null
        }
        val expectedDigest = expectedSha256?.takeIf { it.matches(SHA256_PATTERN) }
            ?: expectedMd5?.takeIf { it.matches(MD5_PATTERN) }
        if ((expectedSha256 != null || expectedMd5 != null) &&
            (digestAlgorithm == null || expectedDigest == null)
        ) {
            emit(DownloadState.Failed("Manifest checksum audio/model tidak valid atau belum tersedia"))
            return@flow
        }
        val parsedUrl = url.toHttpUrlOrNull()
        if (parsedUrl?.scheme != "https" || parsedUrl.host.isBlank()) {
            emit(DownloadState.Failed("URL unduhan harus HTTPS dan memiliki host yang valid"))
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
        var existingBytes = tempFile.length()
        if (expectedSizeBytes != null && existingBytes > expectedSizeBytes) {
            tempFile.delete()
            existingBytes = 0L
        }

        try {
            val requestBuilder = Request.Builder()
                .url(url)
                // Keep byte ranges aligned with the stored .tmp candidate.
                .header("Accept-Encoding", "identity")
            if (existingBytes > 0L) {
                requestBuilder.header("Range", "bytes=$existingBytes-")
            }

            executeCancellable(requestBuilder.build()).use { response ->
                val append = existingBytes > 0L && response.code == 206
                if (existingBytes > 0L && response.code == 416) {
                    tempFile.delete()
                    emit(DownloadState.Paused("Rentang resume tidak berlaku; file sementara diulang"))
                    return@flow
                }
                if (!response.isSuccessful && response.code != 206) {
                    if (response.code in 500..599) {
                        emit(DownloadState.Paused("Server belum siap (${response.code}); unduhan akan dicoba lagi"))
                    } else {
                        emit(DownloadState.Failed("Gagal mengunduh: HTTP ${response.code}"))
                    }
                    return@flow
                }
                if (existingBytes > 0L && response.code == 200) {
                    // The server ignored Range. Restart the candidate from byte zero.
                    tempFile.delete()
                }
                if (response.code == 206) {
                    val contentRange = response.header("Content-Range")
                    val range = contentRange
                        ?.let { CONTENT_RANGE_PATTERN.matchEntire(it.trim()) }
                    val rangeStart = range?.groupValues?.getOrNull(1)?.toLongOrNull()
                    val rangeTotal = range?.groupValues?.getOrNull(3)?.toLongOrNull()
                    val expectedRangeStart = if (existingBytes > 0L) existingBytes else 0L
                    if (rangeStart != expectedRangeStart ||
                        (expectedSizeBytes != null && rangeTotal != expectedSizeBytes)
                    ) {
                        emit(DownloadState.Failed("Server mengirim Content-Range yang tidak sesuai"))
                        return@flow
                    }
                }

                val body = response.body ?: run {
                    emit(DownloadState.Failed("Response body kosong"))
                    return@flow
                }
                val responseBytes = body.contentLength()
                val totalBytes = when {
                    expectedSizeBytes != null && expectedSizeBytes > 0L -> expectedSizeBytes
                    responseBytes >= 0L && append -> existingBytes + responseBytes
                    responseBytes >= 0L -> responseBytes
                    else -> 0L
                }
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
                                emit(DownloadState.Transferring(downloaded, totalBytes, progress, speed))
                                lastReport = now
                                bytesSinceReport = 0L
                            }
                        }
                    }
                }
            }

            emit(DownloadState.Verifying)
            if (expectedSizeBytes != null &&
                expectedSizeBytes > 0L &&
                tempFile.length() < expectedSizeBytes
            ) {
                emit(
                    DownloadState.Paused(
                        "Transfer belum lengkap (${tempFile.length()} dari $expectedSizeBytes byte); akan dilanjutkan"
                    )
                )
                return@flow
            }
            if (expectedSizeBytes != null &&
                expectedSizeBytes > 0L &&
                tempFile.length() > expectedSizeBytes
            ) {
                tempFile.delete()
                emit(DownloadState.ChecksumError("Verifikasi ukuran artifact gagal"))
                return@flow
            }
            if (digestAlgorithm != null && expectedDigest != null) {
                val actualDigest = calculateDigest(tempFile, digestAlgorithm)
                if (!actualDigest.equals(expectedDigest, ignoreCase = true)) {
                    tempFile.delete()
                    emit(DownloadState.ChecksumError("Verifikasi $digestAlgorithm gagal: integritas file rusak"))
                    return@flow
                }
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
        } catch (error: IOException) {
            // Keep the .tmp candidate for a later resume; it is never treated as a model.
            if (isRetryableNetworkError(error)) {
                emit(DownloadState.Paused("Koneksi terputus; unduhan akan dilanjutkan dari file sementara"))
            } else {
                emit(DownloadState.Failed("Penyimpanan atau konfigurasi unduhan bermasalah", error))
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            emit(DownloadState.Failed("Terjadi kesalahan saat mengunduh: ${error.localizedMessage}", error))
        }
    }.flowOn(Dispatchers.IO)

    private fun isRetryableNetworkError(error: IOException): Boolean {
        if (!isOnline()) return true
        val causes = generateSequence(error as Throwable?) { it.cause }.toList()
        if (causes.any {
                it is SocketException ||
                    it is SocketTimeoutException ||
                    it is ConnectException ||
                    it is UnknownHostException ||
                    it is InterruptedIOException ||
                    it is EOFException
            }
        ) {
            return true
        }
        val message = causes.joinToString(" ") { it.message.orEmpty() }.lowercase()
        return NETWORK_FAILURE_MARKERS.any(message::contains)
    }

    private suspend fun executeCancellable(request: Request): Response {
        var currentRequest = request
        repeat(MAX_REDIRECTS + 1) {
            val response = executeCancellableOnce(currentRequest)
            if (response.code !in REDIRECT_CODES) return response

            val location = response.header("Location")
            val nextUrl = location?.let { currentRequest.url.resolve(it) }
            response.close()
            if (nextUrl == null || nextUrl.scheme != "https" ||
                !isAllowedRedirectHost(currentRequest.url.host, nextUrl.host)
            ) {
                throw IOException("Redirect unduhan tidak diizinkan")
            }
            currentRequest = currentRequest.newBuilder().url(nextUrl).build()
        }
        throw IOException("Redirect unduhan melebihi batas")
    }

    private suspend fun executeCancellableOnce(request: Request): Response =
        suspendCancellableCoroutine { continuation ->
            val call = redirectSafeClient.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            try {
                val response = call.execute()
                if (continuation.isActive) {
                    continuation.resume(response)
                } else {
                    response.close()
                }
            } catch (error: Throwable) {
                if (continuation.isActive) continuation.resumeWithException(error)
            }
        }

    private fun isAllowedRedirectHost(fromHost: String, toHost: String): Boolean {
        if (fromHost.equals(toHost, ignoreCase = true)) return true
        val fromHuggingFace = fromHost == "huggingface.co" ||
            fromHost.endsWith(".huggingface.co", ignoreCase = true)
        val toHuggingFace = toHost == "huggingface.co" ||
            toHost.endsWith(".huggingface.co", ignoreCase = true) ||
            toHost.endsWith(".hf.co", ignoreCase = true)
        if (fromHuggingFace && toHuggingFace) return true

        val fromGitHub = fromHost == "github.com" ||
            fromHost == "codeload.github.com" ||
            fromHost.endsWith(".github.com", ignoreCase = true)
        val toGitHub = toHost == "github.com" ||
            toHost == "codeload.github.com" ||
            toHost.endsWith(".github.com", ignoreCase = true) ||
            toHost.endsWith(".githubusercontent.com", ignoreCase = true)
        return fromGitHub && toGitHub
    }

    private fun calculateDigest(file: File, algorithm: String): String {
        val digest = MessageDigest.getInstance(algorithm)
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
        const val MAX_REDIRECTS = 3
        val REDIRECT_CODES = setOf(300, 301, 302, 303, 307, 308)
        const val REPORT_INTERVAL_MS = 1_000L
        val NETWORK_FAILURE_MARKERS = listOf(
            "connection reset",
            "connection aborted",
            "connection closed",
            "broken pipe",
            "unexpected end of stream",
            "stream was reset",
            "network is unreachable",
            "connection refused",
            "timed out",
            "timeout"
        )
        val SHA256_PATTERN = Regex("[0-9a-fA-F]{64}")
        val MD5_PATTERN = Regex("[0-9a-fA-F]{32}")
        val CONTENT_RANGE_PATTERN = Regex("bytes (\\d+)-(\\d+)/(\\d+)")
    }
}
