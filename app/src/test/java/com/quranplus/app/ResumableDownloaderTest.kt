package com.quranplus.app

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.quranplus.app.core.network.DownloadState
import com.quranplus.app.core.network.ResumableDownloader
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.toList
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.Source
import okio.Timeout
import okio.buffer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.EOFException
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest

class ResumableDownloaderTest {

    @Test
    fun GIVEN_connectionDropsAfterPartialBody_WHEN_downloadIsRetried_THEN_rangeResumesAndPublishes() = runBlocking {
        val payload = "verified-model-payload".repeat(4).toByteArray()
        val tempDirectory = Files.createTempDirectory("quranplus-download-test").toFile()
        val target = File(tempDirectory, "model.bin")
        val requests = mutableListOf<Request>()
        val client = OkHttpClient.Builder()
            .addInterceptor(ResumeScenarioInterceptor(payload, requests))
            .build()
        val downloader = ResumableDownloader(
            context = onlineContext(),
            client = client
        )

        try {
            val firstAttempt = downloader.downloadFile(
                url = "https://example.test/model.bin",
                targetDestination = target,
                expectedSha256 = payload.sha256(),
                expectedSizeBytes = payload.size.toLong()
            ).toList()

            assertTrue(firstAttempt.last() is DownloadState.Paused)
            assertEquals(5L, File(tempDirectory, "model.bin.tmp").length())

            val secondAttempt = downloader.downloadFile(
                url = "https://example.test/model.bin",
                targetDestination = target,
                expectedSha256 = payload.sha256(),
                expectedSizeBytes = payload.size.toLong()
            ).toList()

            assertTrue(secondAttempt.last() is DownloadState.Completed)
            assertArrayEquals(payload, target.readBytes())
            assertEquals(null, requests[0].header("Range"))
            assertEquals("bytes=5-", requests[1].header("Range"))
        } finally {
            tempDirectory.deleteRecursively()
        }
    }

    @Test
    fun GIVEN_responseEndsBeforeManifestSize_WHEN_downloadRuns_THEN_candidateIsKeptForResume() = runBlocking {
        val payload = "verified-model-payload".repeat(4).toByteArray()
        val tempDirectory = Files.createTempDirectory("quranplus-short-download-test").toFile()
        val target = File(tempDirectory, "model.bin")
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(ByteArrayResponseBody(payload.copyOf(5)))
                    .build()
            }
            .build()
        val downloader = ResumableDownloader(
            context = onlineContext(),
            client = client
        )

        try {
            val states = downloader.downloadFile(
                url = "https://example.test/model.bin",
                targetDestination = target,
                expectedSha256 = payload.sha256(),
                expectedSizeBytes = payload.size.toLong()
            ).toList()

            assertTrue(states.last() is DownloadState.Paused)
            assertEquals(5L, File(tempDirectory, "model.bin.tmp").length())
            assertTrue(!target.exists())
        } finally {
            tempDirectory.deleteRecursively()
        }
    }

    private fun onlineContext(): Context {
        val context = mockk<Context>()
        val connectivityManager = mockk<ConnectivityManager>()
        val network = mockk<Network>()
        val capabilities = mockk<NetworkCapabilities>()
        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
        every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns true
        return context
    }

    private class ResumeScenarioInterceptor(
        private val payload: ByteArray,
        private val requests: MutableList<Request>
    ) : Interceptor {

        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            requests += request
            return if (requests.size == 1) {
                response(request, 200, FailingResponseBody(payload, failAfterBytes = 5))
            } else {
                assertEquals("bytes=5-", request.header("Range"))
                response(
                    request = request,
                    code = 206,
                    body = ByteArrayResponseBody(payload.copyOfRange(5, payload.size))
                ).newBuilder()
                    .header("Content-Range", "bytes 5-${payload.lastIndex}/${payload.size}")
                    .build()
            }
        }

        private fun response(request: Request, code: Int, body: ResponseBody): Response =
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("OK")
                .body(body)
                .build()
    }

    private class FailingResponseBody(
        private val bytes: ByteArray,
        private val failAfterBytes: Int
    ) : ResponseBody() {

        override fun contentType(): MediaType? = null

        override fun contentLength(): Long = bytes.size.toLong()

        override fun source(): BufferedSource = object : Source {
            private var offset = 0
            private var failureRaised = false

            override fun read(sink: Buffer, byteCount: Long): Long {
                if (offset >= failAfterBytes && !failureRaised) {
                    failureRaised = true
                    throw EOFException("synthetic connection drop")
                }
                if (offset >= bytes.size) return -1L
                val count = minOf(
                    byteCount,
                    (failAfterBytes - offset).toLong(),
                    (bytes.size - offset).toLong()
                ).toInt()
                sink.write(bytes, offset, count)
                offset += count
                return count.toLong()
            }

            override fun timeout(): Timeout = Timeout.NONE

            override fun close() = Unit
        }.buffer()
    }

    private class ByteArrayResponseBody(
        private val bytes: ByteArray
    ) : ResponseBody() {

        override fun contentType(): MediaType? = null

        override fun contentLength(): Long = bytes.size.toLong()

        override fun source(): BufferedSource = Buffer().write(bytes)
    }

    private fun ByteArray.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(this)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
