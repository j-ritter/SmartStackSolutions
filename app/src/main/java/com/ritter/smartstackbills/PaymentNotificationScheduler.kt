package com.ritter.smartstackbills

import android.content.Context
import android.app.NotificationManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

object PaymentNotificationScheduler {
    const val EVENT_ADVANCE = "advance"
    const val EVENT_DUE_TODAY = "due_today"
    const val EVENT_OVERDUE = "overdue"
    const val EVENT_WEEKLY_OVERDUE = "weekly_overdue"

    fun rescheduleAll(
        context: Context,
        userUid: String,
        forceReplace: Boolean = true,
        onComplete: (() -> Unit)? = null
    ) {
        FirebaseFirestore.getInstance()
            .collection("users").document(userUid).collection("bills")
            .get()
            .addOnSuccessListener { documents ->
                documents.forEach { document ->
                    document.toObject(Bills::class.java)?.let { bill ->
                        if (bill.billId.isNullOrBlank()) bill.billId = document.id
                        scheduleBill(context, userUid, bill, forceReplace)
                    }
                }
                onComplete?.invoke()
            }
            .addOnFailureListener { onComplete?.invoke() }
    }

    fun scheduleBill(
        context: Context,
        userUid: String,
        bill: Bills,
        forceReplace: Boolean = false
    ) {
        val billId = bill.billId ?: return
        if (!PaymentNotificationPreferences.isEnabled(context) || bill.paid) {
            cancelBill(context, billId)
            return
        }
        if (forceReplace) cancelBill(context, billId)
        val dueTimestamp = bill.date ?: return

        val advanceDays = PaymentNotificationPreferences.advanceDays(context)
        if (advanceDays > 0) {
            enqueue(
                context, userUid, bill, EVENT_ADVANCE,
                reminderTime(dueTimestamp.toDate(), -advanceDays),
                forceReplace
            )
        }
        if (PaymentNotificationPreferences.dueTodayEnabled(context)) {
            enqueue(
                context, userUid, bill, EVENT_DUE_TODAY,
                reminderTime(dueTimestamp.toDate(), 0), forceReplace
            )
        }
        if (PaymentNotificationPreferences.overdueEnabled(context)) {
            enqueue(
                context, userUid, bill, EVENT_OVERDUE,
                reminderTime(dueTimestamp.toDate(), 1), forceReplace
            )
        }
        if (PaymentNotificationPreferences.overdueEnabled(context) &&
            PaymentNotificationPreferences.weeklyOverdueEnabled(context)
        ) {
            enqueue(
                context, userUid, bill, EVENT_WEEKLY_OVERDUE,
                reminderTime(dueTimestamp.toDate(), 7), forceReplace
            )
        }
    }

    fun cancelBill(context: Context, billId: String) {
        listOf(EVENT_ADVANCE, EVENT_DUE_TODAY, EVENT_OVERDUE, EVENT_WEEKLY_OVERDUE)
            .forEach { WorkManager.getInstance(context).cancelUniqueWork(workName(billId, it)) }
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .cancel(billId.hashCode())
    }

    private fun enqueue(
        context: Context,
        userUid: String,
        bill: Bills,
        event: String,
        scheduledAt: Date,
        forceReplace: Boolean
    ) {
        val now = System.currentTimeMillis()
        val dueMillis = bill.date?.toDate()?.time ?: return
        val targetMillis = when {
            scheduledAt.time > now -> scheduledAt.time
            event == EVENT_OVERDUE || event == EVENT_WEEKLY_OVERDUE -> now + 2_000L
            else -> return
        }
        val input = workDataOf(
            "event" to event,
            "billId" to bill.billId,
            "userUid" to userUid,
            "dueDateMillis" to dueMillis
        )
        if (event == EVENT_WEEKLY_OVERDUE) {
            val request = PeriodicWorkRequestBuilder<NotificationWorker>(7, TimeUnit.DAYS)
                .setInitialDelay(targetMillis - now, TimeUnit.MILLISECONDS)
                .setInputData(input)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                workName(bill.billId, event),
                if (forceReplace) ExistingPeriodicWorkPolicy.REPLACE else ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            return
        }
        val request = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(targetMillis - now, TimeUnit.MILLISECONDS)
            .setInputData(input)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(bill.billId, event),
            if (forceReplace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            request
        )
    }

    private fun reminderTime(dueDate: Date, dayOffset: Int): Date =
        Calendar.getInstance().apply {
            time = dueDate
            add(Calendar.DAY_OF_YEAR, dayOffset)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

    private fun workName(billId: String, event: String) = "payment_notification_${billId}_$event"
}
