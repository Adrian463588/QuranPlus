package com.quranplus.app

import com.quranplus.app.features.chatbot.data.readBoundedBody
import okhttp3.MediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.BufferedSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InternetResponseBodyTest {
    @Test
    fun GIVEN_shortResponse_WHEN_readingBoundedBody_THEN_keepsItInsteadOfTreatingEofAsFailure() {
        val text = "قُلْ هُوَ اللَّهُ أَحَدٌ"
        assertEquals(text, readBoundedBody(response(text.toResponseBody()), 1024))
    }

    @Test
    fun GIVEN_unknownContentLength_WHEN_readingSmallBody_THEN_preservesCompleteText() {
        assertEquals("short response", readBoundedBody(response(unknownLengthBody("short response")), 32))
    }

    @Test
    fun GIVEN_knownOrUnknownOversizedBody_WHEN_reading_THEN_rejectsBeforeParsing() {
        val oversized = "x".repeat(33)
        listOf(oversized.toResponseBody(), unknownLengthBody(oversized)).forEach { body ->
            assertNull(readBoundedBody(response(body), 32))
        }
    }

    @Test
    fun GIVEN_multibyteTextAtByteLimit_WHEN_reading_THEN_countsUtf8Bytes() {
        assertEquals("ب", readBoundedBody(response("ب".toResponseBody()), 2))
        assertNull(readBoundedBody(response("بب".toResponseBody()), 3))
    }

    private fun unknownLengthBody(text: String) = object : ResponseBody() {
        private val buffer = Buffer().writeUtf8(text)
        override fun contentType(): MediaType? = null
        override fun contentLength(): Long = -1
        override fun source(): BufferedSource = buffer
    }

    private fun response(body: ResponseBody): Response = Response.Builder()
        .request(Request.Builder().url("https://api.alquran.cloud").build())
        .protocol(Protocol.HTTP_1_1).code(200).message("OK").body(body).build()
}
