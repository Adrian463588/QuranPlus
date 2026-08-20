package com.quranplus.app.features.hadith.data

import com.quranplus.app.features.rag.data.SafAssetStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipFile

data class HadithBundleImportSummary(
    val collectionCount: Int,
    val recordCount: Int
)

/** Stores and imports the real Indonesian book files from the downloaded ZIP. */
class HadithBundleImporter(
    private val assetStore: SafAssetStore,
    private val referenceImporter: HadithReferenceImporter
) {
    suspend fun importArchive(archiveFile: File): HadithBundleImportSummary = withContext(Dispatchers.IO) {
        require(archiveFile.isFile) { "Bundle Hadist tidak ditemukan" }
        assetStore.publishFile(
            source = archiveFile,
            relativeDirectory = "rag/source",
            filename = BUNDLE_FILENAME,
            mimeType = "application/zip"
        )

        val imported = mutableListOf<HadithImportSummary>()
        ZipFile(archiveFile).use { archive ->
            val entries = archive.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val filename = entry.name.substringAfterLast('/')
                if (entry.isDirectory || filename !in HadithReferenceImporter.BUNDLE_BOOK_NAMES) continue

                val temporary = File.createTempFile("hadith-book-", ".json", archiveFile.parentFile)
                try {
                    archive.getInputStream(entry).use { input ->
                        temporary.outputStream().use { output -> input.copyTo(output) }
                    }
                    val storedUri = assetStore.publishFile(
                        source = temporary,
                        relativeDirectory = "rag/source/hadith",
                        filename = filename,
                        mimeType = "application/json"
                    )
                    referenceImporter.import(storedUri)?.let(imported::add)
                } finally {
                    temporary.delete()
                }
            }
        }

        if (imported.isEmpty()) throw IllegalStateException("Bundle tidak berisi buku Hadist yang dikenali")
        val summary = HadithBundleImportSummary(
            collectionCount = imported.size,
            recordCount = imported.sumOf(HadithImportSummary::recordCount)
        )
        val collectionIds = JSONArray().apply {
            imported.forEach { put(it.collectionId) }
        }
        assetStore.publishText(
            text = JSONObject()
                .put("bundle_id", BUNDLE_ID)
                .put("source_url", BUNDLE_URL)
                .put("collections", collectionIds)
                .put("record_count", summary.recordCount)
                .toString(),
            relativeDirectory = "manifests",
            filename = BUNDLE_MANIFEST_FILENAME
        )
        summary
    }

    suspend fun restoreFromSaf(): HadithBundleImportSummary? = withContext(Dispatchers.IO) {
        val imported = mutableListOf<HadithImportSummary>()
        assetStore.listFiles("rag/source/hadith").forEach { uri ->
            referenceImporter.import(uri)?.let(imported::add)
        }
        if (imported.isEmpty()) null else HadithBundleImportSummary(
            collectionCount = imported.size,
            recordCount = imported.sumOf(HadithImportSummary::recordCount)
        )
    }

    companion object {
        const val BUNDLE_ID = "gadingnst-hadith-api"
        const val BUNDLE_FILENAME = "hadith-indonesia-bundle.zip"
        const val BUNDLE_MANIFEST_FILENAME = "hadith-bundle.json"
        const val BUNDLE_URL =
            "https://codeload.github.com/gadingnst/hadith-api/zip/refs/heads/master"
        const val BUNDLE_SOURCE_URL = "https://github.com/gadingnst/hadith-api"
    }
}
