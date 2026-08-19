package com.quranplus.app.core.audio

import java.util.Locale

data class AudioSourceDescriptor(
    val qari: Qari,
    val folder: String,
    val checksumUrl: String,
    val sourceUrl: String = EveryAyahAudioSource.CATALOG_URL
) {
    fun audioUrl(surahNumber: Int, ayahNumber: Int): String =
        "${EveryAyahAudioSource.BASE_URL}/$folder/${fileName(surahNumber, ayahNumber)}"

    fun fileName(surahNumber: Int, ayahNumber: Int): String =
        "%03d%03d.mp3".format(Locale.ROOT, surahNumber, ayahNumber)
}

object EveryAyahAudioSource {
    const val BASE_URL = "https://everyayah.com/data"
    const val CATALOG_URL = "https://everyayah.com/recitations_ayat.html"

    private val checksumLine = Regex("^([0-9a-fA-F]{32})\\s+\\*?([^\\s]+)$")

    fun descriptor(qari: Qari): AudioSourceDescriptor = AudioSourceDescriptor(
        qari = qari,
        folder = qari.everyAyahFolder,
        checksumUrl = "$BASE_URL/${qari.everyAyahFolder}/000_checksum.md5"
    )

    fun parseChecksums(content: String): Map<String, String> = content
        .lineSequence()
        .mapNotNull { line ->
            checksumLine.matchEntire(line.trim())?.destructured?.let { (checksum, fileName) ->
                fileName to checksum.lowercase(Locale.ROOT)
            }
        }
        .toMap()
}
