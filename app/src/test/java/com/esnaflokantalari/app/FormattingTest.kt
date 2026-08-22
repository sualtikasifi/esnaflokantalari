package com.esnaflokantalari.app

import com.esnaflokantalari.app.data.containsTr
import com.esnaflokantalari.app.data.equalsTr
import com.esnaflokantalari.app.ui.formatCount
import com.esnaflokantalari.app.ui.formatDistance
import com.esnaflokantalari.app.ui.formatRating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FormattingTest {

    @Test
    fun `yorum sayisi binlik ayracla gosterilir`() {
        assertEquals("18.823", 18823.formatCount())
        assertEquals("120", 120.formatCount())
    }

    @Test
    fun `puan tek ondalikla gosterilir`() {
        assertEquals("4,9", 4.85.formatRating())
        assertEquals("4,5", 4.5.formatRating())
    }

    @Test
    fun `mesafe bir kilometrenin altinda metre olarak gosterilir`() {
        assertEquals("640 m", 640.0.formatDistance())
        assertEquals("2,4 km", 2350.0.formatDistance())
    }
}

class TurkishSearchTest {

    @Test
    fun `noktasiz i ile arama noktali I ile eslesir`() {
        assertTrue("İstanbul".containsTr("istanbul"))
        assertTrue("İstanbul".containsTr("İSTANBUL"))
        assertTrue("Iğdır".containsTr("ığdır"))
    }

    @Test
    fun `sehir adi buyuk kucuk harften bagimsiz esitlenir`() {
        assertTrue("İzmir".equalsTr("izmir"))
        assertTrue("Şanlıurfa".equalsTr("ŞANLIURFA"))
        assertFalse("İzmir".equalsTr("İzmit"))
    }

    @Test
    fun `alakasiz sorgu eslesmez`() {
        assertFalse("Ankara".containsTr("Bursa"))
    }
}
