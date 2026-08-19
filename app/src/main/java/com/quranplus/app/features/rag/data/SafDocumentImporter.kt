package com.quranplus.app.features.rag.data

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

enum class SafDocumentFormat { TXT, MARKDOWN, JSON, PDF }

data class SafChunk(
    val index: Int,
    val text: String,
    val lineStart: Int,
    val lineEnd: Int
)

data class SafDocumentMetadata(
    val displayName: String,
    val mimeType: String,
    val format: SafDocumentFormat,
    val sha256: String,
    val sourceType: String,
    val collection: String?,
    val identifier: String,
    val persistedUri: String,
    val storageUri: String,
    val chunks: List<SafChunk>
)

sealed interface SafImportResult {
    data class StoredAwaitingEmbedding(val metadata: SafDocumentMetadata) : SafImportResult
    data class Unsupported(val reason: String) : SafImportResult
    data class Error(val reason: String, val cause: Throwable? = null) : SafImportResult
}

/** SAF importer with strict UTF-8/schema checks and durable SAF storage. */
class SafDocumentImporter(
    private val context: Context,
    private val assetStore: SafAssetStore
) {

    private val resolver = context.contentResolver

    suspend fun import(uri: Uri): SafImportResult = withContext(Dispatchers.IO) {
        runCatching { importInternal(uri) }
            .getOrElse { SafImportResult.Error(it.localizedMessage ?: "Dokumen tidak dapat diimpor", it) }
    }

    fun persistPermission(uri: Uri, flags: Int) {
        val takeFlags = flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        if (takeFlags != 0) resolver.takePersistableUriPermission(uri, takeFlags)
    }

    private suspend fun importInternal(uri: Uri): SafImportResult {
        val displayName = queryDisplayName(uri) ?: uri.lastPathSegment ?: return SafImportResult.Error(
            "Nama dokumen dari provider SAF tidak tersedia"
        )
        val mimeType = resolver.getType(uri).orEmpty()
        val format = resolveFormat(displayName, mimeType)
            ?: return SafImportResult.Unsupported("Format tidak didukung. Gunakan UTF-8 TXT, Markdown, JSON, atau PDF text-based.")
        if (format == SafDocumentFormat.PDF) {
            return SafImportResult.Unsupported(
                "PDF ditolak sementara: extractor text-based terverifikasi belum tersedia. OCR tidak digunakan."
            )
        }

        val stagingDirectory = File(context.filesDir, "rag-documents")
        if (!stagingDirectory.exists() && !stagingDirectory.mkdirs()) {
            return SafImportResult.Error("Penyimpanan dokumen aplikasi tidak dapat dibuat")
        }
        val temporary = File(stagingDirectory, "import-${System.nanoTime()}.tmp")
        val digest = MessageDigest.getInstance("SHA-256")
        return try {
            resolver.openInputStream(uri)?.use { input ->
                temporary.outputStream().use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        if (total > MAX_DOCUMENT_BYTES) {
                            return SafImportResult.Unsupported("Dokumen melebihi batas 25 MiB")
                        }
                        digest.update(buffer, 0, read)
                        output.write(buffer, 0, read)
                    }
                }
            } ?: return SafImportResult.Error("Provider SAF tidak mengembalikan isi dokumen")

            val sha256 = digest.digest().toHex()
            val text = decodeUtf8(temporary.readBytes())
            val source = validateSource(format, text, sha256)
                ?: return SafImportResult.Error("Schema dokumen tidak valid atau isinya kosong")
            val chunks = chunkText(source.text)
            if (chunks.isEmpty()) return SafImportResult.Error("Dokumen tidak memiliki token teks")

            val storedUri = assetStore.publishFile(
                source = temporary,
                relativeDirectory = "rag/source",
                filename = "$sha256.source",
                mimeType = "text/plain"
            )
            val metadata = SafDocumentMetadata(
                displayName = displayName,
                mimeType = mimeType,
                format = format,
                sha256 = sha256,
                sourceType = "user_document",
                collection = source.collection,
                identifier = source.identifier,
                persistedUri = uri.toString(),
                storageUri = storedUri.toString(),
                chunks = chunks
            )
            assetStore.publishText(
                text = metadataJson(metadata),
                relativeDirectory = "manifests",
                filename = "$sha256.json"
            )
            SafImportResult.StoredAwaitingEmbedding(metadata)
        } finally {
            temporary.delete()
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
        val cursor: Cursor = resolver.query(uri, projection, null, null, null) ?: return null
        return cursor.use { if (it.moveToFirst()) it.getString(0) else null }
    }

    private fun resolveFormat(name: String, mimeType: String): SafDocumentFormat? {
        val lowerName = name.lowercase()
        return when {
            mimeType == "text/plain" || lowerName.endsWith(".txt") -> SafDocumentFormat.TXT
            mimeType == "text/markdown" || lowerName.endsWith(".md") || lowerName.endsWith(".markdown") -> SafDocumentFormat.MARKDOWN
            mimeType == "application/json" || lowerName.endsWith(".json") -> SafDocumentFormat.JSON
            mimeType == "application/pdf" || lowerName.endsWith(".pdf") -> SafDocumentFormat.PDF
            else -> null
        }
    }

    private fun decodeUtf8(bytes: ByteArray): String {
        val decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        return decoder.decode(java.nio.ByteBuffer.wrap(bytes)).toString()
    }

    private data class ValidatedSource(
        val identifier: String,
        val collection: String?,
        val text: String
    )

    private fun validateSource(format: SafDocumentFormat, text: String, sha256: String): ValidatedSource? {
        if (text.isBlank()) return null
        if (format != SafDocumentFormat.JSON) {
            return ValidatedSource(identifier = sha256, collection = null, text = text)
        }
        val json = runCatching { JSONObject(text) }.getOrNull() ?: return null
        val sourceType = json.optString("source_type")
        val identifier = json.optString("identifier")
        val content = json.optString("text")
        if (sourceType.isBlank() || identifier.isBlank() || content.isBlank()) return null
        return ValidatedSource(
            identifier = identifier,
            collection = json.optString("collection").takeIf(String::isNotBlank),
            text = content
        )
    }

    private fun chunkText(text: String): List<SafChunk> {
        val tokens = text.lines().flatMapIndexed { lineIndex, line ->
            line.trim().split(Regex("\\s+")).filter(String::isNotBlank).map { token -> token to lineIndex + 1 }
        }
        if (tokens.isEmpty()) return emptyList()
        val chunks = mutableListOf<SafChunk>()
        var start = 0
        var index = 0
        while (start < tokens.size) {
            val end = (start + CHUNK_TOKEN_COUNT).coerceAtMost(tokens.size)
            val window = tokens.subList(start, end)
            chunks += SafChunk(
                index = index++,
                text = window.joinToString(" ") { it.first },
                lineStart = window.first().second,
                lineEnd = window.last().second
            )
            if (end == tokens.size) break
            start = (end - CHUNK_OVERLAP).coerceAtLeast(start + 1)
        }
        return chunks
    }

    private fun metadataJson(metadata: SafDocumentMetadata): String {
        return JSONObject()
            .put("display_name", metadata.displayName)
            .put("mime_type", metadata.mimeType)
            .put("format", metadata.format.name)
            .put("sha256", metadata.sha256)
            .put("source_type", metadata.sourceType)
            .put("collection", metadata.collection)
            .put("identifier", metadata.identifier)
            .put("persisted_uri", metadata.persistedUri)
            .put("storage_uri", metadata.storageUri)
            .put("chunk_count", metadata.chunks.size)
            .toString(2)
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private companion object {
        const val BUFFER_SIZE = 8192
        const val MAX_DOCUMENT_BYTES = 25L * 1024L * 1024L
        const val CHUNK_TOKEN_COUNT = 512
        const val CHUNK_OVERLAP = 50
    }
}
