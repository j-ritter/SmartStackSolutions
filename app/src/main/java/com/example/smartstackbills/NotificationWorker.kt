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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class NotificationWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    private val db = FirebaseFirestore.getInstance()

    override fun doWork(): Result {
        val title = inputData.getString("title")
        val amount = inputData.getString("amount")
        val billId = inputData.getString("billId")
        val createdAtLong = inputData.getLong("createdAt", System.currentTimeMillis())
        val createdAt = Date(createdAtLong)

        // Check if the notification is older than 30 days
        if (isNotificationOlderThan30Days(createdAt)) {
            // Delete the notification from the database or storage
            deleteNotificationById(billId)
            return Result.success() // Return success after deletion
        }

        sendNotification(title, amount, billId)
        // Increment unread notification count when the notification is triggered
        NotificationsActivity.incrementUnreadNotificationCount(applicationContext)

        return Result.success()
    }

    // Method to check if a notification is older than 30 days
    private fun isNotificationOlderThan30Days(createdAt: Date): Boolean {
        val currentTime = System.currentTimeMillis()
        val thirtyDaysInMillis = TimeUnit.DAYS.toMillis(30)
        return (currentTime - createdAt.time) > thirtyDaysInMillis
    }

    // Method to delete a notification from Firebase by billId
    private fun deleteNotificationById(billId: String?) {
        if (billId != null) {
            db.collection("notifications")
                .whereEqualTo("billId", billId)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    for (document in querySnapshot) {
                        // Delete the specific notification
                        document.reference.delete()
                    }
                }
                .addOnFailureListener { exception ->
                    // Log or handle failure if needed
                }
        }
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
                    notificationDateMillis - currentTimeMillis - TimeUnit.DAYS.toMillis(3)
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
                        "billId" to notification.title,
                        "createdAt" to notification.createdAt.time
                    )
                )
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()

            androidx.work.WorkManager.getInstance(context).enqueue(notificationWork)
        }
    }
}
