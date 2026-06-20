package com.ritter.smartstackbills

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date
import java.util.concurrent.TimeUnit

class NotificationsActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificationsAdapter
    private val notificationsList = ArrayList<Notifications>()
    private val db = FirebaseFirestore.getInstance()
    private val userUid get() = FirebaseAuth.getInstance().currentUser?.uid
    private var dataLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        recyclerView = findViewById(R.id.recyclerViewNotifications)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = createAdapter()
        recyclerView.adapter = adapter

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar_notifications)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_notification_settings) {
                showNotificationSettings()
                true
            } else false
        }

        resetUnreadNotificationCount(this)
        loadAndCleanNotifications()
    }

    override fun onResume() {
        super.onResume()
        if (dataLoaded) updateEmptyState()
    }

    private fun createAdapter() = NotificationsAdapter(
        this,
        notificationsList,
        object : NotificationsAdapter.OnNotificationClickListener {
            override fun onNotificationClick(notificationId: String, billId: String) {
                markReadAndOpen(notificationId, billId)
            }

            override fun onDeleteNotificationClick(notificationId: String) {
                deleteNotification(notificationId)
            }
        }
    )

    private fun loadAndCleanNotifications() {
        val uid = userUid ?: return
        db.collection("users").document(uid).collection("notifications")
            .get()
            .addOnSuccessListener { documents ->
                val cutoff = Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30))
                notificationsList.clear()
                documents.forEach { document ->
                    val notification = document.toObject(Notifications::class.java)
                    if (notification.createdAt?.toDate()?.after(cutoff) == true) {
                        if (notification.notificationId.isNullOrBlank()) {
                            notification.notificationId = document.id
                        }
                        notificationsList.add(notification)
                    } else {
                        document.reference.delete()
                    }
                }
                notificationsList.sortByDescending { it.createdAt?.toDate()?.time ?: 0L }
                adapter = createAdapter()
                recyclerView.adapter = adapter
                dataLoaded = true
                updateEmptyState()
            }
            .addOnFailureListener {
                Toast.makeText(this, R.string.notifications_load_failed, Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateEmptyState() {
        if (!dataLoaded) return
        val emptyConfig = notificationEmptyConfig()
        EmptyStateTutorial.bind(
            this,
            findViewById(R.id.emptyStateNotifications),
            emptyConfig,
            hasAnyEntries = false,
            hasFilteredEntries = notificationsList.isNotEmpty(),
            hasPremiumAccess = true,
            onAdd = ::openNotificationAction,
            onPremium = ::openNotificationAction
        )
        if (notificationsList.isEmpty()) {
            EmptyStateTutorial.showFirstTimeIfNeeded(
                this,
                emptyConfig,
                hasPremiumAccess = true,
                onAdd = ::openNotificationAction,
                onPremium = ::openNotificationAction
            )
        }
    }

    private fun notificationEmptyConfig(): EmptyStateConfig {
        if (!hasNotificationPermission()) {
            return EmptyStateConfig(
                preferenceKey = "notifications_tutorial_shown",
                imageRes = R.drawable.image_openpayments,
                titleRes = R.string.notifications_turned_off_title,
                messageRes = R.string.notifications_turned_off_message,
                addActionRes = R.string.enable_notifications
            )
        }
        if (!PaymentNotificationPreferences.isEnabled(this)) {
            return EmptyStateConfig(
                preferenceKey = "notifications_tutorial_shown",
                imageRes = R.drawable.image_openpayments,
                titleRes = R.string.payment_reminders_paused_title,
                messageRes = R.string.payment_reminders_paused_message,
                addActionRes = R.string.notification_settings
            )
        }
        return EmptyStateConfig(
            preferenceKey = "notifications_tutorial_shown",
            imageRes = R.drawable.image_openpayments,
            titleRes = R.string.empty_notifications_title,
            messageRes = R.string.empty_notifications_message,
            addActionRes = R.string.notification_settings,
            compactTitleRes = R.string.notifications_all_caught_up_title,
            compactMessageRes = R.string.notifications_all_caught_up_message
        )
    }

    private fun openNotificationAction() {
        if (!hasNotificationPermission()) {
            NotificationPermissionHelper.requestIfNeeded(this)
        } else {
            showNotificationSettings()
        }
    }

    private fun hasNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun showNotificationSettings() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_notification_settings)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val enabled = dialog.findViewById<SwitchCompat>(R.id.switchPaymentNotifications)
        val advance = dialog.findViewById<Spinner>(R.id.spinnerAdvanceReminder)
        val dueToday = dialog.findViewById<SwitchCompat>(R.id.switchDueToday)
        val overdue = dialog.findViewById<SwitchCompat>(R.id.switchOverdue)
        val weekly = dialog.findViewById<SwitchCompat>(R.id.switchWeeklyOverdue)
        val values = intArrayOf(0, 1, 3, 7)
        advance.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf(
                getString(R.string.reminder_off),
                getString(R.string.reminder_one_day),
                getString(R.string.reminder_three_days),
                getString(R.string.reminder_seven_days)
            )
        )

        enabled.isChecked = PaymentNotificationPreferences.isEnabled(this)
        advance.setSelection(values.indexOf(PaymentNotificationPreferences.advanceDays(this)).coerceAtLeast(0))
        dueToday.isChecked = PaymentNotificationPreferences.dueTodayEnabled(this)
        overdue.isChecked = PaymentNotificationPreferences.overdueEnabled(this)
        weekly.isChecked = PaymentNotificationPreferences.weeklyOverdueEnabled(this)

        fun updateEnabledState() {
            val active = enabled.isChecked
            advance.isEnabled = active
            dueToday.isEnabled = active
            overdue.isEnabled = active
            weekly.isEnabled = active && overdue.isChecked
        }
        enabled.setOnCheckedChangeListener { _, _ -> updateEnabledState() }
        overdue.setOnCheckedChangeListener { _, _ -> updateEnabledState() }
        updateEnabledState()

        dialog.findViewById<Button>(R.id.btnCancelNotificationSettings).setOnClickListener {
            dialog.dismiss()
        }
        dialog.findViewById<Button>(R.id.btnSaveNotificationSettings).setOnClickListener {
            PaymentNotificationPreferences.save(
                this,
                enabled.isChecked,
                values[advance.selectedItemPosition],
                dueToday.isChecked,
                overdue.isChecked,
                overdue.isChecked && weekly.isChecked
            )
            userUid?.let { uid ->
                PaymentNotificationScheduler.rescheduleAll(this, uid) {
                    runOnUiThread {
                        Toast.makeText(this, R.string.notification_settings_saved, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            if (enabled.isChecked) NotificationPermissionHelper.requestIfNeeded(this)
            dialog.dismiss()
            updateEmptyState()
        }

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun markReadAndOpen(notificationId: String, billId: String) {
        val uid = userUid ?: return
        db.collection("users").document(uid).collection("notifications")
            .document(notificationId)
            .update("isUnread", false)
            .addOnCompleteListener {
                startActivity(Intent(this, MyBills::class.java).apply {
                    putExtra("BILL_ID", billId)
                    putExtra(AuthUtils.EXTRA_USER_EMAIL, AuthUtils.currentUserEmail())
                })
            }
    }

    private fun deleteNotification(notificationId: String) {
        val uid = userUid ?: return
        db.collection("users").document(uid).collection("notifications")
            .document(notificationId).delete()
            .addOnSuccessListener {
                notificationsList.removeAll { it.notificationId == notificationId }
                adapter = createAdapter()
                recyclerView.adapter = adapter
                updateEmptyState()
            }
    }

    companion object {
        fun incrementUnreadNotificationCount(context: Context) {
            val prefs = context.getSharedPreferences("notifications", Context.MODE_PRIVATE)
            prefs.edit().putInt("unreadCount", prefs.getInt("unreadCount", 0) + 1).apply()
        }

        fun resetUnreadNotificationCount(context: Context) {
            context.getSharedPreferences("notifications", Context.MODE_PRIVATE)
                .edit().putInt("unreadCount", 0).apply()
        }

        fun getUnreadNotificationCount(context: Context): Int =
            context.getSharedPreferences("notifications", Context.MODE_PRIVATE)
                .getInt("unreadCount", 0)
    }
}
