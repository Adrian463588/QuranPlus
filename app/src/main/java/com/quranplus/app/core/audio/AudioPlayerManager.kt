package com.quranplus.app.core.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * Qari Profiles for Murottal Playback
 */
enum class Qari(
    val id: String,
    val displayName: String,
    val style: String,
    val cdnSubfolder: String
) {
    MISHARY_ALAFASY(
        id = "alafasy",
        displayName = "Mishary Rashid Alafasy",
        style = "Murattal Hafs - Suara Merdu & Jelas",
        cdnSubfolder = "Alafasy_128kbps"
    ),
    HUSARY(
        id = "husary",
        displayName = "Mahmud Khalil Al-Husary",
        style = "Mu'allim (Tartil Standar Tajwid)",
        cdnSubfolder = "Husary_128kbps"
    ),
    SUDAIS(
        id = "sudais",
        displayName = "Abdurrahman As-Sudais",
        style = "Imam Masjidil Haram",
        cdnSubfolder = "Abdurrahmaan_As-Sudais_192kbps"
    );

    companion object {
        fun fromId(id: String) = entries.firstOrNull { it.id == id } ?: MISHARY_ALAFASY
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

class AudioPlayerManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var mediaPlayer: MediaPlayer? = null

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
    private var progressJob: Job? = null

    /**
     * Constructs URL or local file path for an ayah.
     */
    fun getAyahAudioUrl(qari: Qari, surahNumber: Int, ayahNumber: Int): String {
        val sStr = surahNumber.toString().padStart(3, '0')
        val aStr = ayahNumber.toString().padStart(3, '0')

        // Check local storage first
        val localFile = File(context.filesDir, "audio/${qari.id}/${sStr}${aStr}.mp3")
        if (localFile.exists() && localFile.length() > 0) {
            return localFile.absolutePath
        }

        // Fallback to EveryAyah CDN
        return "https://everyayah.com/data/${qari.cdnSubfolder}/${sStr}${aStr}.mp3"
    }

    /**
     * Plays a specific ayah.
     */
    fun playAyah(
        surahNumber: Int,
        surahName: String,
        ayahNumber: Int,
        totalAyahsInSurah: Int,
        qari: Qari = _selectedQari.value
    ) {
        stop()

        _currentTrack.value = CurrentAudioTrack(
            surahNumber = surahNumber,
            surahName = surahName,
            ayahNumber = ayahNumber,
            totalAyahsInSurah = totalAyahsInSurah,
            qari = qari
        )
        _playbackState.value = PlaybackState.Buffering
        currentRepeatCounter = 0

        val url = getAyahAudioUrl(qari, surahNumber, ayahNumber)

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(url)
                setOnPreparedListener { mp ->
                    applyPlaybackSpeed()
                    mp.start()
                    _playbackState.value = PlaybackState.Playing(surahNumber, ayahNumber)
                    startProgressTracker()
                }
                setOnCompletionListener {
                    handleTrackCompletion()
                }
                setOnErrorListener { _, what, extra ->
                    _playbackState.value = PlaybackState.Error("Gagal memutar audio ($what:$extra)")
                    false
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            _playbackState.value = PlaybackState.Error(e.message ?: "Kesalahan inisialisasi audio")
        }
    }

    fun togglePlayPause() {
        val track = _currentTrack.value ?: return
        mediaPlayer?.let { mp ->
            if (mp.isPlaying) {
                mp.pause()
                _playbackState.value = PlaybackState.Paused(track.surahNumber, track.ayahNumber)
            } else {
                mp.start()
                _playbackState.value = PlaybackState.Playing(track.surahNumber, track.ayahNumber)
            }
        }
    }

    fun pause() {
        val track = _currentTrack.value ?: return
        mediaPlayer?.let { mp ->
            if (mp.isPlaying) {
                mp.pause()
                _playbackState.value = PlaybackState.Paused(track.surahNumber, track.ayahNumber)
            }
        }
    }

    fun stop() {
        progressJob?.cancel()
        mediaPlayer?.let { mp ->
            try {
                if (mp.isPlaying) mp.stop()
                mp.reset()
                mp.release()
            } catch (ignored: Exception) {}
        }
        mediaPlayer = null
        _playbackState.value = PlaybackState.Idle
        _playbackProgress.value = 0f
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        applyPlaybackSpeed()
    }

    fun setRepeatMode(mode: AudioRepeatMode) {
        _repeatMode.value = mode
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
                qari = track.qari
            )
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
                qari = track.qari
            )
        }
    }

    private fun handleTrackCompletion() {
        val track = _currentTrack.value ?: return
        val currentRepeat = _repeatMode.value

        currentRepeatCounter++
        val shouldRepeat = when (currentRepeat) {
            AudioRepeatMode.OFF -> false
            AudioRepeatMode.TWO_TIMES -> currentRepeatCounter < 2
            AudioRepeatMode.THREE_TIMES -> currentRepeatCounter < 3
            AudioRepeatMode.FIVE_TIMES -> currentRepeatCounter < 5
            AudioRepeatMode.INFINITE -> true
        }

        if (shouldRepeat) {
            mediaPlayer?.seekTo(0)
            mediaPlayer?.start()
            _playbackState.value = PlaybackState.Playing(track.surahNumber, track.ayahNumber)
        } else {
            // Auto advance to next ayah if available
            if (track.ayahNumber < track.totalAyahsInSurah) {
                nextAyah()
            } else {
                stop()
            }
        }
    }

    private fun applyPlaybackSpeed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mediaPlayer?.let { mp ->
                try {
                    val params = mp.playbackParams ?: PlaybackParams()
                    params.speed = _playbackSpeed.value
                    params.pitch = 1.0f
                    mp.playbackParams = params
                } catch (ignored: Exception) {}
            }
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (true) {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying && mp.duration > 0) {
                        _playbackProgress.value = (mp.currentPosition.toFloat() / mp.duration.toFloat()).coerceIn(0f, 1f)
                    }
                }
                delay(300)
            }
        }
    }
}
