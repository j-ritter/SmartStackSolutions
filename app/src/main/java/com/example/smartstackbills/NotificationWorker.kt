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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class NotificationWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    private val db = FirebaseFirestore.getInstance()

    override fun doWork(): Result {
        // Retrieve data from input
        val title = inputData.getString("title")
        val amount = inputData.getString("amount")
        val billId = inputData.getString("billId")
        val dueDateMillis = inputData.getLong("dueDateMillis", System.currentTimeMillis())
        val dueDate = Timestamp(Date(dueDateMillis))

        // Current timestamp for createdAt
        val createdAt = Timestamp.now()

        // Check if the notification is older than 30 days
        if (isNotificationOlderThan30Days(createdAt.toDate())) {
            deleteNotificationById(billId)
            return Result.success()
        }

        // Save the notification to Firebase and then send it
        val userUid = FirebaseAuth.getInstance().currentUser?.uid
        if (userUid != null && billId != null) {
            val notification = hashMapOf(
                "notificationId" to billId,
                "title" to title,
                "amount" to amount,
                "date" to dueDate, // Bill's due date
                "createdAt" to createdAt, // Timestamp when notification was created
                "isUnread" to true
            )

            db.collection("users").document(userUid).collection("notifications")
                .document(billId)
                .set(notification)
                .addOnSuccessListener {
                    // Send notification after saving to Firebase
                    sendNotification(title, amount, billId)
                    NotificationsActivity.incrementUnreadNotificationCount(applicationContext)
                }
                .addOnFailureListener {
                    it.printStackTrace() // Log error if Firebase save fails
                }
        }

        return Result.success()
    }

    // Check if a notification is older than 30 days
    private fun isNotificationOlderThan30Days(createdAt: Date): Boolean {
        val currentTime = System.currentTimeMillis()
        val thirtyDaysInMillis = TimeUnit.DAYS.toMillis(30)
        return (currentTime - createdAt.time) > thirtyDaysInMillis
    }

    // Delete a notification from Firebase by billId
    private fun deleteNotificationById(billId: String?) {
        val userUid = FirebaseAuth.getInstance().currentUser?.uid
        if (userUid != null && billId != null) {
            db.collection("users").document(userUid).collection("notifications")
                .whereEqualTo("notificationId", billId)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    for (document in querySnapshot) {
                        document.reference.delete()
                    }
                }
                .addOnFailureListener { exception ->
                    exception.printStackTrace()
                }
        }
    }

    // Send notification
    private fun sendNotification(title: String?, amount: String?, billId: String?) {
        val notificationManager = NotificationManagerCompat.from(applicationContext)

        // Check for notification permission if running on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        // Create notification channel for Android O and above
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
        // Calculate delay based on date
        fun calculateDelay(notificationDate: String): Long {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val notificationDateMillis = sdf.parse(notificationDate)?.time ?: 0
            val currentTimeMillis = System.currentTimeMillis()

            return if (notificationDateMillis > currentTimeMillis) {
                notificationDateMillis - currentTimeMillis - TimeUnit.DAYS.toMillis(1)
            } else {
                0
            }
        }

        // Schedule the notification using WorkManager
        fun scheduleNotification(context: Context, notificationItem: Notifications) {
            val delay = calculateDelay(notificationItem.date.toDate().toString())

            val notificationWork = androidx.work.OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInputData(
                    androidx.work.workDataOf(
                        "title" to notificationItem.title,
                        "amount" to notificationItem.amount,
                        "billId" to notificationItem.notificationId,
                        "createdAt" to notificationItem.date.toDate().time
                    )
                )
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()

            androidx.work.WorkManager.getInstance(context).enqueue(notificationWork)
        }
    }
}
