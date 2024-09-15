package com.example.smartstackbills

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class NotificationsActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificationAdapter
    private val notificationsList = ArrayList<NotificationItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        recyclerView = findViewById(R.id.recyclerViewNotifications)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Example: Populate notifications list (this will come from your data source)
        notificationsList.add(NotificationItem("Reminder 1", "100$", "01/01/2024"))
        notificationsList.add(NotificationItem("Reminder 2",  "200$","05/01/2024"))

        adapter = NotificationAdapter(notificationsList)
        recyclerView.adapter = adapter
    }
}

data class NotificationItem(val title: String, val date: String, val amount: String)

class NotificationAdapter(private val notifications: List<NotificationItem>) :
    RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.items_notifications, parent, false)
        return NotificationViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val currentNotification = notifications[position]
        holder.title.text = currentNotification.title
        holder.date.text = currentNotification.date
    }

    override fun getItemCount() = notifications.size

    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.tvTitleNotification)
        val amount: TextView = itemView.findViewById(R.id.tvAmountNotification)
        val date: TextView = itemView.findViewById(R.id.tvDateNotification)
    }
}