package com.example.practice.ui.formatting

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

fun formatDuration(trackTimeMillis: Long): String {
    val totalSeconds = trackTimeMillis.coerceAtLeast(0) / 1000
    return String.format(Locale.ROOT, "%d:%02d", totalSeconds / 60, totalSeconds % 60)
}

/** Preserve the release's calendar date and support API 24 without desugaring. */
fun formatReleaseDate(releaseDate: String): String {
    val datePart = releaseDate.take(10)
    if (!datePart.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) return releaseDate
    val utc = TimeZone.getTimeZone("UTC")
    val parser = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
        isLenient = false
        timeZone = utc
    }
    val position = ParsePosition(0)
    val date = parser.parse(datePart, position) ?: return releaseDate
    if (position.index != datePart.length) return releaseDate
    return SimpleDateFormat("dd.MM.yyyy", Locale.ROOT).apply { timeZone = utc }.format(date)
}

fun formatPrice(price: Double?, currency: String): String? =
    price?.let { String.format(Locale.ROOT, "%.2f %s", it, currency) }
