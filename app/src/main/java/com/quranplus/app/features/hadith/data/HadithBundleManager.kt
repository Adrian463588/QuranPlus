package com.quranplus.app.features.hadith.data

import android.net.Uri
import com.quranplus.app.core.database.dao.HadithDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

data class HadithBundleStatus(
    val storageLinked: Boolean,
    val localRecordCount: Int,
    val localCollectionCount: Int
)

/** Coordinates user-triggered bundle download and SAF restoration. */
class HadithBundleManager(
    private val hadithDao: HadithDao,
    private val assetStore: com.quranplus.app.features.rag.data.SafAssetStore,
    private val bundleImporter: HadithBundleImporter,
    private val scheduler: HadithBundleDownloadScheduler
) {
    private val restoreMutex = Mutex()

    fun observeStorageRoot(): Flow<Uri?> = assetStore.rootUri

    fun observeDownload(): Flow<HadithBundleWorkState> = scheduler.observe()

    fun enqueueDownload(): UUID = scheduler.enqueue()

    suspend fun status(): HadithBundleStatus {
        val storageStatus = assetStore.getStatus()
        return HadithBundleStatus(
            storageLinked = storageStatus.isAccessible,
            localRecordCount = hadithDao.countHadiths(),
            localCollectionCount = hadithDao.getCollectionIds().first().size
        )
    }

    suspend fun restoreFromSaf(): HadithBundleImportSummary? = restoreMutex.withLock {
        if (hadithDao.countHadiths() > 0) return@withLock null
        runCatching { bundleImporter.restoreFromSaf() }.getOrNull()
    }
}
