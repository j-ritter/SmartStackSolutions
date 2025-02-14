package com.ritter.smartstackbills

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import java.util.concurrent.TimeUnit

class NotificationWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    private val db = FirebaseFirestore.getInstance()

    override fun doWork(): Result {
        // Retrieve data from input
        val title = inputData.getString("title")
        val amount = inputData.getDouble("amount", 0.0)
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
                    val formattedAmount = String.format(Locale.getDefault(), "%.2f", amount)
                    // Send notification after saving to Firebase
                    sendNotification(title, formattedAmount, billId)
                    NotificationsActivity.incrementUnreadNotificationCount(applicationContext)
                }
                .addOnFailureListener {
                    it.printStackTrace() // Log error if Firebase save fails
                }
        }

        return Result.success()
    }

    // Schedule a notification
    fun scheduleNotification(context: Context, title: String, amount: Double, billId: String, dueDate: Timestamp) {
        val delay = calculateDelay(dueDate.toDate())
        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInputData(
                workDataOf(
                    "title" to title,
                    "amount" to amount,
                    "billId" to billId,
                    "dueDateMillis" to dueDate.toDate().time
                )
            )
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
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
        private fun calculateDelay(notificationDate: Date): Long {
            val currentTimeMillis = System.currentTimeMillis()
            return notificationDate.time - currentTimeMillis
        }
    }
}
