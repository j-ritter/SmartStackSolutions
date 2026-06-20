package com.ritter.smartstackbills

import java.util.Calendar
import java.util.Date

object AppDateUtils {
    fun isBeforeToday(date: Date): Boolean {
        return startOfDay(date).before(startOfToday())
    }

    fun isTodayOrAfter(date: Date): Boolean {
        return !startOfDay(date).before(startOfToday())
    }

    private fun startOfToday(): Date = startOfDay(Date())

    private fun startOfDay(date: Date): Date {
        return Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }
}
