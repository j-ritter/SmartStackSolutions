package com.example.smartstackbills

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*
import java.util.concurrent.TimeUnit

class NotificationsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificationsAdapter
    private val notificationsList = ArrayList<Notifications>()
    private val db = FirebaseFirestore.getInstance()
    private val userUid = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        recyclerView = findViewById(R.id.recyclerViewNotifications)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Load notifications from Firebase and remove old ones
        loadAndCleanNotifications()

        // Set up NotificationsAdapter with click listeners for marking as read and deleting
        adapter = NotificationsAdapter(
            this,
            notificationsList,
            object : NotificationsAdapter.OnNotificationClickListener {
                override fun onNotificationClick(notificationId: String) {
                    // Mark the notification as read when clicked
                    if (userUid != null) {
                        db.collection("users").document(userUid).collection("notifications")
                            .document(notificationId)
                            .update("isUnread", false) // Update 'isUnread' to false in Firebase
                            .addOnSuccessListener {
                                // Redirect to MyBills after marking as read
                                val intent = Intent(this@NotificationsActivity, MyBills::class.java)
                                intent.putExtra("NOTIFICATION_ID", notificationId) // Pass data if needed
                                startActivity(intent)
                            }
                            .addOnFailureListener {
                                Toast.makeText(this@NotificationsActivity, "Failed to update notification", Toast.LENGTH_SHORT).show()
                            }
                    }
                }

                override fun onDeleteNotificationClick(notificationId: String) {
                    // Delete the notification when the delete icon is clicked
                    deleteNotification(notificationId)
                }
            }
        )
        recyclerView.adapter = adapter

        // Reset unread notification count when the notifications are viewed
        resetUnreadNotificationCount(this)

        // Set up toolbar
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar_notifications)
        setSupportActionBar(toolbar)

        // Back button functionality
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    // Load notifications and remove those older than 30 days
    private fun loadAndCleanNotifications() {
        if (userUid != null) {
            db.collection("users").document(userUid).collection("notifications")
                .get()
                .addOnSuccessListener { documents ->
                    val currentTime = System.currentTimeMillis()
                    val thirtyDaysInMillis = TimeUnit.DAYS.toMillis(30)
                    val cutoffDate = Date(currentTime - thirtyDaysInMillis)

                    for (document in documents) {
                        val notification = document.toObject(Notifications::class.java)
                        if (notification.date != null && notification.date.toDate().after(cutoffDate)) {
                            notificationsList.add(notification)
                        } else {
                            // Delete notifications older than 30 days from Firebase
                            document.reference.delete()
                        }
                    }
                    adapter.notifyDataSetChanged()
                }
                .addOnFailureListener { e ->
                    // Handle errors if necessary
                }
        }
    }

    private fun deleteNotification(notificationId: String) {
        if (userUid != null) {
            db.collection("users").document(userUid).collection("notifications")
                .whereEqualTo("notificationId", notificationId)
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        document.reference.delete()
                            .addOnSuccessListener {
                                Toast.makeText(this, "Notification deleted", Toast.LENGTH_SHORT).show()
                                notificationsList.removeIf { it.notificationId == notificationId }
                                adapter.notifyDataSetChanged()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Failed to delete notification", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to find notification to delete", Toast.LENGTH_SHORT).show()
                }
        }
    }

    companion object {
        fun incrementUnreadNotificationCount(context: Context) {
            val sharedPref = context.getSharedPreferences("notifications", Context.MODE_PRIVATE)
            val unreadCount = sharedPref.getInt("unreadCount", 0) + 1
            sharedPref.edit().putInt("unreadCount", unreadCount).apply()
        }

        fun resetUnreadNotificationCount(context: Context) {
            val sharedPref = context.getSharedPreferences("notifications", Context.MODE_PRIVATE)
            sharedPref.edit().putInt("unreadCount", 0).apply()
        }

        fun getUnreadNotificationCount(context: Context): Int {
            val sharedPref = context.getSharedPreferences("notifications", Context.MODE_PRIVATE)
            return sharedPref.getInt("unreadCount", 0)
        }

        fun updateNotificationAsRead(context: Context, notificationId: String) {
            val sharedPref = context.getSharedPreferences("notifications", Context.MODE_PRIVATE)
            val gson = Gson()
            val json = sharedPref.getString("notificationsList", null)
            if (json != null) {
                val type = object : TypeToken<ArrayList<Notifications>>() {}.type
                val currentList: ArrayList<Notifications> = gson.fromJson(json, type)

                // Update the notification as read
                currentList.find { it.notificationId == notificationId }?.apply {
                    this.isUnread = false
                }

                // Save the updated list back to SharedPreferences
                val updatedJson = gson.toJson(currentList)
                sharedPref.edit().putString("notificationsList", updatedJson).apply()
            }
        }
    }
}
