package com.ritter.smartstackbills

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object ScanDateValidator {
    private const val MIN_REASONABLE_YEAR = 2000

    fun sanitizeDisplayDate(value: String?): String? {
        val raw = value?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val formats = listOf(
            "dd/MM/yyyy", "d/M/yyyy", "dd.MM.yyyy", "d.M.yyyy",
            "dd-MM-yyyy", "d-M-yyyy", "yyyy-MM-dd", "yyyy/MM/dd",
            "MM/dd/yyyy", "M/d/yyyy"
        )
        formats.forEach { pattern ->
            val parser = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }
            val parsedDate = runCatching { parser.parse(raw) }.getOrNull() ?: return@forEach
            if (isReasonable(parsedDate.time)) {
                return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(parsedDate)
            }
        }
        return null
    }

    fun isReasonable(epochMillis: Long): Boolean {
        val year = Calendar.getInstance().apply { timeInMillis = epochMillis }
            .get(Calendar.YEAR)
        val maxReasonableYear = Calendar.getInstance().get(Calendar.YEAR) + 2
        return year in MIN_REASONABLE_YEAR..maxReasonableYear
    }
}
