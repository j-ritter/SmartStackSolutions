package com.example.smartstackbills

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
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

        setupMonthNavigation()
        setupUI()

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
        val essentialSubcategories = listOf("rent", "utilities", "groceries", "Mobile phone", "Landline phone", "Internet",
            "Health insurance", "Life insurance", "Car insurance", "Home insurance",
            "Fuel", "Vehicle maintenance", "Public transportation",
            "Doctor visits", "Dental care", "Prescription medications", "Medical equipment",
            "Groceries", "Clothing", "Household goods", "Personal care products",
            "Income tax", "Property tax", "Sales tax", "Self-employment tax", "Capital gains tax",
            "Tuition fees", "Textbooks", "School supplies")
        val totalEssential = getSpendings().filter { spending ->
            spending.subcategory in essentialSubcategories && selectedMonthFormat.format(spending.date?.toDate()) == selectedMonth
        }.sumOf { it.amount.toDouble() }.toFloat()

        val nonEssentialSubcategories = listOf("Entertainment", "Dining out", "Hobbies", "Streaming services",
            "Movies", "Music concerts", "Video games", "Sports", "Vacation",
            "Gadgets", "Luxury items", "Alcohol", "Tobacco", "Gym memberships",
            "Clothing", "Decorations", "Jewelry")
        val totalNonEssential = getSpendings().filter { spending ->
            spending.subcategory in nonEssentialSubcategories && selectedMonthFormat.format(spending.date?.toDate()) == selectedMonth
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
        val options = arrayOf("Create an 'Open Payment'", "Create a 'Closed Payment'", "Create an 'Income'")

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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_nav, menu)
        return true
    }

    private fun logoutUser() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, LogIn::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
