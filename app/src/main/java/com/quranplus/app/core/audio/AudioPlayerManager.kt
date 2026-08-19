package com.quranplus.app.core.audio

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

enum class Qari(
    val id: String,
    val displayName: String,
    val style: String
) {
    MISHARY_ALAFASY("alafasy", "Mishary Rashid Alafasy", "Murattal Hafs - Suara Merdu & Jelas"),
    HUSARY("husary", "Mahmud Khalil Al-Husary", "Mu'allim - Tartil Standar Tajwid"),
    SUDAIS("sudais", "Abdurrahman As-Sudais", "Imam Masjidil Haram");

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
class AudioPlayerManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
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

    private val _selectedQari = MutableStateFlow(Qari.MISHARY_ALAFASY)
    val selectedQari: StateFlow<Qari> = _selectedQari.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    private var currentRepeatCounter = 0

    fun getAyahAudioUrl(qari: Qari, surahNumber: Int, ayahNumber: Int): String? {
        return findLocalAudio(qari, surahNumber, ayahNumber)?.absolutePath
    }

    fun playAyah(
        surahNumber: Int,
        surahName: String,
        ayahNumber: Int,
        totalAyahsInSurah: Int,
        qari: Qari = _selectedQari.value
    ) {
        stop()
        val audioFile = findLocalAudio(qari, surahNumber, ayahNumber)
        _currentTrack.value = CurrentAudioTrack(surahNumber, surahName, ayahNumber, totalAyahsInSurah, qari)
        if (audioFile == null) {
            _playbackState.value = PlaybackState.Error(
                "Audio ayat belum tersedia sebagai asset terverifikasi untuk ${qari.displayName}."
            )
            return
        }

        currentRepeatCounter = 0
        _playbackState.value = PlaybackState.Buffering
        player = ExoPlayer.Builder(context).build().also { exoPlayer ->
            exoPlayer.setMediaItem(MediaItem.fromUri(Uri.fromFile(audioFile)))
            exoPlayer.playbackParameters = PlaybackParameters(_playbackSpeed.value)
            exoPlayer.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    when (state) {
                        Player.STATE_BUFFERING -> {
                            _playbackState.value = PlaybackState.Buffering
                        }
                        Player.STATE_READY -> {
                            _playbackState.value = PlaybackState.Playing(surahNumber, ayahNumber)
                            startProgressTracker()
                        }
                        Player.STATE_ENDED -> handleTrackCompletion()
                        Player.STATE_IDLE -> Unit
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    _playbackState.value = PlaybackState.Error("Gagal memutar asset audio: ${error.errorCodeName}")
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
            currentPlayer.pause()
            _playbackState.value = PlaybackState.Paused(track.surahNumber, track.ayahNumber)
        } else {
            currentPlayer.play()
            _playbackState.value = PlaybackState.Playing(track.surahNumber, track.ayahNumber)
        }
    }

    fun pause() {
        val track = _currentTrack.value ?: return
        player?.pause()
        _playbackState.value = PlaybackState.Paused(track.surahNumber, track.ayahNumber)
    }

    fun stop() {
        progressJob?.cancel()
        player?.release()
        player = null
        _playbackState.value = PlaybackState.Idle
        _playbackProgress.value = 0f
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

    fun setSelectedQari(qari: Qari) {
        _selectedQari.value = qari
    }

    fun nextAyah() {
        val track = _currentTrack.value ?: return
        if (track.ayahNumber < track.totalAyahsInSurah) {
            playAyah(track.surahNumber, track.surahName, track.ayahNumber + 1, track.totalAyahsInSurah, track.qari)
        }
    }

    fun previousAyah() {
        val track = _currentTrack.value ?: return
        if (track.ayahNumber > 1) {
            playAyah(track.surahNumber, track.surahName, track.ayahNumber - 1, track.totalAyahsInSurah, track.qari)
        }
    }

    fun close() {
        stop()
        scope.coroutineContext[Job]?.cancel()
    }

    fun getAudioStorageBytes(): Long {
        return audioRoot().walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
    }

    fun clearDownloadedAudio(): Long {
        val bytes = getAudioStorageBytes()
        audioRoot().deleteRecursively()
        return bytes
    }

    fun getSurahAudioBytes(qari: Qari, surahNumber: Int): Long {
        val prefix = surahNumber.toString().padStart(3, '0')
        return File(audioRoot(), qari.id)
            .listFiles()
            ?.filter { it.isFile && it.name.startsWith(prefix) }
            ?.sumOf { it.length() }
            ?: 0L
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
        } else if (track.ayahNumber < track.totalAyahsInSurah) {
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

    private fun findLocalAudio(qari: Qari, surahNumber: Int, ayahNumber: Int): File? {
        val surah = surahNumber.toString().padStart(3, '0')
        val ayah = ayahNumber.toString().padStart(3, '0')
        return File(audioRoot(), "${qari.id}/$surah$ayah.mp3")
            .takeIf { it.isFile && it.length() > 0L }
    }

    private fun audioRoot(): File = File(context.filesDir, "audio")
}
