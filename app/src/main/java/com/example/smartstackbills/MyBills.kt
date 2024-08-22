package com.example.smartstackbills


import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import MyCalendarView
import android.content.Context
import androidx.core.content.ContextCompat
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

class MyBills : AppCompatActivity(), MyAdapter.OnBillClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var billsArrayList: ArrayList<Bills>
    private lateinit var allBillsArrayList: ArrayList<Bills> // Lista para almacenar todas las facturas
    private lateinit var myAdapter: MyAdapter
    private lateinit var db: FirebaseFirestore
    private var listenerRegistration: ListenerRegistration? = null
    private lateinit var fab: FloatingActionButton
    private var userEmail: String? = null
    private lateinit var dialog: Dialog
    private var selectedBill: Bills? = null
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_my_bills)

        drawerLayout = findViewById(R.id.drawer_layout)

        recyclerView = findViewById(R.id.recyclerViewBills)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)
        userEmail = intent.getStringExtra("USER_EMAIL")

        billsArrayList = ArrayList()
        allBillsArrayList = ArrayList() // Inicializamos la lista para todas las facturas
        myAdapter = MyAdapter(this, billsArrayList, this)
        recyclerView.adapter = myAdapter

        fab = findViewById(R.id.fabBills)
        fab.setOnClickListener {
            val intent = Intent(this, createBill::class.java)
            intent.putExtra("USER_EMAIL", userEmail)
            startActivity(intent)
        }
        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottomNavigationViewBills)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.Main -> {
                    val intent = Intent(this, MainMenu::class.java)
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
                R.id.Income -> {  // New navigation option for Income
                    val intent = Intent(this, MyIncome::class.java)
                    intent.putExtra("USER_EMAIL", userEmail)
                    startActivity(intent)
                    true
                }
                R.id.Calendar -> {
                    val intent = Intent(this, MyCalendarView::class.java)
                    intent.putExtra("USER_EMAIL", userEmail)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
        // Initialize the toolbar and set it as the action bar
        val toolbar: Toolbar = findViewById(R.id.materialToolbarBills)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Setup NavigationView
        val navView: NavigationView = findViewById(R.id.nav_viewBills)
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

        // Inicializar botones de filtro
        findViewById<Button>(R.id.btnIncoming).setOnClickListener { filterBills("incoming") }
        findViewById<Button>(R.id.btnDue).setOnClickListener { filterBills("due") }
        findViewById<Button>(R.id.btnRecurring).setOnClickListener { filterBills("recurring") }
        findViewById<Button>(R.id.btnAllBills).setOnClickListener { filterBills("all") }
    }
    private fun loadBills(): ArrayList<Bills> {
        val sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPref.getString("billsList", null)
        val type = object : TypeToken<ArrayList<Bills>>() {}.type
        return gson.fromJson(json, type) ?: ArrayList()
    }

    private fun saveBills() {
        val sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        val gson = Gson()
        val json = gson.toJson(billsArrayList)
        editor.putString("billsList", json)
        editor.apply()
    }

    private fun setupDialog() {
        dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_box_bill)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawable(getDrawable(R.drawable.dialog_box_bills_bg))
        dialog.setCancelable(false)

        val btnCloseDialog = dialog.findViewById<Button>(R.id.btnCloseDialog)
        val imgDeleteBill = dialog.findViewById<ImageView>(R.id.imgDeleteBill)
        val imgEditBill = dialog.findViewById<ImageView>(R.id.imgEditBill)

        btnCloseDialog.setOnClickListener {
            dialog.dismiss()
        }

        imgDeleteBill.setOnClickListener {
            deleteBill()
        }
    }
    private fun setupEventChangeListener() {
        val userUid = FirebaseAuth.getInstance().currentUser?.uid
        if (userUid != null) {
            listenerRegistration = db.collection("users").document(userUid).collection("bills")
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        Toast.makeText(this, "Error loading bills: ${e.message}", Toast.LENGTH_SHORT).show()
                        Log.e("Firestore Error", e.message.toString())
                        return@addSnapshotListener
                    }

                    if (snapshots != null) {
                        billsArrayList.clear()
                        allBillsArrayList.clear() // Limpiamos la lista de todas las facturas
                        for (document in snapshots.documents) {
                            val bill = document.toObject(Bills::class.java)
                            if (bill != null) {
                                billsArrayList.add(bill)
                                allBillsArrayList.add(bill) // Añadimos a la lista de todas las facturas
                                Log.d("Firestore Data", "Bill added: ${bill.name}, ${bill.date}, ${bill.paid}")
                            }
                        }
                        // Mostrar facturas "incoming" por defecto
                        filterBills("all")
                    } else {
                        Log.d("Firestore Data", "No bills found")
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

    override fun onBillClick(position: Int) {
        if (position >= 0 && position < billsArrayList.size) {
            val bill = billsArrayList[position]
            selectedBill = bill
            showBillDetailsDialog(bill)
        } else {
            Toast.makeText(this, "Bill not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showBillDetailsDialog(bill: Bills) {
        val edtTitleDialog = dialog.findViewById<EditText>(R.id.edtTitleDialog)
        val edtAmountDialog = dialog.findViewById<EditText>(R.id.edtAmountDialog)
        val edtCategoryDialog = dialog.findViewById<EditText>(R.id.edtCategoryDialog)
        val edtSubcategoryDialog = dialog.findViewById<EditText>(R.id.edtSubcategoryDialog)
        val edtVendorDialog = dialog.findViewById<EditText>(R.id.edtVendorDialog)
        val edtDateDialog = dialog.findViewById<EditText>(R.id.edtDateDialog)
        val edtRepeatDialog = dialog.findViewById<EditText>(R.id.edtRepeatDialog)
        val edtCommentDialog = dialog.findViewById<EditText>(R.id.edtCommentDialog)
        val edtAttachmentDialog = dialog.findViewById<ImageView>(R.id.edtAttachmentDialog)
        val attachmentUri = bill.attachment
        val btnSaveChanges = dialog.findViewById<Button>(R.id.btnSaveChanges)
        val btnEditChanges = dialog.findViewById<ImageView>(R.id.imgEditBill)

        // Convertir el Timestamp a String
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val billDateString = if (bill.date != null) dateFormat.format(bill.date.toDate()) else ""

        if (attachmentUri != null) {
            edtAttachmentDialog.setImageURI(Uri.parse(attachmentUri))
            edtAttachmentDialog.visibility = View.VISIBLE
        } else {
            edtAttachmentDialog.visibility = View.GONE
        }
        dialog.show()

        edtTitleDialog.setText(bill.name)
        edtAmountDialog.setText(bill.amount)
        edtCategoryDialog.setText(bill.category)
        edtRepeatDialog.setText(bill.repeat)
        edtSubcategoryDialog.setText(bill.subcategory)
        edtVendorDialog.setText(bill.vendor)
        edtDateDialog.setText(billDateString) // Usar la cadena de fecha
        edtCommentDialog.setText(bill.comment)

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
            if (userUid != null && selectedBill != null) {
                // Update the bill object with new values
                selectedBill?.name = edtTitleDialog.text.toString()
                selectedBill?.amount = edtAmountDialog.text.toString()
                selectedBill?.comment = edtCommentDialog.text.toString()

                btnSaveChanges.visibility = View.VISIBLE

                // Save the updated bill to Firebase
                db.collection("users").document(userUid).collection("bills")
                    .document(selectedBill!!.billId)
                    .set(selectedBill!!)
                    .addOnSuccessListener {
                        // Update the local list
                        val index = billsArrayList.indexOfFirst { it.billId == selectedBill?.billId }
                        if (index != -1) {
                            billsArrayList[index] = selectedBill!!
                            myAdapter.notifyItemChanged(index)
                        }
                        Toast.makeText(this, "Bill updated successfully", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to update bill: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Error: Unable to update bill", Toast.LENGTH_SHORT).show()
            }
        }}

    private fun deleteBill() {
        selectedBill?.let { bill ->
            val userUid = FirebaseAuth.getInstance().currentUser?.uid
            if (userUid != null) {
                db.collection("users").document(userUid).collection("bills")
                    .document(bill.billId)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Bill deleted successfully", Toast.LENGTH_SHORT).show()
                        billsArrayList.remove(bill)
                        myAdapter.notifyDataSetChanged()

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

    private fun filterBills(filter: String) {
        val filteredBills = ArrayList<Bills>()
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val currentDate = Calendar.getInstance().time

        findViewById<Button>(R.id.btnAllBills).setBackgroundColor(ContextCompat.getColor(this, R.color.filter_inactive))
        findViewById<Button>(R.id.btnIncoming).setBackgroundColor(ContextCompat.getColor(this, R.color.filter_inactive))
        findViewById<Button>(R.id.btnDue).setBackgroundColor(ContextCompat.getColor(this, R.color.filter_inactive))
        findViewById<Button>(R.id.btnRecurring).setBackgroundColor(ContextCompat.getColor(this, R.color.filter_inactive))

        for (bill in allBillsArrayList) { // Usamos allBillsArrayList para filtrar
            try {
                val billDate = if (bill.date != null) bill.date.toDate() else null

                when (filter) {
                    "all" -> {
                        findViewById<Button>(R.id.btnAllBills).setBackgroundColor(ContextCompat.getColor(this, R.color.filter_active))
                        // Show all bills except the ones that are marked as paid
                        if (!bill.paid) {
                            filteredBills.add(bill)
                            Log.d("Filter", "All (unpaid) bill added: ${bill.name}")
                        }
                    }
                    "due" -> {
                        findViewById<Button>(R.id.btnDue).setBackgroundColor(ContextCompat.getColor(this, R.color.filter_active))
                        if (billDate != null && billDate.before(currentDate)) {
                            filteredBills.add(bill)
                            Log.d("Filter", "Due bill added: ${bill.name}")
                        }
                    }
                    "recurring" -> {
                        findViewById<Button>(R.id.btnRecurring).setBackgroundColor(ContextCompat.getColor(this, R.color.filter_active))
                        if (bill.repeat != "No") {
                            filteredBills.add(bill)
                            Log.d("Filter", "Recurring bill added: ${bill.name}")
                        }
                    }
                    "incoming" -> {
                        findViewById<Button>(R.id.btnIncoming).setBackgroundColor(ContextCompat.getColor(this, R.color.filter_active))
                        if (billDate != null && billDate.after(currentDate) && !bill.paid && bill.repeat == "No") {
                            filteredBills.add(bill)
                            Log.d("Filter", "Incoming bill added: ${bill.name}")
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("Filter Error", "Error parsing date for bill: ${bill.name}")
            }
        }

        if (filter == "incoming") {
            // Mostrar todas las facturas menos las pagadas
            filteredBills.clear()
            for (bill in allBillsArrayList) {
                if (!bill.paid) {
                    filteredBills.add(bill)
                    Log.d("Filter", "Incoming bill added: ${bill.name}")
                }
            }
        }

        myAdapter.updateBills(filteredBills)
        Log.d("Filter", "Filtered bills count for $filter: ${filteredBills.size}")

        myAdapter.notifyDataSetChanged()
    }

    private fun groupBillsByMonth(billsArrayList: ArrayList<Bills>): ArrayList<Any> {
        val groupedBills = LinkedHashMap<String, MutableList<Bills>>()
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

        for (bill in billsArrayList) {
            val monthYear = sdf.format(bill.date.toDate())
            if (!groupedBills.containsKey(monthYear)) {
                groupedBills[monthYear] = ArrayList()
            }
            groupedBills[monthYear]?.add(bill)
        }

        val items = ArrayList<Any>()
        for ((monthYear, bills) in groupedBills) {
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