package com.quranplus.app.features.rag.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.quranplus.app.features.settings.data.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

data class SafStorageStatus(
    val rootUri: Uri?,
    val isAccessible: Boolean,
    val manifestCount: Int = 0,
    val sourceCount: Int = 0,
    val indexCount: Int = 0
)

/** Durable user-owned storage for model, corpus, manifest, and index files. */
class SafAssetStore(
    private val context: Context,
    private val preferencesManager: PreferencesManager
) {
    private val resolver = context.contentResolver

    val rootUri: Flow<Uri?> = preferencesManager.safRootUri.map { value ->
        value?.let(Uri::parse)
    }

    suspend fun linkTree(uri: Uri, grantFlags: Int): SafStorageStatus = withContext(Dispatchers.IO) {
        val takeFlags = grantFlags and
            (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        if (takeFlags == 0) throw IllegalArgumentException("Folder SAF tidak memberikan izin baca/tulis")
        resolver.takePersistableUriPermission(uri, takeFlags)
        val root = requireAccessibleRoot(uri)
        createRequiredDirectories(root)
        preferencesManager.setSafRootUri(uri.toString())
        inspectRoot(uri, root)
    }

    suspend fun getStatus(): SafStorageStatus = withContext(Dispatchers.IO) {
        val uri = preferencesManager.safRootUri.first()?.let(Uri::parse) ?: return@withContext SafStorageStatus(null, false)
        val root = runCatching { requireAccessibleRoot(uri) }.getOrNull()
        if (root == null) SafStorageStatus(uri, false) else inspectRoot(uri, root)
    }

    suspend fun clearLink() {
        preferencesManager.clearSafRootUri()
    }

    suspend fun publishFile(
        source: File,
        relativeDirectory: String,
        filename: String,
        mimeType: String = "application/octet-stream"
    ): Uri = withContext(Dispatchers.IO) {
        require(filename == File(filename).name) { "Nama asset tidak boleh mengandung path" }
        val root = linkedRootOrThrow()
        val directory = createDirectoryPath(root, relativeDirectory)
        directory.findFile("$filename.part")?.delete()
        val temporary = directory.createFile(mimeType, "$filename.part")
            ?: throw IllegalStateException("Provider SAF tidak dapat membuat file sementara")
        try {
            resolver.openOutputStream(temporary.uri, "w")?.use { output ->
                source.inputStream().use { input -> input.copyTo(output) }
            } ?: error("Provider SAF tidak membuka output stream")
            // Keep the previously active asset until the new candidate is complete.
            directory.findFile(filename)?.delete()
            if (!temporary.renameTo(filename)) {
                throw IllegalStateException("Provider SAF tidak mendukung publish atomik")
            }
            directory.findFile(filename)?.uri
                ?: throw IllegalStateException("Asset tidak ditemukan setelah publish")
        } catch (error: Throwable) {
            temporary.delete()
            throw error
        }
    }

    suspend fun verifyFile(
        uri: Uri,
        expectedSizeBytes: Long,
        expectedSha256: String
    ): Boolean = withContext(Dispatchers.IO) {
        if (expectedSizeBytes <= 0L || !expectedSha256.matches(SHA256_PATTERN)) return@withContext false
        val digest = MessageDigest.getInstance("SHA-256")
        var totalBytes = 0L
        resolver.openInputStream(uri)?.use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                totalBytes += read
                digest.update(buffer, 0, read)
            }
        } ?: return@withContext false
        totalBytes == expectedSizeBytes &&
            digest.digest().joinToString("") { "%02x".format(it) }
                .equals(expectedSha256, ignoreCase = true)
    }

    suspend fun publishText(
        text: String,
        relativeDirectory: String,
        filename: String
    ): Uri = withContext(Dispatchers.IO) {
        val temporary = File.createTempFile("quranplus-saf-", ".tmp", context.cacheDir)
        try {
            temporary.writeText(text, Charsets.UTF_8)
            publishFile(temporary, relativeDirectory, filename, "application/json")
        } finally {
            temporary.delete()
        }
    }

    suspend fun materialize(
        relativePath: String,
        destination: File,
        expectedSha256: String
    ): Boolean = withContext(Dispatchers.IO) {
        val root = linkedRootOrThrow()
        val parts = relativePath.split('/').filter(String::isNotBlank)
        require(parts.isNotEmpty()) { "Path asset kosong" }
        val file = parts.dropLast(1).fold(root) { parent, segment ->
            parent.findFile(segment) ?: throw IllegalStateException("Folder SAF tidak ditemukan: $segment")
        }.findFile(parts.last()) ?: throw IllegalStateException("Asset SAF tidak ditemukan: $relativePath")
        val temporary = File(destination.parentFile ?: context.cacheDir, "${destination.name}.tmp")
        try {
            resolver.openInputStream(file.uri)?.use { input ->
                temporary.outputStream().use { output -> input.copyTo(output) }
            } ?: error("Provider SAF tidak membuka input stream")
            if (!sha256(temporary).equals(expectedSha256, ignoreCase = true)) return@withContext false
            if (destination.exists() && !destination.delete()) return@withContext false
            temporary.renameTo(destination)
        } finally {
            temporary.delete()
        }
    }

    private suspend fun linkedRootOrThrow(): DocumentFile {
        val uri = preferencesManager.safRootUri.first()?.let(Uri::parse)
            ?: throw IllegalStateException("Folder SAF belum dipilih")
        return requireAccessibleRoot(uri)
    }

    private fun requireAccessibleRoot(uri: Uri): DocumentFile {
        val root = DocumentFile.fromTreeUri(context, uri)
            ?: throw IllegalStateException("URI SAF bukan folder yang valid")
        if (!root.canRead() || !root.canWrite()) {
            throw IllegalStateException("Izin folder SAF tidak lagi tersedia; pilih folder ulang")
        }
        return root
    }

    private fun inspectRoot(uri: Uri, root: DocumentFile): SafStorageStatus = SafStorageStatus(
        rootUri = uri,
        isAccessible = true,
        manifestCount = countFiles(root, "manifests"),
        sourceCount = countFiles(root, "rag/source"),
        indexCount = countFiles(root, "rag/index")
    )

    private fun countFiles(root: DocumentFile, relativeDirectory: String): Int =
        createDirectoryPath(root, relativeDirectory).listFiles().count { it.isFile }

    private fun createRequiredDirectories(root: DocumentFile) {
        listOf("models", "rag", "rag/source", "rag/index", "manifests")
            .forEach { createDirectoryPath(root, it) }
    }

    private fun createDirectoryPath(root: DocumentFile, relativeDirectory: String): DocumentFile =
        relativeDirectory.split('/').filter(String::isNotBlank).fold(root) { parent, segment ->
            parent.findFile(segment)?.takeIf { it.isDirectory }
                ?: parent.createDirectory(segment)
                ?: throw IllegalStateException("Provider SAF tidak dapat membuat folder $segment")
        }

    private fun sha256(file: File): String {
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
        val SHA256_PATTERN = Regex("[0-9a-fA-F]{64}")
    }
}
