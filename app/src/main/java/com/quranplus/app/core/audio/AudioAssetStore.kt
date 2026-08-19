package com.quranplus.app.core.audio

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.Locale

enum class AudioChecksumAlgorithm {
    MD5,
    SHA256;

    companion object {
        fun fromValue(value: String): AudioChecksumAlgorithm? =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
    }
}

data class VerifiedAudioAsset(
    val qariId: String,
    val surahNumber: Int,
    val ayahNumber: Int,
    val fileName: String,
    val checksum: String,
    val checksumAlgorithm: AudioChecksumAlgorithm,
    val sourceUrl: String
)

class AudioAssetStore(context: Context) {
    private val root = File(context.filesDir, AUDIO_DIRECTORY)
    private val manifestFile = File(root, AUDIO_MANIFEST_NAME)

    @Synchronized
    fun fileFor(qari: Qari, surahNumber: Int, ayahNumber: Int): File {
        require(surahNumber in 1..114) { "Nomor surah audio tidak valid" }
        require(ayahNumber > 0) { "Nomor ayat audio tidak valid" }
        return File(
            File(root, qari.id),
            EveryAyahAudioSource.descriptor(qari).fileName(surahNumber, ayahNumber)
        )
    }

    @Synchronized
    fun findVerifiedFile(qari: Qari, surahNumber: Int, ayahNumber: Int): File? {
        val entry = readManifest().firstOrNull {
            it.qariId == qari.id &&
                it.surahNumber == surahNumber &&
                it.ayahNumber == ayahNumber
        } ?: return null
        return verifiedFile(entry)
    }

    @Synchronized
    fun hasVerifiedFile(qari: Qari, surahNumber: Int, ayahNumber: Int): Boolean =
        findVerifiedFile(qari, surahNumber, ayahNumber) != null

    @Synchronized
    fun publish(entry: VerifiedAudioAsset) {
        val file = File(File(root, entry.qariId), entry.fileName)
        require(file.isFile && file.length() > 0L) { "Audio candidate belum siap dipublikasikan" }
        val entries = readManifest()
            .filterNot {
                it.qariId == entry.qariId &&
                    it.surahNumber == entry.surahNumber &&
                    it.ayahNumber == entry.ayahNumber
            }
            .plus(entry)
            .sortedWith(compareBy(VerifiedAudioAsset::qariId, VerifiedAudioAsset::surahNumber, VerifiedAudioAsset::ayahNumber))
        writeManifest(entries)
    }

    @Synchronized
    fun getSurahAudioBytes(qari: Qari, surahNumber: Int): Long = readManifest()
        .asSequence()
        .filter { it.qariId == qari.id && it.surahNumber == surahNumber }
        .mapNotNull(::storedFile)
        .sumOf(File::length)

    @Synchronized
    fun getAudioStorageBytes(): Long = readManifest()
        .asSequence()
        .mapNotNull(::storedFile)
        .sumOf(File::length)

    @Synchronized
    fun clear() {
        root.deleteRecursively()
    }

    private fun verifiedFile(entry: VerifiedAudioAsset): File? {
        val file = storedFile(entry) ?: return null
        val actual = calculateDigest(file, entry.checksumAlgorithm)
        return file.takeIf { actual.equals(entry.checksum, ignoreCase = true) }
    }

    /**
     * Cache metrics must stay cheap during Compose recomposition. Playback still
     * calls [verifiedFile], which performs the digest check before opening a file.
     */
    private fun storedFile(entry: VerifiedAudioAsset): File? {
        if (entry.qariId.isBlank() || File(entry.qariId).name != entry.qariId) return null
        if (entry.fileName.isBlank() || File(entry.fileName).name != entry.fileName) return null
        val directory = File(root, entry.qariId)
        val file = File(directory, entry.fileName)
        val insideDirectory = runCatching {
            file.canonicalPath.startsWith(directory.canonicalPath + File.separator)
        }.getOrDefault(false)
        return file.takeIf { insideDirectory && it.isFile && it.length() > 0L }
    }

    private fun readManifest(): List<VerifiedAudioAsset> {
        if (!manifestFile.isFile) return emptyList()
        return runCatching {
            val array = JSONArray(manifestFile.readText(Charsets.UTF_8))
            buildList(array.length()) {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val algorithm = AudioChecksumAlgorithm.fromValue(
                        item.optString("checksum_algorithm", "SHA256")
                    ) ?: continue
                    val checksum = item.optString("checksum")
                        .ifBlank { item.optString("sha256") }
                    val expectedLength = if (algorithm == AudioChecksumAlgorithm.MD5) 32 else 64
                    if (checksum.length != expectedLength ||
                        !checksum.matches(Regex("[0-9a-fA-F]{$expectedLength}"))
                    ) continue
                    val qariId = item.optString("qari_id")
                    val fileName = item.optString("file_name")
                    val surahNumber = item.optInt("surah_number", -1)
                    val ayahNumber = item.optInt("ayah_number", -1)
                    if (qariId.isBlank() || File(qariId).name != qariId ||
                        fileName.isBlank() || File(fileName).name != fileName ||
                        surahNumber <= 0 || ayahNumber <= 0
                    ) continue
                    add(
                        VerifiedAudioAsset(
                            qariId = qariId,
                            surahNumber = surahNumber,
                            ayahNumber = ayahNumber,
                            fileName = fileName,
                            checksum = checksum.lowercase(Locale.ROOT),
                            checksumAlgorithm = algorithm,
                            sourceUrl = item.optString("source_url")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun writeManifest(entries: List<VerifiedAudioAsset>) {
        if (!root.exists() && !root.mkdirs()) {
            error("Folder audio tidak dapat dibuat")
        }
        val temporary = File(root, "$AUDIO_MANIFEST_NAME.tmp")
        val json = JSONArray().apply {
            entries.forEach { entry ->
                put(
                    JSONObject()
                        .put("qari_id", entry.qariId)
                        .put("surah_number", entry.surahNumber)
                        .put("ayah_number", entry.ayahNumber)
                        .put("file_name", entry.fileName)
                        .put("checksum", entry.checksum)
                        .put("checksum_algorithm", entry.checksumAlgorithm.name)
                        .put("source_url", entry.sourceUrl)
                )
            }
        }
        temporary.writeText(json.toString(), Charsets.UTF_8)
        try {
            Files.move(
                temporary.toPath(),
                manifestFile.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
        } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
            Files.move(
                temporary.toPath(),
                manifestFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }

    private fun calculateDigest(file: File, algorithm: AudioChecksumAlgorithm): String {
        val digest = MessageDigest.getInstance(
            when (algorithm) {
                AudioChecksumAlgorithm.MD5 -> "MD5"
                AudioChecksumAlgorithm.SHA256 -> "SHA-256"
            }
        )
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(Locale.ROOT, it) }
    }

    private companion object {
        const val AUDIO_DIRECTORY = "audio"
        const val AUDIO_MANIFEST_NAME = "manifest.json"
    }
}
