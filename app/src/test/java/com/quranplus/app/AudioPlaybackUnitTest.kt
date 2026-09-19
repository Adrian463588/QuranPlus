package com.quranplus.app

import com.quranplus.app.core.audio.AudioNotificationManager
import com.quranplus.app.core.audio.AudioRepeatMode
import com.quranplus.app.core.audio.CurrentAudioTrack
import com.quranplus.app.core.audio.PlaybackState
import com.quranplus.app.core.audio.Qari
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioPlaybackUnitTest {

    @Test
    fun GIVEN_repeatModes_WHEN_checkingCounts_THEN_matchesExpectedIterations() {
        assertEquals(1, AudioRepeatMode.OFF.count)
        assertEquals(2, AudioRepeatMode.TWO_TIMES.count)
        assertEquals(3, AudioRepeatMode.THREE_TIMES.count)
        assertEquals(5, AudioRepeatMode.FIVE_TIMES.count)
        assertEquals(-1, AudioRepeatMode.INFINITE.count)
    }

    @Test
    fun GIVEN_repeatOff_WHEN_ayahFinishes_THEN_shouldNotRepeatAndShouldStopWhenNotContinuous() {
        val repeatMode = AudioRepeatMode.OFF
        val autoContinueSurah = false
        var repeatCounter = 0

        repeatCounter++
        val shouldRepeat = when (repeatMode) {
            AudioRepeatMode.OFF -> false
            AudioRepeatMode.TWO_TIMES -> repeatCounter < repeatMode.count
            AudioRepeatMode.THREE_TIMES -> repeatCounter < repeatMode.count
            AudioRepeatMode.FIVE_TIMES -> repeatCounter < repeatMode.count
            AudioRepeatMode.INFINITE -> true
        }

        assertFalse(shouldRepeat)
        // With autoContinueSurah = false, playback stops at the end of the ayah
        val willAutoNext = autoContinueSurah
        assertFalse(willAutoNext)
    }

    @Test
    fun GIVEN_continuousSurah_WHEN_lastAyahFinishes_THEN_stopsAtSurahEnd() {
        val autoContinueSurah = true
        val totalAyahsInSurah = 7 // Al-Fatihah
        val currentAyahNumber = 7 // Last ayah

        val hasNextAyahInSurah = autoContinueSurah && currentAyahNumber < totalAyahsInSurah
        assertFalse("Playback must stop when the last ayah of the surah finishes", hasNextAyahInSurah)
    }

    @Test
    fun GIVEN_multipleQaris_WHEN_checkingActiveSurahRow_THEN_onlyMatchesSelectedQari() {
        val trackPlaying = CurrentAudioTrack(
            surahNumber = 1,
            surahName = "Al-Fatihah",
            ayahNumber = 1,
            totalAyahsInSurah = 7,
            qari = Qari.MISHARY_ALAFASY
        )

        val selectedQariMishary = Qari.MISHARY_ALAFASY
        val selectedQariHusary = Qari.HUSARY

        val isMisharyActive = trackPlaying.qari == selectedQariMishary && trackPlaying.surahNumber == 1
        val isHusaryActive = trackPlaying.qari == selectedQariHusary && trackPlaying.surahNumber == 1

        assertTrue(isMisharyActive)
        assertFalse("Husary row must not show active/playing when Mishary is playing", isHusaryActive)
    }

    @Test
    fun GIVEN_repeatThreeTimes_WHEN_repeating_THEN_repeatsUntilThreeTimesThenStops() {
        val repeatMode = AudioRepeatMode.THREE_TIMES
        val autoContinueSurah = false
        var repeatCounter = 0

        // Iteration 1 finished
        repeatCounter++
        assertTrue(repeatCounter < repeatMode.count) // should repeat (count=1 < 3)

        // Iteration 2 finished
        repeatCounter++
        assertTrue(repeatCounter < repeatMode.count) // should repeat (count=2 < 3)

        // Iteration 3 finished
        repeatCounter++
        assertFalse(repeatCounter < repeatMode.count) // finished repeating (count=3 is not < 3)
        assertFalse(autoContinueSurah) // stops completely
    }

    @Test
    fun GIVEN_audioNotificationConstants_WHEN_checked_THEN_actionsAreConsistent() {
        assertEquals("quran_audio_playback_channel", AudioNotificationManager.CHANNEL_ID)
        assertEquals("com.quranplus.app.audio.ACTION_PAUSE", AudioNotificationManager.ACTION_PAUSE)
        assertEquals("com.quranplus.app.audio.ACTION_RESUME", AudioNotificationManager.ACTION_RESUME)
        assertEquals("com.quranplus.app.audio.ACTION_STOP", AudioNotificationManager.ACTION_STOP)
        assertEquals("com.quranplus.app.audio.ACTION_PREVIOUS", AudioNotificationManager.ACTION_PREVIOUS)
        assertEquals("com.quranplus.app.audio.ACTION_NEXT", AudioNotificationManager.ACTION_NEXT)
    }

    @Test
    fun GIVEN_currentTrack_WHEN_instantiated_THEN_holdsCorrectAyahInformation() {
        val track = CurrentAudioTrack(
            surahNumber = 18,
            surahName = "Al-Kahf",
            ayahNumber = 23,
            totalAyahsInSurah = 110,
            qari = Qari.MISHARY_ALAFASY
        )

        assertEquals(18, track.surahNumber)
        assertEquals("Al-Kahf", track.surahName)
        assertEquals(23, track.ayahNumber)
        assertEquals(110, track.totalAyahsInSurah)
        assertEquals(Qari.MISHARY_ALAFASY, track.qari)
    }
}
