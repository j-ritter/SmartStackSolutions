package com.example.smartstackbills

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CalendarView
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
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
import java.util.*

class CalendarActivity : AppCompatActivity(), MyAdapter.OnBillClickListener, MyAdapterIncome.OnIncomeClickListener {

    private var userEmail: String? = null

    private lateinit var drawerLayoutCalendar: DrawerLayout
    private lateinit var calendarView: CalendarView
    private lateinit var recyclerViewBills: RecyclerView
    private lateinit var recyclerViewIncome: RecyclerView
    private lateinit var billsList: ArrayList<Bills>
    private lateinit var incomeList: ArrayList<Income>
    private lateinit var filteredBills: ArrayList<Bills>
    private lateinit var filteredIncome: ArrayList<Income>
    private lateinit var myAdapter: MyAdapter
    private lateinit var myAdapterIncome: MyAdapterIncome
    private lateinit var dialogBills: Dialog
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
        recyclerViewBills = findViewById(R.id.recyclerViewCalendarBill)
        recyclerViewIncome = findViewById(R.id.recyclerViewCalendarIncome)

        // Initialize RecyclerView for Bills
        recyclerViewBills.layoutManager = LinearLayoutManager(this)
        myAdapter = MyAdapter(this, ArrayList(), this)
        recyclerViewBills.adapter = myAdapter

        // Initialize RecyclerView for Income
        recyclerViewIncome.layoutManager = LinearLayoutManager(this)
        myAdapterIncome = MyAdapterIncome(this, ArrayList(), this)
        recyclerViewIncome.adapter = myAdapterIncome

        // Load bills and income data
        billsList = loadBills()
        incomeList = loadIncome()

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = "$dayOfMonth/${month + 1}/$year"
            Toast.makeText(this, "Date selected: $selectedDate", Toast.LENGTH_SHORT).show()

            // Filter bills based on the selected date
            filteredBills = billsList.filter { bill ->
                val sdf = SimpleDateFormat("d/M/yyyy", Locale.getDefault())
                bill.date != null && sdf.format(bill.date.toDate()) == selectedDate
            } as ArrayList<Bills>
            // Filter income based on the selected date
            filteredIncome = incomeList.filter { income ->
                val sdf = SimpleDateFormat("d/M/yyyy", Locale.getDefault())
                income.date != null && sdf.format(income.date.toDate()) == selectedDate
            } as ArrayList<Income>


            // Update RecyclerView with filtered bills
            myAdapter.updateBills(filteredBills)
            myAdapterIncome.updateIncome(filteredIncome)
        }

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottomNavigationViewCalendar)
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
                R.id.Income -> {  // New navigation option for Income
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

        setupDialogBills() // Initialize the dialog
        setupDialogIncome()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_nav, menu)
        return true
    }

    private fun loadBills(): ArrayList<Bills> {
        val sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPref.getString("billsList", null)
        val type = object : TypeToken<ArrayList<Bills>>() {}.type
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
        dialogBills.setContentView(R.layout.dialog_box_bill)
        dialogBills.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialogBills.window?.setBackgroundDrawable(getDrawable(R.drawable.dialog_box_bills_bg))
        dialogBills.setCancelable(true) // Allow dialog to be closed

        // Disable edit buttons and make fields non-editable
        val btnSaveChanges = dialogBills.findViewById<ImageView>(R.id.imgEditBill)
        btnSaveChanges?.visibility = View.GONE // Hide the save button

        val edtTitleDialog = dialogBills.findViewById<EditText>(R.id.edtTitleDialog)
        val edtAmountDialog = dialogBills.findViewById<EditText>(R.id.edtAmountDialog)
        val edtCategoryDialog = dialogBills.findViewById<EditText>(R.id.edtCategoryDialog)
        val edtSubcategoryDialog = dialogBills.findViewById<EditText>(R.id.edtSubcategoryDialog)
        val edtVendorDialog = dialogBills.findViewById<EditText>(R.id.edtVendorDialog)
        val edtDateDialog = dialogBills.findViewById<EditText>(R.id.edtDateDialog)
        val edtRepeatDialog = dialogBills.findViewById<EditText>(R.id.edtRepeatDialog)
        val edtCommentDialog = dialogBills.findViewById<EditText>(R.id.edtCommentDialog)

        // Make all fields non-editable
        edtTitleDialog?.isEnabled = false
        edtAmountDialog?.isEnabled = false
        edtCategoryDialog?.isEnabled = false
        edtSubcategoryDialog?.isEnabled = false
        edtVendorDialog?.isEnabled = false
        edtDateDialog?.isEnabled = false
        edtRepeatDialog?.isEnabled = false
        edtCommentDialog?.isEnabled = false
    }

    private fun setupDialogIncome() {
        dialogIncome = Dialog(this)
        dialogIncome.setContentView(R.layout.dialog_box_income)
        dialogIncome.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialogIncome.window?.setBackgroundDrawable(getDrawable(R.drawable.dialog_box_income_bg))
        dialogIncome.setCancelable(true)

        // Disable edit buttons and make fields non-editable
        val btnSaveChanges = dialogIncome.findViewById<ImageView>(R.id.imgEditIncome)
        btnSaveChanges?.visibility = View.GONE // Hide the save button

        val edtTitleDialog = dialogIncome.findViewById<EditText>(R.id.edtTitleDialogIncome)
        val edtAmountDialog = dialogIncome.findViewById<EditText>(R.id.edtAmountDialogIncome)
        val edtCategoryDialog = dialogIncome.findViewById<EditText>(R.id.edtCategoryDialogIncome)
        val edtSubcategoryDialog = dialogIncome.findViewById<EditText>(R.id.edtSubcategoryDialogIncome)
        val edtDateDialog = dialogIncome.findViewById<EditText>(R.id.edtDateDialogIncome)
        val edtRepeatDialog = dialogIncome.findViewById<EditText>(R.id.edtRepeatDialogIncome)
        val edtCommentDialog = dialogIncome.findViewById<EditText>(R.id.edtCommentDialogIncome)

        // Make all fields non-editable
        edtTitleDialog?.isEnabled = false
        edtAmountDialog?.isEnabled = false
        edtCategoryDialog?.isEnabled = false
        edtSubcategoryDialog?.isEnabled = false
        edtDateDialog?.isEnabled = false
        edtRepeatDialog?.isEnabled = false
        edtCommentDialog?.isEnabled = false
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

        // Convert Timestamp to String
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val billDateString = bill.date?.let { dateFormat.format(it.toDate()) } ?: ""

        edtTitleDialog?.setText(bill.name)
        edtAmountDialog?.setText(bill.amount)
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

    private fun showIncomeDetailsDialog(income: Income) {
        val edtTitleDialog = dialogIncome.findViewById<EditText>(R.id.edtTitleDialogIncome)
        val edtAmountDialog = dialogIncome.findViewById<EditText>(R.id.edtAmountDialogIncome)
        val edtCategoryDialog = dialogIncome.findViewById<EditText>(R.id.edtCategoryDialogIncome)
        val edtSubcategoryDialog = dialogIncome.findViewById<EditText>(R.id.edtSubcategoryDialogIncome)
        val edtDateDialog = dialogIncome.findViewById<EditText>(R.id.edtDateDialogIncome)
        val edtRepeatDialog = dialogIncome.findViewById<EditText>(R.id.edtRepeatDialogIncome)
        val edtCommentDialog = dialogIncome.findViewById<EditText>(R.id.edtCommentDialogIncome)

        // Convert Timestamp to String
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val incomeDateString = income.date?.let { dateFormat.format(it.toDate()) } ?: ""

        edtTitleDialog?.setText(income.name)
        edtAmountDialog?.setText(income.amount)
        edtCategoryDialog?.setText(income.category)
        edtSubcategoryDialog?.setText(income.subcategory)
        edtDateDialog?.setText(incomeDateString)
        edtRepeatDialog?.setText(income.repeat)
        edtCommentDialog?.setText(income.comment)

        dialogIncome.show()
    }

    override fun onBillClick(position: Int) {
        if (position >= 0 && position < filteredBills.size) {
            val clickedBill = filteredBills[position]
            showBillDetailsDialog(clickedBill)
        } else {
            Toast.makeText(this, "Invalid bill selected. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onIncomeClick(position: Int) {
        if (position >= 0 && position < filteredIncome.size) {
            val clickedIncome = filteredIncome[position]
            showIncomeDetailsDialog(clickedIncome)
        } else {
            Toast.makeText(this, "Invalid income selected. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun logoutUser() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, LogIn::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
