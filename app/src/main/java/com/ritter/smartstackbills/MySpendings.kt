package com.ritter.smartstackbills

import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MySpendings : AppCompatActivity(), MyAdapterSpendings.OnSpendingClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var spendingsArrayList: ArrayList<Spendings>
    private lateinit var allSpendingsArrayList: ArrayList<Spendings>
    private lateinit var myAdapter: MyAdapterSpendings
    private lateinit var db: FirebaseFirestore
    private var listenerRegistration: ListenerRegistration? = null
    private lateinit var fab: FloatingActionButton
    private var userEmail: String? = null
    private lateinit var dialog: Dialog
    private var selectedSpending: Spendings? = null
    private lateinit var drawerLayout: DrawerLayout

    private val essentialSubcategories = setOf(
        "Rent", "Mortgage", "Home maintenance", "Utilities", "Repairs and renovations",
        "Property management", "Home security",
        "Mobile phone", "Landline phone", "Internet",
        "Health insurance", "Life insurance", "Car insurance", "Home insurance",
        "Disability insurance", "Business insurance",
        "Fuel", "Vehicle maintenance", "Public transportation", "Tolls", "Car lease",
        "Bank fees", "Loan interest", "Credit card fees", "Income tax", "Property tax",
        "Sales tax", "Self-employment tax", "Capital gains tax", "VAT (Value Added Tax)",
        "Doctor visits", "Dental care", "Prescription medications", "Medical equipment",
        "Mental health services", "Vaccinations",
        "Tuition fees", "Textbooks", "School supplies", "Professional development",
        "Clothing", "Household goods", "Personal care products",
        "Groceries - Basic Food", "Groceries - Household Necessities", "Groceries - Frozen Foods", "Groceries - Organic Products"
    )

    private val essentialCategories = setOf(
        "Accommodation", "Communication", "Insurance", "Transportation",
        "Finances/Fees", "Taxes", "Health", "Education", "Shopping & Consumption"
    )

    private val nonEssentialSubcategories = setOf(
        "Streaming services", "Movies", "Gym memberships", "Software subscriptions",
        "Magazine/newspaper subscriptions", "Clubs and associations", "Music services",
        "Cable/satellite TV", "Messaging services", "Cloud storage", "VPN services", "VOIP services",
        "Investment fees", "Brokerage fees", "Financial advisor fees", "ATM withdrawal fees",
        "Foreign transaction fees", "Travel insurance", "Pet insurance",
        "Entertainment", "Dining out", "Hobbies", "Movies", "Vacation", "Gadgets",
        "Luxury tax", "Health supplements", "Alternative medicine",
        "Online courses", "Extracurricular activities", "Tutoring", "Educational software",
        "Electronics", "Beauty & cosmetics", "Luxury goods", "Office supplies", "Gifts",
        "Groceries - Beverages", "Groceries - Alcoholic Beverages", "Groceries - Snacks and Sweets", "Groceries - Luxury Foods",
        "Miscellaneous", "Donations", "Gambling", "Unexpected expenses", "Legal fees",
        "Lottery tickets", "Pet expenses", "Festivals & events"
    )

    private val nonEssentialCategories = setOf(
        "Subscription and Memberships", "Others"
    )


    private val spendingsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // This will be triggered when the broadcast is received
            refreshSpendingsList()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_my_spendings)

        recyclerView = findViewById(R.id.recyclerViewSpendings)
        drawerLayout = findViewById(R.id.drawer_layout_spendings)
        recyclerView = findViewById(R.id.recyclerViewSpendings)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)
        userEmail = intent.getStringExtra("USER_EMAIL")

        spendingsArrayList = ArrayList()
        allSpendingsArrayList = ArrayList()
        myAdapter = MyAdapterSpendings(this, spendingsArrayList, this)
        recyclerView.adapter = myAdapter

        fab = findViewById(R.id.fabSpendings)
        fab.setOnClickListener {
            val intent = Intent(this, createSpending::class.java)
            intent.putExtra("USER_EMAIL", userEmail)
            startActivity(intent)
        }
        registerReceiver(spendingsReceiver, IntentFilter("com.example.smartstackbills.REFRESH_SPENDINGS"))

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottomNavigationViewSpendings)
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
                    intent.putExtra("USER_EMAIL", userEmail) // Pasar el correo electrónico
                    startActivity(intent)
                    true
                }
                R.id.Spendings -> {
                    val intent = Intent(this, MySpendings::class.java)
                    intent.putExtra("USER_EMAIL", userEmail) // Pasar el correo electrónico
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
                    val intent = Intent(this,CalendarActivity::class.java)
                    intent.putExtra("USER_EMAIL", userEmail)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
        // Initialize the toolbar and set it as the action bar
        val toolbar: Toolbar = findViewById(R.id.materialToolbarSpendings)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
        // Setup NavigationView
        val navView: NavigationView = findViewById(R.id.nav_viewSpendings)
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

        db = FirebaseFirestore.getInstance()
        setupDialog()
        setupEventChangeListener()

        // Check if the activity was started with a specific filter
        val filterType = intent.getStringExtra("FILTER_TYPE") ?: "all"  // Default to "all"
        filterSpendings(filterType)

        // Initialize filter buttons
        findViewById<Button>(R.id.btnEssential).setOnClickListener { filterSpendings("essential") }
        findViewById<Button>(R.id.btnNonEssential).setOnClickListener { filterSpendings("non-essential") }
        findViewById<Button>(R.id.btnSpendingsAll).setOnClickListener { filterSpendings("all") }
    }
    private fun loadSpendings(): ArrayList<Spendings> {
        val sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPref.getString("spendingsList", null)
        val type = object : TypeToken<ArrayList<Spendings>>() {}.type
        return gson.fromJson(json, type) ?: ArrayList()
    }

    private fun saveSpendings() {
        val sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        val gson = Gson()
        val json = gson.toJson(spendingsArrayList)
        editor.putString("spendingsList", json)
        editor.apply()
    }

    private fun setupDialog() {
        dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_box_spendings)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawable(getDrawable(R.drawable.dialog_box_spendings_bg))
        dialog.setCancelable(false)

        val btnCloseDialog = dialog.findViewById<Button>(R.id.btnCloseDialogSpendings)
        val imgDeleteSpending = dialog.findViewById<ImageView>(R.id.imgDeleteSpendings)
        val imgEditSpending = dialog.findViewById<ImageView>(R.id.imgEditSpendings)

        btnCloseDialog.setOnClickListener {
            dialog.dismiss()
        }
        imgDeleteSpending.setOnClickListener {
            deleteSpending()
        }
    }

    private fun setupEventChangeListener() {
        val userUid = FirebaseAuth.getInstance().currentUser?.uid
        if (userUid != null) {
            listenerRegistration = db.collection("users").document(userUid).collection("spendings")
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        Toast.makeText(this, "Error loading spendings: ${e.message}", Toast.LENGTH_SHORT).show()
                        Log.e("Firestore Error", e.message.toString())
                        return@addSnapshotListener
                    }

                    if (snapshots != null) {
                        spendingsArrayList.clear()
                        allSpendingsArrayList.clear()
                        for (document in snapshots.documents) {
                            val spending = document.toObject(Spendings::class.java)
                            if (spending != null) {
                                spendingsArrayList.add(spending)
                                allSpendingsArrayList.add(spending)
                                Log.d("Firestore Data", "Spending added: ${spending.name}, ${spending.date}")
                            }
                        }
                        // Notify the adapter of the updated data
                        myAdapter.updateSpendings(spendingsArrayList)

                        // Save the updated list 
                        saveSpendings()
                        // Show all spendings by default or apply filter if specified
                        filterSpendings("all")
                    } else {
                        Log.d("Firestore Data", "No spendings found")
                    }
                }
        } else {
            Toast.makeText(this, "Error: User not authenticated", Toast.LENGTH_SHORT).show()
            Log.e("Authentication Error", "User not authenticated")
        }
    }
    private fun refreshSpendingsList() {
        val userUid = FirebaseAuth.getInstance().currentUser?.uid
        if (userUid != null) {
            db.collection("users").document(userUid).collection("spendings")
                .get()
                .addOnSuccessListener { documents ->
                    spendingsArrayList.clear()
                    allSpendingsArrayList.clear()
                    for (document in documents) {
                        val spending = document.toObject(Spendings::class.java)
                        spendingsArrayList.add(spending)
                        allSpendingsArrayList.add(spending)
                    }
                    // Notify the adapter of the updated data
                    myAdapter.updateSpendings(spendingsArrayList)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error loading spendings: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        listenerRegistration?.remove()
    }

    override fun onSpendingClick(position: Int) {
        val item = myAdapter.getItemAtPosition(position)
        // Check if the clicked item is a spending
        if (item is Spendings) {
            selectedSpending = item
            showSpendingDetailsDialog(item)
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

    private fun showSpendingDetailsDialog(spending: Spendings) {
        val edtTitleDialog = dialog.findViewById<EditText>(R.id.edtTitleDialogSpendings)
        val edtAmountDialog = dialog.findViewById<EditText>(R.id.edtAmountDialogSpendings)
        val edtCategoryDialog = dialog.findViewById<EditText>(R.id.edtCategoryDialogSpendings)
        val edtSubcategoryDialog = dialog.findViewById<EditText>(R.id.edtSubcategoryDialogSpendings)
        val edtVendorDialog = dialog.findViewById<EditText>(R.id.edtVendorDialogSpendings)
        val edtDateDialog = dialog.findViewById<EditText>(R.id.edtDateDialogSpendings)
        val edtCommentDialog = dialog.findViewById<EditText>(R.id.edtCommentDialogSpendings)
        val edtAttachmentDialog = dialog.findViewById<ImageView>(R.id.edtAttachmentDialogSpendings)
        val attachmentUri = spending.attachment
        val btnSaveChanges = dialog.findViewById<Button>(R.id.btnSaveChangesSpendings)
        val btnEditChanges = dialog.findViewById<ImageView>(R.id.imgEditSpendings)

        // Convertir el Timestamp a String
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val spendingDateString = if (spending.date != null) dateFormat.format(spending.date.toDate()) else ""

        if (attachmentUri != null) {
            edtAttachmentDialog.setImageURI(Uri.parse(attachmentUri))
            edtAttachmentDialog.visibility = View.VISIBLE
        } else {
            edtAttachmentDialog.visibility = View.GONE
        }
        dialog.show()

        edtTitleDialog.setText(spending.name)
        edtAmountDialog.setText(String.format(Locale.getDefault(), "%.2f", spending.amount))

        edtCategoryDialog.setText(spending.category ?: "-")
        edtSubcategoryDialog.setText(spending.subcategory ?: "-")
        edtVendorDialog.setText(spending.vendor ?: "-")
        edtDateDialog.setText(spendingDateString)
        edtCommentDialog.setText(spending.comment)

        // Initially disable fields
        edtTitleDialog.isEnabled = false
        edtAmountDialog.isEnabled = false
        edtCommentDialog.isEnabled = false

        // Hide save button initially
        btnSaveChanges.visibility = View.GONE

        btnEditChanges.setOnClickListener {
            edtTitleDialog.isEnabled = true
            edtAmountDialog.isEnabled = true
            edtCommentDialog.isEnabled = true

            btnSaveChanges.visibility = View.VISIBLE
        }
        btnSaveChanges.setOnClickListener {
            val userUid = FirebaseAuth.getInstance().currentUser?.uid
            if (userUid != null && selectedSpending != null) {
                // Update the bill object with new values
                selectedSpending?.name = edtTitleDialog.text.toString()
                selectedSpending?.amount = edtAmountDialog.text.toString().toDoubleOrNull() ?: 0.0

                selectedSpending?.comment = edtCommentDialog.text.toString()

                btnSaveChanges.visibility = View.VISIBLE

                // Save the updated bill to Firebase
                db.collection("users").document(userUid).collection("spendings")
                    .document(selectedSpending!!.spendingId)
                    .set(selectedSpending!!)
                    .addOnSuccessListener {
                        // Update the local list
                        val index = spendingsArrayList.indexOfFirst { it.spendingId == selectedSpending?.spendingId }
                        if (index != -1) {
                            spendingsArrayList[index] = selectedSpending!!
                            myAdapter.notifyItemChanged(index)
                        }
                        Toast.makeText(this, "'Closed Payment' updated successfully", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to update 'Closed Payment': ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Error: Unable to update 'Closed Payment'", Toast.LENGTH_SHORT).show()
            }
        }}

    private fun deleteSpending() {
        selectedSpending?.let { spending ->
            val userUid = FirebaseAuth.getInstance().currentUser?.uid
            if (userUid != null) {
                db.collection("users").document(userUid).collection("spendings")
                    .document(spending.spendingId)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Spending deleted successfully", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to delete bill", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Error: User not authenticated", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun filterSpendings(filter: String) {
        val filteredSpendings = ArrayList<Spendings>()

        // Reset button colors
        findViewById<Button>(R.id.btnSpendingsAll).setBackgroundColor(ContextCompat.getColor(this, R.color.filter_inactive))
        findViewById<Button>(R.id.btnEssential).setBackgroundColor(ContextCompat.getColor(this, R.color.filter_inactive))
        findViewById<Button>(R.id.btnNonEssential).setBackgroundColor(ContextCompat.getColor(this, R.color.filter_inactive))

        for (spending in allSpendingsArrayList) {
            val subcategory = spending.subcategory?.trim()
            val category = spending.category?.trim()

            val isEssential = when {
                subcategory in essentialSubcategories -> true
                category in essentialCategories && (subcategory == null || subcategory.isEmpty()) -> true
                else -> false
            }

            val isNonEssential = when {
                subcategory in nonEssentialSubcategories -> true
                category in nonEssentialCategories && (subcategory == null || subcategory.isEmpty()) -> true
                else -> false
            }

            when (filter) {
                "essential" -> {
                    findViewById<Button>(R.id.btnEssential).setBackgroundColor(ContextCompat.getColor(this, R.color.filter_active))
                    if (isEssential) filteredSpendings.add(spending)
                }
                "non-essential" -> {
                    findViewById<Button>(R.id.btnNonEssential).setBackgroundColor(ContextCompat.getColor(this, R.color.filter_active))
                    if (isNonEssential) filteredSpendings.add(spending)
                }
                "all" -> {
                    findViewById<Button>(R.id.btnSpendingsAll).setBackgroundColor(ContextCompat.getColor(this, R.color.filter_active))
                    filteredSpendings.add(spending)
                }
            }
        }

        myAdapter.updateSpendings(filteredSpendings)
        Log.d("Filter", "Filtered spendings count for $filter: ${filteredSpendings.size}")
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