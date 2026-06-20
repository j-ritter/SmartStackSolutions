package com.ritter.smartstackbills

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.View
import android.view.ViewGroup
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
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
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
    private var billsListenerRegistration: ListenerRegistration? = null
    private var spendingsListenerRegistration: ListenerRegistration? = null
    private var incomeListenerRegistration: ListenerRegistration? = null
    private val billsList = arrayListOf<Bills>()
    private val spendingsList = arrayListOf<Spendings>()
    private val incomeList = arrayListOf<Income>()
    private var billsLoaded = false
    private var spendingsLoaded = false
    private var incomeLoaded = false
    private var currentActualMonthlySavings = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main_menu)
        PremiumPurchaseVerifier.refresh(this)

        MobileAds.initialize(this) {}

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            startActivity(AuthUtils.loginIntent(this))
            finish()
            return
        }

        userEmail = intent.getStringExtra(AuthUtils.EXTRA_USER_EMAIL) ?: currentUser.email
        userUid = currentUser.uid

        // Create and load the AdView
        setupBannerAd()

        db = FirebaseFirestore.getInstance()

        fabMainMenu = findViewById(R.id.fabMainMenu)
        fabMainMenu.setOnClickListener {
            showCreateOptionsDialog()
        }

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
        setupDashboardNavigation()
        setupDashboardListeners()


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
                    val intent = Intent(this, CalendarActivity::class.java)
                    intent.putExtra(AuthUtils.EXTRA_USER_EMAIL, userEmail)
                    startActivity(intent)
                    true
                }

                else -> false
            }
        }

        DrawerNavigation.setup(this, drawerLayout, findViewById(R.id.nav_view))

    }

    private fun setupDashboardNavigation() {
        findViewById<View>(R.id.openPaymentsCard).setOnClickListener {
            openDashboardSection(MyBills::class.java, "all")
        }
        findViewById<View>(R.id.incomingPaymentsMetric).setOnClickListener {
            openDashboardSection(MyBills::class.java, "incoming")
        }
        findViewById<View>(R.id.overduePaymentsMetric).setOnClickListener {
            openDashboardSection(MyBills::class.java, "overdue")
        }
        findViewById<View>(R.id.closedPaymentsCard).setOnClickListener {
            openDashboardSection(MySpendings::class.java, "all")
        }
        findViewById<View>(R.id.essentialPaymentsMetric).setOnClickListener {
            openDashboardSection(MySpendings::class.java, "essential")
        }
        findViewById<View>(R.id.nonEssentialPaymentsMetric).setOnClickListener {
            openDashboardSection(MySpendings::class.java, "non-essential")
        }
        findViewById<View>(R.id.incomeCard).setOnClickListener {
            openDashboardSection(MyIncome::class.java, "all")
        }
        findViewById<View>(R.id.recurringIncomeMetric).setOnClickListener {
            openDashboardSection(MyIncome::class.java, "recurring")
        }
        findViewById<View>(R.id.oneTimeIncomeMetric).setOnClickListener {
            openDashboardSection(MyIncome::class.java, "one-time")
        }
        findViewById<View>(R.id.savingsTargetCard).setOnClickListener {
            openSavingsTarget()
        }
    }

    private fun openDashboardSection(destination: Class<*>, filter: String) {
        startActivity(Intent(this, destination).apply {
            putExtra("FILTER_TYPE", filter)
            putExtra(AuthUtils.EXTRA_USER_EMAIL, userEmail)
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        billsListenerRegistration?.remove()
        spendingsListenerRegistration?.remove()
        incomeListenerRegistration?.remove()
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

            }

            override fun onAdFailedToLoad(error: LoadAdError) {

            }
        }
    }

    private fun formatAmount(value: Double): String {
        return CurrencyPreferences.format(this, value)
    }

    private fun setupDashboardListeners() {
        val uid = userUid
        if (uid == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        val userRef = db.collection("users").document(uid)

        billsListenerRegistration = userRef.collection("bills")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Toast.makeText(this, "Error loading open payments: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                billsList.clear()
                snapshots?.documents?.forEach { document ->
                    document.toObject(Bills::class.java)?.let { bill ->
                        if (bill.billId.isNullOrBlank()) bill.billId = document.id
                        PaymentNotificationScheduler.scheduleBill(this, uid, bill)
                        billsList.add(bill)
                    }
                }
                billsLoaded = true
                refreshDashboard()
            }

        spendingsListenerRegistration = userRef.collection("spendings")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Toast.makeText(this, "Error loading closed payments: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                spendingsList.clear()
                snapshots?.documents?.forEach { document ->
                    document.toObject(Spendings::class.java)?.let { spendingsList.add(it) }
                }
                spendingsLoaded = true
                refreshDashboard()
            }

        incomeListenerRegistration = userRef.collection("income")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Toast.makeText(this, "Error loading income: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                incomeList.clear()
                snapshots?.documents?.forEach { document ->
                    document.toObject(Income::class.java)?.let { incomeList.add(it) }
                }
                incomeLoaded = true
                refreshDashboard()
            }
    }

    private fun refreshDashboard() {
        setAmountForMonth()
        if (billsLoaded && spendingsLoaded && incomeLoaded) {
            loadSavingsTarget()
        }
    }

    private fun getBills(): List<Bills> = billsList

    private fun getSpendings(): List<Spendings> = spendingsList

    private fun getIncome(): List<Income> = incomeList

    private fun setAmountForMonth() {
        val dateFormat = SimpleDateFormat("MM-yyyy", Locale.getDefault())
        val currentMonthString = dateFormat.format(currentMonth.time)

        // Fetch bills, spendings, and income for the current month
        val billsForMonth = getBills().filter { bill ->
            bill.date?.toDate()?.let { dateFormat.format(it) == currentMonthString } == true
        }

        val spendingsForMonth = getSpendings().filter { spending ->
            spending.date?.toDate()?.let { dateFormat.format(it) == currentMonthString } == true
        }

        val incomeForMonth = getIncome().filter { income ->
            income.date?.toDate()?.let { dateFormat.format(it) == currentMonthString } == true
        }

        // Calculate totals for bills, spendings, and income
        val totalBills = billsForMonth.filter { !it.paid }.sumOf { it.amount }
        val totalSpendings = spendingsForMonth.sumOf { it.amount }
        val totalIncome = incomeForMonth.sumOf { it.amount }

        // Calculate actual monthly savings
        val actualMonthlySavings = totalIncome - totalBills - totalSpendings

        currentActualMonthlySavings = actualMonthlySavings

        // Calculate incoming bills (unpaid, in the future)
        val totalIncoming = billsForMonth.filter { bill ->
            val billDate = bill.date?.toDate()
            billDate != null && AppDateUtils.isTodayOrAfter(billDate) && !bill.paid
        }.sumOf { it.amount }

        // Calculate overdue bills (unpaid, in the past)
        val totalOverdue = billsForMonth.filter { bill ->
            val billDate = bill.date?.toDate()
            billDate != null && AppDateUtils.isBeforeToday(billDate) && !bill.paid
        }.sumOf { it.amount }

        val totalEssential = spendingsForMonth.filter { spending ->
            SpendingClassification.classify(spending.category, spending.subcategory) ==
                    SpendingClassification.Type.ESSENTIAL
        }.sumOf { it.amount }

        val totalNonEssential = spendingsForMonth.filter { spending ->
            SpendingClassification.classify(spending.category, spending.subcategory) ==
                    SpendingClassification.Type.NON_ESSENTIAL
        }.sumOf { it.amount }

        // Calculate recurring and one-time income
        val totalRecurringIncome = incomeForMonth.filter { income -> income.repeat != "No" }.sumOf { it.amount }
        val totalOneTimeIncome = incomeForMonth.filter { income -> income.repeat == "No" }.sumOf { it.amount }

        // Calculate monthly savings target amount
        // Update UI fields with calculated values
        etBillsAmount.setText(formatAmount(totalBills))
        etSpendingsAmount.setText(formatAmount(totalSpendings))
        etIncomeAmount.setText(formatAmount(totalIncome))
        etIncomingAmount.setText(formatAmount(totalIncoming))
        etOverdueAmount.setText(formatAmount(totalOverdue))
        etEssentialAmount.setText(formatAmount(totalEssential))
        etNonEssentialAmount.setText(formatAmount(totalNonEssential))
        etRecurringAmount.setText(formatAmount(totalRecurringIncome))
        etOneTimeAmount.setText(formatAmount(totalOneTimeIncome))
        etTotalAmount.setText(formatAmount(actualMonthlySavings))
        etTotalAmount.setTextColor(
            ContextCompat.getColor(
                this,
                if (actualMonthlySavings >= 0) R.color.positive_balance else R.color.negative_balance
            )
        )
    }



    private fun setupMonthNavigation() {
        val tvMonth = findViewById<TextView>(R.id.tvMonth)
        val btnPreviousMonth = findViewById<ImageButton>(R.id.btnPreviousMonth)
        val btnNextMonth = findViewById<ImageButton>(R.id.btnNextMonth)

        updateMonthDisplay(tvMonth)


        btnPreviousMonth.setOnClickListener {
            currentMonth.add(Calendar.MONTH, -1)
            updateMonthDisplay(tvMonth)
            refreshDashboard()


        }

        btnNextMonth.setOnClickListener {
            currentMonth.add(Calendar.MONTH, 1)
            updateMonthDisplay(tvMonth)
            refreshDashboard()

        }
    }

    private fun updateMonthDisplay(tvMonth: TextView) {
        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        tvMonth.text = dateFormat.format(currentMonth.time)
    }

    private fun showCreateOptionsDialog() {
        val options = arrayOf(
            getString(R.string.create_new_open_payment),
            getString(R.string.create_new_closed_payment),
            getString(R.string.create_new_income),
            getString(R.string.set_savings_target)
        )

        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.add_new_entry)
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> startActivity(Intent(this, createBill::class.java).apply {
                    putExtra(AuthUtils.EXTRA_USER_EMAIL, userEmail)
                })
                1 -> {
                    if (!PremiumAccess.isPremiumUser(this)) {
                        showUpgradeDialog()
                    } else {
                        startActivity(Intent(this, createSpending::class.java)
                            .apply { putExtra(AuthUtils.EXTRA_USER_EMAIL, userEmail) })
                    }
                }
                2 -> {
                    if (!PremiumAccess.isPremiumUser(this)) {
                        showUpgradeDialog()
                    } else {
                        startActivity(Intent(this, createIncome::class.java).apply {
                            putExtra(AuthUtils.EXTRA_USER_EMAIL, userEmail)
                        })
                    }
                }
                3 -> {
                    if (!PremiumAccess.isPremiumUser(this)) {
                        showUpgradeDialog()
                    } else {
                        createSavings()
                    }
                }
            }
        }
        builder.show()
    }

    private fun showUpgradeDialog() {
        PremiumUpgradeDialog.show(this, R.string.premium_upgrade_required, userEmail)
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

    private fun updateProgressBar(
        progressBar: LinearProgressIndicator,
        progressTextView: TextView,
        monthlyAmount: Double,
        actualMonthlySavings: Double
    ) {
        // Calculate progress percentage
        val progress = if (monthlyAmount > 0) {
            ((actualMonthlySavings / monthlyAmount) * 100).coerceIn(0.0, 100.0) // Ensure within 0-100%
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
    private fun openSavingsTarget() {
        if (!PremiumAccess.isPremiumUser(this)) {
            showUpgradeDialog()
            return
        }

        currentSavingsTargetDocumentId?.let { documentId ->
            showSavingsDialog(documentId)
        } ?: run {
            createSavings()
        }
    }

    private fun createSavings() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_box_savings, null)
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setView(dialogView).setCancelable(true)

        val dialog = dialogBuilder.create()
        dialog.show()
        styleSavingsDialogWindow(dialog)

        // Find views
        val targetAmountEditText = dialogView.findViewById<EditText>(R.id.edtTargetAmount)
        val startDateEditText = dialogView.findViewById<EditText>(R.id.edtStartDateSavings)
        val endDateEditText = dialogView.findViewById<EditText>(R.id.edtEndDateSavings)
        val monthlySavingsEditText = dialogView.findViewById<EditText>(R.id.edtMonthlySavings)
        val targetNameSpinner = dialogView.findViewById<Spinner>(R.id.spinnerTargetName)
        val btnSaveTarget = dialogView.findViewById<Button>(R.id.btnSaveTargetSavings)

        dialogView.findViewById<View>(R.id.savingsDialogActions).visibility = View.GONE

        // Initialize Spinner Adapter
        val targetNames = resources.getStringArray(R.array.savings_target_names).toList()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, targetNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        targetNameSpinner.adapter = adapter

        // Add DatePicker for Start and End Dates
        startDateEditText.setOnClickListener {
            showDatePickerDialog(startDateEditText)
            updateMonthlySavingsFromFields(
                targetAmountEditText, startDateEditText, endDateEditText, monthlySavingsEditText
            )
        }
        endDateEditText.setOnClickListener {
            showDatePickerDialog(endDateEditText)
            updateMonthlySavingsFromFields(
                targetAmountEditText, startDateEditText, endDateEditText, monthlySavingsEditText
            )
        }

        // Add TextWatcher to dynamically update monthly savings
        targetAmountEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateMonthlySavingsFromFields(
                    targetAmountEditText, startDateEditText, endDateEditText, monthlySavingsEditText
                )
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Save button logic
        btnSaveTarget.setOnClickListener {
            if (validateMandatoryFields(targetAmountEditText, startDateEditText, endDateEditText)) {
                val targetAmount = targetAmountEditText.text.toString().toDoubleOrNull() ?: 0.0
                val startDate = getDateFromEditText(startDateEditText)?.time
                val endDate = getDateFromEditText(endDateEditText)?.time
                val targetName = targetNameSpinner.selectedItem.toString()

                if (startDate != null && endDate != null) {
                    val monthsDifference = getMonthsBetweenDates(
                        Calendar.getInstance().apply { time = startDate },
                        Calendar.getInstance().apply { time = endDate }
                    )
                    val calculatedMonthlySavings =
                        if (monthsDifference > 0) targetAmount / monthsDifference else 0.0
                    saveSavings(targetAmount, startDate, endDate, targetName, calculatedMonthlySavings)
                    dialog.dismiss()
                }
            }
        }

        // Close dialog
        dialogView.findViewById<Button>(R.id.btnCloseDialogSavings).setOnClickListener {
            dialog.dismiss()
        }
    }


    private fun showSavingsDialog(documentId: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_box_savings, null)
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setView(dialogView).setCancelable(true)

        val dialog = dialogBuilder.create()
        dialog.show()
        styleSavingsDialogWindow(dialog)

        // Find views
        val targetAmountEditText = dialogView.findViewById<EditText>(R.id.edtTargetAmount)
        val startDateEditText = dialogView.findViewById<EditText>(R.id.edtStartDateSavings)
        val endDateEditText = dialogView.findViewById<EditText>(R.id.edtEndDateSavings)
        val monthlySavingsEditText = dialogView.findViewById<EditText>(R.id.edtMonthlySavings)
        val targetNameSpinner = dialogView.findViewById<Spinner>(R.id.spinnerTargetName)
        val btnSaveTarget = dialogView.findViewById<Button>(R.id.btnSaveTargetSavings)
        val editTargetAction = dialogView.findViewById<View>(R.id.btnEditTargetSavingsAction)
        val deleteTargetAction = dialogView.findViewById<View>(R.id.btnDeleteTargetSavingsAction)
        val savingsDialogActions = dialogView.findViewById<View>(R.id.savingsDialogActions)

        // Initialize Spinner Adapter
        val targetNames = resources.getStringArray(R.array.savings_target_names).toList()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, targetNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        targetNameSpinner.adapter = adapter

        val uid = userUid
        if (uid == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            return
        }

        // Fetch and populate data from Firestore
        db.collection("users").document(uid)
            .collection("savings_targets")
            .document(documentId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    targetAmountEditText.setText(document.getDouble("targetAmount")?.toString() ?: "0.0")
                    document.getTimestamp("startDate")?.toDate()?.let {
                        startDateEditText.setText(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it))
                    }
                    document.getTimestamp("endDate")?.toDate()?.let {
                        endDateEditText.setText(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it))
                    }
                    val targetName = document.getString("targetName") ?: ""
                    targetNameSpinner.setSelection(adapter.getPosition(targetName))
                    monthlySavingsEditText.setText(document.getDouble("monthlySavings")?.toString() ?: "0.0")

                    // Disable fields for view-only mode
                    targetAmountEditText.isEnabled = false
                    startDateEditText.isEnabled = false
                    endDateEditText.isEnabled = false
                    targetNameSpinner.isEnabled = false
                    btnSaveTarget.visibility = View.GONE
                }
            }

        // Enable fields for editing
        editTargetAction.setOnClickListener {
            targetAmountEditText.isEnabled = true
            startDateEditText.isEnabled = true
            endDateEditText.isEnabled = true
            targetNameSpinner.isEnabled = true
            btnSaveTarget.visibility = View.VISIBLE
            savingsDialogActions.visibility = View.GONE

            // Add TextWatcher to dynamically update monthly savings
            targetAmountEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    updateMonthlySavingsFromFields(
                        targetAmountEditText, startDateEditText, endDateEditText, monthlySavingsEditText
                    )
                }
                override fun afterTextChanged(s: Editable?) {}
            })

            // Add DatePicker for Start and End Dates
            startDateEditText.setOnClickListener {
                showDatePickerDialog(startDateEditText)
                updateMonthlySavingsFromFields(
                    targetAmountEditText, startDateEditText, endDateEditText, monthlySavingsEditText
                )
            }
            endDateEditText.setOnClickListener {
                showDatePickerDialog(endDateEditText)
                updateMonthlySavingsFromFields(
                    targetAmountEditText, startDateEditText, endDateEditText, monthlySavingsEditText
                )
            }
        }

        // Delete savings target
        deleteTargetAction.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.delete_target)
                .setMessage(R.string.delete_savings_target_confirmation)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete) { _, _ ->
                    deleteSavingsTarget(dialog, documentId)
                }
                .show()
        }

        // Save button logic
        btnSaveTarget.setOnClickListener {
            editSavingsTarget(targetAmountEditText, targetNameSpinner, dialog, documentId)
        }

        // Close dialog
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

            // 21.01. Trigger monthly savings update after date selection in order to update savings dynamically
            val parentDialogView = editText.rootView
            val targetAmountEditText = parentDialogView.findViewById<EditText>(R.id.edtTargetAmount)
            val startDateEditText = parentDialogView.findViewById<EditText>(R.id.edtStartDateSavings)
            val endDateEditText = parentDialogView.findViewById<EditText>(R.id.edtEndDateSavings)
            val monthlySavingsEditText = parentDialogView.findViewById<EditText>(R.id.edtMonthlySavings)

            updateMonthlySavingsFromFields(
                targetAmountEditText, startDateEditText, endDateEditText, monthlySavingsEditText
            )
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
                    rangesOverlap(
                        startMonth,
                        endMonth,
                        document.getString("startMonth"),
                        document.getString("endMonth")
                    )
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
                    "userUid" to userUid,
                    "monthlySavings" to calculatedMonthlySavings
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

                        // Check if the savings target is 100% achieved - 27.01
                        isSavingsTargetAchieved(docRef.id) { isAchieved ->
                            if (isAchieved && !hasSavingsDialogBeenShown(docRef.id)) {
                                showSavingsCompleteDialog()
                                markSavingsDialogAsShown(docRef.id)
                            }
                        }
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

    // Function to check if the savings target is achieved - 27.01
    private fun isSavingsTargetAchieved(documentId: String, onCompletion: (Boolean) -> Unit) {
        val userUid = FirebaseAuth.getInstance().currentUser?.uid

        if (userUid == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show()
            onCompletion(false)
            return
        }

        // Retrieve the savings target details from Firebase
        db.collection("users").document(userUid)
            .collection("savings_targets")
            .document(documentId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val targetAmount = document.getDouble("targetAmount") ?: 0.0
                    val startDate = document.getTimestamp("startDate")?.toDate()
                    val endDate = document.getTimestamp("endDate")?.toDate()

                    if (startDate == null || endDate == null) {
                        Toast.makeText(this, "Invalid target date range.", Toast.LENGTH_SHORT).show()
                        onCompletion(false)
                        return@addOnSuccessListener
                    }

                    // Filter data within the target's time period
                    val incomeWithinPeriod = getIncome().filter { income ->
                        income.date?.toDate()?.let { it >= startDate && it <= endDate } == true
                    }.sumOf { it.amount }

                    val spendingsWithinPeriod = getSpendings().filter { spending ->
                        spending.date?.toDate()?.let { it >= startDate && it <= endDate } == true
                    }.sumOf { it.amount }

                    val billsWithinPeriod = getBills().filter { bill ->
                        !bill.paid &&
                                bill.date?.toDate()?.let { it >= startDate && it <= endDate } == true
                    }.sumOf { it.amount }

                    // Cumulative savings within the time period
                    val currentSavingsAmount = incomeWithinPeriod - spendingsWithinPeriod - billsWithinPeriod

                    onCompletion(currentSavingsAmount >= targetAmount)
                } else {
                    Toast.makeText(this, "Savings target not found.", Toast.LENGTH_SHORT).show()
                    onCompletion(false)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching savings target: ${e.message}", Toast.LENGTH_SHORT).show()
                onCompletion(false)
            }
    }

    // Function to show the savings complete dialog - 09.01
    private fun showSavingsCompleteDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_savings_complete)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.findViewById<Button>(R.id.btnCloseSavingsComplete).setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
        styleSavingsDialogWindow(dialog)
    }

    // Method to load and display the savings target for the current month
    private fun loadSavingsTarget() {
        val userUid = FirebaseAuth.getInstance().currentUser?.uid
        val currentTimestamp = com.google.firebase.Timestamp(currentMonth.time)

        val tvTargetAchieved = findViewById<TextView>(R.id.tvTargetAchieved)
        val progressBarSavings = findViewById<LinearProgressIndicator>(R.id.progressBarSavings)
        val tvProgressPercentage = findViewById<TextView>(R.id.tvProgressPercentage)

        if (userUid != null) {
            db.collection("users").document(userUid)
                .collection("savings_targets")
                .get()
                .addOnSuccessListener { documents ->
                    val filteredDocuments = documents.filter { document ->
                        val startDate = document.getTimestamp("startDate")
                        val endDate = document.getTimestamp("endDate")
                        startDate != null && endDate != null &&
                                startDate <= currentTimestamp && endDate >= currentTimestamp
                    }

                    val document = filteredDocuments.firstOrNull()

                    if (document == null) {
                        // No active savings target
                        etMonthlySavingsMain.text = formatAmount(0.0)
                        etMonthlySavingsMain.visibility = View.GONE
                        findViewById<TextView>(R.id.tvSetSavingTarget).text = getString(R.string.set_savings_target)
                        currentSavingsTargetDocumentId = null
                        updateProgressBar(progressBarSavings, tvProgressPercentage, 0.0, currentActualMonthlySavings)
                        tvTargetAchieved.visibility = View.GONE
                        tvProgressPercentage.visibility = View.GONE
                        progressBarSavings.visibility = View.GONE
                    } else {
                        val targetName = document.getString("targetName") ?: "Unnamed Target"
                        val monthlyAmount = document.getDouble("monthlySavings") ?: 0.0

                        updateProgressBar(progressBarSavings, tvProgressPercentage, monthlyAmount, currentActualMonthlySavings)
                        etMonthlySavingsMain.text = formatAmount(monthlyAmount)
                        etMonthlySavingsMain.visibility = View.VISIBLE
                        findViewById<TextView>(R.id.tvSetSavingTarget).text = targetName
                        currentSavingsTargetDocumentId = document.id

                        if (monthlyAmount > 0) {
                            tvTargetAchieved.visibility = View.VISIBLE
                            tvProgressPercentage.visibility = View.VISIBLE
                            progressBarSavings.visibility = View.VISIBLE
                        } else {
                            tvTargetAchieved.visibility = View.GONE
                            tvProgressPercentage.visibility = View.GONE
                            progressBarSavings.visibility = View.GONE
                        }
                        // Only check if the savings target is achieved for the first time this month
                        if (!isSavingsTargetCheckedForMonth(document.id)) {
                            isSavingsTargetAchieved(document.id) { isAchieved ->
                                if (isAchieved && !hasSavingsDialogBeenShown(document.id)) {
                                    showSavingsCompleteDialog()
                                    markSavingsDialogAsShown(document.id)
                                }
                                markSavingsTargetCheckedForMonth(document.id)
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error loading savings target: ${e.message}", Toast.LENGTH_SHORT).show()
                    etMonthlySavingsMain.text = "0.00"
                    etMonthlySavingsMain.visibility = View.GONE
                    findViewById<TextView>(R.id.tvSetSavingTarget).text = getString(R.string.set_savings_target)
                    updateProgressBar(progressBarSavings, tvProgressPercentage, 0.0, currentActualMonthlySavings)
                    tvTargetAchieved.visibility = View.GONE
                    tvProgressPercentage.visibility = View.GONE
                    progressBarSavings.visibility = View.GONE
                }
        } else {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
            etMonthlySavingsMain.text = "0.00"
            etMonthlySavingsMain.visibility = View.GONE
            findViewById<TextView>(R.id.tvSetSavingTarget).text = getString(R.string.set_savings_target)
            updateProgressBar(progressBarSavings, tvProgressPercentage, 0.0, currentActualMonthlySavings)
            tvTargetAchieved.visibility = View.GONE
            tvProgressPercentage.visibility = View.GONE
            progressBarSavings.visibility = View.GONE
        }
    }

    // Method to distribute savings across months and save them in Firebase
    private fun distributeSavingsAcrossMonths(
        startDate: Date,
        endDate: Date,
        monthlySavings: Double, // pre-calculated monthly savings
        savingsId: String
    ) {
        val uid = userUid ?: return
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
            db.collection("users").document(uid).collection("savings_targets")
                .document(savingsId).collection("monthly_savings").document(monthString)
                .set(
                    hashMapOf(
                        "month" to monthString,
                        "monthlyAmount" to monthlySavings,

                        "savingsId" to savingsId
                    )
                )
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
                        val existingDocumentId = document.id

                        existingDocumentId != documentId &&
                                rangesOverlap(
                                    startMonth,
                                    endMonth,
                                    document.getString("startMonth"),
                                    document.getString("endMonth")
                                )
                    }

                    if (isOverlap) {
                        // Show error message for overlapping dates
                        Toast.makeText(
                            this,
                            "Cannot update savings target. Overlapping time periods detected.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        // Calculate new monthly savings
                        val monthsDifference = getMonthsBetweenDates(
                            Calendar.getInstance().apply { time = startDate },
                            Calendar.getInstance().apply { time = endDate }
                        )
                        val newMonthlySavings = if (monthsDifference > 0) targetAmount / monthsDifference else 0.0
                        // Update the savings target
                        val updatedData = mapOf(
                            "targetAmount" to targetAmount,
                            "targetName" to targetName,
                            "startDate" to timestampStart,
                            "endDate" to timestampEnd,
                            "startMonth" to startMonth,
                            "endMonth" to endMonth,
                            "monthlySavings" to newMonthlySavings
                        )

                        db.collection("users").document(userUid)
                            .collection("savings_targets")
                            .document(documentId)
                            .update(updatedData)
                            .addOnSuccessListener {
                                // 09.01. next 10 lines added to Clear old monthly savings
                                clearMonthlySavings(userUid, documentId) {
                                    // Distribute updated savings across months
                                    val monthsDifference = getMonthsBetweenDates(
                                        Calendar.getInstance().apply { time = startDate },
                                        Calendar.getInstance().apply { time = endDate }
                                    )
                                    val newMonthlySavings =
                                        if (monthsDifference > 0) targetAmount / monthsDifference else 0.0
                                    distributeSavingsAcrossMonths(startDate, endDate, newMonthlySavings, documentId)
                                }
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
    // 09.01. added as part of update to clear old monthly savings in editSavingsTarget
    private fun clearMonthlySavings(userUid: String, savingsId: String, onComplete: () -> Unit) {
        val savingsRef = db.collection("users").document(userUid)
            .collection("savings_targets").document(savingsId)
            .collection("monthly_savings")

        savingsRef.get().addOnSuccessListener { documents ->
            val batch = db.batch()
            for (doc in documents) {
                batch.delete(doc.reference)
            }
            batch.commit().addOnCompleteListener {
                onComplete()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed to clear monthly savings: ${e.message}", Toast.LENGTH_SHORT).show()
            onComplete() // Proceed even if clearing fails
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

    private fun rangesOverlap(
        startMonth: String,
        endMonth: String,
        existingStartMonth: String?,
        existingEndMonth: String?
    ): Boolean {
        if (existingStartMonth.isNullOrBlank() || existingEndMonth.isNullOrBlank()) {
            return false
        }

        val dateFormat = SimpleDateFormat("MM-yyyy", Locale.getDefault())
        val start = dateFormat.parse(startMonth) ?: return false
        val end = dateFormat.parse(endMonth) ?: return false
        val existingStart = dateFormat.parse(existingStartMonth) ?: return false
        val existingEnd = dateFormat.parse(existingEndMonth) ?: return false

        return !start.after(existingEnd) && !end.before(existingStart)
    }

    private fun styleSavingsDialogWindow(dialog: Dialog) {
        val width = (resources.displayMetrics.widthPixels * 0.92f).toInt()
        val maximumHeight = (resources.displayMetrics.heightPixels * 0.90f).toInt()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.decorView?.post {
            val measuredHeight = dialog.window?.decorView?.measuredHeight ?: return@post
            dialog.window?.setLayout(width, minOf(measuredHeight, maximumHeight))
        }
    }
    // this method helps to manage SharedPreferences for shown achieved savings dialogs
    private fun hasSavingsDialogBeenShown(documentId: String): Boolean {
        val sharedPrefs = getSharedPreferences("SmartStackBillsPrefs", MODE_PRIVATE)
        val shownDialogs = sharedPrefs.getStringSet("shownSavingsDialogs", HashSet()) ?: HashSet()
        return shownDialogs.contains(documentId)
    }

    private fun markSavingsDialogAsShown(documentId: String) {
        val sharedPrefs = getSharedPreferences("SmartStackBillsPrefs", MODE_PRIVATE)
        val shownDialogs = sharedPrefs.getStringSet("shownSavingsDialogs", HashSet()) ?: HashSet()
        shownDialogs.add(documentId)
        sharedPrefs.edit().putStringSet("shownSavingsDialogs", shownDialogs).apply()
    }
    private fun savingsTargetCheckedKey(documentId: String): String {
        val uid = userUid ?: "unknown"
        val month = SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(currentMonth.time)
        return "checkedSavingsTarget_${uid}_${documentId}_$month"
    }

    private fun isSavingsTargetCheckedForMonth(documentId: String): Boolean {
        val sharedPrefs = getSharedPreferences("SmartStackBillsPrefs", MODE_PRIVATE)
        return sharedPrefs.getBoolean(savingsTargetCheckedKey(documentId), false)
    }

    private fun markSavingsTargetCheckedForMonth(documentId: String) {
        val sharedPrefs = getSharedPreferences("SmartStackBillsPrefs", MODE_PRIVATE)
        sharedPrefs.edit().putBoolean(savingsTargetCheckedKey(documentId), true).apply()
    }


    private fun logoutUser() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, LogIn::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
