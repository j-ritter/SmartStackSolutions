package com.example.smartstackbills

import android.content.Context
import android.os.Bundle
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

        // Set up NotificationsAdapter with a click listener to mark notifications as read
        adapter = NotificationsAdapter(this, notificationsList, object : NotificationsAdapter.OnNotificationClickListener {
            override fun onNotificationClick(notificationId: String) {
                // Mark the notification as read when clicked
                updateNotificationAsRead(this@NotificationsActivity, notificationId)
                adapter.notifyDataSetChanged()
            }
        })
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
                        if (notification.createdAt != null && notification.createdAt.toDate().after(cutoffDate)) {
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
