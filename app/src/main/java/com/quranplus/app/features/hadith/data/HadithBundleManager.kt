package com.quranplus.app.features.hadith.data

import android.net.Uri
import com.quranplus.app.core.database.dao.HadithDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

data class HadithBundleStatus(
    val storageLinked: Boolean,
    val localRecordCount: Int,
    val localCollectionCount: Int,
    val canonicalBundleReady: Boolean = false,
    val archiveBundleReady: Boolean = false
)

/** Coordinates canonical bundled corpus sync, user-triggered archive download, and SAF restoration. */
class HadithBundleManager(
    private val hadithDao: HadithDao,
    private val assetStore: com.quranplus.app.features.rag.data.SafAssetStore,
    private val bundleImporter: HadithBundleImporter,
    private val scheduler: HadithBundleDownloadScheduler,
    private val canonicalSynchronizer: CanonicalHadithSynchronizer? = null
) {
    private val restoreMutex = Mutex()

    fun observeStorageRoot(): Flow<Uri?> = assetStore.rootUri

    fun observeDownload(): Flow<HadithBundleWorkState> = scheduler.observe()

    fun enqueueDownload(): UUID = scheduler.enqueue()

    suspend fun syncCanonicalIfNeeded(): CanonicalSyncResult? =
        canonicalSynchronizer?.ensureSynchronized()

    suspend fun status(): HadithBundleStatus {
        val storageStatus = assetStore.getStatus()
        val canonicalManifest = hadithDao.getSourceManifest(CanonicalHadithSynchronizer.CANONICAL_SOURCE_ID)
        val canonicalReady = canonicalManifest?.status == CanonicalHadithSynchronizer.STATUS_VERIFIED &&
            canonicalManifest.activeRecordCount > 0
        val archiveCounts = verifiedCollectionCounts()
        val legacyReady = HadithBundleManifest.VERIFIED.isVerifiedCorpus(archiveCounts)
        val totalRecords = if (canonicalReady || legacyReady) {
            hadithDao.countActiveDistinctHadiths()
        } else {
            archiveCounts.values.sum()
        }
        val collectionCount = if (canonicalReady) {
            CanonicalHadithSynchronizer.CANONICAL_COLLECTION_IDS.size
        } else if (legacyReady) {
            archiveCounts.size
        } else {
            0
        }
        return HadithBundleStatus(
            storageLinked = storageStatus.isAccessible,
            localRecordCount = totalRecords,
            localCollectionCount = collectionCount,
            canonicalBundleReady = canonicalReady,
            archiveBundleReady = canonicalReady || legacyReady
        )
    }

    suspend fun restoreFromSaf(): HadithBundleImportSummary? = restoreMutex.withLock {
        val syncResult = canonicalSynchronizer?.ensureSynchronized()
        val canonicalManifest = hadithDao.getSourceManifest(CanonicalHadithSynchronizer.CANONICAL_SOURCE_ID)
        val canonicalReady = syncResult is CanonicalSyncResult.AlreadyCurrent ||
            syncResult is CanonicalSyncResult.Synchronized ||
            (canonicalManifest?.status == CanonicalHadithSynchronizer.STATUS_VERIFIED &&
                canonicalManifest.activeRecordCount > 0)
        if (canonicalReady) {
            return@withLock null
        }
        val verifiedCounts = verifiedCollectionCounts()
        if (HadithBundleManifest.VERIFIED.isVerifiedCorpus(verifiedCounts)) {
            return@withLock null
        }
        runCatching { bundleImporter.restoreFromSaf() }.getOrNull()
    }

    private suspend fun verifiedCollectionCounts(): Map<String, Int> =
        hadithDao.getVerifiedBundleCollectionCounts(
            sourceRevision = HadithBundleManifest.VERIFIED.revision,
            sourceSha256 = HadithBundleManifest.VERIFIED.archiveSha256,
            licenseStatus = "licensed"
        ).associate { it.collectionId to it.recordCount }

    companion object {
        private const val ARCHIVE_AHMAD_COUNT = 4_305
        private const val ARCHIVE_DARIMI_COUNT = 2_949
    }
}
