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
        require(HadithBundleManifest.VERIFIED.verifyArchive(archiveFile)) {
            "Checksum atau ukuran bundle Hadist tidak cocok dengan manifest"
        }
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
                    referenceImporter.import(
                        uri = storedUri,
                        source = VerifiedHadithSource(
                            revision = HadithBundleManifest.VERIFIED.revision,
                            licenseId = HadithBundleManifest.VERIFIED.licenseId,
                            licenseUrl = HadithBundleManifest.VERIFIED.licenseUrl,
                            // The trust boundary is the verified immutable ZIP,
                            // not an individual extracted book. Every row must
                            // carry the bundle digest so partial/foreign JSON
                            // cannot enter the verified RAG corpus.
                            sourceSha256 = HadithBundleManifest.VERIFIED.archiveSha256
                        )
                    )?.let(imported::add)
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
                .put("source_url", BUNDLE_SOURCE_URL)
                .put("revision", HadithBundleManifest.VERIFIED.revision)
                .put("archive_size_bytes", HadithBundleManifest.VERIFIED.archiveSizeBytes)
                .put("archive_sha256", HadithBundleManifest.VERIFIED.archiveSha256)
                .put("license_id", HadithBundleManifest.VERIFIED.licenseId)
                .put("license_url", HadithBundleManifest.VERIFIED.licenseUrl)
                .put("collections", collectionIds)
                .put("record_count", summary.recordCount)
                .toString(),
            relativeDirectory = "manifests",
            filename = BUNDLE_MANIFEST_FILENAME
        )
        summary
    }

    suspend fun restoreFromSaf(): HadithBundleImportSummary? = withContext(Dispatchers.IO) {
        val manifest = assetStore.readText("manifests", BUNDLE_MANIFEST_FILENAME)
            ?.let { runCatching { JSONObject(it) }.getOrNull() }
        if (manifest == null ||
            manifest.optString("bundle_id") != BUNDLE_ID ||
            manifest.optString("revision") != HadithBundleManifest.VERIFIED.revision ||
            manifest.optLong("archive_size_bytes", -1L) !=
            HadithBundleManifest.VERIFIED.archiveSizeBytes ||
            manifest.optString("archive_sha256")
                .equals(HadithBundleManifest.VERIFIED.archiveSha256, ignoreCase = true).not() ||
            manifest.optString("license_id") != HadithBundleManifest.VERIFIED.licenseId ||
            manifest.optString("source_url") != HadithBundleManifest.VERIFIED.sourceUrl ||
            manifest.optString("license_url") != HadithBundleManifest.VERIFIED.licenseUrl
        ) {
            return@withContext null
        }
        // Rebuild from the verified archive, not from loose SAF JSON files.
        // This prevents a modified per-book file from being promoted to a
        // trusted Hadist source merely because the manifest still exists.
        val temporary = File.createTempFile("quranplus-hadith-restore-", ".zip")
        try {
            val materialized = assetStore.materialize(
                relativePath = "rag/source/$BUNDLE_FILENAME",
                destination = temporary,
                expectedSha256 = HadithBundleManifest.VERIFIED.archiveSha256
            )
            if (!materialized || temporary.length() != HadithBundleManifest.VERIFIED.archiveSizeBytes) {
                return@withContext null
            }
            importArchive(temporary)
        } finally {
            temporary.delete()
        }
    }

    companion object {
        const val BUNDLE_ID = "gadingnst-hadith-api"
        const val BUNDLE_FILENAME = "hadith-indonesia-bundle.zip"
        const val BUNDLE_MANIFEST_FILENAME = "hadith-bundle.json"
        val BUNDLE_URL: String
            get() = HadithBundleManifest.VERIFIED.archiveUrl
        val BUNDLE_SOURCE_URL: String
            get() = HadithBundleManifest.VERIFIED.sourceUrl
    }

}
