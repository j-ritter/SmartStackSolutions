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
import java.util.Locale
import java.util.concurrent.TimeUnit

class NotificationWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        // Retrieve the data sent when the work was scheduled
        val title = inputData.getString("title")
        val amount = inputData.getString("amount")
        val billId = inputData.getString("billId")

        // Send the notification to the user
        sendNotification(title, amount, billId)
        return Result.success()
    }

    private fun sendNotification(title: String?, amount: String?, billId: String?) {
        // Get Notification Manager
        val notificationManager = NotificationManagerCompat.from(applicationContext)

        // Check if POST_NOTIFICATIONS permission is granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Log the error or handle it accordingly
            return
        }

        // Create Notification Channel (only for Android Oreo and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "bill_notifications",
                "Bill Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Build the notification
        val notification = NotificationCompat.Builder(applicationContext, "bill_notifications")
            .setContentTitle(title ?: "Upcoming Payment Reminder")
            .setContentText("Your bill '$title' of $amount is due soon.")
            .setSmallIcon(R.drawable.ic_alarm)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        // Show the notification
        notificationManager.notify(billId?.hashCode() ?: 0, notification)
    }

    companion object {
        // Utility function to calculate the delay (24 hours before the due date)
        fun calculateDelay(notificationDate: String): Long {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val notificationDateMillis = sdf.parse(notificationDate)?.time ?: 0
            val currentTimeMillis = System.currentTimeMillis()

            // Set the notification to trigger immediately (no delay)
            return if (notificationDateMillis > currentTimeMillis) {
                notificationDateMillis - currentTimeMillis - TimeUnit.DAYS.toMillis(1)
            } else {
                0 // No delay, trigger immediately
            }
        }

        // Function to schedule the notification using WorkManager
        fun scheduleNotification(context: Context, notification: NotificationItem) {
            val delay = calculateDelay(notification.date)

            val notificationWork = androidx.work.OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInputData(
                    androidx.work.workDataOf(
                        "title" to notification.title,
                        "amount" to notification.amount,
                        "billId" to notification.title  // Assuming title is unique
                    )
                )
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)  // Immediate if delay is 0
                .build()

            androidx.work.WorkManager.getInstance(context).enqueue(notificationWork)
        }
    }
}