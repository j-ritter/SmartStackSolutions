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
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
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

        recyclerView = findViewById(R.id.recyclerView)
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
        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
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
                else -> false
            }
        }

        db = FirebaseFirestore.getInstance()
        setupDialog()
        setupEventChangeListener()

        // Initialize filter buttons
        findViewById<Button>(R.id.btnEssential).setOnClickListener { filterSpendings("essential") }
        findViewById<Button>(R.id.btnNonEssential).setOnClickListener { filterSpendings("non-essential") }
    }

    private fun setupDialog() {
        dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_box_spendings)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawable(getDrawable(R.drawable.dialog_box_spendings_bg))
        dialog.setCancelable(false)

        val btnCloseDialog = dialog.findViewById<Button>(R.id.btnCloseDialogSpendings)
        btnCloseDialog.setOnClickListener {
            dialog.dismiss()
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
        val spending = spendingsArrayList[position]
        showSpendingDetailsDialog(spending)
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
        edtDateDialog.setText(spending.date)
        edtCommentDialog.setText(spending.comment)

        dialog.show()
    }

    private fun filterSpendings(filter: String) {
        val filteredSpendings = ArrayList<Spendings>()

        for (spending in allSpendingsArrayList) {
            when (filter) {
                "essential" -> {
                    if (spending.subcategory in essentialSubcategories) {
                        filteredSpendings.add(spending)
                        Log.d("Filter", "Essential spending added: ${spending.name}")
                    }
                }
                "non-essential" -> {
                    if (spending.subcategory !in essentialSubcategories) {
                        filteredSpendings.add(spending)
                        Log.d("Filter", "Non-essential spending added: ${spending.name}")
                    }
                }
            }
        }

        myAdapter.updateSpendings(filteredSpendings)
        Log.d("Filter", "Filtered spendings count for $filter: ${filteredSpendings.size}")
    }
}