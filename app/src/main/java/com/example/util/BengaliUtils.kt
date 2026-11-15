package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object BengaliUtils {
    private val bengaliDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    fun toBengaliDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(bengaliDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun toEnglishDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            val idx = bengaliDigits.indexOf(ch)
            if (idx >= 0) {
                sb.append(('0'.code + idx).toChar())
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun formatInt(value: Int): String = toBengaliDigits(value.toString())

    fun formatCountdownMmSs(remainingMillis: Long): String {
        val totalSeconds = (remainingMillis.coerceAtLeast(0L) + 999L) / 1000L
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    fun formatCountdownBengaliMmSs(remainingMillis: Long): String {
        return toBengaliDigits(formatCountdownMmSs(remainingMillis))
    }

    fun formatDateHeader(timestampMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val calNow = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val calTarget = Calendar.getInstance().apply { timeInMillis = timestampMillis }

        val sameDay = calNow.get(Calendar.YEAR) == calTarget.get(Calendar.YEAR) &&
            calNow.get(Calendar.DAY_OF_YEAR) == calTarget.get(Calendar.DAY_OF_YEAR)
        if (sameDay) return "আজ"

        val yesterday = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val isYesterday = yesterday.get(Calendar.YEAR) == calTarget.get(Calendar.YEAR) &&
            yesterday.get(Calendar.DAY_OF_YEAR) == calTarget.get(Calendar.DAY_OF_YEAR)
        if (isYesterday) return "গতকাল"

        val sdf = SimpleDateFormat("dd MMM yyyy", Locale("bn", "BD"))
        return toBengaliDigits(sdf.format(Date(timestampMillis)))
    }

    fun formatTimeRange(startMillis: Long, endMillis: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale("bn", "BD"))
        return "${toBengaliDigits(sdf.format(Date(startMillis)))} – ${toBengaliDigits(sdf.format(Date(endMillis)))}"
    }

    fun dayStartMillis(nowMillis: Long = System.currentTimeMillis()): Long {
        return Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun weekStartMillis(nowMillis: Long = System.currentTimeMillis()): Long {
        return Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -6)
        }.timeInMillis
    }
}

sealed interface EntryValidationResult {
    data class Valid(val purpose: String, val minutes: Int) : EntryValidationResult
    data class Invalid(val purposeError: String?, val minutesError: String?) : EntryValidationResult
}

object EntryValidator {
    const val MIN_MINUTES = 1
    const val MAX_MINUTES = 180

    fun validate(purposeInput: String, minutesInput: String): EntryValidationResult {
        val cleanPurpose = purposeInput.trim()
        val purposeError = if (cleanPurpose.isEmpty()) {
            "অনুগ্রহ করে কাজের উদ্দেশ্য লিখুন।"
        } else {
            null
        }

        val normalizedDigits = BengaliUtils.toEnglishDigits(minutesInput.trim())
        val parsedMinutes = normalizedDigits.toIntOrNull()
        val minutesError = when {
            normalizedDigits.isEmpty() -> "অনুগ্রহ করে মিনিট লিখুন (১ থেকে ১৮০)।"
            parsedMinutes == null -> "শুধুমাত্র সংখ্যা লিখুন (১ থেকে ১৮০)।"
            parsedMinutes < MIN_MINUTES || parsedMinutes > MAX_MINUTES ->
                "সময় ১ থেকে ১৮০ মিনিটের মধ্যে হতে হবে।"
            else -> null
        }

        return if (purposeError == null && minutesError == null && parsedMinutes != null) {
            EntryValidationResult.Valid(cleanPurpose, parsedMinutes)
        } else {
            EntryValidationResult.Invalid(purposeError, minutesError)
        }
    }
}
