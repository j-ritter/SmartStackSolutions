package com.ritter.smartstackbills

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.View
import android.view.WindowMetrics
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
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
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
    private lateinit var adView: AdView
    private var currentSavingsTargetDocumentId: String? = null
    private val numberFormat: NumberFormat = NumberFormat.getInstance(Locale.getDefault())

    private val adSize: AdSize
        get() {
            val displayMetrics = resources.displayMetrics
            val adWidthPixels = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val windowMetrics: WindowMetrics = this.windowManager.currentWindowMetrics
                windowMetrics.bounds.width()
            } else {
                displayMetrics.widthPixels
            }
            val density = displayMetrics.density
            val adWidth = (adWidthPixels / density).toInt()
            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main_menu)

        MobileAds.initialize(this) {}

        // Configure test devices
        val testDeviceIds = listOf(
            AdRequest.DEVICE_ID_EMULATOR, // Emulator
            "YOUR_DEVICE_HASH"            // Replace with your physical device's hash if testing
        )
        val requestConfiguration = RequestConfiguration.Builder()
            .setTestDeviceIds(testDeviceIds)
            .build()
        MobileAds.setRequestConfiguration(requestConfiguration)

        // Create and load the AdView
        setupBannerAd()

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
        etIncomeAmount = findViewById(R.id.etIncomeAmount)
        etIncomingAmount = findViewById(R.id.etIncomingAmount)
        etOverdueAmount = findViewById(R.id.etOverdueAmount)
        etEssentialAmount = findViewById(R.id.etEssentialAmount)
        etNonEssentialAmount = findViewById(R.id.etNonEssentialAmount)
        etRecurringAmount = findViewById(R.id.etRecurringAmount)
        etOneTimeAmount = findViewById(R.id.etOneTimeAmount)
        etTotalAmount = findViewById(R.id.etTotalAmount)
        etMonthlySavingsMain = findViewById(R.id.etMonthlySavings)

        setupMonthNavigation()
        setupUI()

        // Call this to load the saved target when the activity starts
        loadSavingsTarget()


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
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
        val etIncome: EditText = findViewById(R.id.etIncomeAmount)
        etIncome.setOnClickListener {
            val intent = Intent(this, MyIncome::class.java)
            intent.putExtra("FILTER_TYPE", "all")
            startActivity(intent)
        }
    }

    private fun setupBannerAd() {
        // Find the AdView from the layout
        val adView = findViewById<AdView>(R.id.adView)

        // Create an AdRequest with the test device included
        val adRequest = AdRequest.Builder()

            .build()

        // Load the ad into the AdView
        adView.loadAd(adRequest)

        // Optional: Set AdListener to handle ad events
        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                // Handle successful ad load
                Toast.makeText(this@MainMenu, "Ad Loaded Successfully", Toast.LENGTH_SHORT).show()
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                // Handle ad load failure
                Toast.makeText(this@MainMenu, "Ad Failed to Load: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun formatAmount(value: Double): String {
        val numberFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
        return if (value < 0) {
            // Handle negative values appropriately
            if (Locale.getDefault() == Locale.US) {
                "(${numberFormat.format(-value)})" // Parentheses for US locale
            } else {
                "-${numberFormat.format(-value)}" // Explicit negative sign for other locales
            }
        } else {
            numberFormat.format(value)
        }
    }

    // Method to retrieve all bills, spendings and income from SharedPreferences
    private fun getBills(): ArrayList<Bills> {
        val sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPref.getString("billsList", null)
        val type = object : TypeToken<ArrayList<Bills>>() {}.type
        val billsList: ArrayList<Bills> = gson.fromJson(json, type) ?: ArrayList()

        // Parse amounts correctly
        billsList.forEach { bill ->
            bill.amount = parseLocalizedDouble(bill.amount.toString())
        }
        return billsList
    }

    private fun getSpendings(): ArrayList<Spendings> {
        val sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPref.getString("spendingsList", null)
        val type = object : TypeToken<ArrayList<Spendings>>() {}.type
        val spendingsList: ArrayList<Spendings> = gson.fromJson(json, type) ?: ArrayList()

        // Parse amounts correctly
        spendingsList.forEach { spending ->
            spending.amount = parseLocalizedDouble(spending.amount.toString())
        }
        return spendingsList
    }

    private fun getIncome(): ArrayList<Income> {
        val sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPref.getString("incomeList", null)
        val type = object : TypeToken<ArrayList<Income>>() {}.type
        val incomeList: ArrayList<Income> = gson.fromJson(json, type) ?: ArrayList()

        // Parse amounts correctly
        incomeList.forEach { income ->
            income.amount = parseLocalizedDouble(income.amount.toString())
        }
        return incomeList
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
            bill.date?.toDate()?.let { dateFormat.format(it) == currentMonthString } == true
        }

        val spendingsForMonth = spendingsList.filter { spending ->
            spending.date?.toDate()?.let { dateFormat.format(it) == currentMonthString } == true
        }

        val incomeForMonth = incomeList.filter { income ->
            income.date?.toDate()?.let { dateFormat.format(it) == currentMonthString } == true
        }

        // Calculate totals for bills, spendings, and income
        val totalBills = billsForMonth.sumOf { it.amount }
        val totalSpendings = spendingsForMonth.sumOf { it.amount }
        val totalIncome = incomeForMonth.sumOf { it.amount }
        val totalSavings = totalIncome - totalBills - totalSpendings

        // Calculate the savings target (monthlyAmount) based on local logic
        val monthlyAmount = etMonthlySavingsMain.text.toString().toDoubleOrNull() ?: 0.0

        // Update progress bar using locally calculated values
        updateProgressBar(
            findViewById(R.id.progressBarSavings),
            findViewById(R.id.tvProgressPercentage),
            monthlyAmount,
            totalSavings
        )

        // Calculate incoming bills (unpaid, in the future)
        val currentDate = Date()
        val totalIncoming = billsList.filter { bill ->
            val billDate = bill.date?.toDate()
            billDate != null && billDate.after(currentDate) && !bill.paid &&
                    dateFormat.format(billDate) == currentMonthString
        }.sumOf { it.amount }

        // Calculate overdue bills (unpaid, in the past)
        val totalOverdue = billsList.filter { bill ->
            val billDate = bill.date?.toDate()
            billDate != null && billDate.before(currentDate) && !bill.paid &&
                    dateFormat.format(billDate) == currentMonthString
        }.sumOf { it.amount }

        // Essential and non-essential categories
        val essentialCategories = listOf("Accommodation", "Communication", "Insurance", "Transportation", "Finances/Fees", "Taxes", "Health", "Education", "Shopping & Consumption")
        val essentialSubcategories = listOf("Rent", "Utilities", "Groceries - Basic Food", "Groceries - Household Necessities", "Mobile phone", "Internet", "Health insurance", "Car insurance", "Fuel", "Public transportation")

        val totalEssential = spendingsForMonth.filter { spending ->
            val category = spending.category
            val subcategory = spending.subcategory
            spending.date?.toDate()?.let { dateFormat.format(it) == currentMonthString } == true &&
                    (subcategory in essentialSubcategories || (subcategory == null && category in essentialCategories))
        }.sumOf { it.amount }

        val nonEssentialCategories = listOf("Subscription and Memberships", "Others")
        val nonEssentialSubcategories = listOf("Entertainment", "Dining out", "Hobbies", "Streaming services", "Movies", "Vacation", "Gadgets")

        val totalNonEssential = spendingsForMonth.filter { spending ->
            val category = spending.category
            val subcategory = spending.subcategory
            spending.date?.toDate()?.let { dateFormat.format(it) == currentMonthString } == true &&
                    (subcategory in nonEssentialSubcategories || (subcategory == null && category in nonEssentialCategories))
        }.sumOf { it.amount }

        // Calculate recurring and one-time income
        val totalRecurringIncome = incomeForMonth.filter { income -> income.repeat != "No" }.sumOf { it.amount }
        val totalOneTimeIncome = incomeForMonth.filter { income -> income.repeat == "No" }.sumOf { it.amount }

        // Display calculated values
        etBillsAmount.setText(formatAmount(totalBills))
        etSpendingsAmount.setText(formatAmount(totalSpendings))
        etIncomeAmount.setText(formatAmount(totalIncome))
        etIncomingAmount.setText(formatAmount(totalIncoming))
        etOverdueAmount.setText(formatAmount(totalOverdue))
        etEssentialAmount.setText(formatAmount(totalEssential))
        etNonEssentialAmount.setText(formatAmount(totalNonEssential))
        etRecurringAmount.setText(formatAmount(totalRecurringIncome))
        etOneTimeAmount.setText(formatAmount(totalOneTimeIncome))
        etTotalAmount.setText(formatAmount(totalSavings))
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
        // Find views related to savings
        val tvSetSavingTarget: TextView = findViewById(R.id.tvSetSavingTarget)
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

        // Set initial visibility
        updateVisibility()

        // Add TextWatcher to listen for changes in etMonthlySavings
        etMonthlySavings.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateVisibility() // Adjust visibility dynamically
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Handle click on "Set Saving Target"
        tvSetSavingTarget.setOnClickListener {
            if (currentSavingsTargetDocumentId != null) {
                // Show details of the existing savings target
                showSavingsDialog(currentSavingsTargetDocumentId)
            } else {
                Toast.makeText(this, "No active savings target to display.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validateMandatoryFields(
        targetAmountEditText: EditText,
        startDateEditText: EditText,
        endDateEditText: EditText
    ): Boolean {
        val targetAmount = targetAmountEditText.text.toString().toDoubleOrNull()
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
    private fun parseLocalizedDouble(input: String): Double {
        return try {
            val numberFormat = NumberFormat.getInstance(Locale.getDefault())
            numberFormat.parse(input)?.toDouble() ?: 0.0
        } catch (e: Exception) {
            0.0 // Return 0.0 if parsing fails
        }
    }

    private fun updateProgressBar(
        progressBar: LinearProgressIndicator,
        progressTextView: TextView,
        monthlyAmount: Double,
        totalSavings: Double
    ) {
        // Calculate progress percentage
        val progress = if (monthlyAmount > 0) {
            ((totalSavings / monthlyAmount) * 100).coerceIn(0.0, 100.0) // Ensure within 0-100%
        } else {
            0.0 // No progress if monthly amount is zero
        }

        // Update the progress bar and text view
        progressBar.setProgressCompat(progress.toInt(), true)
        progressTextView.text = String.format("%.0f%%", progress)
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
            if (validateMandatoryFields(targetAmountEditText, startDateEditText, endDateEditText)) {
                val targetAmount = targetAmountEditText.text.toString().toDoubleOrNull() ?: 0.0
                val startDate = getDateFromEditText(startDateEditText)?.time
                val endDate = getDateFromEditText(endDateEditText)?.time
                val targetName = targetNameSpinner.selectedItem.toString()

                if (startDate != null && endDate != null) {
                    if (documentId == null) {
                        // New target creation
                        val monthsDifference = getMonthsBetweenDates(
                            Calendar.getInstance().apply { time = startDate },
                            Calendar.getInstance().apply { time = endDate }
                        )
                        val calculatedMonthlySavings = if (monthsDifference > 0) targetAmount / monthsDifference else 0.0
                        saveSavings(targetAmount, startDate, endDate, targetName, calculatedMonthlySavings)
                    } else {
                        // Update existing target
                        editSavingsTarget(targetAmountEditText, targetNameSpinner, dialog, documentId)
                    }
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, "Invalid dates selected.", Toast.LENGTH_SHORT).show()
                }
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
        val targetAmount = targetAmountEditText.text.toString().toDoubleOrNull() ?: 0.0
        val startDate = getDateFromEditText(startDateEditText)
        val endDate = getDateFromEditText(endDateEditText)

        if (targetAmount > 0 && startDate != null && endDate != null && startDate.before(endDate)) {
            val monthsDifference = getMonthsBetweenDates(startDate, endDate)
            val monthlySavings = targetAmount / monthsDifference
            monthlySavingsEditText.setText(String.format("%.2f", monthlySavings))
        } else {
            monthlySavingsEditText.setText("")
            Toast.makeText(this, "Invalid inputs for monthly savings calculation.", Toast.LENGTH_SHORT).show()
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
    private fun saveSavings(
        targetAmount: Double,
        startDate: Date,
        endDate: Date,
        targetName: String,
        calculatedMonthlySavings: Double
    ) {
        val userUid = FirebaseAuth.getInstance().currentUser?.uid

        if (userUid == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show()
            return
        }

        val timestampStart = com.google.firebase.Timestamp(startDate)
        val timestampEnd = com.google.firebase.Timestamp(endDate)
        val dateFormat = SimpleDateFormat("MM-yyyy", Locale.getDefault())
        val startMonth = dateFormat.format(startDate)
        val endMonth = dateFormat.format(endDate)

        db.collection("users").document(userUid)
            .collection("savings_targets")
            .get()
            .addOnSuccessListener { documents ->

                // Check for overlapping savings targets
                val isOverlap = documents.any { document ->
                    val existingStartMonth = document.getString("startMonth").orEmpty()
                    val existingEndMonth = document.getString("endMonth").orEmpty()

                    isWithinDateRange(startMonth, endMonth, existingStartMonth) ||
                            isWithinDateRange(startMonth, endMonth, existingEndMonth)
                }

                if (isOverlap) {
                    Toast.makeText(
                        this,
                        "Cannot create savings target. Overlapping time periods detected.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addOnSuccessListener
                }

                // Create the savings target
                val savingsData = hashMapOf(
                    "targetAmount" to targetAmount,
                    "startDate" to timestampStart,
                    "endDate" to timestampEnd,
                    "startMonth" to startMonth,
                    "endMonth" to endMonth,
                    "targetName" to targetName,
                    "userUid" to userUid
                )

                // Generate a unique document ID
                val docRef = db.collection("users")
                    .document(userUid)
                    .collection("savings_targets")
                    .document()

                savingsData["documentId"] = docRef.id

                // Save the data to Firestore
                docRef.set(savingsData)
                    .addOnSuccessListener {
                        Toast.makeText(
                            this,
                            "Savings target saved successfully!",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Distribute savings across months
                        distributeSavingsAcrossMonths(startDate, endDate, calculatedMonthlySavings, docRef.id)

                        // Reload savings targets to reflect the new data
                        loadSavingsTarget()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this,
                            "Error saving target: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Error checking existing savings targets: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
    // Method to load and display the savings target for the current month
    private fun loadSavingsTarget() {
        val userUid = FirebaseAuth.getInstance().currentUser?.uid
        val currentTimestamp = com.google.firebase.Timestamp(currentMonth.time)
        val currentMonthFormatted = SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(currentMonth.time)

        if (userUid != null) {
            db.collection("users").document(userUid)
                .collection("savings_targets")
                .get()
                .addOnSuccessListener { documents ->
                    // Filter documents for the current savings target
                    val filteredDocuments = documents.filter { document ->
                        val startDate = document.getTimestamp("startDate")
                        val endDate = document.getTimestamp("endDate")
                        startDate != null && endDate != null &&
                                startDate <= currentTimestamp && endDate >= currentTimestamp
                    }

                    val document = filteredDocuments.firstOrNull()

                    if (document == null) {
                        // No active savings target
                        etMonthlySavingsMain.text = "0.00"
                        findViewById<TextView>(R.id.tvSetSavingTarget).text = "Saving Target"
                        currentSavingsTargetDocumentId = null
                        updateProgressBar(
                            findViewById(R.id.progressBarSavings),
                            findViewById(R.id.tvProgressPercentage),
                            0.0,
                            0.0
                        )
                    } else {
                        // Active savings target exists
                        val targetName = document.getString("targetName") ?: "Unnamed Target"
                        val targetAmount = document.getDouble("targetAmount") ?: 0.0
                        val startDate = document.getTimestamp("startDate")?.toDate()
                        val endDate = document.getTimestamp("endDate")?.toDate()

                        if (startDate != null && endDate != null) {
                            val monthsDifference = getMonthsBetweenDates(
                                Calendar.getInstance().apply { time = startDate },
                                Calendar.getInstance().apply { time = endDate }
                            )
                            val calculatedMonthlySavings = targetAmount / monthsDifference.toDouble()
                            etMonthlySavingsMain.text = String.format("%.2f", calculatedMonthlySavings)

                            val savingsId = document.id

                            // Fetch monthly savings data for progress calculation
                            db.collection("users").document(userUid)
                                .collection("savings_targets").document(savingsId)
                                .collection("monthly_savings")
                                .document(currentMonthFormatted)
                                .get()
                                .addOnSuccessListener { monthlyDoc ->
                                    val firebaseMonthlyAmount =
                                        monthlyDoc.getDouble("monthlyAmount") ?: calculatedMonthlySavings
                                    val firebaseTotalSavings =
                                        monthlyDoc.getDouble("totalSavings") ?: 0.0

                                    updateProgressBar(
                                        findViewById(R.id.progressBarSavings),
                                        findViewById(R.id.tvProgressPercentage),
                                        firebaseMonthlyAmount,
                                        firebaseTotalSavings
                                    )

                                    // Update UI with target details
                                    findViewById<TextView>(R.id.tvSetSavingTarget).text = targetName
                                    currentSavingsTargetDocumentId = savingsId
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        this,
                                        "Error retrieving monthly savings: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    updateProgressBar(
                                        findViewById(R.id.progressBarSavings),
                                        findViewById(R.id.tvProgressPercentage),
                                        calculatedMonthlySavings,
                                        0.0
                                    )
                                }
                        } else {
                            // Handle cases where date parsing fails
                            Toast.makeText(this, "Invalid target dates.", Toast.LENGTH_SHORT).show()
                            updateProgressBar(
                                findViewById(R.id.progressBarSavings),
                                findViewById(R.id.tvProgressPercentage),
                                0.0,
                                0.0
                            )
                            etMonthlySavingsMain.text = "0.00"
                            findViewById<TextView>(R.id.tvSetSavingTarget).text = "Saving Target"
                            currentSavingsTargetDocumentId = null
                        }
                    }
                }
                .addOnFailureListener { e ->
                    // Handle errors during document retrieval
                    Toast.makeText(this, "Error loading savings target: ${e.message}", Toast.LENGTH_SHORT).show()
                    etMonthlySavingsMain.text = "0.00"
                    findViewById<TextView>(R.id.tvSetSavingTarget).text = "Set Saving Target"
                    updateProgressBar(
                        findViewById(R.id.progressBarSavings),
                        findViewById(R.id.tvProgressPercentage),
                        0.0,
                        0.0
                    )
                    currentSavingsTargetDocumentId = null
                }
        } else {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
            updateProgressBar(
                findViewById(R.id.progressBarSavings),
                findViewById(R.id.tvProgressPercentage),
                0.0,
                0.0
            )
        }
    }

    // Method to distribute savings across months and save them in Firebase
    private fun distributeSavingsAcrossMonths(
        startDate: Date,
        endDate: Date,
        monthlySavings: Double, // Accept the pre-calculated monthly savings
        savingsId: String
    ) {
        val startCalendar = Calendar.getInstance()
        startCalendar.time = startDate

        val endCalendar = Calendar.getInstance()
        endCalendar.time = endDate

        val months = getMonthsBetweenDates(startCalendar, endCalendar)
        val dateFormat = SimpleDateFormat("MM-yyyy", Locale.getDefault())

        for (i in 0 until months) {
            val targetMonth = startCalendar.clone() as Calendar
            targetMonth.add(Calendar.MONTH, i)
            val monthString = dateFormat.format(targetMonth.time)

            // Save monthly savings data for each month within the target period
            db.collection("users").document(userUid!!).collection("savings_targets")
                .document(savingsId).collection("monthly_savings").document(monthString)
                .set(
                    hashMapOf(
                        "month" to monthString,
                        "monthlyAmount" to monthlySavings,
                        "savingsId" to savingsId
                    )
                )
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
        val targetAmount = targetAmountEditText.text.toString().toDoubleOrNull()
        val targetName = targetNameSpinner.selectedItem.toString()

        // Ensure Firebase is updated with all relevant fields, including dates
        val startDate = getDateFromEditText(dialog.findViewById<EditText>(R.id.edtStartDateSavings))?.time
        val endDate = getDateFromEditText(dialog.findViewById<EditText>(R.id.edtEndDateSavings))?.time

        if (userUid != null && targetAmount != null && startDate != null && endDate != null) {
            val timestampStart = com.google.firebase.Timestamp(startDate)
            val timestampEnd = com.google.firebase.Timestamp(endDate)
            val dateFormat = SimpleDateFormat("MM-yyyy", Locale.getDefault())
            val startMonth = dateFormat.format(startDate)
            val endMonth = dateFormat.format(endDate)

            db.collection("users").document(userUid)
                .collection("savings_targets")
                .get()
                .addOnSuccessListener { documents ->
                    // Check for overlapping savings targets, excluding the current one being edited
                    val isOverlap = documents.any { document ->
                        val existingStartMonth = document.getString("startMonth")
                        val existingEndMonth = document.getString("endMonth")
                        val existingDocumentId = document.id

                        existingDocumentId != documentId && // Exclude the current savings target
                                existingStartMonth != null && existingEndMonth != null &&
                                (isWithinDateRange(startMonth, endMonth, existingStartMonth) ||
                                        isWithinDateRange(startMonth, endMonth, existingEndMonth))
                    }

                    if (isOverlap) {
                        // Show error message for overlapping dates
                        Toast.makeText(
                            this,
                            "Cannot update savings target. Overlapping time periods detected.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        // Update the savings target
                        val updatedData = mapOf(
                            "targetAmount" to targetAmount,
                            "targetName" to targetName,
                            "startDate" to timestampStart,
                            "endDate" to timestampEnd,
                            "startMonth" to startMonth,
                            "endMonth" to endMonth
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
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        "Error checking existing savings targets: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } else {
            Toast.makeText(this, "Please enter valid inputs.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteSavingsTarget(dialog: Dialog, documentId: String) {
        val userUid = FirebaseAuth.getInstance().currentUser?.uid

        if (userUid != null) {
            // Delete nested collections first
            val targetRef = db.collection("users").document(userUid)
                .collection("savings_targets").document(documentId)

            targetRef.collection("monthly_savings")
                .get()
                .addOnSuccessListener { documents ->
                    for (doc in documents) {
                        doc.reference.delete()
                    }
                    // After deleting nested collections, delete the main document
                    targetRef.delete()
                        .addOnSuccessListener {
                            Toast.makeText(this, "Savings target deleted successfully.", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            loadSavingsTarget() // Refresh the target list
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to delete savings target: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to retrieve associated data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show()
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

    private fun logoutUser() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, LogIn::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
