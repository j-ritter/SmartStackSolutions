package com.example.smartstackbills

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class NotificationWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val title = inputData.getString("title")
        val amount = inputData.getString("amount")
        val billId = inputData.getString("billId")

        sendNotification(title, amount, billId)
        return Result.success()
    }

    private fun sendNotification(title: String?, amount: String?, billId: String?) {
        val notificationManager = NotificationManagerCompat.from(applicationContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "bill_notifications",
                "Bill Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, "bill_notifications")
            .setContentTitle(title ?: "Upcoming Payment Reminder")
            .setContentText("Your bill '$title' of $amount is due soon.")
            .setSmallIcon(R.drawable.ic_alarm)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(billId?.hashCode() ?: 0, notification)
    }

    companion object {
        // Calculate delay based on String date
        fun calculateDelay(notificationDateString: String): Long {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val notificationDate: Date? = try {
                sdf.parse(notificationDateString)
            } catch (e: Exception) {
                null
            }

            notificationDate?.let {
                val notificationDateMillis = it.time
                val currentTimeMillis = System.currentTimeMillis()

                return if (notificationDateMillis > currentTimeMillis) {
                    notificationDateMillis - currentTimeMillis - TimeUnit.DAYS.toMillis(1)
                } else {
                    0
                }
            } ?: run {
                return 0
            }
        }

        // Schedule the notification
        fun scheduleNotification(context: Context, notification: NotificationsActivity.NotificationItem) {
            val delay = calculateDelay(notification.date)

            val notificationWork = androidx.work.OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInputData(
                    androidx.work.workDataOf(
                        "title" to notification.title,
                        "amount" to notification.amount,
                        "billId" to notification.title
                    )
                )
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()

            androidx.work.WorkManager.getInstance(context).enqueue(notificationWork)
        }
    }
}
