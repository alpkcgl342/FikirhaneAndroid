package com.alpkcgl.fikirhane.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PostRulesTest {

    @Test
    fun `başlık ve içerik`() {
        assertNotNull(PostRules.title("ab"))
        assertNull(PostRules.title("  Merhaba  "))
        assertNotNull(PostRules.title("a".repeat(201)))
        assertNotNull(PostRules.content("   "))
        assertNull(PostRules.content("# Başlık"))
    }

    @Test
    fun `etiketler virgülle ayrılır, boşluklar sadeleşir`() {
        assertEquals(listOf("Yapay Zeka", "eğitim"), PostRules.parseTags(" Yapay   Zeka ,, eğitim , "))
        assertEquals(emptyList<String>(), PostRules.parseTags("  "))
    }

    @Test
    fun `etiket kuralları API ile aynı`() {
        assertNull(PostRules.tags(listOf("yapay zeka", "bilim-kurgu", "Işık")))
        assertNotNull(PostRules.tags(List(6) { "etiket$it" }))
        assertNotNull(PostRules.tags(listOf("a")))
        assertNotNull(PostRules.tags(listOf("<b>")))
    }

    @Test
    fun `okuma süresi dakikada 200 kelime, en az 1`() {
        assertEquals(1, PostRules.readingTime(""))
        assertEquals(1, PostRules.readingTime("kelime ".repeat(200)))
        assertEquals(2, PostRules.readingTime("kelime ".repeat(201)))
    }

    @Test
    fun `tarih Türkçe biçimlenir`() {
        // Saat dilimi farkı günü değiştirmesin diye gün ortası seçildi.
        assertEquals("29 Eylül 2026", Format.date("2026-09-29T12:00:00Z"))
        assertEquals("", Format.date("bozuk"))
    }
}
