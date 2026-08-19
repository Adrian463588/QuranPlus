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
import org.json.JSONArray
import java.security.MessageDigest
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

    private data class AudioManifestEntry(
        val qariId: String,
        val surahNumber: Int,
        val ayahNumber: Int,
        val fileName: String,
        val sha256: String
    )

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
        return readManifest().sumOf { entry ->
            verifiedFile(entry)?.length() ?: 0L
        }
    }

    fun clearDownloadedAudio(): Long {
        val bytes = getAudioStorageBytes()
        audioRoot().deleteRecursively()
        return bytes
    }

    fun getSurahAudioBytes(qari: Qari, surahNumber: Int): Long {
        return readManifest()
            .asSequence()
            .filter { it.qariId == qari.id && it.surahNumber == surahNumber }
            .sumOf { entry -> verifiedFile(entry)?.length() ?: 0L }
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
        if (surahNumber <= 0 || ayahNumber <= 0) return null
        val entry = readManifest().firstOrNull {
            it.qariId == qari.id &&
                it.surahNumber == surahNumber &&
                it.ayahNumber == ayahNumber
        } ?: return null
        return verifiedFile(entry)
    }

    private fun verifiedFile(entry: AudioManifestEntry): File? {
        val qariDirectory = File(audioRoot(), entry.qariId)
        val file = File(qariDirectory, entry.fileName)
        val isInsideQariDirectory = runCatching {
            file.canonicalPath.startsWith(qariDirectory.canonicalPath + File.separator)
        }.getOrDefault(false)
        return file.takeIf {
            isInsideQariDirectory &&
                it.isFile &&
                it.length() > 0L &&
                calculateSha256(it).equals(entry.sha256, ignoreCase = true)
        }
    }

    private fun audioRoot(): File = File(context.filesDir, "audio")

    private fun readManifest(): List<AudioManifestEntry> {
        val manifestFile = File(audioRoot(), AUDIO_MANIFEST_NAME)
        if (!manifestFile.isFile) return emptyList()
        return runCatching {
            val assets = JSONArray(manifestFile.readText(Charsets.UTF_8))
            buildList(assets.length()) {
                for (index in 0 until assets.length()) {
                    val item = assets.getJSONObject(index)
                    val fileName = item.getString("file_name")
                    val sha256 = item.getString("sha256")
                    if (fileName.isBlank() || File(fileName).name != fileName ||
                        !sha256.matches(SHA256_PATTERN)
                    ) {
                        continue
                    }
                    add(
                        AudioManifestEntry(
                            qariId = item.getString("qari_id"),
                            surahNumber = item.getInt("surah_number"),
                            ayahNumber = item.getInt("ayah_number"),
                            fileName = fileName,
                            sha256 = sha256
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val AUDIO_MANIFEST_NAME = "manifest.json"
        val SHA256_PATTERN = Regex("[0-9a-fA-F]{64}")
    }
}
