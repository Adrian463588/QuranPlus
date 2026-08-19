package com.quranplus.app.features.audio.domain

data class AudioDownloadKey(
    val qariId: String,
    val surahNumber: Int
)

sealed interface AudioDownloadState {
    data object Idle : AudioDownloadState
    data object Queued : AudioDownloadState
    data class Downloading(
        val currentAyah: Int,
        val totalAyahs: Int,
        val progressPercentage: Int,
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : AudioDownloadState
    data class Verifying(val currentAyah: Int, val totalAyahs: Int) : AudioDownloadState
    data class Paused(val reason: String) : AudioDownloadState
    data object Completed : AudioDownloadState
    data class Failed(val message: String) : AudioDownloadState
}
