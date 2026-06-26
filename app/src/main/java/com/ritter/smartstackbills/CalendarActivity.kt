package com.ritter.smartstackbills

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CalendarView
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


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
    private val db = FirebaseFirestore.getInstance()
    private val listenerRegistrations = mutableListOf<ListenerRegistration>()
    private var billsLoaded = false
    private var spendingsLoaded = false
    private var incomeLoaded = false
    private var selectedDate: String = SimpleDateFormat("d/M/yyyy", Locale.getDefault())
        .format(Calendar.getInstance().time)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_calendar)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_Calendar)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        userEmail = intent.getStringExtra(AuthUtils.EXTRA_USER_EMAIL) ?: AuthUtils.currentUserEmail()

        drawerLayoutCalendar = findViewById(R.id.drawer_layout_calendar)
        calendarView = findViewById(R.id.calendarView)
        recyclerViewCalendar = findViewById(R.id.recyclerViewCalendarEntries)

        // Initialize RecyclerView for Calendar Entries
        recyclerViewCalendar.layoutManager = LinearLayoutManager(this)
        calendarEntries = ArrayList()
        myAdapterCalendar = MyAdapterCalendar(this, calendarEntries)
        myAdapterCalendar.setOnItemClickListener(this)
        recyclerViewCalendar.adapter = myAdapterCalendar

        billsList = ArrayList()
        spendingList = ArrayList()
        incomeList = ArrayList()
        setupCalendarDataListeners()

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDate = "$dayOfMonth/${month + 1}/$year"
            updateSelectedDateEntries()
        }

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottomNavigationViewCalendar)
        bottomNavigationView.selectedItemId = R.id.Calendar
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.Main -> {
                    val intent = Intent(this, MainMenu::class.java)
                    intent.putExtra(AuthUtils.EXTRA_USER_EMAIL, userEmail)
                    startActivity(intent)
                    true
                }
                R.id.Bills -> {
                    val intent = Intent(this, MyBills::class.java)
                    intent.putExtra(AuthUtils.EXTRA_USER_EMAIL, userEmail)
                    startActivity(intent)
                    true
                }
                R.id.Spendings -> {
                    val intent = Intent(this, MySpendings::class.java)
                    intent.putExtra(AuthUtils.EXTRA_USER_EMAIL, userEmail)
                    startActivity(intent)
                    true
                }
                R.id.Income -> {
                    val intent = Intent(this, MyIncome::class.java)
                    intent.putExtra(AuthUtils.EXTRA_USER_EMAIL, userEmail)
                    startActivity(intent)
                    true
                }
                R.id.Calendar -> {
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
        DrawerNavigation.setup(this, drawerLayoutCalendar, findViewById(R.id.nav_viewCalendar))

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

    private fun setupCalendarDataListeners() {
        val userUid = FirebaseAuth.getInstance().currentUser?.uid
        if (userUid == null) {
            Toast.makeText(this, R.string.user_not_authenticated, Toast.LENGTH_SHORT).show()
            return
        }

        val userDocument = db.collection("users").document(userUid)
        listenerRegistrations.add(userDocument.collection("bills").addSnapshotListener { snapshots, error ->
            if (error != null) {
                billsLoaded = true
                updateSelectedDateEntries()
                Toast.makeText(this, getString(R.string.load_open_payments_failed, error.message.orEmpty()), Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }
            billsList.clear()
            snapshots?.documents?.mapNotNullTo(billsList) { it.toObject(Bills::class.java) }
            billsLoaded = true
            updateSelectedDateEntries()
        })

        listenerRegistrations.add(userDocument.collection("spendings").addSnapshotListener { snapshots, error ->
            if (error != null) {
                spendingsLoaded = true
                updateSelectedDateEntries()
                Toast.makeText(this, getString(R.string.load_closed_payments_failed, error.message.orEmpty()), Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }
            spendingList.clear()
            snapshots?.documents?.mapNotNullTo(spendingList) { it.toObject(Spendings::class.java) }
            spendingsLoaded = true
            updateSelectedDateEntries()
        })

        listenerRegistrations.add(userDocument.collection("income").addSnapshotListener { snapshots, error ->
            if (error != null) {
                incomeLoaded = true
                updateSelectedDateEntries()
                Toast.makeText(this, getString(R.string.load_income_failed, error.message.orEmpty()), Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }
            incomeList.clear()
            snapshots?.documents?.mapNotNullTo(incomeList) { it.toObject(Income::class.java) }
            incomeLoaded = true
            updateSelectedDateEntries()
        })
    }

    private fun updateSelectedDateEntries() {
        val loaded = billsLoaded && spendingsLoaded && incomeLoaded
        findViewById<ProgressBar>(R.id.progressCalendar).visibility =
            if (loaded) View.GONE else View.VISIBLE
        if (!loaded) {
            findViewById<LinearLayout>(R.id.calendarEmptyState).visibility = View.GONE
            return
        }
        val allEntries = ArrayList<Any>()
        allEntries.addAll(billsList)
        allEntries.addAll(spendingList)
        allEntries.addAll(incomeList)

        val sdf = SimpleDateFormat("d/M/yyyy", Locale.getDefault())
        calendarEntries.clear()
        calendarEntries.addAll(allEntries.filter { entry ->
            when (entry) {
                is Bills -> entry.date?.let { sdf.format(it.toDate()) == selectedDate } ?: false
                is Spendings -> entry.date?.let { sdf.format(it.toDate()) == selectedDate } ?: false
                is Income -> entry.date?.let { sdf.format(it.toDate()) == selectedDate } ?: false
                else -> false
            }
        })
        myAdapterCalendar.updateEntries(calendarEntries)
        findViewById<LinearLayout>(R.id.calendarEmptyState).visibility =
            if (calendarEntries.isEmpty()) View.VISIBLE else View.GONE
        recyclerViewCalendar.visibility = if (calendarEntries.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun setupDialogBills() {
        dialogBills = Dialog(this)
        dialogBills.setContentView(R.layout.dialog_box_bill)
        dialogBills.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialogBills.window?.setBackgroundDrawable(getDrawable(R.drawable.dialog_box_bills_bg))
        dialogBills.setCancelable(true)

        // Find and disable the paid checkbox
        dialogBills.findViewById<ImageView>(R.id.imgEditBill).visibility = View.INVISIBLE
        dialogBills.findViewById<ImageView>(R.id.imgDeleteBill).visibility = View.INVISIBLE
        dialogBills.findViewById<Button>(R.id.btnSaveChanges).visibility = View.GONE
        val btnCloseDialog = dialogBills.findViewById<Button>(R.id.btnCloseDialog)
        btnCloseDialog.setOnClickListener {
            dialogBills.dismiss()
        }
    }
    private fun setupDialogSpendings() {
        dialogSpendings = Dialog(this)
        dialogSpendings.setContentView(R.layout.dialog_box_spendings)
        dialogSpendings.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialogSpendings.window?.setBackgroundDrawable(getDrawable(R.drawable.dialog_box_spendings_bg))
        dialogSpendings.setCancelable(true)

        // Find and disable the paid checkbox
        dialogSpendings.findViewById<ImageView>(R.id.imgEditSpendings).visibility = View.INVISIBLE
        dialogSpendings.findViewById<ImageView>(R.id.imgDeleteSpendings).visibility = View.INVISIBLE
        dialogSpendings.findViewById<Button>(R.id.btnSaveChangesSpendings).visibility = View.GONE
        val btnCloseDialog = dialogSpendings.findViewById<Button>(R.id.btnCloseDialogSpendings)
        btnCloseDialog.setOnClickListener {
            dialogSpendings.dismiss()
        }
    }

    private fun setupDialogIncome() {
        dialogIncome = Dialog(this)
        dialogIncome.setContentView(R.layout.dialog_box_income)
        dialogIncome.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialogIncome.window?.setBackgroundDrawable(getDrawable(R.drawable.dialog_box_income_bg))
        dialogIncome.setCancelable(true)

        dialogIncome.findViewById<ImageView>(R.id.imgEditIncome).visibility = View.INVISIBLE
        dialogIncome.findViewById<ImageView>(R.id.imgDeleteIncome).visibility = View.INVISIBLE
        dialogIncome.findViewById<Button>(R.id.btnSaveChangesIncome).visibility = View.GONE
        val btnCloseDialog = dialogIncome.findViewById<Button>(R.id.btnCloseDialogIncome)
        btnCloseDialog.setOnClickListener {
            dialogIncome.dismiss()
        }
    }

    private fun showBillDetailsDialog(bill: Bills) {
        val edtTitleDialog = dialogBills.findViewById<EditText>(R.id.edtTitleDialog)
        val edtAmountDialog = dialogBills.findViewById<EditText>(R.id.edtAmountDialog)
        val edtCategoryDialog = dialogBills.findViewById<EditText>(R.id.edtCategoryDialog)
        val edtSubcategoryDialog = dialogBills.findViewById<EditText>(R.id.edtSubcategoryDialog)
        val edtVendorDialog = dialogBills.findViewById<EditText>(R.id.edtVendorDialog)
        val edtDateDialog = dialogBills.findViewById<EditText>(R.id.edtDateDialog)
        val edtRepeatDialog = dialogBills.findViewById<EditText>(R.id.edtRepeatDialog)
        val edtCommentDialog = dialogBills.findViewById<EditText>(R.id.edtCommentDialog)
        val edtAttachmentDialog = dialogBills.findViewById<ImageView>(R.id.edtAttachmentDialog)

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val billDateString = bill.date?.let { dateFormat.format(it.toDate()) } ?: ""

        edtTitleDialog.setText(bill.name.orEmpty())
        edtAmountDialog.setText(CurrencyPreferences.formatPlain(bill.amount))
        edtAmountDialog.setTextColor(
            ContextCompat.getColor(
                this,
                if (bill.date != null && AppDateUtils.isBeforeToday(bill.date.toDate()) && !bill.paid) {
                    R.color.red
                } else {
                    R.color.bill_color
                }
            )
        )
        edtCategoryDialog.setText(FinancialEntryOptions.displayCategory(this, bill.category))
        edtSubcategoryDialog.setText(
            FinancialEntryOptions.displaySubcategory(this, bill.category, bill.subcategory)
        )
        edtVendorDialog.setText(bill.vendor.orEmpty())
        edtDateDialog.setText(billDateString)
        edtRepeatDialog.setText(bill.repeat.orEmpty())
        edtCommentDialog.setText(bill.comment.orEmpty())

        if (bill.attachment != null) {
            edtAttachmentDialog.setImageURI(Uri.parse(bill.attachment))
            edtAttachmentDialog.visibility = View.VISIBLE
        } else {
            edtAttachmentDialog.visibility = View.GONE
        }

        dialogBills.show()
        styleDetailsDialogWindow(dialogBills)
    }
    private fun showSpendingsDetailsDialog(spending: Spendings) {
        val edtTitleDialog = dialogSpendings.findViewById<EditText>(R.id.edtTitleDialogSpendings)
        val edtAmountDialog = dialogSpendings.findViewById<EditText>(R.id.edtAmountDialogSpendings)
        val edtCategoryDialog = dialogSpendings.findViewById<EditText>(R.id.edtCategoryDialogSpendings)
        val edtSubcategoryDialog = dialogSpendings.findViewById<EditText>(R.id.edtSubcategoryDialogSpendings)
        val edtVendorDialog = dialogSpendings.findViewById<EditText>(R.id.edtVendorDialogSpendings)
        val edtDateDialog = dialogSpendings.findViewById<EditText>(R.id.edtDateDialogSpendings)
        val edtCommentDialog = dialogSpendings.findViewById<EditText>(R.id.edtCommentDialogSpendings)
        val attachment = dialogSpendings.findViewById<ImageView>(R.id.edtAttachmentDialogSpendings)

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val spendingDateString = spending.date?.let { dateFormat.format(it.toDate()) } ?: ""

        edtTitleDialog.setText(spending.name.orEmpty())
        edtAmountDialog.setText(CurrencyPreferences.formatPlain(spending.amount))

        edtCategoryDialog.setText(FinancialEntryOptions.displayCategory(this, spending.category))
        edtSubcategoryDialog.setText(
            FinancialEntryOptions.displaySubcategory(this, spending.category, spending.subcategory)
        )
        edtVendorDialog.setText(spending.vendor.orEmpty())
        edtDateDialog.setText(spendingDateString)
        edtCommentDialog.setText(spending.comment.orEmpty())
        if (spending.attachment.isNullOrBlank()) attachment.visibility = View.GONE
        else {
            attachment.setImageURI(Uri.parse(spending.attachment))
            attachment.visibility = View.VISIBLE
        }

        dialogSpendings.show()
        styleDetailsDialogWindow(dialogSpendings)
    }

    private fun showIncomeDetailsDialog(income: Income) {
        val edtTitleDialog = dialogIncome.findViewById<EditText>(R.id.edtTitleDialogIncome)
        val edtAmountDialog = dialogIncome.findViewById<EditText>(R.id.edtAmountDialogIncome)
        val edtCategoryDialog = dialogIncome.findViewById<EditText>(R.id.edtCategoryDialogIncome)
        val edtSubcategoryDialog = dialogIncome.findViewById<EditText>(R.id.edtSubcategoryDialogIncome)
        val edtDateDialog = dialogIncome.findViewById<EditText>(R.id.edtDateDialogIncome)
        val edtRepeatDialog = dialogIncome.findViewById<EditText>(R.id.edtRepeatDialogIncome)
        val edtCommentDialog = dialogIncome.findViewById<EditText>(R.id.edtCommentDialogIncome)
        val edtSourceDialog = dialogIncome.findViewById<EditText>(R.id.edtSourceDialogIncome)
        val attachmentView =
            dialogIncome.findViewById<ImageView>(R.id.edtAttachmentDialogIncome)

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val incomeDateString = income.date?.let { dateFormat.format(it.toDate()) } ?: ""

        edtTitleDialog.setText(income.name.orEmpty())
        edtAmountDialog.setText(CurrencyPreferences.formatPlain(income.amount))

        edtCategoryDialog.setText(FinancialEntryOptions.displayCategory(this, income.category))
        edtSubcategoryDialog.setText(
            FinancialEntryOptions.displaySubcategory(this, income.category, income.subcategory)
        )
        edtDateDialog.setText(incomeDateString)
        edtRepeatDialog.setText(income.repeat.orEmpty())
        edtCommentDialog.setText(income.comment.orEmpty())
        edtSourceDialog.setText(income.source.orEmpty())
        if (income.attachment.isNullOrBlank()) {
            attachmentView.visibility = View.GONE
        } else {
            attachmentView.setImageURI(Uri.parse(income.attachment))
            attachmentView.visibility = View.VISIBLE
        }

        dialogIncome.show()
        styleDetailsDialogWindow(dialogIncome)
    }

    override fun onItemClick(position: Int) {
        val item = myAdapterCalendar.getItemAtPosition(position)
        when (item) {
            is Bills -> showBillDetailsDialog(item)
            is Spendings -> showSpendingsDetailsDialog(item)
            is Income -> showIncomeDetailsDialog(item)
            else -> Toast.makeText(this, R.string.unknown_item_clicked, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        listenerRegistrations.forEach { it.remove() }
        listenerRegistrations.clear()
    }

    private fun styleDetailsDialogWindow(dialog: Dialog) {
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    // Update the unread count badge from SharedPreferences
    private fun updateUnreadCountBadge(badgeCountTextView: TextView?) {
        val unreadCount = NotificationsActivity.getUnreadNotificationCount(this)
        if (unreadCount > 0) {
            badgeCountTextView?.text = if (unreadCount > 99) "99+" else unreadCount.toString()
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
