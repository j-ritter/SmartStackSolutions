package com.example.smartstackbills

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainMenu : AppCompatActivity() {
    private var userEmail: String? = null
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
    private lateinit var dialog: Dialog
    private lateinit var etMonthlySavingsMain: EditText


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main_menu)

        fabMainMenu = findViewById(R.id.fabMainMenu)
        fabMainMenu.setOnClickListener {
            showCreateOptionsDialog()
        }

        userEmail = intent.getStringExtra("USER_EMAIL")

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
        val entriesForMonth = getEntriesForCurrentMonth()

        // Split the entries into bills and income for separate totals
        val totalBills = entriesForMonth.filterIsInstance<Bills>().sumOf { it.amount.toDouble() }.toFloat()
        val totalSpendings = entriesForMonth.filterIsInstance<Spendings>().sumOf { it.amount.toDouble() }.toFloat()
        val totalIncome = entriesForMonth.filterIsInstance<Income>().sumOf { it.amount.toDouble() }.toFloat()

        // Calculate the incoming bills based on the provided conditions
        val selectedMonthFormat = SimpleDateFormat("MM/yyyy", Locale.getDefault())
        val selectedMonth = selectedMonthFormat.format(currentMonth.time)
        val currentDate = Date()
        val totalIncoming = getBills().filter { bill ->
            val billDate = bill.date?.toDate()
            // Check if the bill date is after the current date, in the selected month, unpaid, and non-recurring
            billDate != null && billDate.after(currentDate) && !bill.paid && bill.repeat == "No" &&
                    selectedMonthFormat.format(billDate) == selectedMonth
        }.sumOf { it.amount.toDouble() }.toFloat()

        // Calculate the overdue bills based on the provided conditions

        val totalOverdue = getBills().filter { bill ->
            val billDate = bill.date?.toDate()
            // Check if the bill date is before the current date, unpaid, non-recurring, and belongs to the selected month
            billDate != null && billDate.before(currentDate) && !bill.paid && bill.repeat == "No" &&
                    selectedMonthFormat.format(billDate) == selectedMonth
        }.sumOf { it.amount.toDouble() }.toFloat()

        // Calculate essential and nonessential spendings
        val essentialCategories = listOf(
            "Accommodation", "Communication", "Insurance", "Transportation", "Finances/Fees", "Taxes", "Health", "Education", "Shopping & Consumption")
        val essentialSubcategories = listOf( "Rent", "Utilities", "Groceries - Basic Food", "Groceries - Household Necessities",
            "Mobile phone", "Landline phone", "Internet", "Health insurance", "Life insurance",
            "Car insurance", "Home insurance", "Fuel", "Vehicle maintenance",
            "Public transportation", "Doctor visits", "Dental care", "Prescription medications",
            "Medical equipment", "Clothing", "Household goods", "Personal care products",
            "Income tax", "Property tax", "Sales tax", "Self-employment tax", "Capital gains tax",
            "Tuition fees", "Textbooks", "School supplies")
        val totalEssential = getSpendings().filter { spending ->
            val category = spending.category
            val subcategory = spending.subcategory

            // Only consider spendings for the selected month
            val spendingDate = spending.date?.toDate()
            val isInSelectedMonth = spendingDate != null && selectedMonthFormat.format(spendingDate) == selectedMonth

            // Essential if:
            // 1. No category or subcategory (default to essential)
            // 2. The subcategory is in essentialSubcategories
            // 3. If no subcategory, the category is in essentialCategories
            isInSelectedMonth && (
                    category == null && subcategory == null ||
                            subcategory in essentialSubcategories ||
                            (subcategory == null && category in essentialCategories)
                    )
        }.sumOf { it.amount.toDouble() }.toFloat()

        val nonEssentialCategories = listOf(
            "Subscription and Memberships", "Others")
        val nonEssentialSubcategories = listOf("Entertainment", "Dining out", "Hobbies", "Streaming services", "Movies",
            "Music concerts", "Video games", "Sports", "Vacation", "Gadgets",
            "Luxury items", "Alcohol", "Tobacco", "Gym memberships",
            "Groceries - Beverages", "Groceries - Luxury Foods", "Decorations", "Jewelry")
        val totalNonEssential = getSpendings().filter { spending ->
            val category = spending.category
            val subcategory = spending.subcategory

            // Only consider spendings for the selected month
            val spendingDate = spending.date?.toDate()
            val isInSelectedMonth = spendingDate != null && selectedMonthFormat.format(spendingDate) == selectedMonth

            // Non-essential if:
            // 1. The subcategory is in nonEssentialSubcategories
            // 2. If no subcategory, the category is in nonEssentialCategories
            isInSelectedMonth && (
                    subcategory in nonEssentialSubcategories ||
                            (subcategory == null && category in nonEssentialCategories)
                    )
        }.sumOf { it.amount.toDouble() }.toFloat()


        // Calculate income
        val totalRecurringIncome = getIncome().filter { income ->
            income.repeat != "No" && selectedMonthFormat.format(income.date?.toDate()) == selectedMonth
        }.sumOf { it.amount.toDouble() }.toFloat()

        val totalOneTimeIncome = getIncome().filter { income ->
            income.repeat == "No" && selectedMonthFormat.format(income.date?.toDate()) == selectedMonth
        }.sumOf { it.amount.toDouble() }.toFloat()

        // Ensure that the items are displayed as negative
        val displayBillsAmount = -totalBills
        val displayIncomingAmount = -totalIncoming
        val displayOverdueAmount = -totalOverdue
        val displaySpendingsAmount = -totalSpendings
        val displayEssentialAmount = -totalEssential
        val displayNonEssentialAmount = -totalNonEssential
        val displayIncomeAmount = +totalIncome
        val displayRecurringAmount = +totalRecurringIncome
        val displayOneTimeAmount = +totalOneTimeIncome

        // Calculate the total
        val total = totalIncome + displayBillsAmount + displaySpendingsAmount

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

        btnPreviousMonth.setOnClickListener {
            currentMonth.add(Calendar.MONTH, -1)
            updateMonthDisplay(tvMonth)
            setAmountForMonth()
        }

        btnNextMonth.setOnClickListener {
            currentMonth.add(Calendar.MONTH, 1)
            updateMonthDisplay(tvMonth)
            setAmountForMonth()
        }
    }

    private fun updateMonthDisplay(tvMonth: TextView) {
        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        tvMonth.text = dateFormat.format(currentMonth.time)
    }

    private fun showCreateOptionsDialog() {
        val options = arrayOf("Create an Open Payment", "Create a Closed Payment", "Create an Income")

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
    private fun setupUI(){
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

    private fun updateProgressBar(targetAmount: Float) {
        val savedAmountSoFar = etTotalAmount.text.toString().toFloatOrNull() ?: 0f
        val progressBarSavings = findViewById<com.google.android.material.progressindicator.LinearProgressIndicator>(R.id.progressBarSavings)
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
    // Method to display the savings dialog
    fun openSavingsDialog(view: View) {
        showSavingsDialog()
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
        val targetPeriodSpinner = dialogView.findViewById<Spinner>(R.id.spinnerTargetPeriod)
        val monthlySavingsEditText = dialogView.findViewById<EditText>(R.id.edtMonthlySavings)
        val targetNameSpinner = dialogView.findViewById<Spinner>(R.id.spinnerTargetName)
        val btnSaveTarget = dialogView.findViewById<Button>(R.id.btnSaveTargetSavings)
        val btnCloseDialog = dialogView.findViewById<Button>(R.id.btnCloseDialogSavings)
        val progressBarSavings = dialogView.findViewById<com.google.android.material.progressindicator.LinearProgressIndicator>(R.id.progressBarSavings)


        // Find the etTotal, which represents the savedAmountSoFar
        val etTotal = findViewById<EditText>(R.id.etTotal)
        val savedAmountSoFar = etTotal.text.toString().toFloatOrNull() ?: 0f

        // Load saved values from SharedPreferences
        val sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val savedTargetName = sharedPref.getString("savingsTargetName", "")
        val savedTargetAmount = sharedPref.getFloat("savingsTargetAmount", 0f)
        val savedTargetPeriod = sharedPref.getString("savingsTargetPeriod", "")

        // Set the saved values in the dialog's input fields
        targetAmountEditText.setText(String.format("%.2f", savedTargetAmount))

        // Set saved target name in the spinner (if spinner contains the saved target name)
        if (!savedTargetName.isNullOrEmpty()) {
            val namePosition = (targetNameSpinner.adapter as ArrayAdapter<String>).getPosition(savedTargetName)
            targetNameSpinner.setSelection(namePosition)
        }

        // Set the saved target period in the spinner
        if (!savedTargetPeriod.isNullOrEmpty()) {
            val periodPosition = (targetPeriodSpinner.adapter as ArrayAdapter<String>).getPosition(savedTargetPeriod)
            targetPeriodSpinner.setSelection(periodPosition)
        }

        // Initial progress update
        updateProgressBar(savedTargetAmount)

        // Function to calculate and set the monthly savings
        fun calculateMonthlySavings() {
            val targetAmount = targetAmountEditText.text.toString().toFloatOrNull()
            val selectedPeriod = targetPeriodSpinner.selectedItem.toString()

            val months = when (selectedPeriod) {
                "1 Month" -> 1
                "2 Months" -> 2
                "3 Months" -> 3
                "6 Months" -> 6
                "12 Months" -> 12
                else -> 1 // Default to 1 month if not selected
            }

            // Calculate and set the monthly savings if target amount is valid
            if (targetAmount != null && months > 0) {
                val monthlySavings = targetAmount / months
                monthlySavingsEditText.setText(String.format("%.2f", monthlySavings))
                // Set the monthly savings in MainMenu's etMonthlySavings field
                etMonthlySavingsMain.setText(String.format("%.2f", monthlySavings))
                // Update the progress bar when the target amount is changed
                progressBarSavings?.let {
                    val progress = (savedAmountSoFar / targetAmount) * 100
                    it.setProgressCompat(progress.toInt().coerceAtMost(100), true)
                }
                // Update progress bar and progress percentage
                updateProgressBar(targetAmount)
            }
        }

        // Add TextWatcher to update monthly savings on text change
        targetAmountEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                calculateMonthlySavings()
                updateProgressBar(targetAmountEditText.text.toString().toFloatOrNull() ?: 0f)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        targetPeriodSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                calculateMonthlySavings()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Save button logic
        btnSaveTarget.setOnClickListener {
            val targetAmount = targetAmountEditText.text.toString().toFloatOrNull()
            val targetPeriod = targetPeriodSpinner.selectedItem.toString()

            // Validation for required fields
            if (targetAmount == null || targetAmount <= 0f) {
                Toast.makeText(this, "Please enter a valid target amount", Toast.LENGTH_SHORT).show()
            } else if (targetPeriod.isEmpty() || targetPeriod == "Select Target Period") {
                Toast.makeText(this, "Please select a target period", Toast.LENGTH_SHORT).show()
            } else {
                // Save target data if all fields are valid
                saveSavingsTarget(targetPeriod, targetAmount, targetPeriod)

                // Update progress after saving the new target
                progressBarSavings?.let {
                    val progress = (savedAmountSoFar / targetAmount) * 100
                    it.setProgressCompat(progress.toInt().coerceAtMost(100), true)
                }

                dialog.dismiss() // Close the dialog
            }
        }

        // Close button logic
        btnCloseDialog.setOnClickListener {
            dialog.dismiss() // Simply close the dialog
        }
    }

    // saving logic
    private fun saveSavingsTarget(targetName: String, targetAmount: Float, targetPeriod: String) {
        // Get the SharedPreferences editor
        val sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()

        // Save the target data
        editor.putString("savingsTargetName", targetName)
        editor.putFloat("savingsTargetAmount", targetAmount)
        editor.putString("savingsTargetPeriod", targetPeriod)

        // Commit the changes
        editor.apply()

        // Notify the user that the target was saved successfully
        Toast.makeText(this, "Savings target saved successfully!", Toast.LENGTH_SHORT).show()
    }
    private fun loadSavingsTarget() {
        val sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val targetAmount = sharedPref.getFloat("savingsTargetAmount", 0f)
        val savedTargetPeriod = sharedPref.getString("savingsTargetPeriod", "")

        // Calculate monthly savings
        val months = when (savedTargetPeriod) {
            "1 Month" -> 1
            "2 Months" -> 2
            "3 Months" -> 3
            "6 Months" -> 6
            "12 Months" -> 12
            else -> 1
        }

        val monthlySavings = if (months > 0) targetAmount / months else 0f
        etMonthlySavingsMain.setText(String.format("%.2f", monthlySavings))

        // Update the progress bar with the saved target amount
        updateProgressBar(targetAmount)

    }


    private fun logoutUser() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, LogIn::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
