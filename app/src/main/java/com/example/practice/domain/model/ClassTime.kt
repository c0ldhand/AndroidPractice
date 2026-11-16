package com.example.practice.domain.model

import java.util.Calendar
import java.util.Locale

internal data class ClassTime(val hour: Int, val minute: Int) {
    fun format(): String = String.format(Locale.ROOT, "%02d:%02d", hour, minute)

    fun todayAt(now: Calendar): Long? {
        val target = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val millis = target.timeInMillis
        // A local time skipped during a clock change cannot be scheduled as entered.
        return millis.takeIf { target.get(Calendar.HOUR_OF_DAY) == hour && target.get(Calendar.MINUTE) == minute }
    }

    companion object {
        private val pattern = Regex("(?:[01][0-9]|2[0-3]):[0-5][0-9]")

        fun parse(text: String): ClassTime? = if (pattern.matches(text)) {
            ClassTime(text.substring(0, 2).toInt(), text.substring(3).toInt())
        } else null
    }
}
