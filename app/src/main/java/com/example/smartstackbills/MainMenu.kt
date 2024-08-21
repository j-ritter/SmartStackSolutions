package com.example.smartstackbills

import android.content.Context
import android.content.Intent
import android.os.Bundle
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
import MyCalendarView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainMenu : AppCompatActivity() {
    private var userEmail: String? = null
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var currentMonth: Calendar


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main_menu)


        userEmail = intent.getStringExtra("USER_EMAIL")

        drawerLayout = findViewById(R.id.drawer_layout)

        currentMonth = Calendar.getInstance()

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
                R.id.Main -> {
                    // Stay on the same activity
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

                R.id.Income -> {  // New navigation option for Income
                    val intent = Intent(this, MyIncome::class.java)
                    intent.putExtra("USER_EMAIL", userEmail)
                    startActivity(intent)
                    true
                }

                R.id.Calendar -> {
                    // Intent for Calendar (assumed to be implemented)
                    val intent = Intent(this, MyCalendarView::class.java)
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
        setSpendingsAmount()
        setIncomingAmount()
        setBillsAmount()
        setTotalIncomeAmount()

        //Connection to other folders
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

    private fun setSpendingsAmount() {
        val totalSpendings = getTotalSpendings()
        val etSpendingsAmount: EditText = findViewById(R.id.etSpendingsAmount)
        etSpendingsAmount.setText(totalSpendings.toString())
    }
    private fun setBillsAmount() {
        val totalBills = getTotalBills()
        val etBillsAmount: EditText = findViewById(R.id.etBillsAmount)
        etBillsAmount.setText(totalBills.toString())
    }
    private fun setIncomingAmount() {
        val totalIncoming = getTotalIncoming()
        val etIncomingAmount: EditText = findViewById(R.id.etIncomingAmount)
        etIncomingAmount.setText(totalIncoming.toString())
    }
    private fun setTotalIncomeAmount() {
        val totalIncome = getTotalIncome()
        val etIncomeAmount: EditText = findViewById(R.id.etIncome)
        etIncomeAmount.setText(totalIncome.toString())
    }

    private fun getTotalSpendings(): Float {
        val sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPref.getString("spendingsList", null)
        val type = object : TypeToken<ArrayList<Spendings>>() {}.type
        val spendingsList: ArrayList<Spendings> = gson.fromJson(json, type) ?: ArrayList()
        return spendingsList.sumOf { it.amount.toDouble() }.toFloat()
    }
    private fun getTotalBills(): Float {
        val sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPref.getString("billsList", null)
        val type = object : TypeToken<ArrayList<Bills>>() {}.type
        val billsList: ArrayList<Bills> = gson.fromJson(json, type) ?: ArrayList()
        return billsList.sumOf { it.amount.toDouble() }.toFloat()
    }
    private fun getTotalIncoming(): Float {
        val sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPref.getString("billsList", null)
        val type = object : TypeToken<ArrayList<Bills>>() {}.type
        val billsList: ArrayList<Bills> = gson.fromJson(json, type) ?: ArrayList()

        val currentDate = Calendar.getInstance().time

        return billsList.filter { bill ->
            val billDate = bill.date?.toDate()
            billDate != null && billDate.after(currentDate) && !bill.paid && bill.repeat == "No"
        }.sumOf { it.amount.toDouble() }.toFloat()
    }
    private fun getTotalIncome(): Float {
        val sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPref.getString("incomeList", null)
        val type = object : TypeToken<ArrayList<Income>>() {}.type
        val incomeList: ArrayList<Income> = gson.fromJson(json, type) ?: ArrayList()
        return incomeList.sumOf { it.amount.toDouble() }.toFloat()
    }


    private fun setupMonthNavigation() {
        val tvMonth = findViewById<TextView>(R.id.tvMonth)
        val btnPreviousMonth = findViewById<ImageButton>(R.id.btnPreviousMonth)
        val btnNextMonth = findViewById<ImageButton>(R.id.btnNextMonth)

        updateMonthDisplay(tvMonth)

        btnPreviousMonth.setOnClickListener {
            currentMonth.add(Calendar.MONTH, -1)
            updateMonthDisplay(tvMonth)

        }

        btnNextMonth.setOnClickListener {
            currentMonth.add(Calendar.MONTH, 1)
            updateMonthDisplay(tvMonth)

        }
    }

    private fun updateMonthDisplay(tvMonth: TextView) {
        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        tvMonth.text = dateFormat.format(currentMonth.time)
    }



    private fun logoutUser() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, LogIn::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}