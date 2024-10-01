package com.example.smartstackbills

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.*

class NotificationsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificationsAdapter
    private val notificationsList = ArrayList<NotificationItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        recyclerView = findViewById(R.id.recyclerViewNotifications)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Load notifications from local storage (SharedPreferences)
        loadNotifications()

        // Pass the list and listener to the adapter
        adapter = NotificationsAdapter(
            this,
            notificationsList,
            object : NotificationsAdapter.OnNotificationClickListener {
                override fun onNotificationClick(position: Int) {
                    // Retrieve the clicked notification
                    val selectedNotification = notificationsList[position]

                    // Create an Intent to open MyBills activity
                    val intent = Intent(this@NotificationsActivity, MyBills::class.java)

                    // Pass the billId from the notification to MyBills
                    intent.putExtra("BILL_ID", selectedNotification.billId)

                    // Start the MyBills activity
                    startActivity(intent)
                }
                override fun onDeleteClick(position: Int) {
                    // Remove the notification from the list
                    val notificationToRemove = notificationsList[position]
                    notificationsList.removeAt(position)
                    adapter.notifyItemRemoved(position)

                    // Also remove it from the shared preferences
                    removeNotificationFromSharedPreferences(notificationToRemove)
                }
            }
        )
        recyclerView.adapter = adapter

        // Reset unread notification count when the notifications are viewed
        resetUnreadNotificationCount(this)
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar_notifications)
        setSupportActionBar(toolbar)

        // Back button functionality
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }
    private fun removeNotificationFromSharedPreferences(notification: NotificationItem) {
        val sharedPref = getSharedPreferences("notifications", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPref.getString("notificationsList", null)
        if (json != null) {
            val type: Type = object : TypeToken<ArrayList<NotificationItem>>() {}.type
            val currentList: ArrayList<NotificationItem> = gson.fromJson(json, type)

            // Remove the notification that matches
            currentList.removeIf { it.title == notification.title && it.date == notification.date }

            // Save the updated list
            val editor = sharedPref.edit()
            val updatedJson = gson.toJson(currentList)
            editor.putString("notificationsList", updatedJson)
            editor.apply()
        }
    }

    // Load Notifications using Gson
    private fun loadNotifications() {
        val sharedPref = getSharedPreferences("notifications", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPref.getString("notificationsList", null)
        if (json != null) {
            val type: Type = object : TypeToken<ArrayList<NotificationItem>>() {}.type
            val savedNotifications: ArrayList<NotificationItem> = gson.fromJson(json, type)
            notificationsList.addAll(savedNotifications)
        }
    }

    companion object {
        fun saveNotification(context: Context, notification: NotificationItem) {
            val sharedPref = context.getSharedPreferences("notifications", Context.MODE_PRIVATE)
            val editor = sharedPref.edit()
            val gson = Gson()

            // Load existing notifications
            val currentList = loadCurrentNotifications(context)

            // Add the new notification
            val notificationWithTimestamp = notification.copy(createdAt = Date())

            currentList.add(notificationWithTimestamp)

            // Save the updated list back
            val json = gson.toJson(currentList)
            editor.putString("notificationsList", json)
            editor.apply()

            incrementUnreadNotificationCount(context)
        }

        private fun loadCurrentNotifications(context: Context): ArrayList<NotificationItem> {
            val sharedPref = context.getSharedPreferences("notifications", Context.MODE_PRIVATE)
            val gson = Gson()
            val json = sharedPref.getString("notificationsList", null)
            return if (json != null) {
                val type: Type = object : TypeToken<ArrayList<NotificationItem>>() {}.type
                gson.fromJson(json, type)
            } else {
                ArrayList()
            }
        }

        private fun incrementUnreadNotificationCount(context: Context) {
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

        fun updateNotificationAsRead(context: Context, notification: NotificationItem) {
            val sharedPref = context.getSharedPreferences("notifications", Context.MODE_PRIVATE)
            val gson = Gson()

            // Load existing notifications
            val currentList = loadCurrentNotifications(context)

            // Mark the notification as read
            val updatedList = currentList.map {
                if (it.title == notification.title && it.date == notification.date && it.amount == notification.amount) {
                    it.copy(isUnread = false)
                } else {
                    it
                }
            }

            // Save the updated list back
            val json = gson.toJson(updatedList)
            sharedPref.edit().putString("notificationsList", json).apply()
        }
    }

    // Data class for NotificationItem with date as String
    data class NotificationItem(
        val title: String,
        val date: String,  // String instead of Timestamp
        val amount: String,
        val billId: String,
        var isUnread: Boolean = true,
        val createdAt: Date
    )
}
