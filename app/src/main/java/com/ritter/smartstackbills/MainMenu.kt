package com.ritter.smartstackbills

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.ClipData
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.KeyEvent
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
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
import androidx.core.content.FileProvider
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.io.File
import java.io.FileOutputStream
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
    private var currentMonthlySavingsTarget = 0.0
    private var selectedInsightMonths = 1
    private var displayedPremiumState: Boolean? = null
    private var savingsTargetsForInsights = emptyList<SavingsTargetInfo>()
    private var latestMoneySnapshot: MoneyMapSnapshot? = null
    private var latestMoneyTrend: List<MonthlyMoneyMapView.TrendPoint> = emptyList()
    private var latestOpenRisk: OpenRiskSnapshot? = null
    private var latestOpenOutlook: List<OpenPaymentRiskView.MonthlyOutlook> = emptyList()
    private var latestSpendingComposition: Map<String, Double> = emptyMap()

    private data class SavingsTargetInfo(
        val startDate: Date,
        val endDate: Date,
        val monthlyAmount: Double
    )

    private data class MoneyMapSnapshot(
        val income: Double,
        val paid: Double,
        val open: Double,
        val available: Double,
        val target: Double
    )

    private data class OpenRiskSnapshot(
        val overdueAmount: Double,
        val overdueCount: Int,
        val dueSoonAmount: Double,
        val dueSoonCount: Int,
        val laterAmount: Double,
        val laterCount: Int
    )

    private data class ActiveSavingsTarget(
        val id: String,
        val name: String,
        val monthlyAmount: Double
    )

    private enum class InsightDetailType {
        MONEY_FLOW,
        OPEN_PAYMENTS,
        SPENDING_COMPOSITION
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main_menu)
        PremiumPurchaseVerifier.refresh(this) {
            if (!isFinishing && ::currentMonth.isInitialized) {
                val hasPremium = PremiumAccess.isPremiumUser(this)
                refreshPremiumInsightsState(
                    selectDefaultPeriod = hasPremium && displayedPremiumState != true
                )
            }
        }

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

        setupFinancialInsights()
        setupSavingsTargetCollapse()
        setupMonthlyExport()
        setupMonthNavigation()
        setupDashboardNavigation()
        setupDashboardWelcome()
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
        findViewById<View>(R.id.savingsTargetActionRow).setOnClickListener {
            openSavingsTarget()
        }
    }

    private fun openDashboardSection(destination: Class<*>, filter: String) {
        startActivity(Intent(this, destination).apply {
            putExtra("FILTER_TYPE", filter)
            putExtra(AuthUtils.EXTRA_USER_EMAIL, userEmail)
        })
    }

    private fun setupDashboardWelcome() {
        findViewById<View>(R.id.dashboardWelcomeAdd).setOnClickListener {
            EntryCreationFlow.show(this, EntryType.OPEN_PAYMENT, userEmail)
        }
        findViewById<View>(R.id.dashboardWelcomeLearn).setOnClickListener {
            startActivity(Intent(this, GettingStartedActivity::class.java))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        billsListenerRegistration?.remove()
        spendingsListenerRegistration?.remove()
        incomeListenerRegistration?.remove()
    }

    private fun setupBannerAd() {
        val adView = findViewById<AdView>(R.id.adView)
        if (PremiumAccess.isPremiumUser(this)) {
            adView.visibility = View.GONE
            return
        }

        MobileAds.initialize(this) {
            adView.loadAd(AdRequest.Builder().build())
        }
    }

    private fun formatAmount(value: Double): String {
        return CurrencyPreferences.format(this, value)
    }

    private fun setupFinancialInsights() {
        val preferences = getSharedPreferences("dashboard_preferences", MODE_PRIVATE)
        val content = findViewById<LinearLayout>(R.id.financialInsightsContent)
        val arrow = findViewById<ImageView>(R.id.financialInsightsArrow)

        fun applyState(expanded: Boolean, animate: Boolean) {
            content.visibility = if (expanded) View.VISIBLE else View.GONE
            if (animate) {
                arrow.animate().rotation(if (expanded) 180f else 0f).setDuration(180L).start()
            } else {
                arrow.rotation = if (expanded) 180f else 0f
            }
            arrow.contentDescription = getString(
                if (expanded) R.string.hide_details else R.string.show_details
            )
        }

        applyState(preferences.getBoolean("financial_insights_expanded", true), false)
        findViewById<View>(R.id.financialInsightsHeader).setOnClickListener {
            val expanded = content.visibility != View.VISIBLE
            preferences.edit().putBoolean("financial_insights_expanded", expanded).apply()
            applyState(expanded, true)
        }

        findViewById<View>(R.id.premiumInsightsLocked).setOnClickListener {
            PremiumUpgradeDialog.show(this, R.string.premium_insights_upgrade, userEmail)
        }
        findViewById<View>(R.id.categoryChangesInfo).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.category_changes_info_title)
                .setMessage(R.string.category_changes_info_message)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
        findViewById<Button>(R.id.btnInsightsMonth).setOnClickListener {
            selectInsightPeriod(1)
        }
        findViewById<Button>(R.id.btnInsightsSixMonths).setOnClickListener {
            requestInsightPeriod(6)
        }
        findViewById<Button>(R.id.btnInsightsTwelveMonths).setOnClickListener {
            requestInsightPeriod(12)
        }
        findViewById<View>(R.id.monthlyMoneyMapPanel).setOnClickListener {
            showInsightDetail(InsightDetailType.MONEY_FLOW)
        }
        findViewById<View>(R.id.openPaymentRiskPanel).setOnClickListener {
            showInsightDetail(InsightDetailType.OPEN_PAYMENTS)
        }
        findViewById<View>(R.id.spendingCompositionPanel).setOnClickListener {
            showInsightDetail(InsightDetailType.SPENDING_COMPOSITION)
        }
        refreshPremiumInsightsState(selectDefaultPeriod = true)
    }

    private fun setupSavingsTargetCollapse() {
        val preferences = getSharedPreferences("dashboard_preferences", MODE_PRIVATE)
        val content = findViewById<LinearLayout>(R.id.savingsTargetContent)
        val arrow = findViewById<ImageView>(R.id.savingsTargetArrow)

        fun applyState(expanded: Boolean, animate: Boolean) {
            content.visibility = if (expanded) View.VISIBLE else View.GONE
            if (animate) {
                arrow.animate().rotation(if (expanded) 180f else 0f).setDuration(180L).start()
            } else {
                arrow.rotation = if (expanded) 180f else 0f
            }
            arrow.contentDescription = getString(
                if (expanded) R.string.hide_details else R.string.show_details
            )
        }

        applyState(preferences.getBoolean("savings_target_expanded", true), false)
        findViewById<View>(R.id.savingsTargetHeader).setOnClickListener {
            val expanded = content.visibility != View.VISIBLE
            preferences.edit().putBoolean("savings_target_expanded", expanded).apply()
            applyState(expanded, true)
        }
    }

    private fun refreshPremiumInsightsState(selectDefaultPeriod: Boolean) {
        val hasPremium = PremiumAccess.isPremiumUser(this)
        listOf(R.id.btnInsightsSixMonths, R.id.btnInsightsTwelveMonths).forEach { id ->
            findViewById<Button>(id).apply {
                setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    0,
                    if (hasPremium) 0 else R.drawable.ic_star_orange,
                    0
                )
                compoundDrawablePadding = if (hasPremium) 0 else 4
            }
        }
        findViewById<View>(R.id.premiumInsightsLocked).visibility =
            if (hasPremium) View.GONE else View.VISIBLE

        when {
            hasPremium && selectDefaultPeriod -> selectInsightPeriod(6)
            !hasPremium && selectedInsightMonths > 1 -> selectInsightPeriod(1)
            else -> selectInsightPeriod(selectedInsightMonths)
        }
        displayedPremiumState = hasPremium
    }

    private fun requestInsightPeriod(months: Int) {
        if (months > 1 && !PremiumAccess.isPremiumUser(this)) {
            PremiumUpgradeDialog.show(this, R.string.premium_insights_upgrade, userEmail)
            selectInsightPeriod(1)
            return
        }
        selectInsightPeriod(months)
    }

    private fun selectInsightPeriod(months: Int) {
        selectedInsightMonths = months
        val monthly = findViewById<View>(R.id.monthlyInsightsContainer)
        val premium = findViewById<View>(R.id.premiumInsightsContainer)
        monthly.visibility = View.VISIBLE
        premium.visibility =
            if (months > 1 && PremiumAccess.isPremiumUser(this)) View.VISIBLE else View.GONE

        listOf(
            R.id.btnInsightsMonth to 1,
            R.id.btnInsightsSixMonths to 6,
            R.id.btnInsightsTwelveMonths to 12
        ).forEach { (id, value) ->
            findViewById<Button>(id).apply {
                backgroundTintList = null
                setBackgroundResource(
                    if (months == value) R.drawable.insight_period_selected
                    else android.R.color.transparent
                )
            }
        }
        updateFinancialInsights()
    }

    private fun showInsightDetail(type: InsightDetailType) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18.dp, 6.dp, 18.dp, 4.dp)
        }
        val description = TextView(this).apply {
            textSize = 13f
            setTextColor(ContextCompat.getColor(this@MainMenu, R.color.textSecondary))
            setLineSpacing(2f, 1f)
        }
        val chartHeight = when (type) {
            InsightDetailType.MONEY_FLOW -> 330.dp
            InsightDetailType.OPEN_PAYMENTS -> 260.dp
            InsightDetailType.SPENDING_COMPOSITION -> 300.dp
        }
        val chart = when (type) {
            InsightDetailType.MONEY_FLOW -> MonthlyMoneyMapView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    chartHeight
                ).apply { topMargin = 12.dp }
                if (selectedInsightMonths > 1) {
                    setTrendData(latestMoneyTrend)
                } else {
                    latestMoneySnapshot?.let {
                        setData(it.income, it.paid, it.open, it.available, it.target)
                    }
                }
            }
            InsightDetailType.OPEN_PAYMENTS -> OpenPaymentRiskView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    chartHeight
                ).apply { topMargin = 12.dp }
                if (selectedInsightMonths > 1) {
                    setMonthlyOutlook(latestOpenOutlook)
                } else {
                    latestOpenRisk?.let {
                        setData(
                            it.overdueAmount,
                            it.overdueCount,
                            it.dueSoonAmount,
                            it.dueSoonCount,
                            it.laterAmount,
                            it.laterCount
                        )
                    }
                }
            }
            InsightDetailType.SPENDING_COMPOSITION -> SpendingCompositionView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    chartHeight
                ).apply { topMargin = 12.dp }
                setData(latestSpendingComposition)
            }
        }

        val title = when (type) {
            InsightDetailType.MONEY_FLOW ->
                if (selectedInsightMonths > 1) getString(R.string.money_flow_development) else getString(R.string.monthly_money_map)
            InsightDetailType.OPEN_PAYMENTS ->
                if (selectedInsightMonths > 1) getString(R.string.open_payment_outlook_period) else getString(R.string.open_payment_risk_timeline)
            InsightDetailType.SPENDING_COMPOSITION -> getString(R.string.spending_composition)
        }
        description.text = when (type) {
            InsightDetailType.MONEY_FLOW ->
                if (selectedInsightMonths > 1) {
                    getString(R.string.money_flow_development_description, selectedInsightMonths)
                } else {
                    getString(R.string.monthly_money_map_description)
                }
            InsightDetailType.OPEN_PAYMENTS ->
                if (selectedInsightMonths > 1) {
                    getString(R.string.open_payment_outlook_period_description, selectedInsightMonths)
                } else {
                    getString(R.string.open_payment_risk_description)
                }
            InsightDetailType.SPENDING_COMPOSITION ->
                if (selectedInsightMonths > 1) {
                    getString(R.string.spending_composition_period_description, selectedInsightMonths)
                } else {
                    getString(R.string.spending_composition_description)
                }
        }
        container.addView(description)
        container.addView(chart)

        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(container)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun setupMonthlyExport() {
        findViewById<View>(R.id.btnExportMonthlyOverview).setOnClickListener {
            showMonthlyExportFormatPicker()
        }
    }

    private fun showMonthlyExportFormatPicker() {
        AlertDialog.Builder(this)
            .setCustomTitle(monthlyExportTitleView())
            .setItems(
                arrayOf(
                    getString(R.string.export_pdf_summary),
                    getString(R.string.export_csv_data)
                )
            ) { _, which ->
                runCatching {
                    if (which == 0) exportSelectedMonthPdf() else exportSelectedMonthCsv()
                }.onFailure { error ->
                    Toast.makeText(
                        this,
                        getString(R.string.export_failed, error.message.orEmpty()),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .show()
    }

    private fun monthlyExportTitleView(): View {
        val density = resources.displayMetrics.density
        val paddingHorizontal = (24 * density).toInt()
        val paddingTop = (20 * density).toInt()
        val paddingBottom = (8 * density).toInt()
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(paddingHorizontal, paddingTop, paddingHorizontal, paddingBottom)

            addView(TextView(this@MainMenu).apply {
                text = getString(R.string.export_monthly_title)
                textSize = 20f
                setTextColor(ContextCompat.getColor(this@MainMenu, R.color.textPrimary))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })

            addView(ImageButton(this@MainMenu).apply {
                setImageResource(R.drawable.ic_info)
                background = null
                contentDescription = getString(R.string.export_scope_info_content_description)
                setColorFilter(ContextCompat.getColor(this@MainMenu, R.color.colorPrimary))
                setPadding((8 * density).toInt(), (8 * density).toInt(), (8 * density).toInt(), (8 * density).toInt())
                layoutParams = LinearLayout.LayoutParams((40 * density).toInt(), (40 * density).toInt())
                setOnClickListener { showMonthlyExportScopeInfo() }
            })
        }
    }

    private fun showMonthlyExportScopeInfo() {
        AlertDialog.Builder(this)
            .setTitle(R.string.export_scope_info_title)
            .setMessage(R.string.export_scope_info_bullets)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private data class MonthlyExportRow(
        val date: Date?,
        val type: String,
        val title: String,
        val category: String,
        val subcategory: String,
        val amount: Double,
        val currency: String,
        val status: String
    )

    private fun selectedMonthRows(): List<MonthlyExportRow> {
        val monthKey = SimpleDateFormat("MM-yyyy", Locale.getDefault())
        fun isSelectedMonth(date: Date?): Boolean =
            date?.let { monthKey.format(it) == monthKey.format(currentMonth.time) } == true

        val appCurrency = CurrencyPreferences.selectedCode(this)
        val openRows = billsList
            .filter { isSelectedMonth(it.date?.toDate()) }
            .map {
                MonthlyExportRow(
                    it.date?.toDate(),
                    getString(R.string.export_open_payments),
                    it.name.orEmpty(),
                    FinancialEntryOptions.displayCategory(this, it.category),
                    FinancialEntryOptions.displaySubcategory(this, it.category, it.subcategory),
                    it.amount,
                    it.currency?.takeIf { currency -> currency.isNotBlank() } ?: appCurrency,
                    if (it.paid) getString(R.string.paid) else getString(R.string.open_payments)
                )
            }
        val closedRows = spendingsList
            .filter { isSelectedMonth(it.date?.toDate()) }
            .map {
                MonthlyExportRow(
                    it.date?.toDate(),
                    getString(R.string.export_closed_payments),
                    it.name.orEmpty(),
                    FinancialEntryOptions.displayCategory(this, it.category),
                    FinancialEntryOptions.displaySubcategory(this, it.category, it.subcategory),
                    it.amount,
                    it.currency?.takeIf { currency -> currency.isNotBlank() } ?: appCurrency,
                    getString(R.string.paid)
                )
            }
        val incomeRows = incomeList
            .filter { isSelectedMonth(it.date?.toDate()) }
            .map {
                MonthlyExportRow(
                    it.date?.toDate(),
                    getString(R.string.export_income),
                    it.name.orEmpty(),
                    FinancialEntryOptions.displayCategory(this, it.category),
                    FinancialEntryOptions.displaySubcategory(this, it.category, it.subcategory),
                    it.amount,
                    it.currency?.takeIf { currency -> currency.isNotBlank() } ?: appCurrency,
                    it.repeat?.takeIf { repeat -> repeat.isNotBlank() } ?: "-"
                )
            }
        return (openRows + closedRows + incomeRows).sortedBy { it.date ?: Date(0) }
    }

    private fun exportBaseName(): String {
        val month = SimpleDateFormat("yyyy-MM", Locale.US).format(currentMonth.time)
        return "SmartStack-$month"
    }

    private fun exportDirectory(): File =
        File(cacheDir, "exports").apply { mkdirs() }

    private fun shareExport(file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.provider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(contentResolver, file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.export_share_chooser)))
    }

    private fun exportSelectedMonthCsv() {
        val rows = selectedMonthRows()
        fun csv(value: String): String = "\"" + value.replace("\"", "\"\"") + "\""
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val content = buildString {
            appendLine("Date,Type,Title,Category,Subcategory,Amount,Currency,Status")
            rows.forEach { row ->
                appendLine(
                    listOf(
                        row.date?.let { dateFormat.format(it) }.orEmpty(),
                        row.type,
                        row.title,
                        row.category,
                        row.subcategory,
                        CurrencyPreferences.formatPlain(row.amount),
                        row.currency,
                        row.status
                    ).joinToString(",") { csv(it) }
                )
            }
        }
        val file = File(exportDirectory(), "${exportBaseName()}.csv")
        file.writeText(content, Charsets.UTF_8)
        shareExport(file, "text/csv")
    }

    private fun exportSelectedMonthPdf() {
        val rows = selectedMonthRows()
        val incomeTotal = rows.filter { it.type == getString(R.string.export_income) }.sumOf { it.amount }
        val openTotal = rows.filter { it.type == getString(R.string.export_open_payments) }.sumOf { it.amount }
        val closedTotal = rows.filter { it.type == getString(R.string.export_closed_payments) }.sumOf { it.amount }
        val balance = incomeTotal - openTotal - closedTotal
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val monthLabel = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(currentMonth.time)
        val categoryTotals = rows
            .filter { it.type == getString(R.string.export_closed_payments) }
            .groupBy { it.category }
            .mapValues { (_, values) -> values.sumOf { it.amount } }
            .entries
            .sortedByDescending { it.value }

        val lines = buildList {
            add(getString(R.string.export_report_title))
            add(monthLabel)
            add("")
            add(getString(R.string.export_summary))
            add("${getString(R.string.export_income)}: ${formatAmount(incomeTotal)}")
            add("${getString(R.string.export_open_payments)}: ${formatAmount(openTotal)}")
            add("${getString(R.string.export_closed_payments)}: ${formatAmount(closedTotal)}")
            add("${getString(R.string.export_balance)}: ${formatAmount(balance)}")
            add("")
            add(getString(R.string.export_category_totals))
            if (categoryTotals.isEmpty()) {
                add(getString(R.string.export_no_entries))
            } else {
                categoryTotals.take(12).forEach { add("${it.key}: ${formatAmount(it.value)}") }
            }
            add("")
            add(getString(R.string.export_entries))
            if (rows.isEmpty()) {
                add(getString(R.string.export_no_entries))
            } else {
                rows.forEach { row ->
                    add(
                        "${row.date?.let { dateFormat.format(it) }.orEmpty()} · ${row.type} · " +
                            "${row.title.ifBlank { "-" }} · ${row.category} · ${formatAmount(row.amount)}"
                    )
                }
            }
        }

        val document = PdfDocument()
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(2, 48, 71)
            textSize = 18f
            isFakeBoldText = true
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(55, 71, 79)
            textSize = 11f
        }
        val pageWidth = 595
        val pageHeight = 842
        val margin = 42f
        var pageNumber = 0
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, ++pageNumber).create())
        var canvas = page.canvas
        var y = margin

        fun finishAndStartPage() {
            document.finishPage(page)
            page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, ++pageNumber).create())
            canvas = page.canvas
            y = margin
        }

        fun drawWrapped(text: String, paint: Paint) {
            if (text.isBlank()) {
                y += 12f
                return
            }
            val maxWidth = pageWidth - (margin * 2)
            val words = text.split(" ")
            var line = ""
            words.forEach { word ->
                val candidate = if (line.isBlank()) word else "$line $word"
                if (paint.measureText(candidate) <= maxWidth) {
                    line = candidate
                } else {
                    if (y > pageHeight - margin) finishAndStartPage()
                    canvas.drawText(line, margin, y, paint)
                    y += 16f
                    line = word
                }
            }
            if (line.isNotBlank()) {
                if (y > pageHeight - margin) finishAndStartPage()
                canvas.drawText(line, margin, y, paint)
                y += 16f
            }
        }

        lines.forEachIndexed { index, line ->
            drawWrapped(line, if (index == 0) titlePaint else bodyPaint)
            if (index == 1) y += 8f
        }
        document.finishPage(page)

        val file = File(exportDirectory(), "${exportBaseName()}.pdf")
        FileOutputStream(file).use { output -> document.writeTo(output) }
        document.close()
        shareExport(file, "application/pdf")
    }

    override fun onResume() {
        super.onResume()
        if (::currentMonth.isInitialized) {
            val hasPremium = PremiumAccess.isPremiumUser(this)
            refreshPremiumInsightsState(
                selectDefaultPeriod = hasPremium && displayedPremiumState == false
            )
            val adView = findViewById<AdView>(R.id.adView)
            adView.visibility = if (hasPremium) View.GONE else View.VISIBLE
        }
    }

    private fun setupDashboardListeners() {
        val uid = userUid
        if (uid == null) {
            Toast.makeText(this, R.string.user_not_authenticated, Toast.LENGTH_SHORT).show()
            return
        }

        val userRef = db.collection("users").document(uid)

        billsListenerRegistration = userRef.collection("bills")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Toast.makeText(this, getString(R.string.load_open_payments_failed, error.message.orEmpty()), Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(this, getString(R.string.load_closed_payments_failed, error.message.orEmpty()), Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(this, getString(R.string.load_income_failed, error.message.orEmpty()), Toast.LENGTH_SHORT).show()
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
            findViewById<View>(R.id.dashboardWelcomeCard).visibility =
                if (billsList.isEmpty() && spendingsList.isEmpty() && incomeList.isEmpty()) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
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
            SpendingClassification.classify(this, spending.category, spending.subcategory) ==
                    SpendingClassification.Type.ESSENTIAL
        }.sumOf { it.amount }

        val totalNonEssential = spendingsForMonth.filter { spending ->
            SpendingClassification.classify(this, spending.category, spending.subcategory) ==
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
        val statusView = findViewById<TextView>(R.id.tvBalanceStatus)
        val hasActivity = totalIncome != 0.0 || totalBills != 0.0 || totalSpendings != 0.0
        when {
            !hasActivity -> {
                statusView.setText(R.string.no_activity_status)
                statusView.setBackgroundResource(R.drawable.balance_status_neutral)
            }
            actualMonthlySavings > 0.005 -> {
                statusView.setText(R.string.positive_balance_status)
                statusView.setBackgroundResource(R.drawable.balance_status_positive)
            }
            actualMonthlySavings < -0.005 -> {
                statusView.setText(R.string.shortfall_status)
                statusView.setBackgroundResource(R.drawable.balance_status_negative)
            }
            else -> {
                statusView.setText(R.string.balanced_status)
                statusView.setBackgroundResource(R.drawable.balance_status_neutral)
            }
        }
        updateFinancialInsights()
    }

    private fun updateFinancialInsights() {
        if (!::currentMonth.isInitialized) return
        val monthFormat = SimpleDateFormat("MM-yyyy", Locale.getDefault())
        val monthLabel = SimpleDateFormat("MMM", Locale.getDefault())
        val selectedMonth = monthFormat.format(currentMonth.time)
        val insightMonths = selectedInsightMonths.coerceAtLeast(1)
        val calendars = (insightMonths - 1 downTo 0).map { offset ->
            (currentMonth.clone() as Calendar).apply { add(Calendar.MONTH, -offset) }
        }
        fun isSameMonth(date: Date?, calendar: Calendar): Boolean =
            date?.let { monthFormat.format(it) == monthFormat.format(calendar.time) } == true
        fun targetFor(calendar: Calendar): Double {
            val monthStart = (calendar.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            val monthEnd = (calendar.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.time
            return savingsTargetsForInsights
                .filter { !it.startDate.after(monthEnd) && !it.endDate.before(monthStart) }
                .sumOf { it.monthlyAmount }
        }
        val billsForMonth = billsList.filter {
            it.date?.toDate()?.let { date -> monthFormat.format(date) == selectedMonth } == true
        }
        val spendingsForMonth = spendingsList.filter {
            it.date?.toDate()?.let { date -> monthFormat.format(date) == selectedMonth } == true
        }
        val incomeForMonth = incomeList.filter {
            it.date?.toDate()?.let { date -> monthFormat.format(date) == selectedMonth } == true
        }

        val openAmount = billsForMonth.filter { !it.paid }.sumOf { it.amount }
        val closedAmount = spendingsForMonth.sumOf { it.amount }
        val incomeAmount = incomeForMonth.sumOf { it.amount }
        val available = incomeAmount - openAmount - closedAmount
        latestMoneySnapshot = MoneyMapSnapshot(
            incomeAmount,
            closedAmount,
            openAmount,
            available,
            currentMonthlySavingsTarget
        )
        val moneyMapTitle = findViewById<TextView>(R.id.monthlyMoneyMapTitle)
        val moneyMapDescription = findViewById<TextView>(R.id.monthlyMoneyMapDescription)
        val openRiskTitle = findViewById<TextView>(R.id.openPaymentRiskTitle)
        val openRiskDescription = findViewById<TextView>(R.id.openPaymentRiskDescription)
        val spendingCompositionDescription = findViewById<TextView>(R.id.spendingCompositionDescription)
        val moneyMap = findViewById<MonthlyMoneyMapView>(R.id.monthlyMoneyMap)
        val openRiskView = findViewById<OpenPaymentRiskView>(R.id.openPaymentRiskView)
        val spendingCompositionView = findViewById<SpendingCompositionView>(R.id.spendingCompositionView)

        if (insightMonths <= 1) {
            moneyMapTitle.setText(R.string.monthly_money_map)
            moneyMapDescription.setText(R.string.monthly_money_map_description)
            openRiskTitle.setText(R.string.open_payment_risk_timeline)
            openRiskDescription.setText(R.string.open_payment_risk_description)
            spendingCompositionDescription.setText(R.string.spending_composition_description)

            moneyMap.setData(
                incomeAmount,
                closedAmount,
                openAmount,
                available,
                currentMonthlySavingsTarget
            )
        } else {
            moneyMapTitle.setText(R.string.money_flow_development)
            moneyMapDescription.text = getString(R.string.money_flow_development_description, insightMonths)
            openRiskTitle.setText(R.string.open_payment_outlook_period)
            openRiskDescription.text = getString(R.string.open_payment_outlook_period_description, insightMonths)
            spendingCompositionDescription.text = getString(R.string.spending_composition_period_description, insightMonths)

            val trendPoints = calendars.map { calendar ->
                val periodBills = billsList.filter { isSameMonth(it.date?.toDate(), calendar) }
                val periodSpendings = spendingsList.filter { isSameMonth(it.date?.toDate(), calendar) }
                val periodIncome = incomeList.filter { isSameMonth(it.date?.toDate(), calendar) }
                MonthlyMoneyMapView.TrendPoint(
                    monthLabel.format(calendar.time),
                    periodIncome.sumOf { it.amount },
                    periodSpendings.sumOf { it.amount },
                    periodBills.filter { !it.paid }.sumOf { it.amount },
                    targetFor(calendar)
                )
            }
            latestMoneyTrend = trendPoints
            moneyMap.setTrendData(trendPoints)
        }

        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val sevenDaysFromToday = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 7) }
        val overdue = billsForMonth.filter {
            !it.paid && it.date?.toDate()?.before(today.time) == true
        }
        val dueSoon = billsForMonth.filter {
            val date = it.date?.toDate()
            !it.paid && date != null && !date.before(today.time) && !date.after(sevenDaysFromToday.time)
        }
        val later = billsForMonth.filter {
            val date = it.date?.toDate()
            !it.paid && date != null && date.after(sevenDaysFromToday.time)
        }
        latestOpenRisk = OpenRiskSnapshot(
            overdue.sumOf { it.amount },
            overdue.size,
            dueSoon.sumOf { it.amount },
            dueSoon.size,
            later.sumOf { it.amount },
            later.size
        )
        if (insightMonths <= 1) {
            openRiskView.setData(
                overdue.sumOf { it.amount },
                overdue.size,
                dueSoon.sumOf { it.amount },
                dueSoon.size,
                later.sumOf { it.amount },
                later.size
            )
        } else {
            val selectedMonthStart = (currentMonth.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            val monthlyOutlook = calendars.map { calendar ->
                val openBills = billsList.filter {
                    !it.paid && isSameMonth(it.date?.toDate(), calendar)
                }
                val calendarStart = (calendar.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.time
                OpenPaymentRiskView.MonthlyOutlook(
                    monthLabel.format(calendar.time),
                    openBills.sumOf { it.amount },
                    openBills.size,
                    calendarStart.before(selectedMonthStart),
                    calendarStart == selectedMonthStart
                )
            }
            latestOpenOutlook = monthlyOutlook
            openRiskView.setMonthlyOutlook(monthlyOutlook)
        }

        val spendingsForComposition = if (insightMonths <= 1) {
            spendingsForMonth
        } else {
            spendingsList.filter { spending ->
                calendars.any { calendar -> isSameMonth(spending.date?.toDate(), calendar) }
            }
        }
        val categoryTotals = spendingsForComposition
            .groupBy {
                it.category?.takeIf { category -> category.isNotBlank() }
                    ?: getString(R.string.uncategorized)
            }
            .mapValues { (_, entries) -> entries.sumOf { it.amount } }
            .mapKeys { (category, _) ->
                FinancialEntryOptions.displayCategory(this, category)
            }
        latestSpendingComposition = categoryTotals
        spendingCompositionView.setData(categoryTotals)
        updateBiggestChangeInsight()
        if (selectedInsightMonths > 1 && PremiumAccess.isPremiumUser(this)) {
            updatePremiumInsights(selectedInsightMonths)
        }
    }

    private fun updateBiggestChangeInsight() {
        if (!::currentMonth.isInitialized) return

        val monthKey = SimpleDateFormat("MM-yyyy", Locale.getDefault())
        fun sameMonth(date: Date?, calendar: Calendar): Boolean =
            date?.let { monthKey.format(it) == monthKey.format(calendar.time) } == true

        fun billsFor(calendar: Calendar) = billsList.filter { sameMonth(it.date?.toDate(), calendar) }
        fun spendingsFor(calendar: Calendar) = spendingsList.filter { sameMonth(it.date?.toDate(), calendar) }
        fun incomeFor(calendar: Calendar) = incomeList.filter { sameMonth(it.date?.toDate(), calendar) }

        fun recurringCommitments(calendar: Calendar): Double {
            val recurringOpen = billsFor(calendar)
                .filter { !it.paid && it.repeat != "No" }
                .sumOf { it.amount }
            val recurringClosed = spendingsFor(calendar)
                .filter { it.repeat != "No" || it.isRecurring }
                .sumOf { it.amount }
            return recurringOpen + recurringClosed
        }

        fun valuesFor(calendar: Calendar): Map<String, BiggestChangeValue> {
            val spendingCategories = spendingsFor(calendar)
                .groupBy { it.category?.takeIf { category -> category.isNotBlank() } ?: getString(R.string.uncategorized) }
                .mapKeys { (category, _) -> "spending:$category" }
                .mapValues { (categoryKey, values) ->
                    BiggestChangeValue(
                        FinancialEntryOptions.displayCategory(this, categoryKey.removePrefix("spending:")),
                        values.sumOf { it.amount }
                    )
                }

            val incomeCategories = incomeFor(calendar)
                .groupBy { it.category?.takeIf { category -> category.isNotBlank() } ?: getString(R.string.uncategorized) }
                .mapKeys { (category, _) -> "income:$category" }
                .mapValues { (categoryKey, values) ->
                    BiggestChangeValue(
                        FinancialEntryOptions.displayCategory(this, categoryKey.removePrefix("income:")),
                        values.sumOf { it.amount }
                    )
                }

            return spendingCategories +
                incomeCategories +
                mapOf(
                    "metric:open_payments" to BiggestChangeValue(
                        getString(R.string.biggest_change_open_payments),
                        billsFor(calendar).filter { !it.paid }.sumOf { it.amount }
                    ),
                    "metric:income" to BiggestChangeValue(
                        getString(R.string.biggest_change_income),
                        incomeFor(calendar).sumOf { it.amount }
                    ),
                    "metric:recurring" to BiggestChangeValue(
                        getString(R.string.biggest_change_recurring_commitments),
                        recurringCommitments(calendar)
                    )
                )
        }

        fun labelForKey(key: String): String =
            when {
                key == "metric:open_payments" -> getString(R.string.biggest_change_open_payments)
                key == "metric:income" -> getString(R.string.biggest_change_income)
                key == "metric:recurring" -> getString(R.string.biggest_change_recurring_commitments)
                key.startsWith("spending:") -> FinancialEntryOptions.displayCategory(this, key.removePrefix("spending:"))
                key.startsWith("income:") -> FinancialEntryOptions.displayCategory(this, key.removePrefix("income:"))
                else -> key
            }

        val currentValues = valuesFor(currentMonth)
        val previousCalendars = (1 until selectedInsightMonths.coerceAtLeast(2)).map { offset ->
            (currentMonth.clone() as Calendar).apply { add(Calendar.MONTH, -offset) }
        }.ifEmpty {
            listOf((currentMonth.clone() as Calendar).apply { add(Calendar.MONTH, -1) })
        }

        val referenceValues = previousCalendars
            .map { valuesFor(it) }
            .fold(mutableMapOf<String, Double>()) { totals, values ->
                values.forEach { (key, value) -> totals[key] = (totals[key] ?: 0.0) + value.amount }
                totals
            }
            .mapValues { (_, total) -> total / previousCalendars.size.coerceAtLeast(1) }

        val labels = currentValues.keys + referenceValues.keys
        val biggest = labels.map { key ->
                val current = currentValues[key]?.amount ?: 0.0
                val reference = referenceValues[key] ?: 0.0
                val label = currentValues[key]?.label ?: labelForKey(key)
                BiggestChange(label, current - reference)
            }
            .filter { kotlin.math.abs(it.delta) > 0.005 }
            .maxByOrNull { kotlin.math.abs(it.delta) }

        val insight = findViewById<TextView>(R.id.tvBiggestChangeInsight)
        val insightIcon = findViewById<ImageView>(R.id.imgBiggestChangeIcon)
        if (biggest == null) {
            val hasAnyData = currentValues.values.any { it.amount > 0.005 } || referenceValues.values.any { it > 0.005 }
            insight.setText(if (hasAnyData) R.string.biggest_change_stable else R.string.biggest_change_no_data)
            insightIcon.setImageResource(R.drawable.ic_change)
            insightIcon.clearColorFilter()
            return
        }

        val referenceLabel = if (selectedInsightMonths <= 1) {
            getString(R.string.biggest_change_previous_month)
        } else {
            getString(R.string.biggest_change_period_average, selectedInsightMonths - 1)
        }
        insight.text = getString(
            if (biggest.delta > 0.0) R.string.biggest_change_increased else R.string.biggest_change_decreased,
            biggest.label,
            formatAmount(kotlin.math.abs(biggest.delta)),
            referenceLabel
        )
        insightIcon.setImageResource(R.drawable.ic_change)
        insightIcon.clearColorFilter()
    }

    private data class BiggestChange(
        val label: String,
        val delta: Double
    )

    private data class BiggestChangeValue(
        val label: String,
        val amount: Double
    )

    private fun updatePremiumInsights(monthCount: Int) {
        val calendars = (monthCount - 1 downTo 0).map { offset ->
            (currentMonth.clone() as Calendar).apply { add(Calendar.MONTH, -offset) }
        }
        val monthKey = SimpleDateFormat("MM-yyyy", Locale.getDefault())
        val monthLabel = SimpleDateFormat("MMM", Locale.getDefault())

        fun billsFor(calendar: Calendar) = billsList.filter {
            it.date?.toDate()?.let { date -> monthKey.format(date) == monthKey.format(calendar.time) } == true
        }
        fun spendingsFor(calendar: Calendar) = spendingsList.filter {
            it.date?.toDate()?.let { date -> monthKey.format(date) == monthKey.format(calendar.time) } == true
        }
        fun incomeFor(calendar: Calendar) = incomeList.filter {
            it.date?.toDate()?.let { date -> monthKey.format(date) == monthKey.format(calendar.time) } == true
        }

        val trendPoints = calendars.map { calendar ->
            val income = incomeFor(calendar).sumOf { it.amount }
            val obligations = billsFor(calendar).filter { !it.paid }.sumOf { it.amount } +
                spendingsFor(calendar).sumOf { it.amount }
            FinancialTrendView.Point(monthLabel.format(calendar.time), income, obligations)
        }
        findViewById<FinancialTrendView>(R.id.financialTrendView).setData(trendPoints)

        val currentCategories = spendingsFor(calendars.last())
            .groupBy { it.category?.takeIf { value -> value.isNotBlank() } ?: getString(R.string.uncategorized) }
            .mapValues { (_, values) -> values.sumOf { it.amount } }
        val earlierCalendars = calendars.dropLast(1)
        val earlierTotals = earlierCalendars
            .flatMap { spendingsFor(it) }
            .groupBy { it.category?.takeIf { value -> value.isNotBlank() } ?: getString(R.string.uncategorized) }
            .mapValues { (_, values) ->
                values.sumOf { it.amount } / earlierCalendars.size.coerceAtLeast(1)
            }
        val categoryChanges = currentCategories.entries
            .sortedByDescending { it.value }
            .take(4)
            .map { (category, amount) ->
                val previous = earlierTotals[category] ?: 0.0
                CategoryChangeView.Change(
                    FinancialEntryOptions.displayCategory(this, category),
                    amount,
                    if (previous > 0.0) ((amount - previous) / previous) * 100.0 else null,
                    CategoryColorPalette.colorFor(this, category)
                )
            }
        findViewById<CategoryChangeView>(R.id.categoryChangeView).setData(categoryChanges)

        val recurringPoints = calendars.map { calendar ->
            val recurringOpen = billsFor(calendar)
                .filter { it.repeat != "No" }
                .sumOf { it.amount }
            val recurringClosed = spendingsFor(calendar)
                .filter { it.repeat != "No" || it.isRecurring }
                .sumOf { it.amount }
            RecurringCommitmentView.Point(
                monthLabel.format(calendar.time),
                recurringOpen + recurringClosed
            )
        }
        findViewById<RecurringCommitmentView>(R.id.recurringCommitmentView)
            .setData(recurringPoints)

        val savingsMonths = calendars.map { calendar ->
            val monthStart = (calendar.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            val monthEnd = (calendar.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }.time
            val target = savingsTargetsForInsights.firstOrNull {
                !it.startDate.after(monthEnd) && !it.endDate.before(monthStart)
            }
            val income = incomeFor(calendar).sumOf { it.amount }
            val obligations = billsFor(calendar).filter { !it.paid }.sumOf { it.amount } +
                spendingsFor(calendar).sumOf { it.amount }
            SavingsConsistencyView.Month(
                monthLabel.format(calendar.time),
                target != null,
                target != null && income - obligations >= target.monthlyAmount
            )
        }
        findViewById<SavingsConsistencyView>(R.id.savingsConsistencyView)
            .setData(savingsMonths)
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
        findViewById<Button>(R.id.btnInsightsMonth).text =
            SimpleDateFormat("MMMM", Locale.getDefault()).format(currentMonth.time)
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
                0 -> EntryCreationFlow.show(this, EntryType.OPEN_PAYMENT, userEmail)
                1 -> EntryCreationFlow.show(this, EntryType.CLOSED_PAYMENT, userEmail)
                2 -> EntryCreationFlow.show(this, EntryType.INCOME, userEmail)
                3 -> {
                    if (!PremiumAccess.isPremiumUser(this)) {
                        showUpgradeDialog()
                    } else {
                        requestCreateSavingsTarget()
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
        setTodayIfBlank(startDateEditText)
        val targetAmount = targetAmountEditText.text.toString().toDoubleOrNull()
            ?.let(CurrencyPreferences::roundToTwoDecimals)
        val startDate = getDateFromEditText(startDateEditText)
        val endDate = getDateFromEditText(endDateEditText)

        // Check if all mandatory fields are filled
        return when {
            targetAmount == null || targetAmount <= 0f -> {
                Toast.makeText(this, R.string.invalid_target_amount, Toast.LENGTH_SHORT).show()
                false
            }
            endDate == null -> {
                Toast.makeText(this, R.string.invalid_end_date, Toast.LENGTH_SHORT).show()
                false
            }
            startDate == null || !startDate.before(endDate) -> {
                Toast.makeText(this, R.string.start_date_before_end_date, Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    private fun setTodayIfBlank(editText: EditText) {
        if (editText.text.isNullOrBlank()) {
            editText.setText(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()))
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
    private fun openSavingsTarget() {
        if (!PremiumAccess.isPremiumUser(this)) {
            showUpgradeDialog()
            return
        }

        requestCreateSavingsTarget()
    }

    private fun requestCreateSavingsTarget() {
        val uid = userUid
        if (uid == null) {
            Toast.makeText(this, R.string.user_not_authenticated, Toast.LENGTH_SHORT).show()
            return
        }
        db.collection("users").document(uid)
            .collection("savings_targets")
            .get()
            .addOnSuccessListener { documents ->
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.time
                val activeOrFutureCount = documents.count { document ->
                    document.getTimestamp("endDate")?.toDate()?.let { !it.before(today) } == true
                }
                if (activeOrFutureCount >= 3) {
                    Toast.makeText(this, R.string.savings_target_limit_reached, Toast.LENGTH_LONG).show()
                } else {
                    createSavings()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, getString(R.string.load_savings_target_failed, e.message.orEmpty()), Toast.LENGTH_SHORT).show()
            }
    }

    private fun createSavings() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_box_savings, null)
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setView(dialogView).setCancelable(false)

        val dialog = dialogBuilder.create()
        dialog.show()
        dialog.setCanceledOnTouchOutside(false)
        styleSavingsDialogWindow(dialog)

        // Find views
        val targetAmountEditText = dialogView.findViewById<EditText>(R.id.edtTargetAmount)
        val startDateEditText = dialogView.findViewById<EditText>(R.id.edtStartDateSavings)
        val endDateEditText = dialogView.findViewById<EditText>(R.id.edtEndDateSavings)
        val monthlySavingsEditText = dialogView.findViewById<EditText>(R.id.edtMonthlySavings)
        val targetNameSpinner = dialogView.findViewById<Spinner>(R.id.spinnerTargetName)
        val savingsGoalImage = dialogView.findViewById<ImageView>(R.id.imgSavingsGoal)
        val btnSaveTarget = dialogView.findViewById<Button>(R.id.btnSaveTargetSavings)

        dialogView.findViewById<View>(R.id.savingsDialogActions).visibility = View.GONE
        setTodayIfBlank(startDateEditText)

        // Initialize Spinner Adapter
        val targetNames = resources.getStringArray(R.array.savings_target_names).toList()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, targetNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        targetNameSpinner.adapter = adapter
        setupSavingsTargetIconPreview(targetNameSpinner, savingsGoalImage)

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
                val targetAmount = CurrencyPreferences.roundToTwoDecimals(
                    targetAmountEditText.text.toString().toDoubleOrNull() ?: 0.0
                )
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
            confirmDismissSavingsCreateDialog(
                dialog,
                targetAmountEditText,
                endDateEditText
            )
        }
        dialog.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                confirmDismissSavingsCreateDialog(
                    dialog,
                    targetAmountEditText,
                    endDateEditText
                )
                true
            } else {
                false
            }
        }
    }

    private fun confirmDismissSavingsCreateDialog(
        dialog: Dialog,
        targetAmountEditText: EditText,
        endDateEditText: EditText
    ) {
        val hasUnsavedInput =
            targetAmountEditText.text?.toString()?.trim().orEmpty().isNotBlank() ||
                endDateEditText.text?.toString()?.trim().orEmpty().isNotBlank()
        if (!hasUnsavedInput) {
            dialog.dismiss()
            return
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.unsaved_changes_title)
            .setMessage(R.string.unsaved_changes_message)
            .setNegativeButton(R.string.keep_editing, null)
            .setPositiveButton(R.string.discard_changes) { _, _ -> dialog.dismiss() }
            .show()
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
        val savingsGoalImage = dialogView.findViewById<ImageView>(R.id.imgSavingsGoal)
        val btnSaveTarget = dialogView.findViewById<Button>(R.id.btnSaveTargetSavings)
        val editTargetAction = dialogView.findViewById<View>(R.id.btnEditTargetSavingsAction)
        val deleteTargetAction = dialogView.findViewById<View>(R.id.btnDeleteTargetSavingsAction)
        val savingsDialogActions = dialogView.findViewById<View>(R.id.savingsDialogActions)

        // Initialize Spinner Adapter
        val targetNames = resources.getStringArray(R.array.savings_target_names).toList()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, targetNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        targetNameSpinner.adapter = adapter
        setupSavingsTargetIconPreview(targetNameSpinner, savingsGoalImage)

        val uid = userUid
        if (uid == null) {
            Toast.makeText(this, R.string.user_not_authenticated, Toast.LENGTH_SHORT).show()
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
            val date = sdf.parse(editText.text.toString()) ?: return null
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
        val targetAmount = CurrencyPreferences.roundToTwoDecimals(
            targetAmountEditText.text.toString().toDoubleOrNull() ?: 0.0
        )
        val startDate = getDateFromEditText(startDateEditText)
        val endDate = getDateFromEditText(endDateEditText)

        if (targetAmount > 0 && startDate != null && endDate != null && startDate.before(endDate)) {
            val monthsDifference = getMonthsBetweenDates(startDate, endDate)
            val monthlySavings = targetAmount / monthsDifference
            monthlySavingsEditText.setText(CurrencyPreferences.formatPlain(monthlySavings))
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
            Toast.makeText(this, R.string.user_not_authenticated, Toast.LENGTH_SHORT).show()
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
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.time
                val activeOrFutureCount = documents.count { document ->
                    document.getTimestamp("endDate")?.toDate()?.let { !it.before(today) } == true
                }

                if (activeOrFutureCount >= 3) {
                    Toast.makeText(this, R.string.savings_target_limit_reached, Toast.LENGTH_LONG).show()
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
            Toast.makeText(this, R.string.user_not_authenticated, Toast.LENGTH_SHORT).show()
            onCompletion(false)
            return
        }

        // Retrieve the savings target details from Firebase
        db.collection("users").document(userUid)
            .collection("savings_targets")
            .document(documentId)
            .get()
            .addOnSuccessListener { targetDocument ->
                if (targetDocument.exists()) {
                    val targetAmount = targetDocument.getDouble("targetAmount") ?: 0.0
                    val targetMonthlyAmount = targetDocument.getDouble("monthlySavings") ?: 0.0
                    val startDate = targetDocument.getTimestamp("startDate")?.toDate()
                    val endDate = targetDocument.getTimestamp("endDate")?.toDate()

                    if (startDate == null || endDate == null) {
                        Toast.makeText(this, R.string.invalid_target_date_range, Toast.LENGTH_SHORT).show()
                        onCompletion(false)
                        return@addOnSuccessListener
                    }

                    db.collection("users").document(userUid)
                        .collection("savings_targets")
                        .get()
                        .addOnSuccessListener { allTargets ->
                            var allocatedSavingsForTarget = 0.0
                            monthsBetween(startDate, endDate).forEach { month ->
                                val actualSavings = actualSavingsForMonth(month).coerceAtLeast(0.0)
                                val monthStart = monthStart(month)
                                val monthEnd = monthEnd(month)
                                val totalRequirement = allTargets.sumOf { otherTarget ->
                                    val otherStart = otherTarget.getTimestamp("startDate")?.toDate()
                                    val otherEnd = otherTarget.getTimestamp("endDate")?.toDate()
                                    if (otherStart != null && otherEnd != null &&
                                        !otherStart.after(monthEnd) && !otherEnd.before(monthStart)
                                    ) {
                                        otherTarget.getDouble("monthlySavings") ?: 0.0
                                    } else {
                                        0.0
                                    }
                                }
                                if (totalRequirement > 0.0 && targetMonthlyAmount > 0.0) {
                                    allocatedSavingsForTarget += actualSavings * (targetMonthlyAmount / totalRequirement)
                                }
                            }
                            onCompletion(allocatedSavingsForTarget >= targetAmount)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, getString(R.string.load_savings_target_failed, e.message.orEmpty()), Toast.LENGTH_SHORT).show()
                            onCompletion(false)
                        }
                } else {
                    Toast.makeText(this, R.string.savings_target_not_found, Toast.LENGTH_SHORT).show()
                    onCompletion(false)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, getString(R.string.load_savings_target_failed, e.message.orEmpty()), Toast.LENGTH_SHORT).show()
                onCompletion(false)
            }
    }

    private fun actualSavingsForMonth(month: Calendar): Double {
        val key = SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(month.time)
        fun sameMonth(date: Date?): Boolean =
            date?.let { SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(it) == key } == true

        val income = getIncome()
            .filter { sameMonth(it.date?.toDate()) }
            .sumOf { it.amount }
        val spendings = getSpendings()
            .filter { sameMonth(it.date?.toDate()) }
            .sumOf { it.amount }
        val openPayments = getBills()
            .filter { !it.paid && sameMonth(it.date?.toDate()) }
            .sumOf { it.amount }
        return income - spendings - openPayments
    }

    private fun monthsBetween(startDate: Date, endDate: Date): List<Calendar> {
        val start = Calendar.getInstance().apply {
            time = startDate
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val end = Calendar.getInstance().apply {
            time = endDate
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val months = mutableListOf<Calendar>()
        val cursor = start.clone() as Calendar
        while (!cursor.after(end)) {
            months += cursor.clone() as Calendar
            cursor.add(Calendar.MONTH, 1)
        }
        return months
    }

    private fun monthStart(month: Calendar): Date =
        (month.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

    private fun monthEnd(month: Calendar): Date =
        (month.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.time

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
        val selectedMonthStart = (currentMonth.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        val selectedMonthEnd = (currentMonth.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.time

        val tvTargetAchieved = findViewById<TextView>(R.id.tvTargetAchieved)
        val progressBarSavings = findViewById<LinearProgressIndicator>(R.id.progressBarSavings)
        val tvProgressPercentage = findViewById<TextView>(R.id.tvProgressPercentage)
        val savingsTargetsList = findViewById<LinearLayout>(R.id.savingsTargetsList)
        val savingsTargetActionRow = findViewById<View>(R.id.savingsTargetActionRow)
        val savingsTargetLegacyProgressRow = findViewById<View>(R.id.savingsTargetLegacyProgressRow)
        savingsTargetLegacyProgressRow.visibility = View.GONE
        progressBarSavings.visibility = View.GONE
        tvProgressPercentage.visibility = View.GONE

        if (userUid != null) {
            db.collection("users").document(userUid)
                .collection("savings_targets")
                .get()
                .addOnSuccessListener { documents ->
                    savingsTargetsForInsights = documents.mapNotNull { document ->
                        val start = document.getTimestamp("startDate")?.toDate()
                        val end = document.getTimestamp("endDate")?.toDate()
                        if (start == null || end == null) {
                            null
                        } else {
                            SavingsTargetInfo(
                                start,
                                end,
                                document.getDouble("monthlySavings") ?: 0.0
                            )
                        }
                    }
                    val activeTargets = documents.mapNotNull { document ->
                        val startDate = document.getTimestamp("startDate")?.toDate()
                        val endDate = document.getTimestamp("endDate")?.toDate()
                        if (startDate != null && endDate != null &&
                            !startDate.after(selectedMonthEnd) &&
                            !endDate.before(selectedMonthStart)
                        ) {
                            ActiveSavingsTarget(
                                document.id,
                                document.getString("targetName") ?: getString(R.string.savings_target),
                                document.getDouble("monthlySavings") ?: 0.0
                            )
                        } else {
                            null
                        }
                    }.sortedByDescending { it.monthlyAmount }

                    currentMonthlySavingsTarget = activeTargets.sumOf { it.monthlyAmount }
                    etMonthlySavingsMain.text = formatAmount(currentMonthlySavingsTarget)
                    etMonthlySavingsMain.visibility =
                        if (currentMonthlySavingsTarget > 0.0) View.VISIBLE else View.GONE
                    currentSavingsTargetDocumentId = activeTargets.firstOrNull()?.id
                    renderSavingsTargets(
                        savingsTargetsList,
                        activeTargets,
                        currentActualMonthlySavings,
                        currentMonthlySavingsTarget
                    )

                    if (activeTargets.isEmpty()) {
                        savingsTargetActionRow.visibility = View.VISIBLE
                        findViewById<TextView>(R.id.tvSetSavingTarget).text = getString(R.string.set_savings_target)
                    } else {
                        savingsTargetActionRow.visibility = View.GONE
                    }

                    tvTargetAchieved.visibility = View.VISIBLE
                    updateFinancialInsights()
                    activeTargets.forEach { target ->
                        isSavingsTargetAchieved(target.id) { isAchieved ->
                            if (isAchieved && !hasSavingsDialogBeenShown(target.id)) {
                                showSavingsCompleteDialog()
                                markSavingsDialogAsShown(target.id)
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, getString(R.string.load_savings_target_failed, e.message.orEmpty()), Toast.LENGTH_SHORT).show()
                    clearSavingsTargetDashboard(savingsTargetsList, savingsTargetActionRow)
                    updateFinancialInsights()
                }
        } else {
            Toast.makeText(this, R.string.user_not_authenticated, Toast.LENGTH_SHORT).show()
            clearSavingsTargetDashboard(savingsTargetsList, savingsTargetActionRow)
            updateFinancialInsights()
        }
    }

    private fun clearSavingsTargetDashboard(
        savingsTargetsList: LinearLayout,
        savingsTargetActionRow: View
    ) {
        savingsTargetsForInsights = emptyList()
        currentMonthlySavingsTarget = 0.0
        currentSavingsTargetDocumentId = null
        etMonthlySavingsMain.text = "0.00"
        etMonthlySavingsMain.visibility = View.GONE
        savingsTargetsList.removeAllViews()
        savingsTargetActionRow.visibility = View.VISIBLE
        findViewById<TextView>(R.id.tvSetSavingTarget).text = getString(R.string.set_savings_target)
        findViewById<TextView>(R.id.tvTargetAchieved).visibility = View.VISIBLE
        findViewById<TextView>(R.id.tvProgressPercentage).visibility = View.GONE
        findViewById<LinearProgressIndicator>(R.id.progressBarSavings).visibility = View.GONE
    }

    private fun renderSavingsTargets(
        container: LinearLayout,
        targets: List<ActiveSavingsTarget>,
        actualMonthlySavings: Double,
        totalMonthlyRequirement: Double
    ) {
        container.removeAllViews()
        targets.take(3).forEachIndexed { index, target ->
            if (index > 0) {
                container.addView(View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1.dp
                    ).apply {
                        topMargin = 10.dp
                        bottomMargin = 10.dp
                    }
                    setBackgroundColor(Color.parseColor("#E6ECF1"))
                })
            }
            container.addView(createSavingsTargetRow(target, actualMonthlySavings, totalMonthlyRequirement))
        }
    }

    private fun createSavingsTargetRow(
        target: ActiveSavingsTarget,
        actualMonthlySavings: Double,
        totalMonthlyRequirement: Double
    ): View {
        val allocatedSavings = if (totalMonthlyRequirement > 0.0) {
            actualMonthlySavings.coerceAtLeast(0.0) * (target.monthlyAmount / totalMonthlyRequirement)
        } else {
            0.0
        }
        val progress = if (target.monthlyAmount > 0) {
            ((allocatedSavings / target.monthlyAmount) * 100.0).coerceIn(0.0, 100.0)
        } else {
            0.0
        }
        val row = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true
            isFocusable = true
            val ripple = obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground))
            foreground = ripple.getDrawable(0)
            ripple.recycle()
            setPadding(0, 2.dp, 0, 2.dp)
            setOnClickListener { showSavingsDialog(target.id) }
        }

        val icon = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(42.dp, 42.dp).apply {
                marginEnd = 12.dp
            }
            setBackgroundResource(R.drawable.savings_target_icon_bg)
            setPadding(8.dp, 8.dp, 8.dp, 8.dp)
            setImageResource(savingsIconForTarget(target.name))
            setColorFilter(ContextCompat.getColor(this@MainMenu, R.color.colorPrimary))
            contentDescription = target.name
        }
        row.addView(icon)

        val content = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            orientation = LinearLayout.VERTICAL
        }
        val topLine = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
        }
        topLine.addView(TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            text = target.name
            setTextColor(ContextCompat.getColor(this@MainMenu, R.color.colorSecondary))
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        })
        topLine.addView(TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            text = formatAmount(target.monthlyAmount)
            setTextColor(ContextCompat.getColor(this@MainMenu, R.color.savings_color))
            textSize = 13f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        content.addView(topLine)

        val progressLine = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 7.dp }
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
        }
        progressLine.addView(LinearProgressIndicator(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            max = 100
            setProgressCompat(progress.toInt(), false)
            setIndicatorColor(ContextCompat.getColor(this@MainMenu, R.color.colorPrimary))
            trackColor = Color.parseColor("#DDE7EF")
            trackThickness = 8.dp
        })
        progressLine.addView(TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(48.dp, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                marginStart = 8.dp
            }
            gravity = Gravity.END
            text = String.format(Locale.getDefault(), "%.0f%%", progress)
            setTextColor(ContextCompat.getColor(this@MainMenu, R.color.colorSecondary))
            textSize = 12f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        content.addView(progressLine)
        row.addView(content)
        return row
    }

    private fun savingsIconForTarget(targetName: String): Int {
        val value = targetName.lowercase(Locale.getDefault())
        return when {
            listOf("emergency", "notfall", "emergencia", "awaryjny", "emergência").any { it in value } -> R.drawable.ic_goal_emergency
            listOf("vacation", "holiday", "travel", "urlaub", "reise", "vacaciones", "férias", "wakacje").any { it in value } -> R.drawable.ic_goal_vacation
            listOf("home", "house", "down payment", "haus", "immobil", "casa", "imóvel", "wkład").any { it in value } -> R.drawable.ic_goal_home
            listOf("car", "auto", "vehicle", "coche", "samoch", "carro").any { it in value } -> R.drawable.ic_goal_car
            listOf("education", "school", "bildung", "educ", "eduk", "éducation").any { it in value } -> R.drawable.ic_goal_education
            listOf("retirement", "pension", "ruhestand", "emeryt", "aposent").any { it in value } -> R.drawable.ic_goal_retirement
            listOf("wedding", "hochzeit", "boda", "ślub", "casamento").any { it in value } -> R.drawable.ic_goal_wedding
            listOf("investment", "invest", "anlage", "invers", "inwest", "investimento").any { it in value } -> R.drawable.ic_goal_investment
            else -> R.drawable.ic_goal_general
        }
    }

    private fun setupSavingsTargetIconPreview(spinner: Spinner, imageView: ImageView) {
        fun updateIcon() {
            val targetName = spinner.selectedItem?.toString().orEmpty()
            imageView.setImageResource(savingsIconForTarget(targetName))
            imageView.setColorFilter(ContextCompat.getColor(this, R.color.colorPrimary))
        }
        spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateIcon()
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER
        imageView.setPadding(24.dp, 24.dp, 24.dp, 24.dp)
        updateIcon()
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
                    Toast.makeText(this, getString(R.string.savings_target_update_failed, e.message.orEmpty()), Toast.LENGTH_SHORT).show()
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
            ?.let(CurrencyPreferences::roundToTwoDecimals)
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
                                Toast.makeText(this, R.string.savings_target_updated, Toast.LENGTH_SHORT).show()
                                dialog.dismiss()
                                loadSavingsTarget() // Refresh the target list
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, getString(R.string.savings_target_update_failed, e.message.orEmpty()), Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, R.string.valid_inputs_required, Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, getString(R.string.monthly_savings_clear_failed, e.message.orEmpty()), Toast.LENGTH_SHORT).show()
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
                            Toast.makeText(this, R.string.savings_target_deleted, Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            loadSavingsTarget() // Refresh the target list
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, getString(R.string.savings_target_delete_failed, e.message.orEmpty()), Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, getString(R.string.associated_data_failed, e.message.orEmpty()), Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, R.string.user_not_authenticated, Toast.LENGTH_SHORT).show()
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

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}
