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

class AudioDownloadViewModel(
    private val scheduler: AudioDownloadScheduler
) : ViewModel() {
    private val _states = MutableStateFlow<Map<AudioDownloadKey, AudioDownloadState>>(emptyMap())
    val states: StateFlow<Map<AudioDownloadKey, AudioDownloadState>> = _states.asStateFlow()
    private val observationJobs = mutableMapOf<AudioDownloadKey, Job>()

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

    override fun onCleared() {
        observationJobs.values.forEach(Job::cancel)
        observationJobs.clear()
        super.onCleared()
    }

    private fun AudioDownloadState?.isBusy(): Boolean = this is AudioDownloadState.Queued ||
        this is AudioDownloadState.Downloading ||
        this is AudioDownloadState.Verifying
}
