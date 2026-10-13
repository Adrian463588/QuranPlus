package com.quranplus.app.features.hadith.data

import com.quranplus.app.core.database.entity.HadithEntity
import org.json.JSONObject
import java.security.MessageDigest

/** Content-bound crosswalk; archive identities remain stable for saved user data. */
class HadithNumbering(private val loadJson: () -> String = { "{}" }) {
    private data class Entry(val archive: Int, val number: Int, val book: Int, val hadith: Int, val hash: String)

    private val entries: Map<String, Map<Int, Entry>> by lazy {
        val document = JSONObject(loadJson())
        val collections = document.optJSONObject("collections") ?: return@lazy emptyMap()
        require(document.getString("bundle_sha256") == HadithBundleManifest.VERIFIED.archiveSha256)
        collections.keys().asSequence().associateWith { collection ->
            val rows = collections.getJSONArray(collection)
            (0 until rows.length()).associate { index ->
                val row = rows.getJSONArray(index)
                val entry = Entry(row.getInt(0), row.getInt(1), row.getInt(2), row.getInt(3), row.getString(4))
                entry.archive to entry
            }
        }
    }

    fun archiveNumbers(collection: String, referenceNumber: Int): List<Int> =
        entries[collection].orEmpty().values.filter { it.number == referenceNumber }.map { it.archive }

    fun hasReferenceNumbers(collection: String): Boolean = collection in entries

    fun referenceNumber(row: HadithEntity): Int? = entry(row)?.number

    fun isContentValid(row: HadithEntity): Boolean =
        entries[row.collectionId]?.containsKey(row.hadithNumber) != true || entry(row) != null

    fun indonesianTranslation(row: HadithEntity): String =
        if (row.collectionId == "abudawud" && referenceNumber(row) == 3837) {
            // زبد is butter, as confirmed by sunnah.com/abudawud:3837.
            row.translationId.replace("keju dan kurma", "mentega dan kurma")
        } else row.translationId

    fun canonicalUrl(row: HadithEntity): String? = entry(row)?.let { mapped ->
        // A Muslim base-number URL selects variant a. In-book references are
        // retained in the label; no unverified variant suffix/URL is invented.
        if (row.collectionId == "muslim") return@let null
        "https://sunnah.com/${row.collectionId}:${mapped.number}"
    }

    fun numberLabel(row: HadithEntity): String = entry(row)?.let {
        "No. ${it.number} (Sunnah.com)"
    } ?: "No. ${row.hadithNumber} (nomor arsip)"

    fun reference(row: HadithEntity): String {
        if (row.sourceRevision != HadithBundleManifest.VERIFIED.revision) return row.reference
        val mapped = entry(row)
        val archive = "edisi tafsirq/gading no. ${row.hadithNumber}"
        return if (mapped == null) {
            "${row.title} — $archive; nomor Sunnah.com belum dipetakan"
        } else {
            "${row.title} no. ${mapped.number} (Sunnah.com, Kitab ${mapped.book}, Hadis ${mapped.hadith}); $archive"
        }
    }

    private fun entry(row: HadithEntity): Entry? {
        if (!HadithBundleManifest.VERIFIED.isVerifiedRecord(row)) return null
        val mapped = entries[row.collectionId]?.get(row.hadithNumber) ?: return null
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest("${row.textArabic}\n${row.translationId}".toByteArray(Charsets.UTF_8))
        val digest = buildString(64) {
            bytes.forEach { byte ->
                val value = byte.toInt() and 255
                append("0123456789abcdef"[value ushr 4])
                append("0123456789abcdef"[value and 15])
            }
        }
        return mapped.takeIf { it.hash == digest }
    }
}
