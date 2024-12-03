package com.example.smartstackbills

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CalendarView
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.LinkedHashMap
import java.util.Locale
import com.example.smartstackbills.NotificationsActivity


class CalendarActivity : AppCompatActivity(), MyAdapterCalendar.OnItemClickListener {

    private var userEmail: String? = null

    private lateinit var drawerLayoutCalendar: DrawerLayout
    private lateinit var calendarView: CalendarView
    private lateinit var recyclerViewCalendar: RecyclerView
    private lateinit var billsList: ArrayList<Bills>
    private lateinit var spendingList: ArrayList<Spendings>
    private lateinit var incomeList: ArrayList<Income>
    private lateinit var calendarEntries: ArrayList<Any>
    private lateinit var myAdapterCalendar: MyAdapterCalendar
    private lateinit var dialogBills: Dialog
    private lateinit var dialogSpendings: Dialog
    private lateinit var dialogIncome: Dialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_calendar)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_Calendar)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        userEmail = intent.getStringExtra("USER_EMAIL")

        drawerLayoutCalendar = findViewById(R.id.drawer_layout_calendar)
        calendarView = findViewById(R.id.calendarView)
        recyclerViewCalendar = findViewById(R.id.recyclerViewCalendarEntries)

        // Initialize RecyclerView for Calendar Entries
        recyclerViewCalendar.layoutManager = LinearLayoutManager(this)
        calendarEntries = ArrayList()
        myAdapterCalendar = MyAdapterCalendar(this, calendarEntries)
        myAdapterCalendar.setOnItemClickListener(this)
        recyclerViewCalendar.adapter = myAdapterCalendar

        // Load bills, spendings  and income data
        billsList = loadBills()
        spendingList = loadSpendings()
        incomeList = loadIncome()

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = "$dayOfMonth/${month + 1}/$year"
            Toast.makeText(this, "Date selected: $selectedDate", Toast.LENGTH_SHORT).show()

            // Combine all entries into a single list
            val allEntries = ArrayList<Any>()
            allEntries.addAll(billsList)
            allEntries.addAll(spendingList)
            allEntries.addAll(incomeList)

            // Filter combined entries based on the selected date
            val sdf = SimpleDateFormat("d/M/yyyy", Locale.getDefault())
            calendarEntries.clear()
            calendarEntries.addAll(allEntries.filter { entry ->
                when (entry) {
                    is Bills -> entry.date != null && sdf.format(entry.date.toDate()) == selectedDate
                    is Spendings -> entry.date != null && sdf.format(entry.date.toDate()) == selectedDate
                    is Income -> entry.date != null && sdf.format(entry.date.toDate()) == selectedDate
                    else -> false
                }
            })
            // Group entries by date
            val groupedEntries = groupEntriesByDate(calendarEntries)

            // Update RecyclerView with combined entries
            myAdapterCalendar.updateEntries(groupedEntries)
        }

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottomNavigationViewCalendar)
        bottomNavigationView.selectedItemId = R.id.Calendar
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.Main -> {
                    val intent = Intent(this, MainMenu::class.java)
                    intent.putExtra("USER_EMAIL", userEmail)
                    startActivity(intent)
                    true
                }
                R.id.Bills -> {
                    val intent = Intent(this, MyBills::class.java)
                    intent.putExtra("USER_EMAIL", userEmail)
                    startActivity(intent)
                    true
                }
                R.id.Spendings -> {
                    val intent = Intent(this, MySpendings::class.java)
                    intent.putExtra("USER_EMAIL", userEmail)
                    startActivity(intent)
                    true
                }
                R.id.Income -> {
                    val intent = Intent(this, MyIncome::class.java)
                    intent.putExtra("USER_EMAIL", userEmail)
                    startActivity(intent)
                    true
                }
                R.id.Calendar -> {
                    val intent = Intent(this, CalendarActivity::class.java)
                    intent.putExtra("USER_EMAIL", userEmail)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

        // Initialize the toolbar and set it as the action bar
        val toolbar: Toolbar = findViewById(R.id.toolbar_main_Calendar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            drawerLayoutCalendar.openDrawer(GravityCompat.START)
        }
        // Setup NavigationView
        val navView: NavigationView = findViewById(R.id.nav_viewCalendar)
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_item_premium -> {
                    val intent = Intent(this, Premium::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_item_aboutus -> {
                    val intent = Intent(this, AboutUs::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_item_faq -> {
                    val intent = Intent(this, FAQs::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_item_datasec -> {
                    val intent = Intent(this, Datasecurity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_item_help -> {
                    val intent = Intent(this, Help::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_item_terms -> {
                    val intent = Intent(this, Terms::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_item_logout -> {
                    logoutUser()
                    true
                }
                else -> false
            }
        }

        setupDialogBills()
        setupDialogSpendings()
        setupDialogIncome()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_nav, menu)
        // Find and update the unread notification count TextView on the alarm icon badge
        val menuItem = menu?.findItem(R.id.alarm)
        val actionView = menuItem?.actionView
        // Find the badge TextView
        val badgeCountTextView = actionView?.findViewById<TextView>(R.id.badge_count)

        // Update badge count with the current unread notification count
        updateUnreadCountBadge(badgeCountTextView)

        // Set click listener on the alarm icon
        actionView?.setOnClickListener {
            resetUnreadNotificationCount(badgeCountTextView)
            val intent = Intent(this, NotificationsActivity::class.java)
            startActivity(intent)  // Open the notifications activity
        }

        return true
    }

    private fun loadBills(): ArrayList<Bills> {
        val sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPref.getString("billsList", null)
        val type = object : TypeToken<ArrayList<Bills>>() {}.type
        return gson.fromJson(json, type) ?: ArrayList()
    }
    private fun loadSpendings(): ArrayList<Spendings> {
        val sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPref.getString("spendingList", null)
        val type = object : TypeToken<ArrayList<Spendings>>() {}.type
        return gson.fromJson(json, type) ?: ArrayList()
    }
    private fun loadIncome(): ArrayList<Income> {
        val sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPref.getString("incomeList", null)
        val type = object : TypeToken<ArrayList<Income>>() {}.type
        return gson.fromJson(json, type) ?: ArrayList()
    }

    private fun setupDialogBills() {
        dialogBills = Dialog(this)
        dialogBills.setContentView(R.layout.dialog_box_bill_calendar)
        dialogBills.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialogBills.window?.setBackgroundDrawable(getDrawable(R.drawable.dialog_box_bills_bg))
        dialogBills.setCancelable(true)

        val btnCloseDialog = dialogBills.findViewById<Button>(R.id.btnCloseDialogBillCalendar)
        btnCloseDialog.setOnClickListener {
            dialogBills.dismiss()
        }
    }
    private fun setupDialogSpendings() {
        dialogSpendings = Dialog(this)
        dialogSpendings.setContentView(R.layout.dialog_box_spendings_calendar)
        dialogSpendings.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialogSpendings.window?.setBackgroundDrawable(getDrawable(R.drawable.dialog_box_spendings_bg))
        dialogSpendings.setCancelable(true)

        val btnCloseDialog = dialogSpendings.findViewById<Button>(R.id.btnCloseDialogSpendingsCalendar)
        btnCloseDialog.setOnClickListener {
            dialogSpendings.dismiss()
        }
    }

    private fun setupDialogIncome() {
        dialogIncome = Dialog(this)
        dialogIncome.setContentView(R.layout.dialog_box_income_calendar)
        dialogIncome.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialogIncome.window?.setBackgroundDrawable(getDrawable(R.drawable.dialog_box_income_bg))
        dialogIncome.setCancelable(true)

        val btnCloseDialog = dialogIncome.findViewById<Button>(R.id.btnCloseDialogIncomeCalendar)
        btnCloseDialog.setOnClickListener {
            dialogIncome.dismiss()
        }
    }

    private fun showBillDetailsDialog(bill: Bills) {
        val edtTitleDialog = dialogBills.findViewById<EditText>(R.id.edtTitleDialogBillCalendar)
        val edtAmountDialog = dialogBills.findViewById<EditText>(R.id.edtAmountDialogBillCalendar)
        val edtCategoryDialog = dialogBills.findViewById<EditText>(R.id.edtCategoryDialogBillCalendar)
        val edtSubcategoryDialog = dialogBills.findViewById<EditText>(R.id.edtSubcategoryDialogBillCalendar)
        val edtVendorDialog = dialogBills.findViewById<EditText>(R.id.edtVendorDialogBillCalendar)
        val edtDateDialog = dialogBills.findViewById<EditText>(R.id.edtDateDialogBillCalendar)
        val edtRepeatDialog = dialogBills.findViewById<EditText>(R.id.edtRepeatDialogBillCalendar)
        val edtCommentDialog = dialogBills.findViewById<EditText>(R.id.edtCommentDialogBillCalendar)
        val edtAttachmentDialog = dialogBills.findViewById<ImageView>(R.id.edtAttachmentDialogBillCalendar)

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val billDateString = bill.date?.let { dateFormat.format(it.toDate()) } ?: ""

        edtTitleDialog?.setText(bill.name)
        edtAmountDialog.setText(String.format(Locale.getDefault(), "%.2f", bill.amount))
        edtCategoryDialog?.setText(bill.category)
        edtSubcategoryDialog?.setText(bill.subcategory)
        edtVendorDialog?.setText(bill.vendor)
        edtDateDialog?.setText(billDateString)
        edtRepeatDialog?.setText(bill.repeat)
        edtCommentDialog?.setText(bill.comment)

        if (bill.attachment != null) {
            edtAttachmentDialog?.setImageURI(Uri.parse(bill.attachment))
            edtAttachmentDialog?.visibility = View.VISIBLE
        } else {
            edtAttachmentDialog?.visibility = View.GONE
        }

        dialogBills.show()
    }
    private fun showSpendingsDetailsDialog(spending: Spendings) {
        val edtTitleDialog = dialogSpendings.findViewById<EditText>(R.id.edtTitleDialogSpendingsCalendar)
        val edtAmountDialog = dialogSpendings.findViewById<EditText>(R.id.edtAmountDialogSpendingsCalendar)
        val edtCategoryDialog = dialogSpendings.findViewById<EditText>(R.id.edtCategoryDialogSpendingsCalendar)
        val edtSubcategoryDialog = dialogSpendings.findViewById<EditText>(R.id.edtSubcategoryDialogSpendingsCalendar)
        val edtVendorDialog = dialogSpendings.findViewById<EditText>(R.id.edtVendorDialogSpendingsCalendar)
        val edtDateDialog = dialogSpendings.findViewById<EditText>(R.id.edtDateDialogSpendingsCalendar)

        val edtCommentDialog = dialogSpendings.findViewById<EditText>(R.id.edtCommentDialogSpendingsCalendar)

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val spendingDateString = spending.date?.let { dateFormat.format(it.toDate()) } ?: ""

        edtTitleDialog?.setText(spending.name)
        edtAmountDialog.setText(String.format(Locale.getDefault(), "%.2f", spending.amount))

        edtCategoryDialog?.setText(spending.category)
        edtSubcategoryDialog?.setText(spending.subcategory)
        edtVendorDialog?.setText(spending.vendor)
        edtDateDialog?.setText(spendingDateString)

        edtCommentDialog?.setText(spending.comment)

        dialogSpendings.show()
    }

    private fun showIncomeDetailsDialog(income: Income) {
        val edtTitleDialog = dialogIncome.findViewById<EditText>(R.id.edtTitleDialogIncomeCalendar)
        val edtAmountDialog = dialogIncome.findViewById<EditText>(R.id.edtAmountDialogIncomeCalendar)
        val edtCategoryDialog = dialogIncome.findViewById<EditText>(R.id.edtCategoryDialogIncomeCalendar)
        val edtSubcategoryDialog = dialogIncome.findViewById<EditText>(R.id.edtSubcategoryDialogIncomeCalendar)
        val edtDateDialog = dialogIncome.findViewById<EditText>(R.id.edtDateDialogIncomeCalendar)
        val edtRepeatDialog = dialogIncome.findViewById<EditText>(R.id.edtRepeatDialogIncomeCalendar)
        val edtCommentDialog = dialogIncome.findViewById<EditText>(R.id.edtCommentDialogIncomeCalendar)

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val incomeDateString = income.date?.let { dateFormat.format(it.toDate()) } ?: ""

        edtTitleDialog?.setText(income.name)
        edtAmountDialog.setText(String.format(Locale.getDefault(), "%.2f", income.amount))

        edtCategoryDialog?.setText(income.category)
        edtSubcategoryDialog?.setText(income.subcategory)
        edtDateDialog?.setText(incomeDateString)
        edtRepeatDialog?.setText(income.repeat)
        edtCommentDialog?.setText(income.comment)

        dialogIncome.show()
    }

    override fun onItemClick(position: Int) {
        val item = myAdapterCalendar.getItemAtPosition(position)
        when (item) {
            is Bills -> showBillDetailsDialog(item)
            is Spendings -> showSpendingsDetailsDialog(item)
            is Income -> showIncomeDetailsDialog(item)
            else -> Toast.makeText(this, "Unknown item clicked", Toast.LENGTH_SHORT).show()
        }
    }

    private fun groupEntriesByDate(entries: ArrayList<Any>): ArrayList<Any> {
        val groupedEntries = LinkedHashMap<String, MutableList<Any>>()
        val sdf = SimpleDateFormat("d/M/yyyy", Locale.getDefault())

        for (entry in entries) {
            val date = when (entry) {
                is Bills -> sdf.format(entry.date.toDate())
                is Spendings -> sdf.format(entry.date.toDate())
                is Income -> sdf.format(entry.date.toDate())
                else -> continue
            }
            if (!groupedEntries.containsKey(date)) {
                groupedEntries[date] = ArrayList()
            }
            groupedEntries[date]?.add(entry)
        }

        val items = ArrayList<Any>()
        for ((date, groupedEntriesList) in groupedEntries) {
            items.add(date)
            items.addAll(groupedEntriesList)
        }

        return items
    }
    // Update the unread count badge from SharedPreferences
    private fun updateUnreadCountBadge(badgeCountTextView: TextView?) {
        val unreadCount = NotificationsActivity.getUnreadNotificationCount(this)
        if (unreadCount > 0) {
            badgeCountTextView?.text = unreadCount.toString()
            badgeCountTextView?.visibility = View.VISIBLE // Show the badge
        } else {
            badgeCountTextView?.visibility = View.GONE // Hide the badge if no unread notifications
        }
    }

    // Reset unread notification count when notifications are viewed
    private fun resetUnreadNotificationCount(badgeCountTextView: TextView?) {
        NotificationsActivity.resetUnreadNotificationCount(this)
        updateUnreadCountBadge(badgeCountTextView) // Update the badge display immediately
    }

    private fun logoutUser() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, LogIn::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}