package com.example.practice

import com.example.practice.ui.formatting.formatDuration
import com.example.practice.ui.formatting.formatPrice
import com.example.practice.ui.formatting.formatReleaseDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrackFormattingTest {
    @Test
    fun durationKeepsMinutesAndPadsSeconds() {
        assertEquals("5:20", formatDuration(320357))
        assertEquals("0:05", formatDuration(5999))
        assertEquals("60:00", formatDuration(3600000))
        assertEquals("0:00", formatDuration(-1))
    }

    @Test
    fun dateHandlesIsoAndInvalidInput() {
        assertEquals("30.11.2000", formatReleaseDate("2000-11-30T08:00:00Z"))
        assertEquals("29.02.2024", formatReleaseDate("2024-02-29T23:30:00Z"))
        assertEquals("2001-02-29T08:00:00Z", formatReleaseDate("2001-02-29T08:00:00Z"))
        assertEquals("unknown", formatReleaseDate("unknown"))
    }

    @Test
    fun priceHandlesMissingValueAndDecimalPrecision() {
        assertEquals("1.29 USD", formatPrice(1.29, "USD"))
        assertEquals("0.99 EUR", formatPrice(0.99, "EUR"))
        assertNull(formatPrice(null, "USD"))
    }
}
