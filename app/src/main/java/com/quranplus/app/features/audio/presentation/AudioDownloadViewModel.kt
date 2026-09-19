package com.quranplus.app.features.audio.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranplus.app.core.audio.Qari
import com.quranplus.app.features.audio.data.AudioDownloadScheduler
import com.quranplus.app.features.audio.domain.AudioDownloadKey
import com.quranplus.app.features.audio.domain.AudioDownloadState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.quranplus.app.core.audio.AudioPlayerManager
import com.quranplus.app.features.quran.domain.Surah
import com.quranplus.app.features.rag.data.SafAssetStore
import com.quranplus.app.features.rag.data.SafStorageStatus

class AudioDownloadViewModel(
    private val scheduler: AudioDownloadScheduler,
    private val safAssetStore: SafAssetStore,
    private val audioPlayerManager: AudioPlayerManager
) : ViewModel() {
    private val _states = MutableStateFlow<Map<AudioDownloadKey, AudioDownloadState>>(emptyMap())
    val states: StateFlow<Map<AudioDownloadKey, AudioDownloadState>> = _states.asStateFlow()
    private val observationJobs = mutableMapOf<AudioDownloadKey, Job>()

    private val _safStatus = MutableStateFlow<SafStorageStatus?>(null)
    val safStatus: StateFlow<SafStorageStatus?> = _safStatus.asStateFlow()

    private val _isBatchDownloading = MutableStateFlow(false)
    val isBatchDownloading: StateFlow<Boolean> = _isBatchDownloading.asStateFlow()

    init {
        refreshSafStatus()
    }

    fun refreshSafStatus() {
        viewModelScope.launch {
            _safStatus.value = runCatching { safAssetStore.getStatus() }.getOrNull()
        }
    }

    fun download(qari: Qari, surahNumber: Int, totalAyahs: Int) {
        val key = AudioDownloadKey(qari.id, surahNumber)
        if (_states.value[key].isBusy()) return
        scheduler.enqueue(qari, surahNumber, totalAyahs)
        observationJobs[key]?.cancel()
        observationJobs[key] = viewModelScope.launch {
            scheduler.observe(qari, surahNumber, totalAyahs).collect { state ->
                _states.update { current -> current + (key to state) }
            }
        }
    }

    fun cancel(qari: Qari, surahNumber: Int) {
        val key = AudioDownloadKey(qari.id, surahNumber)
        scheduler.cancel(qari, surahNumber)
        observationJobs[key]?.cancel()
        _states.update { current -> current + (key to AudioDownloadState.Idle) }
    }

    fun downloadAllSurahs(qari: Qari, surahs: List<Surah>) {
        viewModelScope.launch {
            _isBatchDownloading.value = true
            surahs.forEach { surah ->
                download(qari, surah.number, surah.ayahCount)
            }
            _isBatchDownloading.value = false
        }
    }

    fun restoreFromSaf(onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val count = audioPlayerManager.restoreFromSaf()
            refreshSafStatus()
            onComplete(count)
        }
    }

    override fun onCleared() {
        observationJobs.values.forEach(Job::cancel)
        observationJobs.clear()
        super.onCleared()
    }

    private fun AudioDownloadState?.isBusy(): Boolean = this is AudioDownloadState.Queued ||
        this is AudioDownloadState.Downloading ||
        this is AudioDownloadState.Verifying
}
