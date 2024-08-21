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
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import MyCalendarView
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.collections.ArrayList
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MySpendings : AppCompatActivity(), MyAdapterSpendings.OnSpendingClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var spendingsArrayList: ArrayList<Spendings>
    private lateinit var allSpendingsArrayList: ArrayList<Spendings>
    private lateinit var myAdapterSpendings: MyAdapterSpendings
    private lateinit var db: FirebaseFirestore
    private var listenerRegistration: ListenerRegistration? = null
    private lateinit var fab: FloatingActionButton
    private var userEmail: String? = null
    private lateinit var dialog: Dialog
    private var selectedSpending: Spendings? = null
    private lateinit var drawerLayout: DrawerLayout

    private val essentialSubcategories = setOf(
        "Rent", "Mortgage", "Home maintenance", "Utilities",
        "Mobile phone", "Landline phone", "Internet",
        "Health insurance", "Life insurance", "Car insurance", "Home insurance",
        "Fuel", "Vehicle maintenance", "Public transportation",
        "Doctor visits", "Dental care", "Prescription medications", "Medical equipment",
        "Groceries", "Clothing", "Household goods", "Personal care products",
        "Income tax", "Property tax", "Sales tax", "Self-employment tax", "Capital gains tax",
        "Tuition fees", "Textbooks", "School supplies"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_my_spendings)

        drawerLayout = findViewById(R.id.drawer_layout_spendings)

        recyclerView = findViewById(R.id.recyclerViewSpendings)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)
        userEmail = intent.getStringExtra("USER_EMAIL")

        spendingsArrayList = ArrayList()
        allSpendingsArrayList = ArrayList()
        myAdapterSpendings = MyAdapterSpendings(this, spendingsArrayList, this)
        recyclerView.adapter = myAdapterSpendings

        fab = findViewById(R.id.fabSpendings)
        fab.setOnClickListener {
            val intent = Intent(this, createSpending::class.java)
            intent.putExtra("USER_EMAIL", userEmail)
            startActivity(intent)
        }

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottomNavigationViewSpendings)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.Main -> {
                    val intent = Intent(this, MainMenu::class.java)
                    startActivity(intent)
                    true
                }
                R.id.Bills -> {
                    val intent = Intent(this, MyBills::class.java)
                    startActivity(intent)
                    true
                }
                R.id.Spendings -> {
                    // Do nothing since we're already on this screen
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

        // Initialize filter buttons
        findViewById<Button>(R.id.btnSpendingsAll).setOnClickListener { filterSpendings("all") }
        findViewById<Button>(R.id.btnEssential).setOnClickListener { filterSpendings("essential") }
        findViewById<Button>(R.id.btnNonEssential).setOnClickListener { filterSpendings("non-essential") }
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
                        // Show essential spendings by default
                        saveSpendings()
                        filterSpendings("essential")
                    } else {
                        Log.d("Firestore Data", "No spendings found")
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

    override fun onSpendingClick(position: Int) {
        if (position >= 0 && position < spendingsArrayList.size) {
            val spending = spendingsArrayList[position]
            selectedSpending = spending
            showSpendingDetailsDialog(spending)
        } else {
            Toast.makeText(this, "Spending item not found", Toast.LENGTH_SHORT).show()
        }
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_nav, menu)
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
        edtAmountDialog.setText(spending.amount)
        edtCategoryDialog.setText(spending.category)
        edtSubcategoryDialog.setText(spending.subcategory)
        edtVendorDialog.setText(spending.vendor)
        edtDateDialog.setText(spendingDateString)
        edtCommentDialog.setText(spending.comment)


    }

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
        // Reset the button background to inactive color
        findViewById<Button>(R.id.btnSpendingsAll).setBackgroundColor(ContextCompat.getColor(this, R.color.filter_inactive))
        findViewById<Button>(R.id.btnEssential).setBackgroundColor(ContextCompat.getColor(this, R.color.filter_inactive))
        findViewById<Button>(R.id.btnNonEssential).setBackgroundColor(ContextCompat.getColor(this, R.color.filter_inactive))

        for (spending in allSpendingsArrayList) {
            when (filter) {
                "essential" -> {
                    findViewById<Button>(R.id.btnEssential).setBackgroundColor(ContextCompat.getColor(this, R.color.filter_active))
                    for (spending in allSpendingsArrayList) {
                        if (spending.subcategory in essentialSubcategories) {
                            filteredSpendings.add(spending)
                            Log.d("Filter", "Essential spending added: ${spending.name}")
                        }
                    }
                }
                "non-essential" -> {
                    findViewById<Button>(R.id.btnNonEssential).setBackgroundColor(ContextCompat.getColor(this, R.color.filter_active))
                    for (spending in allSpendingsArrayList) {
                        if (spending.subcategory !in essentialSubcategories) {
                            filteredSpendings.add(spending)
                            Log.d("Filter", "Non-essential spending added: ${spending.name}")
                        }
                    }
                }
                "all" -> {
                    findViewById<Button>(R.id.btnSpendingsAll).setBackgroundColor(ContextCompat.getColor(this, R.color.filter_active))
                    filteredSpendings.addAll(allSpendingsArrayList)
                    Log.d("Filter", "All spendings added")
                }
            }
        }

        myAdapterSpendings.updateSpendings(filteredSpendings)
        Log.d("Filter", "Filtered spendings count for $filter: ${filteredSpendings.size}")
    }
    private fun groupSpendingsByMonth(spendingsArrayList: ArrayList<Spendings>): ArrayList<Any> {
        val groupedSpendings = LinkedHashMap<String, MutableList<Spendings>>()
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

        for (spending in spendingsArrayList) {
            val monthYear = sdf.format(spending.date.toDate())
            if (!groupedSpendings.containsKey(monthYear)) {
                groupedSpendings[monthYear] = ArrayList()
            }
            groupedSpendings[monthYear]?.add(spending)
        }

        val items = ArrayList<Any>()
        for ((monthYear, bills) in groupedSpendings) {
            items.add(monthYear)
            items.addAll(bills)
        }

        return items
    }

    private fun logoutUser() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, LogIn::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

}
