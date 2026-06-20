package com.ritter.smartstackbills

import android.content.Context

object PaymentNotificationPreferences {
    private const val PREFS = "payment_notification_settings"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_ADVANCE_DAYS = "advance_days"
    private const val KEY_DUE_TODAY = "due_today"
    private const val KEY_OVERDUE = "overdue"
    private const val KEY_WEEKLY_OVERDUE = "weekly_overdue"

    fun isEnabled(context: Context) = prefs(context).getBoolean(KEY_ENABLED, true)
    fun advanceDays(context: Context) = prefs(context).getInt(KEY_ADVANCE_DAYS, 3)
    fun dueTodayEnabled(context: Context) = prefs(context).getBoolean(KEY_DUE_TODAY, true)
    fun overdueEnabled(context: Context) = prefs(context).getBoolean(KEY_OVERDUE, true)
    fun weeklyOverdueEnabled(context: Context) = prefs(context).getBoolean(KEY_WEEKLY_OVERDUE, true)

    fun save(
        context: Context,
        enabled: Boolean,
        advanceDays: Int,
        dueToday: Boolean,
        overdue: Boolean,
        weeklyOverdue: Boolean
    ) {
        prefs(context).edit()
            .putBoolean(KEY_ENABLED, enabled)
            .putInt(KEY_ADVANCE_DAYS, advanceDays)
            .putBoolean(KEY_DUE_TODAY, dueToday)
            .putBoolean(KEY_OVERDUE, overdue)
            .putBoolean(KEY_WEEKLY_OVERDUE, weeklyOverdue)
            .apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
