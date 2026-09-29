package com.alpkcgl.fikirhane.core

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Editör kuralları; API ile aynı (hatalı yazı sunucuya gönderilmeden yakalanır). */
object PostRules {
    const val MAX_TAGS = 5
    private val TAG = Regex("^[\\p{L}\\p{N}][\\p{L}\\p{N} -]*$")

    fun title(value: String): String? = when (value.trim().length) {
        in 0..2 -> "Başlık en az 3 karakter olmalı"
        in 3..200 -> null
        else -> "Başlık en fazla 200 karakter olabilir"
    }

    fun content(value: String): String? = when {
        value.isBlank() -> "İçerik boş olamaz"
        value.length > 100_000 -> "İçerik en fazla 100.000 karakter olabilir"
        else -> null
    }

    /** "Yapay Zeka,  eğitim ," → ["Yapay Zeka", "eğitim"] */
    fun parseTags(value: String): List<String> =
        value.split(',').map { it.trim().replace(Regex("\\s+"), " ") }.filter { it.isNotEmpty() }

    fun tags(tags: List<String>): String? = when {
        tags.size > MAX_TAGS -> "En fazla $MAX_TAGS etiket eklenebilir"
        tags.any { it.length !in 2..30 } -> "Her etiket 2-30 karakter olmalı"
        tags.any { !TAG.matches(it) } -> "Etiketler yalnızca harf, rakam, boşluk ve - içerebilir"
        else -> null
    }

    /** Sunucuyla aynı tahmin: dakikada 200 kelime, en az 1 dakika. */
    fun readingTime(markdown: String): Int {
        val words = markdown.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
        return maxOf(1, (words + 199) / 200)
    }
}

object Format {
    private val TURKISH = Locale.forLanguageTag("tr-TR")
    private val DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", TURKISH)

    /** "2026-09-29T13:33:15.958Z" → "29 Eylül 2026" (cihazın saat diliminde). */
    fun date(iso: String): String = runCatching {
        DATE.format(Instant.parse(iso).atZone(ZoneId.systemDefault()))
    }.getOrDefault("")
}
