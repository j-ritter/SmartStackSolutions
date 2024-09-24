package com.example.smartstackbills

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class NotificationsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificationAdapter
    private val notificationsList = ArrayList<NotificationItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        recyclerView = findViewById(R.id.recyclerViewNotifications)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Load notifications from local storage (SharedPreferences)
        loadNotifications()

        // Set up the adapter with the notifications list
        adapter = NotificationAdapter(notificationsList)
        recyclerView.adapter = adapter

        // Reset unread notification count when the notifications are viewed
        resetUnreadNotificationCount(this)
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar_notifications)
        setSupportActionBar(toolbar)

        // Back button functionality
        toolbar.setNavigationOnClickListener {
            onBackPressed() // Handle back navigation
        }
    }

    // Updated: Load Notifications using Gson for better serialization and deserialization
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

            // Add the new notification to the list
            currentList.add(notification)

            // Save the updated list back to SharedPreferences
            val json = gson.toJson(currentList)
            editor.putString("notificationsList", json)
            editor.apply()

            // Increment unread notification count
            incrementUnreadNotificationCount(context)
        }

        // Utility function to load current notifications as a list
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
        // Increment unread notification count
        private fun incrementUnreadNotificationCount(context: Context) {
            val sharedPref = context.getSharedPreferences("notifications", Context.MODE_PRIVATE)
            val unreadCount = sharedPref.getInt("unreadCount", 0) + 1
            sharedPref.edit().putInt("unreadCount", unreadCount).apply()
        }

        // Reset unread notification count
        fun resetUnreadNotificationCount(context: Context) {
            val sharedPref = context.getSharedPreferences("notifications", Context.MODE_PRIVATE)
            sharedPref.edit().putInt("unreadCount", 0).apply()
        }

        // Get unread notification count
        fun getUnreadNotificationCount(context: Context): Int {
            val sharedPref = context.getSharedPreferences("notifications", Context.MODE_PRIVATE)
            return sharedPref.getInt("unreadCount", 0)
        }
    }
}

data class NotificationItem(val title: String, val date: String, val amount: String)

class NotificationAdapter(private val notifications: List<NotificationItem>) :
    RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.items_notifications, parent, false)
        return NotificationViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val currentNotification = notifications[position]
        holder.title.text = currentNotification.title
        holder.date.text = currentNotification.date
        holder.amount.text = currentNotification.amount
    }

    override fun getItemCount() = notifications.size

    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.tvTitleNotification)
        val amount: TextView = itemView.findViewById(R.id.tvAmountNotification)
        val date: TextView = itemView.findViewById(R.id.tvDateNotification)
    }
}
