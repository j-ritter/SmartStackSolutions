package com.example.smartstackbills

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainMenu : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var currentMonth: Calendar
    private lateinit var fabMainMenu: FloatingActionButton
    private lateinit var etBillsAmount: EditText
    private lateinit var etIncomingAmount: EditText
    private lateinit var etOverdueAmount: EditText
    private lateinit var etSpendingsAmount: EditText
    private lateinit var etEssentialAmount: EditText
    private lateinit var etNonEssentialAmount: EditText
    private lateinit var etIncomeAmount: EditText
    private lateinit var etRecurringAmount: EditText
    private lateinit var etOneTimeAmount: EditText
    private lateinit var etTotalAmount: EditText
    private lateinit var etMonthlySavingsMain: EditText

    private var userEmail: String? = null
    private var userUid: String? = null
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main_menu)

        db = FirebaseFirestore.getInstance()

        fabMainMenu = findViewById(R.id.fabMainMenu)
        fabMainMenu.setOnClickListener {
            showCreateOptionsDialog()
        }

        userEmail = intent.getStringExtra("USER_EMAIL")

        userUid = FirebaseAuth.getInstance().currentUser?.uid

        drawerLayout = findViewById(R.id.drawer_layout)

        currentMonth = Calendar.getInstance()

        etBillsAmount = findViewById(R.id.etBillsAmount)
        etSpendingsAmount = findViewById(R.id.etSpendingsAmount)
        etIncomeAmount = findViewById(R.id.etIncome)
        etIncomingAmount = findViewById(R.id.etIncomingAmount)
        etOverdueAmount = findViewById(R.id.etOverdueAmount)
        etEssentialAmount = findViewById(R.id.etEssential)
        etNonEssentialAmount = findViewById(R.id.etNonEssential)
        etRecurringAmount = findViewById(R.id.etRecurring)
        etOneTimeAmount = findViewById(R.id.etOneTime)
        etTotalAmount = findViewById(R.id.etTotal)
        etMonthlySavingsMain = findViewById(R.id.etMonthlySavings)

        setupMonthNavigation()
        setupUI()
        setupSavingsVisibility()
        // Call this to load the saved target when the activity starts
        loadSavingsTarget()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val tvTotal: TextView = findViewById(R.id.tvTotal)
        tvTotal.setOnClickListener {
            showSavingsDialog()
        }

        val toolbar: Toolbar = findViewById(R.id.toolbar_main)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.Main

        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.Main -> true
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
                    startActivity(intent)
                    true
                }

                else -> false
            }
        }

        val navView: NavigationView = findViewById(R.id.nav_view)
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

        // Set values
        setAmountForMonth()

        // Connection to other folders
        val etBills: EditText = findViewById(R.id.etBillsAmount)
        etBills.setOnClickListener {
            val intent = Intent(this, MyBills::class.java)
            intent.putExtra("FILTER_TYPE", "all")
            startActivity(intent)
        }
        val etSpendings: EditText = findViewById(R.id.etSpendingsAmount)
        etSpendings.setOnClickListener {
            val intent = Intent(this, MySpendings::class.java)
            intent.putExtra("FILTER_TYPE", "all")
            startActivity(intent)
        }
        val etIncome: EditText = findViewById(R.id.etIncome)
        etIncome.setOnClickListener {
            val intent = Intent(this, MyIncome::class.java)
            intent.putExtra("FILTER_TYPE", "all")
            startActivity(intent)
        }
    }

    // Method to retrieve all bills, spendings and income from SharedPreferences
    private fun getBills(): ArrayList<Bills> {
        val sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPref.getString("billsList", null)
        val type = object : TypeToken<ArrayList<Bills>>() {}.type
        return gson.fromJson(json, type) ?: ArrayList()
    }

    private fun getSpendings(): ArrayList<Spendings> {
        val sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPref.getString("spendingsList", null)
        val type = object : TypeToken<ArrayList<Spendings>>() {}.type
        return gson.fromJson(json, type) ?: ArrayList()
    }

    private fun getIncome(): ArrayList<Income> {
        val sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPref.getString("incomeList", null)
        val type = object : TypeToken<ArrayList<Income>>() {}.type
        return gson.fromJson(json, type) ?: ArrayList()
    }

    // Method to filter bills and income by the currently selected month
    private fun getEntriesForCurrentMonth(): List<Any> {
        val billsList = getBills()
        val spendingsList = getSpendings()
        val incomeList = getIncome()

        val dateFormat = SimpleDateFormat("MM/yyyy", Locale.getDefault())
        val selectedMonth = dateFormat.format(currentMonth.time)

        val billsForMonth = billsList.filter { bill ->
            val billDate = bill.date?.toDate()
            billDate != null && dateFormat.format(billDate) == selectedMonth
        }
        val spendingsForMonth = spendingsList.filter { spending ->
            val spendingDate = spending.date?.toDate()
            spendingDate != null && dateFormat.format(spendingDate) == selectedMonth
        }
        val incomeForMonth = incomeList.filter { income ->
            val incomeDate = income.date?.toDate()
            incomeDate != null && dateFormat.format(incomeDate) == selectedMonth
        }

        // Combine lists
        return billsForMonth + incomeForMonth + spendingsForMonth
    }

    // Method to calculate and set the total amount for the selected month
    private fun setAmountForMonth() {
        // Retrieve the savings for the current month
        val savingsForCurrentMonth = getSavingsForCurrentMonth()
        etMonthlySavingsMain.setText(String.format(Locale.getDefault(), "%.2f", savingsForCurrentMonth))

        // Get the savings target for the current month from SharedPreferences
        val sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPref.getString("monthlySavingsMap", null)
        val monthlySavingsMap: Map<String, Float> = if (json != null) {
            gson.fromJson(json, object : TypeToken<Map<String, Float>>() {}.type)
        } else {
            emptyMap()
        }

        val dateFormat = SimpleDateFormat("MM/yyyy", Locale.getDefault())
        val currentMonthString = dateFormat.format(currentMonth.time)
        val savingsForCurrentMonthTarget = monthlySavingsMap[currentMonthString] ?: 0f

        // Display the savings target
        etMonthlySavingsMain.setText(String.format(Locale.getDefault(), "%.2f", savingsForCurrentMonthTarget))

        // Fetch bills, spendings, and income for the current month
        val billsList = getBills()
        val spendingsList = getSpendings()
        val incomeList = getIncome()

        // Filter entries for the current month
        val billsForMonth = billsList.filter { bill ->
            val billDate = bill.date?.toDate()
            billDate != null && dateFormat.format(billDate) == currentMonthString
        }

        val spendingsForMonth = spendingsList.filter { spending ->
            val spendingDate = spending.date?.toDate()
            spendingDate != null && dateFormat.format(spendingDate) == currentMonthString
        }

        val incomeForMonth = incomeList.filter { income ->
            val incomeDate = income.date?.toDate()
            incomeDate != null && dateFormat.format(incomeDate) == currentMonthString
        }

        // Calculate totals for bills, spendings, and income
        val totalBills = billsForMonth.sumOf { it.amount.toDouble() }.toFloat()
        val totalSpendings = spendingsForMonth.sumOf { it.amount.toDouble() }.toFloat()
        val totalIncome = incomeForMonth.sumOf { it.amount.toDouble() }.toFloat()

        // Calculate incoming bills (unpaid, non-recurring, in the future)
        val currentDate = Date()
        val totalIncoming = billsList.filter { bill ->
            val billDate = bill.date?.toDate()
            billDate != null && billDate.after(currentDate) && !bill.paid && bill.repeat == "No" &&
                    dateFormat.format(billDate) == currentMonthString
        }.sumOf { it.amount.toDouble() }.toFloat()

        // Calculate overdue bills (unpaid, non-recurring, in the past)
        val totalOverdue = billsList.filter { bill ->
            val billDate = bill.date?.toDate()
            billDate != null && billDate.before(currentDate) && !bill.paid && bill.repeat == "No" &&
                    dateFormat.format(billDate) == currentMonthString
        }.sumOf { it.amount.toDouble() }.toFloat()

        // Calculate essential and non-essential spendings based on predefined categories
        val essentialCategories = listOf("Accommodation", "Communication", "Insurance", "Transportation", "Finances/Fees", "Taxes", "Health", "Education", "Shopping & Consumption")
        val essentialSubcategories = listOf("Rent", "Utilities", "Groceries - Basic Food", "Groceries - Household Necessities", "Mobile phone", "Internet", "Health insurance", "Car insurance", "Fuel", "Public transportation")

        val totalEssential = spendingsForMonth.filter { spending ->
            val category = spending.category
            val subcategory = spending.subcategory
            val spendingDate = spending.date?.toDate()
            val isInSelectedMonth = spendingDate != null && dateFormat.format(spendingDate) == currentMonthString
            isInSelectedMonth && (
                    subcategory in essentialSubcategories ||
                            (subcategory == null && category in essentialCategories)
                    )
        }.sumOf { it.amount.toDouble() }.toFloat()

        val nonEssentialCategories = listOf("Subscription and Memberships", "Others")
        val nonEssentialSubcategories = listOf("Entertainment", "Dining out", "Hobbies", "Streaming services", "Movies", "Vacation", "Gadgets")

        val totalNonEssential = spendingsForMonth.filter { spending ->
            val category = spending.category
            val subcategory = spending.subcategory
            val spendingDate = spending.date?.toDate()
            val isInSelectedMonth = spendingDate != null && dateFormat.format(spendingDate) == currentMonthString
            isInSelectedMonth && (
                    subcategory in nonEssentialSubcategories ||
                            (subcategory == null && category in nonEssentialCategories)
                    )
        }.sumOf { it.amount.toDouble() }.toFloat()

        // Calculate recurring and one-time income
        val totalRecurringIncome = incomeForMonth.filter { income ->
            income.repeat != "No"
        }.sumOf { it.amount.toDouble() }.toFloat()

        val totalOneTimeIncome = incomeForMonth.filter { income ->
            income.repeat == "No"
        }.sumOf { it.amount.toDouble() }.toFloat()

        // Ensure that the items are displayed as negative for bills and spendings
        val displayBillsAmount = -totalBills
        val displayIncomingAmount = -totalIncoming
        val displayOverdueAmount = -totalOverdue
        val displaySpendingsAmount = -totalSpendings
        val displayEssentialAmount = -totalEssential
        val displayNonEssentialAmount = -totalNonEssential
        val displayIncomeAmount = +totalIncome
        val displayRecurringAmount = +totalRecurringIncome
        val displayOneTimeAmount = +totalOneTimeIncome

        // Calculate the total amount for the month
        val total = totalIncome - totalBills - totalSpendings

        // Display the calculated values in the appropriate fields
        etBillsAmount.setText(String.format(Locale.getDefault(), "%.2f", displayBillsAmount))
        etSpendingsAmount.setText(String.format(Locale.getDefault(), "%.2f", displaySpendingsAmount))
        etIncomeAmount.setText(String.format(Locale.getDefault(), "%.2f", displayIncomeAmount))
        etIncomingAmount.setText(String.format(Locale.getDefault(), "%.2f", displayIncomingAmount))
        etOverdueAmount.setText(String.format(Locale.getDefault(), "%.2f", displayOverdueAmount))
        etEssentialAmount.setText(String.format(Locale.getDefault(), "%.2f", displayEssentialAmount))
        etNonEssentialAmount.setText(String.format(Locale.getDefault(), "%.2f", displayNonEssentialAmount))
        etRecurringAmount.setText(String.format(Locale.getDefault(), "%.2f", displayRecurringAmount))
        etOneTimeAmount.setText(String.format(Locale.getDefault(), "%.2f", displayOneTimeAmount))
        etTotalAmount.setText(String.format(Locale.getDefault(), "%.2f", total))
    }

    private fun getSavingsForCurrentMonth(): Float {
        val sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPref.getString("monthlySavingsMap", null)
        val monthlySavingsMap: Map<String, Float> = if (json != null) {
            gson.fromJson(json, object : TypeToken<Map<String, Float>>() {}.type)
        } else {
            emptyMap()
        }

        val currentMonthString = SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(currentMonth.time)
        return monthlySavingsMap[currentMonthString] ?: 0f
    }

    private fun setupMonthNavigation() {
        val tvMonth = findViewById<TextView>(R.id.tvMonth)
        val btnPreviousMonth = findViewById<ImageButton>(R.id.btnPreviousMonth)
        val btnNextMonth = findViewById<ImageButton>(R.id.btnNextMonth)

        updateMonthDisplay(tvMonth)

        btnPreviousMonth.setOnClickListener {
            currentMonth.add(Calendar.MONTH, -1)
            updateMonthDisplay(tvMonth)
            setAmountForMonth()
            loadSavingsTarget()

        }

        btnNextMonth.setOnClickListener {
            currentMonth.add(Calendar.MONTH, 1)
            updateMonthDisplay(tvMonth)
            setAmountForMonth()
            loadSavingsTarget()
        }
    }

    private fun updateMonthDisplay(tvMonth: TextView) {
        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        tvMonth.text = dateFormat.format(currentMonth.time)
    }

    private fun showCreateOptionsDialog() {
        val options =
            arrayOf("Create an Open Payment", "Create a Closed Payment", "Create an Income")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select an option")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> startActivity(Intent(this, createBill::class.java).apply {
                    putExtra("USER_EMAIL", userEmail)
                })

                1 -> startActivity(Intent(this, createSpending::class.java).apply {
                    putExtra("USER_EMAIL", userEmail)
                })

                2 -> startActivity(Intent(this, createIncome::class.java).apply {
                    putExtra("USER_EMAIL", userEmail)
                })
            }
        }
        builder.show()
    }

    // Apply alternating background colors
    private fun setupUI() {
        val navView: NavigationView = findViewById(R.id.nav_view)
        val menu = navView.menu
        for (i in 0 until menu.size()) {
            val menuItem = menu.getItem(i)
            if (i % 2 == 0) {
                menuItem.actionView?.setBackgroundResource(R.drawable.nav_item_background_light)
            } else {
                menuItem.actionView?.setBackgroundResource(R.drawable.nav_item_background_dark)
            }
        }
    }

    private fun setupSavingsVisibility() {
        val etMonthlySavings: EditText = findViewById(R.id.etMonthlySavings)
        val tvTargetAchieved: TextView = findViewById(R.id.tvTargetAchieved)
        val tvProgressPercentage: TextView = findViewById(R.id.tvProgressPercentage)
        val progressBarSavings: LinearProgressIndicator = findViewById(R.id.progressBarSavings)

        // Initially hide all three views
        tvTargetAchieved.visibility = View.GONE
        tvProgressPercentage.visibility = View.GONE
        progressBarSavings.visibility = View.GONE

        // Add a TextWatcher to listen for changes in etMonthlySavings
        etMonthlySavings.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Get the value from etMonthlySavings
                val savingsAmount = s.toString().toFloatOrNull() ?: 0f

                // Show the views only if the amount is greater than 0
                if (savingsAmount > 0) {
                    tvTargetAchieved.visibility = View.VISIBLE
                    tvProgressPercentage.visibility = View.VISIBLE
                    progressBarSavings.visibility = View.VISIBLE
                } else {
                    tvTargetAchieved.visibility = View.GONE
                    tvProgressPercentage.visibility = View.GONE
                    progressBarSavings.visibility = View.GONE
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }
    private fun validateMandatoryFields(): Boolean {
        val targetAmountEditText = findViewById<EditText>(R.id.edtTargetAmount)
        val startDateEditText = findViewById<EditText>(R.id.edtStartDateSavings)
        val endDateEditText = findViewById<EditText>(R.id.edtEndDateSavings)

        val targetAmount = targetAmountEditText.text.toString().toFloatOrNull()
        val startDate = getDateFromEditText(startDateEditText)
        val endDate = getDateFromEditText(endDateEditText)

        // Check if all mandatory fields are filled
        return when {
            targetAmount == null || targetAmount <= 0f -> {
                Toast.makeText(this, "Please enter a valid target amount", Toast.LENGTH_SHORT).show()
                false
            }
            startDate == null -> {
                Toast.makeText(this, "Please select a valid start date", Toast.LENGTH_SHORT).show()
                false
            }
            endDate == null -> {
                Toast.makeText(this, "Please select a valid end date", Toast.LENGTH_SHORT).show()
                false
            }
            !startDate.before(endDate) -> {
                Toast.makeText(this, "Start date must be before the end date", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }
    private fun validateDateFields(): Boolean {
        val startDateEditText = findViewById<EditText>(R.id.edtStartDateSavings)
        val endDateEditText = findViewById<EditText>(R.id.edtEndDateSavings)

        val startDate = getDateFromEditText(startDateEditText)
        val endDate = getDateFromEditText(endDateEditText)

        // Check if start and end dates are valid
        return when {
            startDate == null -> {
                Toast.makeText(this, "Please select a valid start date", Toast.LENGTH_SHORT).show()
                false
            }
            endDate == null -> {
                Toast.makeText(this, "Please select a valid end date", Toast.LENGTH_SHORT).show()
                false
            }
            !startDate.before(endDate) -> {
                Toast.makeText(this, "Start date must be before the end date", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }


    private fun updateProgressBar(targetAmount: Float) {
        val savedAmountSoFar = etTotalAmount.text.toString().toFloatOrNull() ?: 0f
        val progressBarSavings =
            findViewById<com.google.android.material.progressindicator.LinearProgressIndicator>(R.id.progressBarSavings)
        val tvProgressPercentage = findViewById<TextView>(R.id.tvProgressPercentage)

        if (targetAmount > 0) {
            val progress = (savedAmountSoFar / targetAmount) * 100
            progressBarSavings?.let {
                it.setProgressCompat(progress.toInt().coerceAtMost(100), true)
            }
            // Update the percentage text
            tvProgressPercentage.text = String.format("%.0f%%", progress.coerceAtMost(100f))
        } else {
            progressBarSavings?.setProgressCompat(0, true)
            tvProgressPercentage.text = "0%"
        }
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
    fun openSavingsDialog(view: View) {
        showSavingsDialog() // This method should already be defined in your class
    }

    private fun showSavingsDialog() {
        // Inflate the custom layout for the savings dialog
        val dialogView = layoutInflater.inflate(R.layout.dialog_box_savings, null)

        // Initialize the dialog box with the inflated view
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setView(dialogView)
            .setCancelable(true)

        // Show the dialog
        val dialog = dialogBuilder.create()
        dialog.show()

        // Find views
        val targetAmountEditText = dialogView.findViewById<EditText>(R.id.edtTargetAmount)
        val startDateEditText = dialogView.findViewById<EditText>(R.id.edtStartDateSavings)
        val endDateEditText = dialogView.findViewById<EditText>(R.id.edtEndDateSavings)
        val monthlySavingsEditText = dialogView.findViewById<EditText>(R.id.edtMonthlySavings)
        val targetNameSpinner = dialogView.findViewById<Spinner>(R.id.spinnerTargetName)

        val btnCloseDialog = dialogView.findViewById<Button>(R.id.btnCloseDialogSavings)

        saveNewTarget(dialogView, targetAmountEditText, startDateEditText, endDateEditText, dialog, monthlySavingsEditText)

        // Set DatePickers for start and end dates
        startDateEditText.setOnClickListener {
            showDatePickerDialog(startDateEditText)
            updateMonthlySavingsFromFields(targetAmountEditText, startDateEditText, endDateEditText, monthlySavingsEditText)
        }
        endDateEditText.setOnClickListener {
            showDatePickerDialog(endDateEditText)
            updateMonthlySavingsFromFields(targetAmountEditText, startDateEditText, endDateEditText, monthlySavingsEditText)
        }

        // Initialize Spinner Adapter
        val targetNames = listOf("Vacation", "New Car", "Emergency Fund", "Home Down Payment", "Car Purchase", "Education Fund", "Retirement", "Wedding", "Investment", "No specific reason")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, targetNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        targetNameSpinner.adapter = adapter

        // Load saved values from SharedPreferences
        val sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val savedTargetName = sharedPref.getString("savingsTargetName", "")
        val savedTargetAmount = sharedPref.getFloat("savingsTargetAmount", 0f)
        val savedTargetStartMonth = sharedPref.getString("savingsTargetStartMonth", "")
        val savedTargetEndMonth = sharedPref.getString("savingsTargetEndMonth", "")

        // Set the saved values in the dialog's input fields
        targetAmountEditText.setText(String.format("%.2f", savedTargetAmount))

        // Set saved target name in the spinner
        if (!savedTargetName.isNullOrEmpty()) {
            val namePosition = (targetNameSpinner.adapter as ArrayAdapter<String>).getPosition(savedTargetName)
            targetNameSpinner.setSelection(namePosition)
        }

        // Set the saved start and end dates if they exist
        startDateEditText.setText(savedTargetStartMonth ?: "")
        endDateEditText.setText(savedTargetEndMonth ?: "")

        btnCloseDialog.setOnClickListener {
            dialog.dismiss() // Simply close the dialog
        }


    // Add TextWatcher for targetAmountEditText
        targetAmountEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Ensure that the update methods use the right scope for monthlySavingsEditText
                updateProgressBar(targetAmountEditText.text.toString().toFloatOrNull() ?: 0f)
                updateMonthlySavings(
                    targetAmountEditText.text.toString().toFloatOrNull(),
                    getDateFromEditText(startDateEditText),
                    getDateFromEditText(endDateEditText),
                    monthlySavingsEditText  // Use monthlySavingsEditText here
                )
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    // Method to calculate the number of months between two dates
    private fun getMonthsBetweenDates(startDate: Calendar, endDate: Calendar): Int {
        val yearsDifference = endDate.get(Calendar.YEAR) - startDate.get(Calendar.YEAR)
        val monthsDifference = endDate.get(Calendar.MONTH) - startDate.get(Calendar.MONTH)
        return (yearsDifference * 12) + monthsDifference + 1 // +1 to include the current month
    }

    // Helper method to parse the date from the EditText
    private fun getDateFromEditText(editText: EditText): Calendar? {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return try {
            val date = sdf.parse(editText.text.toString())
            Calendar.getInstance().apply { time = date }
        } catch (e: Exception) {
            null
        }
    }
    private fun updateMonthlySavingsFromFields(
        targetAmountEditText: EditText,
        startDateEditText: EditText,
        endDateEditText: EditText,
        monthlySavingsEditText: EditText
    ) {
        val targetAmount = targetAmountEditText.text.toString().toFloatOrNull()
        val startDate = getDateFromEditText(startDateEditText)
        val endDate = getDateFromEditText(endDateEditText)

        if (targetAmount != null && startDate != null && endDate != null && startDate.before(endDate)) {
            val monthsDifference = getMonthsBetweenDates(startDate, endDate)
            if (monthsDifference > 0) {
                val monthlySavings = targetAmount / monthsDifference
                monthlySavingsEditText.setText(String.format("%.2f", monthlySavings))
            }
        } else {
            // Clear the monthly savings if inputs are invalid
            monthlySavingsEditText.setText("")
        }
    }

    // Helper method to show the DatePicker dialog
    private fun showDatePickerDialog(editText: EditText) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val selectedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
            editText.setText(selectedDate)
        }, year, month, day)

        datePickerDialog.show()
    }

    // Method to save the target with start and end dates
    private fun saveSavings() {
        if (validateMandatoryFields() && validateDateFields()) {
            if (userEmail != null && userUid != null) {
                val targetAmount = findViewById<EditText>(R.id.edtTargetAmount).text.toString()
                val startDateString = findViewById<EditText>(R.id.edtStartDateSavings).text.toString()
                val endDateString = findViewById<EditText>(R.id.edtEndDateSavings).text.toString()
                val targetName = findViewById<Spinner>(R.id.spinnerTargetName).selectedItem?.toString() ?: "-"

                // Convert String to Date
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val startDate: Date? = try {
                    sdf.parse(startDateString)
                } catch (e: Exception) {
                    null
                }
                val endDate: Date? = try {
                    sdf.parse(endDateString)
                } catch (e: Exception) {
                    null
                }

                val timestampStart = startDate?.let { com.google.firebase.Timestamp(it) }
                val timestampEnd = endDate?.let { com.google.firebase.Timestamp(it) }

                if (startDate != null && endDate != null) {
                    // Create the main savings document
                    val savings = hashMapOf(
                        "targetAmount" to targetAmount,
                        "startDate" to timestampStart,
                        "endDate" to timestampEnd,
                        "targetName" to targetName
                    )

                    val docRef = db.collection("users").document(userUid!!).collection("savings_targets").document()
                    val savingsId = docRef.id
                    savings["savingsId"] = savingsId

                    // Save the savings target
                    docRef.set(savings)
                        .addOnSuccessListener {
                            // Distribute the savings across the months and save in Firebase
                            distributeSavingsAcrossMonths(startDate, endDate, targetAmount.toFloat(), savingsId)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error saving savings target: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            } else {
                Toast.makeText(this, "Error: Unable to retrieve user email or UID", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Method to distribute savings across months and save them in Firebase
    private fun distributeSavingsAcrossMonths(startDate: Date, endDate: Date, targetAmount: Float, savingsId: String) {
        val startCalendar = Calendar.getInstance()
        startCalendar.time = startDate

        val endCalendar = Calendar.getInstance()
        endCalendar.time = endDate

        val months = getMonthsBetweenDates(startCalendar, endCalendar)
        val monthlyAmount = targetAmount / months
        val dateFormat = SimpleDateFormat("MM/yyyy", Locale.getDefault())

        for (i in 0 until months) {
            val targetMonth = startCalendar.clone() as Calendar
            targetMonth.add(Calendar.MONTH, i)
            val monthString = dateFormat.format(targetMonth.time)

            val monthSavings = hashMapOf(
                "month" to monthString,
                "monthlyAmount" to monthlyAmount,
                "savingsId" to savingsId
            )

            // Save each month's savings into Firebase
            db.collection("users").document(userUid!!).collection("savings_targets")
                .document(savingsId).collection("monthly_savings").document(monthString)
                .set(monthSavings)
                .addOnSuccessListener {
                    Toast.makeText(this, "Monthly savings saved for $monthString", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error saving monthly savings: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateMonthlySavings(
        targetAmount: Float?,
        startDate: Calendar?,
        endDate: Calendar?,
        monthlySavingsEditText: EditText
    ) {
        if (targetAmount != null && targetAmount > 0f && startDate != null && endDate != null && startDate.before(endDate)) {
            val monthsDifference = getMonthsBetweenDates(startDate, endDate)
            if (monthsDifference > 0) {
                val monthlySavings = targetAmount / monthsDifference
                monthlySavingsEditText.setText(String.format("%.2f", monthlySavings))
            }
        }
    }

    private fun saveNewTarget(
        dialogView: View,
        targetAmountEditText: EditText,
        startDateEditText: EditText,
        endDateEditText: EditText,
        dialog: AlertDialog,
        monthlySavingsEditText: EditText
    ) {
        val btnSaveTarget = dialogView.findViewById<Button>(R.id.btnSaveTargetSavings)

        // Save button logic
        btnSaveTarget.setOnClickListener {
            val targetAmount = targetAmountEditText.text.toString().toFloatOrNull()
            val startDate = getDateFromEditText(startDateEditText)
            val endDate = getDateFromEditText(endDateEditText)

            // Basic validation
            if (targetAmount == null || targetAmount <= 0f) {
                Toast.makeText(this, "Please enter a valid target amount", Toast.LENGTH_SHORT).show()
            } else if (startDate == null || endDate == null || !startDate.before(endDate)) {
                Toast.makeText(this, "Please select valid start and end dates", Toast.LENGTH_SHORT).show()
            } else {
                // Saving the target
                saveSavingsTarget(targetAmount, startDate, endDate)

                // Notify the user that the target has been updated
                Toast.makeText(this, "Savings target has been updated.", Toast.LENGTH_SHORT).show()

                dialog.dismiss() // Close the dialog
            }
        }

        // DatePicker for start and end date selection
        startDateEditText.setOnClickListener {
            showDatePickerDialog(startDateEditText)
            updateMonthlySavings(
                targetAmountEditText.text.toString().toFloatOrNull(),
                getDateFromEditText(startDateEditText),
                getDateFromEditText(endDateEditText),
                monthlySavingsEditText
            )
        }

        endDateEditText.setOnClickListener {
            showDatePickerDialog(endDateEditText)
            updateMonthlySavings(
                targetAmountEditText.text.toString().toFloatOrNull(),
                getDateFromEditText(startDateEditText),
                getDateFromEditText(endDateEditText),
                monthlySavingsEditText
            )
        }

        // TextWatcher to update monthly savings based on target amount
        targetAmountEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateProgressBar(targetAmountEditText.text.toString().toFloatOrNull() ?: 0f)
                updateMonthlySavings(
                    targetAmountEditText.text.toString().toFloatOrNull(),
                    getDateFromEditText(startDateEditText),
                    getDateFromEditText(endDateEditText),
                    monthlySavingsEditText
                )
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }
    private fun saveSavingsTarget(targetAmount: Float, startDate: Calendar, endDate: Calendar) {
        val userUid = FirebaseAuth.getInstance().currentUser?.uid

        if (userUid != null) {
            // Convert Calendar dates to Timestamp
            val timestampStart = com.google.firebase.Timestamp(startDate.time)
            val timestampEnd = com.google.firebase.Timestamp(endDate.time)

            // Prepare data to save
            val savingsData = hashMapOf(
                "targetAmount" to targetAmount,
                "startDate" to timestampStart,
                "endDate" to timestampEnd,
                "userUid" to userUid
            )

            // Save the target data to Firestore
            db.collection("users")
                .document(userUid)
                .collection("savings_targets")
                .document("mainTarget")
                .set(savingsData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Savings target saved successfully!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error saving target: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show()
        }
    }



    private fun loadSavingsTarget() {
        // Get Firebase reference to the user's savings target
        val userUid = FirebaseAuth.getInstance().currentUser?.uid
        val savingsTargetRef = db.collection("users").document(userUid!!).collection("savingsTargets").document("mainTarget")

        savingsTargetRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Retrieve the target amount, start month, and end month from Firebase
                    val targetAmount = document.getDouble("targetAmount")?.toFloat() ?: 0f
                    val savedStartMonth = document.getString("startMonth") ?: ""
                    val savedEndMonth = document.getString("endMonth") ?: ""

                    // Ensure both start and end dates are available before proceeding
                    if (savedStartMonth.isNotEmpty() && savedEndMonth.isNotEmpty() && targetAmount > 0f) {
                        val dateFormat = SimpleDateFormat("MM/yyyy", Locale.getDefault())

                        // Parse the start and end months
                        val startMonth: Calendar = Calendar.getInstance()
                        val endMonth: Calendar = Calendar.getInstance()

                        try {
                            startMonth.time = dateFormat.parse(savedStartMonth) ?: return@addOnSuccessListener
                            endMonth.time = dateFormat.parse(savedEndMonth) ?: return@addOnSuccessListener
                        } catch (e: Exception) {
                            Toast.makeText(this, "Error parsing saved date. Please set the target again.", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        // Calculate the number of months between the start and end dates
                        val monthsDifference = calculateMonthsDifference(startMonth, endMonth)

                        // If there are months in the range, calculate the monthly savings
                        if (monthsDifference > 0) {
                            val monthlySavings = targetAmount / monthsDifference
                            etMonthlySavingsMain.setText(String.format("%.2f", monthlySavings))

                            // Update the progress bar with the saved target amount
                            updateProgressBar(targetAmount)
                        } else {
                            etMonthlySavingsMain.setText("0.00")
                        }
                    } else {
                        etMonthlySavingsMain.setText("0.00")
                    }
                } else {
                    // No target saved, set default
                    etMonthlySavingsMain.setText("0.00")
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading savings target: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun calculateMonthsDifference(startMonth: Calendar, endMonth: Calendar): Int {
        val yearsDifference = endMonth.get(Calendar.YEAR) - startMonth.get(Calendar.YEAR)
        val monthsDifference = yearsDifference * 12 + (endMonth.get(Calendar.MONTH) - startMonth.get(Calendar.MONTH))
        return monthsDifference + 1
    }


    private fun logoutUser() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, LogIn::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

