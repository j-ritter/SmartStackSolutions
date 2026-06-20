package com.ritter.smartstackbills

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class NotificationWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    private val db = FirebaseFirestore.getInstance()

    override fun doWork(): Result {
        if (!PaymentNotificationPreferences.isEnabled(applicationContext)) return Result.success()
        val event = inputData.getString("event") ?: return Result.success()
        val billId = inputData.getString("billId") ?: return Result.success()
        val userUid = inputData.getString("userUid") ?: return Result.success()

        val document = try {
            Tasks.await(
                db.collection("users").document(userUid).collection("bills")
                    .document(billId).get()
            )
        } catch (_: Exception) {
            return Result.retry()
        }
        if (!document.exists()) return Result.success()

        val bill = document.toObject(Bills::class.java) ?: return Result.success()
        if (bill.billId.isNullOrBlank()) bill.billId = document.id
        if (bill.paid || bill.date == null || !eventEnabled(event)) return Result.success()

        val dueDate = bill.date.toDate()
        val daysOverdue = daysBetween(dueDate, Date()).coerceAtLeast(0)
        if ((event == PaymentNotificationScheduler.EVENT_OVERDUE ||
                event == PaymentNotificationScheduler.EVENT_WEEKLY_OVERDUE) && daysOverdue < 1
        ) return Result.success()

        val notificationId = "${billId}_${event}_${bill.date.toDate().time}"
        if (event != PaymentNotificationScheduler.EVENT_WEEKLY_OVERDUE) {
            val alreadyCreated = try {
                Tasks.await(
                    db.collection("users").document(userUid).collection("notifications")
                        .document(notificationId).get()
                ).exists()
            } catch (_: Exception) {
                return Result.retry()
            }
            if (alreadyCreated) return Result.success()
        }

        val content = contentFor(event, bill, daysOverdue)
        val notification = hashMapOf(
            "notificationId" to notificationId,
            "billId" to billId,
            "type" to event,
            "title" to content.first,
            "message" to content.second,
            "paymentName" to bill.name,
            "amount" to bill.amount,
            "date" to bill.date,
            "createdAt" to Timestamp.now(),
            "isUnread" to true
        )

        return try {
            Tasks.await(
                db.collection("users").document(userUid).collection("notifications")
                    .document(notificationId).set(notification)
            )
            sendNotification(userUid, bill, content.first, content.second)
            NotificationsActivity.incrementUnreadNotificationCount(applicationContext)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private fun eventEnabled(event: String) = when (event) {
        PaymentNotificationScheduler.EVENT_ADVANCE ->
            PaymentNotificationPreferences.advanceDays(applicationContext) > 0
        PaymentNotificationScheduler.EVENT_DUE_TODAY ->
            PaymentNotificationPreferences.dueTodayEnabled(applicationContext)
        PaymentNotificationScheduler.EVENT_OVERDUE ->
            PaymentNotificationPreferences.overdueEnabled(applicationContext)
        PaymentNotificationScheduler.EVENT_WEEKLY_OVERDUE ->
            PaymentNotificationPreferences.overdueEnabled(applicationContext) &&
                PaymentNotificationPreferences.weeklyOverdueEnabled(applicationContext)
        else -> false
    }

    private fun contentFor(event: String, bill: Bills, daysOverdue: Long): Pair<String, String> {
        val amount = CurrencyPreferences.format(applicationContext, bill.amount)
        val dueDate = SimpleDateFormat("MMM d", Locale.getDefault()).format(bill.date.toDate())
        return when (event) {
            PaymentNotificationScheduler.EVENT_ADVANCE ->
                applicationContext.getString(R.string.notification_due_soon_title) to
                    applicationContext.getString(
                        R.string.notification_due_soon_text,
                        bill.name,
                        amount,
                        dueDate
                    )
            PaymentNotificationScheduler.EVENT_DUE_TODAY ->
                applicationContext.getString(R.string.notification_due_today_title) to
                    applicationContext.getString(
                        R.string.notification_due_today_text,
                        bill.name,
                        amount
                    )
            else ->
                applicationContext.getString(R.string.notification_overdue_title) to
                    applicationContext.resources.getQuantityString(
                        R.plurals.notification_overdue_text,
                        daysOverdue.toInt(),
                        bill.name,
                        amount,
                        daysOverdue.toInt()
                    )
        }
    }

    private fun sendNotification(userUid: String, bill: Bills, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val manager = NotificationManagerCompat.from(applicationContext)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    applicationContext.getString(R.string.payment_reminders),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = applicationContext.getString(R.string.payment_reminders_channel_description)
                }
            )
        }

        val viewIntent = Intent(applicationContext, MyBills::class.java).apply {
            putExtra("BILL_ID", bill.billId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val viewPendingIntent = PendingIntent.getActivity(
            applicationContext,
            bill.billId.hashCode(),
            viewIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val paidIntent = Intent(applicationContext, PaymentNotificationActionReceiver::class.java).apply {
            action = PaymentNotificationActionReceiver.ACTION_MARK_PAID
            putExtra(PaymentNotificationActionReceiver.EXTRA_USER_UID, userUid)
            putExtra(PaymentNotificationActionReceiver.EXTRA_BILL_ID, bill.billId)
        }
        val paidPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            bill.billId.hashCode(),
            paidIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        manager.notify(
            bill.billId.hashCode(),
            NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications_bell)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(viewPendingIntent)
                .addAction(0, applicationContext.getString(R.string.mark_as_paid), paidPendingIntent)
                .addAction(0, applicationContext.getString(R.string.view_payment), viewPendingIntent)
                .build()
        )
    }

    private fun daysBetween(from: Date, to: Date): Long {
        val dayMillis = TimeUnit.DAYS.toMillis(1)
        return (startOfDay(to).time - startOfDay(from).time) / dayMillis
    }

    private fun startOfDay(date: Date): Date =
        java.util.Calendar.getInstance().apply {
            time = date
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.time

    companion object {
        private const val CHANNEL_ID = "payment_reminders"
    }
}
