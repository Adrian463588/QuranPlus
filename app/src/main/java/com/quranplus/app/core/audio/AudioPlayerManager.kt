package com.quranplus.app.core.audio

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.quranplus.app.core.utils.SurahMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class Qari(
    val id: String,
    val displayName: String,
    val style: String,
    val everyAyahFolder: String
) {
    MISHARY_ALAFASY(
        "alafasy",
        "Mishary Rashid Alafasy",
        "Murattal Hafs - Suara Merdu & Jelas",
        "Alafasy_128kbps"
    ),
    HUSARY(
        "husary",
        "Mahmud Khalil Al-Husary",
        "Mu'allim - Tartil Standar Tajwid",
        "Husary_128kbps"
    ),
    SUDAIS(
        "sudais",
        "Abdurrahman As-Sudais",
        "Imam Masjidil Haram",
        "Abdurrahmaan_As-Sudais_192kbps"
    );

    companion object {
        fun fromId(id: String): Qari? = entries.firstOrNull { it.id == id }
    }
}

enum class AudioRepeatMode(val count: Int, val label: String) {
    OFF(1, "1x"),
    TWO_TIMES(2, "2x"),
    THREE_TIMES(3, "3x"),
    FIVE_TIMES(5, "5x"),
    INFINITE(-1, "Loop ∞")
}

sealed interface PlaybackState {
    data object Idle : PlaybackState
    data object Buffering : PlaybackState
    data class Playing(val surahNumber: Int, val ayahNumber: Int) : PlaybackState
    data class Paused(val surahNumber: Int, val ayahNumber: Int) : PlaybackState
    data class Error(val message: String) : PlaybackState
}

data class CurrentAudioTrack(
    val surahNumber: Int,
    val surahName: String,
    val ayahNumber: Int,
    val totalAyahsInSurah: Int,
    val qari: Qari = Qari.MISHARY_ALAFASY
)

/** Media3 player for verified, app-private audio assets only. */
class AudioPlayerManager(
    private val context: Context,
    private val audioAssetStore: AudioAssetStore = AudioAssetStore(context)
) {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val notificationManager = AudioNotificationManager(context)
    private var player: ExoPlayer? = null
    private var progressJob: Job? = null

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentTrack = MutableStateFlow<CurrentAudioTrack?>(null)
    val currentTrack: StateFlow<CurrentAudioTrack?> = _currentTrack.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _repeatMode = MutableStateFlow(AudioRepeatMode.OFF)
    val repeatMode: StateFlow<AudioRepeatMode> = _repeatMode.asStateFlow()

    private val _autoContinueSurah = MutableStateFlow(false)
    val autoContinueSurah: StateFlow<Boolean> = _autoContinueSurah.asStateFlow()

    private val _selectedQari = MutableStateFlow(Qari.MISHARY_ALAFASY)
    val selectedQari: StateFlow<Qari> = _selectedQari.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    private var currentRepeatCounter = 0

    fun getAyahAudioUrl(qari: Qari, surahNumber: Int, ayahNumber: Int): String {
        return audioAssetStore.findVerifiedFile(qari, surahNumber, ayahNumber)?.absolutePath
            ?: EveryAyahAudioSource.descriptor(qari).audioUrl(surahNumber, ayahNumber)
    }

    fun playAyah(
        surahNumber: Int,
        surahName: String,
        ayahNumber: Int,
        totalAyahsInSurah: Int,
        qari: Qari = _selectedQari.value,
        autoContinue: Boolean = _autoContinueSurah.value
    ) {
        stop()
        _autoContinueSurah.value = autoContinue
        val localAudioFile = audioAssetStore.findVerifiedFile(qari, surahNumber, ayahNumber)
        val audioUri = if (localAudioFile != null) {
            Uri.fromFile(localAudioFile)
        } else {
            val descriptor = EveryAyahAudioSource.descriptor(qari)
            Uri.parse(descriptor.audioUrl(surahNumber, ayahNumber))
        }
        val resolvedTotalAyahs = if (totalAyahsInSurah > 0) totalAyahsInSurah
            else (SurahMapper.getSurah(surahNumber)?.ayahCount ?: 1)
        val track = CurrentAudioTrack(surahNumber, surahName, ayahNumber, resolvedTotalAyahs, qari)
        _currentTrack.value = track

        currentRepeatCounter = 0
        _playbackState.value = PlaybackState.Buffering
        notificationManager.updateNotification(track, _playbackState.value)

        player = ExoPlayer.Builder(context).build().also { exoPlayer ->
            exoPlayer.setMediaItem(MediaItem.fromUri(audioUri))
            exoPlayer.playbackParameters = PlaybackParameters(_playbackSpeed.value)
            exoPlayer.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    when (state) {
                        Player.STATE_BUFFERING -> {
                            _playbackState.value = PlaybackState.Buffering
                            notificationManager.updateNotification(track, _playbackState.value)
                        }
                        Player.STATE_READY -> {
                            _playbackState.value = PlaybackState.Playing(surahNumber, ayahNumber)
                            startProgressTracker()
                            notificationManager.updateNotification(track, _playbackState.value)
                        }
                        Player.STATE_ENDED -> handleTrackCompletion()
                        Player.STATE_IDLE -> Unit
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    _playbackState.value = PlaybackState.Error("Gagal memutar audio: ${error.errorCodeName}")
                    notificationManager.dismissNotification()
                }
            })
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }
    }

    fun togglePlayPause() {
        val track = _currentTrack.value ?: return
        val currentPlayer = player ?: return
        if (currentPlayer.isPlaying) {
            pause()
        } else {
            resume()
        }
    }

    fun pause() {
        val track = _currentTrack.value ?: return
        player?.pause()
        _playbackState.value = PlaybackState.Paused(track.surahNumber, track.ayahNumber)
        notificationManager.updateNotification(track, _playbackState.value)
    }

    fun resume() {
        val track = _currentTrack.value ?: return
        val currentPlayer = player ?: return
        if (!currentPlayer.isPlaying) {
            currentPlayer.play()
            _playbackState.value = PlaybackState.Playing(track.surahNumber, track.ayahNumber)
            startProgressTracker()
            notificationManager.updateNotification(track, _playbackState.value)
        }
    }

    fun stop() {
        progressJob?.cancel()
        player?.release()
        player = null
        _playbackState.value = PlaybackState.Idle
        _playbackProgress.value = 0f
        _currentTrack.value = null
        _autoContinueSurah.value = false
        currentRepeatCounter = 0
        notificationManager.dismissNotification()
    }

    fun setPlaybackSpeed(speed: Float) {
        val bounded = speed.coerceIn(0.5f, 1.25f)
        _playbackSpeed.value = bounded
        player?.playbackParameters = PlaybackParameters(bounded)
    }

    fun setRepeatMode(mode: AudioRepeatMode) {
        _repeatMode.value = mode
        currentRepeatCounter = 0
    }

    fun setAutoContinueSurah(enabled: Boolean) {
        _autoContinueSurah.value = enabled
    }

    fun setSelectedQari(qari: Qari) {
        _selectedQari.value = qari
    }

    fun nextAyah() {
        val track = _currentTrack.value ?: return
        if (track.ayahNumber < track.totalAyahsInSurah) {
            playAyah(
                surahNumber = track.surahNumber,
                surahName = track.surahName,
                ayahNumber = track.ayahNumber + 1,
                totalAyahsInSurah = track.totalAyahsInSurah,
                qari = track.qari,
                autoContinue = _autoContinueSurah.value
            )
        } else {
            stop()
        }
    }

    fun previousAyah() {
        val track = _currentTrack.value ?: return
        if (track.ayahNumber > 1) {
            playAyah(
                surahNumber = track.surahNumber,
                surahName = track.surahName,
                ayahNumber = track.ayahNumber - 1,
                totalAyahsInSurah = track.totalAyahsInSurah,
                qari = track.qari,
                autoContinue = _autoContinueSurah.value
            )
        }
    }

    fun close() {
        stop()
        scope.coroutineContext[Job]?.cancel()
    }

    fun getAudioStorageBytes(): Long {
        return audioAssetStore.getAudioStorageBytes()
    }

    fun clearDownloadedAudio(): Long {
        val bytes = getAudioStorageBytes()
        audioAssetStore.clear()
        return bytes
    }

    fun getSurahAudioBytes(qari: Qari, surahNumber: Int): Long {
        return audioAssetStore.getSurahAudioBytes(qari, surahNumber)
    }

    fun isSurahFullyDownloaded(qari: Qari, surahNumber: Int, totalAyahs: Int): Boolean {
        return audioAssetStore.isSurahFullyDownloaded(qari, surahNumber, totalAyahs)
    }

    fun getSurahDownloadedAyahCount(qari: Qari, surahNumber: Int, totalAyahs: Int): Int {
        return audioAssetStore.getSurahDownloadedAyahCount(qari, surahNumber, totalAyahs)
    }

    suspend fun restoreFromSaf(): Int {
        return audioAssetStore.restoreFromSaf()
    }

    private fun handleTrackCompletion() {
        val track = _currentTrack.value ?: return
        currentRepeatCounter++
        val shouldRepeat = when (val mode = _repeatMode.value) {
            AudioRepeatMode.OFF -> false
            AudioRepeatMode.TWO_TIMES -> currentRepeatCounter < mode.count
            AudioRepeatMode.THREE_TIMES -> currentRepeatCounter < mode.count
            AudioRepeatMode.FIVE_TIMES -> currentRepeatCounter < mode.count
            AudioRepeatMode.INFINITE -> true
        }
        if (shouldRepeat) {
            player?.seekTo(0)
            player?.play()
            _playbackState.value = PlaybackState.Playing(track.surahNumber, track.ayahNumber)
            notificationManager.updateNotification(track, _playbackState.value)
        } else if (_autoContinueSurah.value && track.ayahNumber < track.totalAyahsInSurah) {
            nextAyah()
        } else {
            stop()
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (true) {
                val currentPlayer = player
                val duration = currentPlayer?.duration ?: 0L
                if (currentPlayer != null && duration > 0L) {
                    _playbackProgress.value = (currentPlayer.currentPosition.toFloat() / duration).coerceIn(0f, 1f)
                }
                delay(300)
            }
        }
    }
}
