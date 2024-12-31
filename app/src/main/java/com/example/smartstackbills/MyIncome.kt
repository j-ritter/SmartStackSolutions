package com.example.smartstackbills

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.collections.ArrayList

class MyIncome : AppCompatActivity(), MyAdapterIncome.OnIncomeClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var incomeArrayList: ArrayList<Income>
    private lateinit var allIncomeArrayList: ArrayList<Income>
    private lateinit var myAdapterIncome: MyAdapterIncome
    private lateinit var db: FirebaseFirestore
    private var listenerRegistration: ListenerRegistration? = null
    private lateinit var fab: FloatingActionButton
    private var userEmail: String? = null
    private lateinit var dialog: Dialog
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var notificationRecyclerView: RecyclerView
    private var selectedIncome: Income? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_my_income)

        drawerLayout = findViewById(R.id.drawer_layout_income)

        recyclerView = findViewById(R.id.recyclerViewIncome)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)
        userEmail = intent.getStringExtra("USER_EMAIL")

        incomeArrayList = ArrayList()
        allIncomeArrayList = ArrayList()
        myAdapterIncome = MyAdapterIncome(this, incomeArrayList, this)
        recyclerView.adapter = myAdapterIncome

        fab = findViewById(R.id.fabIncome)
        fab.setOnClickListener {
            val intent = Intent(this, createIncome::class.java)
            intent.putExtra("USER_EMAIL", userEmail)
            startActivity(intent)
        }

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottomNavigationViewIncome)
        bottomNavigationView.selectedItemId = R.id.Income
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.Main -> {
                    val intent = Intent(this, MainMenu::class.java)
                    intent.putExtra("USER_EMAIL", userEmail) // Pasar el correo electrónico
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

                R.id.Income -> {
                    // Do nothing since we're already on this screen
                    true
                }
                R.id.Calendar -> {
                    // Intent for Calendar (assumed to be implemented)
                    val intent = Intent(this, CalendarActivity::class.java)
                    intent.putExtra("USER_EMAIL", userEmail) // Pasar el correo electrónico
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

        // Initialize the toolbar and set it as the action bar
        val toolbar: Toolbar = findViewById(R.id.materialToolbarIncome)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Setup NavigationView
        val navView: NavigationView = findViewById(R.id.nav_viewIncome)
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

        // Setup notifications drawer
        notificationRecyclerView = findViewById(R.id.recyclerViewNotifications)
        notificationRecyclerView.layoutManager = LinearLayoutManager(this)


        db = FirebaseFirestore.getInstance()
        setupDialog()
        setupEventChangeListener()

        findViewById<Button>(R.id.btnRecurringIncome).setOnClickListener { filterIncome("recurring") }
        findViewById<Button>(R.id.btnOneTimeIncome).setOnClickListener { filterIncome("one-time") }
        findViewById<Button>(R.id.btnAllIncome).setOnClickListener { filterIncome("all income") }
    }
    private fun saveIncome() {
        val sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        val gson = Gson()
        val json = gson.toJson(incomeArrayList)
        editor.putString("incomeList", json)
        editor.apply()
    }

    private fun setupDialog() {
        dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_box_income)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawable(getDrawable(R.drawable.dialog_box_income_bg))
        dialog.setCancelable(false)

        val btnCloseDialog = dialog.findViewById<Button>(R.id.btnCloseDialogIncome)
        val imgDeleteIncome = dialog.findViewById<ImageView>(R.id.imgDeleteIncome)


        btnCloseDialog.setOnClickListener {
            dialog.dismiss()
        }

        imgDeleteIncome.setOnClickListener {
            deleteIncome()
        }
    }

    private fun setupEventChangeListener() {
        val userUid = FirebaseAuth.getInstance().currentUser?.uid
        if (userUid != null) {
            listenerRegistration = db.collection("users").document(userUid).collection("income")
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        Toast.makeText(this, "Error loading income: ${e.message}", Toast.LENGTH_SHORT).show()
                        Log.e("Firestore Error", e.message.toString())
                        return@addSnapshotListener
                    }

                    if (snapshots != null) {
                        incomeArrayList.clear()
                        allIncomeArrayList.clear()

                        for (document in snapshots.documents) {
                            try {
                                val income = document.toObject(Income::class.java)
                                if (income != null) {
                                    // Validate and handle the `amount` field
                                    if (document.contains("amount")) {
                                        val rawAmount = document["amount"]
                                        income.amount = when (rawAmount) {
                                            is Number -> rawAmount.toDouble()
                                            is String -> rawAmount.toDoubleOrNull() ?: 0.0
                                            else -> 0.0 // Default to 0 for invalid values
                                        }
                                    }
                                    incomeArrayList.add(income)
                                    allIncomeArrayList.add(income)
                                    Log.d("Firestore Data", "Income added: ${income.name}, ${income.date}, ${income.repeat}")
                                }
                            } catch (ex: Exception) {
                                Log.e("DataError", "Error processing document ${document.id}: ${ex.message}")
                            }
                        }
                        saveIncome()
                        filterIncome("all income") // Default filter
                    } else {
                        Log.d("Firestore Data", "No income found")
                    }
                }
        } else {
            Toast.makeText(this, "Error: User not authenticated", Toast.LENGTH_SHORT).show()
            Log.e("Authentication Error", "User not authenticated")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        listenerRegistration?.remove()
    }

    override fun onIncomeClick(position: Int) {
        val item = myAdapterIncome.getItemAtPosition(position)
        // Check if the clicked item is an income item
        if (item is Income) {
            selectedIncome = item
            showIncomeDetailsDialog(item)
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

    private fun showIncomeDetailsDialog(income: Income) {
        val edtTitleDialog = dialog.findViewById<EditText>(R.id.edtTitleDialogIncome)
        val edtAmountDialog = dialog.findViewById<EditText>(R.id.edtAmountDialogIncome)
        val edtCategoryDialog = dialog.findViewById<EditText>(R.id.edtCategoryDialogIncome)
        val edtSubcategoryDialog = dialog.findViewById<EditText>(R.id.edtSubcategoryDialogIncome)
        val edtDateDialog = dialog.findViewById<EditText>(R.id.edtDateDialogIncome)
        val edtRepeatDialog = dialog.findViewById<EditText>(R.id.edtRepeatDialogIncome)
        val edtCommentDialog = dialog.findViewById<EditText>(R.id.edtCommentDialogIncome)


        // Convertir el Timestamp a String
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val incomeDateString =
            if (income.date != null) dateFormat.format(income.date.toDate()) else ""

        edtTitleDialog.setText(income.name)
        edtAmountDialog.setText(String.format(Locale.getDefault(), "%.2f", income.amount))
        edtCategoryDialog.setText(income.category)
        edtSubcategoryDialog.setText(income.subcategory)
        edtDateDialog.setText(incomeDateString)
        edtRepeatDialog.setText(income.repeat)
        edtCommentDialog.setText(income.comment)

        dialog.show()
    }

    private fun deleteIncome() {
        selectedIncome?.let { income ->
            val userUid = FirebaseAuth.getInstance().currentUser?.uid
            if (userUid != null) {
                db.collection("users").document(userUid).collection("income")
                    .document(income.incomeId)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Income deleted successfully", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to delete income", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Error: User not authenticated", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun filterIncome(filter: String) {
        val filteredIncome = ArrayList<Income>()

        // Reset the button background to inactive color

        findViewById<Button>(R.id.btnRecurringIncome).setBackgroundColor(ContextCompat.getColor(this, R.color.filter_inactive))
        findViewById<Button>(R.id.btnOneTimeIncome).setBackgroundColor(ContextCompat.getColor(this, R.color.filter_inactive))
        findViewById<Button>(R.id.btnAllIncome).setBackgroundColor(ContextCompat.getColor(this, R.color.filter_inactive))


        for (income in allIncomeArrayList) {
            when (filter) {
                "recurring" -> {
                    findViewById<Button>(R.id.btnRecurringIncome).setBackgroundColor(ContextCompat.getColor(this, R.color.filter_active))

                        if (income.repeat != "No") {
                            filteredIncome.add(income)
                            Log.d("Filter", "Recurring income added: ${income.name}")
                        }

                }
                "one-time" -> {
                    findViewById<Button>(R.id.btnOneTimeIncome).setBackgroundColor(ContextCompat.getColor(this, R.color.filter_active))

                        if (income.repeat == "No") {
                            filteredIncome.add(income)
                            Log.d("Filter", "One-time income added: ${income.name}")
                        }

                }
                "all income" -> {
                    findViewById<Button>(R.id.btnAllIncome).setBackgroundColor(ContextCompat.getColor(this, R.color.filter_active))
                    // Add all spendings regardless of subcategory
                    filteredIncome.add(income)
                    Log.d("Filter", "All income added: ${income.name}")
                }
            }
        }

        myAdapterIncome.updateIncome(filteredIncome)
        Log.d("Filter", "Filtered income count for $filter: ${filteredIncome.size}")
    }
    private fun groupIncomeByMonth(incomeArrayList: ArrayList<Income>): ArrayList<Any> {
        val groupedIncome = LinkedHashMap<String, MutableList<Income>>()
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

        for (income in incomeArrayList) {
            val monthYear = sdf.format(income.date.toDate())
            if (!groupedIncome.containsKey(monthYear)) {
                groupedIncome[monthYear] = ArrayList()
            }
            groupedIncome[monthYear]?.add(income)
        }

        val items = ArrayList<Any>()
        for ((monthYear, income) in groupedIncome) {
            items.add(monthYear)
            items.addAll(income)
        }

        return items
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

    private fun logoutUser() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, LogIn::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
