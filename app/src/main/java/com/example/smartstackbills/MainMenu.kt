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
import android.widget.ImageView
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
import java.text.NumberFormat
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
    private lateinit var etMonthlySavingsMain: TextView

    private var userEmail: String? = null
    private var userUid: String? = null
    private lateinit var db: FirebaseFirestore
    private var currentSavingsTargetDocumentId: String? = null

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
        loadSavingsTargetNameForMonth(currentMonth.time)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val fields = listOf(
            etBillsAmount, etSpendingsAmount, etIncomeAmount, etIncomingAmount,
            etOverdueAmount, etEssentialAmount, etNonEssentialAmount,
            etRecurringAmount, etOneTimeAmount, etTotalAmount
        )

        fields.forEach { field ->
            field.addTextChangedListener(RealTimeFormattingTextWatcher(field))
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

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
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
        val dateFormat = SimpleDateFormat("MM/yyyy", Locale.getDefault())
        val currentMonthString = dateFormat.format(currentMonth.time)

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
        val totalBills = billsForMonth.sumOf {
            parseFormattedNumber(it.amount)
        }.toFloat()

        val totalSpendings = spendingsForMonth.sumOf {
            parseFormattedNumber(it.amount)
        }.toFloat()

        val totalIncome = incomeForMonth.sumOf {
            parseFormattedNumber(it.amount)
        }.toFloat()


        // Calculate incoming bills (unpaid, in the future)
        val currentDate = Date()
        val totalIncoming = billsList.filter { bill ->
            val billDate = bill.date?.toDate()
            billDate != null && billDate.after(currentDate) && !bill.paid &&
                    dateFormat.format(billDate) == currentMonthString
        }.sumOf { parseFormattedNumber(it.amount) }.toFloat()

        // Calculate overdue bills (unpaid, in the past)
        val totalOverdue = billsList.filter { bill ->
            val billDate = bill.date?.toDate()
            billDate != null && billDate.before(currentDate) && !bill.paid &&
                    dateFormat.format(billDate) == currentMonthString
        }.sumOf { parseFormattedNumber(it.amount) }.toFloat()


        // Calculate the total amount for the month
        val total = totalIncome - totalBills - totalSpendings

        // Display the calculated values in the appropriate fields
        etBillsAmount.setText(String.format(Locale.getDefault(), "%.2f", -totalBills))
        etSpendingsAmount.setText(String.format(Locale.getDefault(), "%.2f", -totalSpendings))
        etIncomeAmount.setText(String.format(Locale.getDefault(), "%.2f", totalIncome))
        etIncomingAmount.setText(String.format(Locale.getDefault(), "%.2f", -totalIncoming))
        etOverdueAmount.setText(String.format(Locale.getDefault(), "%.2f", -totalOverdue))

        etTotalAmount.setText(String.format(Locale.getDefault(), "%.2f", total))

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
        }.sumOf { parseFormattedNumber(it.amount) }.toFloat()

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
        }.sumOf { parseFormattedNumber(it.amount) }.toFloat()

        // Calculate recurring and one-time income
        val totalRecurringIncome = incomeForMonth.filter { income ->
            income.repeat != "No"
        }.sumOf { parseFormattedNumber(it.amount) }.toFloat()

        val totalOneTimeIncome = incomeForMonth.filter { income ->
            income.repeat == "No"
        }.sumOf { parseFormattedNumber(it.amount) }.toFloat()

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

    private fun setupMonthNavigation() {
        val tvMonth = findViewById<TextView>(R.id.tvMonth)
        val btnPreviousMonth = findViewById<ImageButton>(R.id.btnPreviousMonth)
        val btnNextMonth = findViewById<ImageButton>(R.id.btnNextMonth)

        updateMonthDisplay(tvMonth)
        loadSavingsTargetNameForMonth(currentMonth.time)

        btnPreviousMonth.setOnClickListener {
            currentMonth.add(Calendar.MONTH, -1)
            updateMonthDisplay(tvMonth)
            setAmountForMonth()
            loadSavingsTarget()
            loadSavingsTargetNameForMonth(currentMonth.time)

        }

        btnNextMonth.setOnClickListener {
            currentMonth.add(Calendar.MONTH, 1)
            updateMonthDisplay(tvMonth)
            setAmountForMonth()
            loadSavingsTarget()
            loadSavingsTargetNameForMonth(currentMonth.time)
        }
    }

    private fun updateMonthDisplay(tvMonth: TextView) {
        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        tvMonth.text = dateFormat.format(currentMonth.time)
    }

    private fun showCreateOptionsDialog() {
        val options =
            arrayOf("Create an Open Payment", "Create a Closed Payment", "Create an Income",  "Set a Saving Target")

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

                3 -> showSavingsDialog()
            }
        }
        builder.show()
    }

    private fun setupUI() {
        val tvSetSavingTarget: TextView = findViewById(R.id.tvSetSavingTarget)

        tvSetSavingTarget.setOnClickListener {
            if (currentSavingsTargetDocumentId != null) {
                // Show details of the existing savings target
                showSavingsDialog(currentSavingsTargetDocumentId)
            } else {
                Toast.makeText(this, "No active savings target to display.", Toast.LENGTH_SHORT).show()
            }
        }

        // Load and display target names for the current month on startup
        loadSavingsTargetNameForMonth(currentMonth.time)

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

    // Separate function to retrieve and return the target names for a given month
    private fun fetchSavingsTargetNamesForMonth(
        selectedMonth: Date,
        callback: (List<String>) -> Unit
    ) {
        val userUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val dateFormat = SimpleDateFormat("MM-yyyy", Locale.getDefault())
        val selectedMonthString = dateFormat.format(selectedMonth)

        db.collection("users").document(userUid)
            .collection("savings_targets")
            .whereLessThanOrEqualTo("startDate", com.google.firebase.Timestamp(selectedMonth))
            .whereGreaterThanOrEqualTo("endDate", com.google.firebase.Timestamp(selectedMonth))
            .get()
            .addOnSuccessListener { documents ->
                val targetNames = documents.mapNotNull { it.getString("targetName") }
                callback(targetNames)
            }
            .addOnFailureListener {
                callback(emptyList()) // In case of failure, return an empty list
            }
    }

    // function to load and display target names for a given month
    private fun loadSavingsTargetNameForMonth(selectedMonth: Date) {
        fetchSavingsTargetNamesForMonth(selectedMonth) { targetNames ->
            val tvSetSavingTarget = findViewById<TextView>(R.id.tvSetSavingTarget)
            tvSetSavingTarget.text = if (targetNames.isEmpty()) "Saving Target" else targetNames.joinToString(", ")
        }
    }


    private fun setupSavingsVisibility() {
        val etMonthlySavings: TextView = findViewById(R.id.etMonthlySavings)
        val tvTargetAchieved: TextView = findViewById(R.id.tvTargetAchieved)
        val tvProgressPercentage: TextView = findViewById(R.id.tvProgressPercentage)
        val progressBarSavings: LinearProgressIndicator = findViewById(R.id.progressBarSavings)

        // Helper function to update visibility based on current value
        fun updateVisibility() {
            val savingsAmount = etMonthlySavings.text.toString().toFloatOrNull() ?: 0f
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

        // Initial check to set visibility based on the initial value of etMonthlySavings
        updateVisibility()

        // Add a TextWatcher to listen for changes in etMonthlySavings
        etMonthlySavings.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Update visibility whenever the value changes
                updateVisibility()
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun validateMandatoryFields(
        targetAmountEditText: EditText,
        startDateEditText: EditText,
        endDateEditText: EditText
    ): Boolean {
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
            val adjustedProgress = progress.coerceIn(0f, 100f) // Ensure progress is between 0% and 100%
            progressBarSavings?.let {
                it.setProgressCompat(adjustedProgress.toInt(), true)
            }
            // Update the percentage text
            tvProgressPercentage.text = String.format("%.0f%%", adjustedProgress)
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

    private fun showSavingsDialog(documentId: String? = null) {
        // Inflate the custom layout for the savings dialog
        val dialogView = layoutInflater.inflate(R.layout.dialog_box_savings, null)
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setView(dialogView)
            .setCancelable(true)

        val dialog = dialogBuilder.create()
        dialog.show()

        // Find views
        val targetAmountEditText = dialogView.findViewById<EditText>(R.id.edtTargetAmount)
        val startDateEditText = dialogView.findViewById<EditText>(R.id.edtStartDateSavings)
        val endDateEditText = dialogView.findViewById<EditText>(R.id.edtEndDateSavings)
        val monthlySavingsEditText = dialogView.findViewById<EditText>(R.id.edtMonthlySavings)
        val targetNameSpinner = dialogView.findViewById<Spinner>(R.id.spinnerTargetName)
        val btnSaveTarget = dialogView.findViewById<Button>(R.id.btnSaveTargetSavings)
        val imgEditTargetSavings = dialogView.findViewById<ImageView>(R.id.imgEditTargetSavings)
        val imgDeleteTargetSavings = dialogView.findViewById<ImageView>(R.id.imgDeleteTargetSavings)

        // Initialize Spinner Adapter
        val targetNames = listOf("Vacation", "New Car", "Emergency Fund", "Home Down Payment", "Car Purchase", "Education Fund", "Retirement", "Wedding", "Investment", "No specific reason")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, targetNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        targetNameSpinner.adapter = adapter

        // Date format for displaying in EditText
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        // Load target data if viewing an existing target
        if (documentId != null) {
            db.collection("users").document(userUid!!)
                .collection("savings_targets")
                .document(documentId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        // Set the dialog fields with the document data
                        targetAmountEditText.setText(document.getDouble("targetAmount")?.toString())

                        // Format and set start and end dates
                        val startDate = document.getTimestamp("startDate")?.toDate()
                        val endDate = document.getTimestamp("endDate")?.toDate()
                        startDateEditText.setText(startDate?.let { dateFormat.format(it) } ?: "")
                        endDateEditText.setText(endDate?.let { dateFormat.format(it) } ?: "")

                        // Set spinner selection for target name
                        val targetName = document.getString("targetName") ?: ""
                        val position = adapter.getPosition(targetName)
                        if (position >= 0) {
                            targetNameSpinner.setSelection(position)
                        }

                        // Update monthly savings if applicable
                        updateMonthlySavingsFromFields(targetAmountEditText, startDateEditText, endDateEditText, monthlySavingsEditText)

                        // Disable fields for view-only mode initially
                        targetAmountEditText.isEnabled = false
                        startDateEditText.isEnabled = false
                        endDateEditText.isEnabled = false
                        targetNameSpinner.isEnabled = false
                        btnSaveTarget.visibility = View.GONE
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to load savings target.", Toast.LENGTH_SHORT).show()
                }
        }

        // TextWatcher for calculating monthly savings and updating progress
        targetAmountEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateMonthlySavingsFromFields(targetAmountEditText, startDateEditText, endDateEditText, monthlySavingsEditText)
                updateProgressBar(targetAmountEditText.text.toString().toFloatOrNull() ?: 0f)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // DatePicker for start and end date selection
        startDateEditText.setOnClickListener { showDatePickerDialog(startDateEditText) }
        endDateEditText.setOnClickListener { showDatePickerDialog(endDateEditText) }

        // Set click listeners for edit and delete actions
        imgEditTargetSavings.setOnClickListener {
            if (documentId != null) {
                // Enable fields for editing mode
                targetAmountEditText.isEnabled = true
                startDateEditText.isEnabled = true
                endDateEditText.isEnabled = true
                targetNameSpinner.isEnabled = true
                btnSaveTarget.visibility = View.VISIBLE
            } else {
                Toast.makeText(this, "No target selected to edit.", Toast.LENGTH_SHORT).show()
            }
        }

        imgDeleteTargetSavings.setOnClickListener {
            if (documentId != null) {
                deleteSavingsTarget(dialog, documentId)
            } else {
                Toast.makeText(this, "No target selected to delete.", Toast.LENGTH_SHORT).show()
            }
        }

        // Save button logic with validation checks
        btnSaveTarget.setOnClickListener {
            if (documentId != null) {  // Only save if we're editing an existing target
                if (validateMandatoryFields(targetAmountEditText, startDateEditText, endDateEditText)) {
                    val targetAmount = targetAmountEditText.text.toString().toFloatOrNull()
                    val startDate = getDateFromEditText(startDateEditText)?.time
                    val endDate = getDateFromEditText(endDateEditText)?.time
                    val targetName = targetNameSpinner.selectedItem.toString()

                    if (targetAmount != null && startDate != null && endDate != null) {
                        editSavingsTarget(targetAmountEditText, targetNameSpinner, dialog, documentId)
                    } else {
                        Toast.makeText(this, "Please fill out all required fields correctly.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "New target creation is not allowed here. Use the '+' button.", Toast.LENGTH_SHORT).show()
            }
        }

        // Close dialog button
        dialogView.findViewById<Button>(R.id.btnCloseDialogSavings).setOnClickListener {
            dialog.dismiss()
        }
    }

    // Method to calculate the number of months between two dates, rounding up for partial months
    private fun getMonthsBetweenDates(startDate: Calendar, endDate: Calendar): Int {
        // Calculate the difference in years and months
        val yearsDifference = endDate.get(Calendar.YEAR) - startDate.get(Calendar.YEAR)
        val monthsDifference = endDate.get(Calendar.MONTH) - startDate.get(Calendar.MONTH)

        // Total months between the years, adding the months difference
        var totalMonths = yearsDifference * 12 + monthsDifference

        // Check if there's any partial month by day, round up if needed
        if (endDate.get(Calendar.DAY_OF_MONTH) > startDate.get(Calendar.DAY_OF_MONTH)) {
            totalMonths += 1
        }

        // Ensure at least 1 month for cases where the dates are in the same month
        return maxOf(totalMonths, 1)
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
    private fun saveSavings(targetAmount: Float, startDate: Date, endDate: Date, targetName: String) {
        val userUid = FirebaseAuth.getInstance().currentUser?.uid

        if (userUid != null) {
            val timestampStart = com.google.firebase.Timestamp(startDate)
            val timestampEnd = com.google.firebase.Timestamp(endDate)
            val dateFormat = SimpleDateFormat("MM-yyyy", Locale.getDefault())
            val startMonth = dateFormat.format(startDate)
            val endMonth = dateFormat.format(endDate)

            val savingsData = hashMapOf(
                "targetAmount" to targetAmount,
                "startDate" to timestampStart,
                "endDate" to timestampEnd,
                "startMonth" to startMonth,
                "endMonth" to endMonth,
                "targetName" to targetName,
                "userUid" to userUid
            )

            // Save each target to its unique document within the 'savings_targets' collection
            val docRef = db.collection("users")
                .document(userUid!!)
                .collection("savings_targets")
                .document() // Automatically generate a unique document ID

            // Add the generated document ID to the savings data for reference
            savingsData["documentId"] = docRef.id

            docRef.set(savingsData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Savings target saved successfully!", Toast.LENGTH_SHORT)
                        .show()
                    // Pass the generated document ID to distributeSavingsAcrossMonths
                    distributeSavingsAcrossMonths(startDate, endDate, targetAmount, docRef.id)
                    loadSavingsTarget() // Refresh the target list
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error saving target: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
        } else {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show()
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
        val dateFormat = SimpleDateFormat("MM-yyyy", Locale.getDefault())

        for (i in 0 until months) {
            val targetMonth = startCalendar.clone() as Calendar
            targetMonth.add(Calendar.MONTH, i)
            val monthString = dateFormat.format(targetMonth.time)

            db.collection("users").document(userUid!!).collection("savings_targets")
                .document(savingsId).collection("monthly_savings").document(monthString)
                .set(hashMapOf(
                    "month" to monthString,
                    "monthlyAmount" to monthlyAmount,
                    "savingsId" to savingsId
                ))
                .addOnSuccessListener {
                    Toast.makeText(this, "Monthly savings saved for $monthString", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error saving monthly savings: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
    private fun editSavingsTarget(
        targetAmountEditText: EditText,
        targetNameSpinner: Spinner,
        dialog: Dialog,
        documentId: String
    ) {
        val userUid = FirebaseAuth.getInstance().currentUser?.uid
        val targetAmount = targetAmountEditText.text.toString().toFloatOrNull()
        val targetName = targetNameSpinner.selectedItem.toString()

        if (userUid != null && targetAmount != null) {
            // Assuming that we have a unique document ID for the target being edited
            val updatedData = mapOf(
                "targetAmount" to targetAmount,
                "targetName" to targetName
            )

            db.collection("users").document(userUid)
                .collection("savings_targets")
                .document(documentId)
                .update(updatedData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Savings target updated!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    loadSavingsTarget() // Refresh the target list
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error updating target: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "Please enter a valid amount.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteSavingsTarget(dialog: Dialog, documentId: String) {
        val userUid = FirebaseAuth.getInstance().currentUser?.uid

        if (userUid != null) {
            db.collection("users").document(userUid)
                .collection("savings_targets")
                .document(documentId)
                .delete()
                .addOnSuccessListener {
                    Toast.makeText(this, "Savings target deleted successfully.", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    loadSavingsTarget() // Refresh the target list
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to delete savings target: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show()
        }
    }

    // Method to load and display the savings target for the current month
    private fun loadSavingsTarget() {
        val userUid = FirebaseAuth.getInstance().currentUser?.uid
        val dateFormat = SimpleDateFormat("MM-yyyy", Locale.getDefault())
        val currentMonthString = dateFormat.format(currentMonth.time)
        val currentTimestamp = com.google.firebase.Timestamp(currentMonth.time)

        if (userUid != null) {
            db.collection("users").document(userUid)
                .collection("savings_targets")
                .whereGreaterThanOrEqualTo("endDate", currentTimestamp)
                .get()
                .addOnSuccessListener { documents ->
                    val document = documents.firstOrNull() // Get the first document, if any

                    if (document == null) {
                        etMonthlySavingsMain.text = "0.00"
                        findViewById<TextView>(R.id.tvSetSavingTarget).text = "Saving Target"
                    } else {
                        val startDate = document.getTimestamp("startDate") ?: return@addOnSuccessListener
                        val endDate = document.getTimestamp("endDate") ?: return@addOnSuccessListener
                        val targetAmount = document.getDouble("targetAmount")?.toFloat() ?: 0f
                        val targetName = document.getString("targetName") ?: "Unnamed Target"

                        // Store the document ID to retrieve details later
                        currentSavingsTargetDocumentId = document.id

                        // Check if the target is active for this month
                        if (startDate <= currentTimestamp && endDate >= currentTimestamp) {
                            val monthsDifference = calculateMonthsDifference(startDate.toDate(), endDate.toDate())
                            val calculatedMonthlySavings = targetAmount / monthsDifference

                            // Display total monthly savings and target name
                            etMonthlySavingsMain.text = String.format("%.2f", calculatedMonthlySavings)
                            findViewById<TextView>(R.id.tvSetSavingTarget).text = targetName
                            updateProgressBar(calculatedMonthlySavings)
                            Toast.makeText(this, "Monthly savings loaded: $calculatedMonthlySavings", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error loading savings targets for $currentMonthString: ${e.message}", Toast.LENGTH_SHORT).show()
                    etMonthlySavingsMain.text = "0.00"
                    findViewById<TextView>(R.id.tvSetSavingTarget).text = "Set Saving Target"
                    currentSavingsTargetDocumentId = null
                }
        } else {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
        }
    }

    // Helper function to check if a month is within a start and end month range
    private fun isWithinDateRange(startMonth: String, endMonth: String, currentMonth: String): Boolean {
        val dateFormat = SimpleDateFormat("MM-yyyy", Locale.getDefault())
        val start = dateFormat.parse(startMonth)
        val end = dateFormat.parse(endMonth)
        val current = dateFormat.parse(currentMonth)

        return current in start..end
    }
    private fun parseFormattedNumber(numberString: String?): Double {
        return try {
            val format = NumberFormat.getInstance(Locale.US) // Use explicit locale (e.g., Locale.US)
            format.parse(numberString)?.toDouble() ?: 0.0
        } catch (e: Exception) {
            e.printStackTrace()
            0.0 // Return 0.0 on failure
        }
    }

    // Updated method to calculate months difference between two Date objects
    private fun calculateMonthsDifference(startDate: Date?, endDate: Date?): Int {
        if (startDate == null || endDate == null) return 0

        val startMonth = Calendar.getInstance().apply { time = startDate }
        val endMonth = Calendar.getInstance().apply { time = endDate }

        val yearsDifference = endMonth.get(Calendar.YEAR) - startMonth.get(Calendar.YEAR)
        val monthsDifference = endMonth.get(Calendar.MONTH) - startMonth.get(Calendar.MONTH)

        return yearsDifference * 12 + monthsDifference + 1
    }

    private fun logoutUser() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, LogIn::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}