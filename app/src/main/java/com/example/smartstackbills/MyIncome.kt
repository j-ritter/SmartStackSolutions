package com.example.smartstackbills

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_my_income)

        recyclerView = findViewById(R.id.recyclerView)
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

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
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
                else -> false
            }
        }

        db = FirebaseFirestore.getInstance()
        setupDialog()
        setupEventChangeListener()

        findViewById<Button>(R.id.btnRecurringIncome).setOnClickListener { filterIncome("recurring") }
        findViewById<Button>(R.id.btnOneTimeIncome).setOnClickListener { filterIncome("one-time") }
    }

    private fun setupDialog() {
        dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_box_income)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawable(getDrawable(R.drawable.dialog_box_income_bg))
        dialog.setCancelable(false)

        val btnCloseDialog = dialog.findViewById<Button>(R.id.btnCloseDialogIncome)
        btnCloseDialog.setOnClickListener {
            dialog.dismiss()
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
                            val income = document.toObject(Income::class.java)
                            if (income != null) {
                                incomeArrayList.add(income)
                                allIncomeArrayList.add(income)
                                Log.d("Firestore Data", "Income added: ${income.name}, ${income.date}, ${income.repeat}")
                            }
                        }
                        filterIncome("recurring") // Default filter
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
        val income = incomeArrayList[position]
        showIncomeDetailsDialog(income)
    }

    private fun showIncomeDetailsDialog(income: Income) {
        val edtTitleDialog = dialog.findViewById<EditText>(R.id.edtTitleDialogIncome)
        val edtAmountDialog = dialog.findViewById<EditText>(R.id.edtAmountDialogIncome)
        val edtCategoryDialog = dialog.findViewById<EditText>(R.id.edtCategoryDialogIncome)
        val edtSubcategoryDialog = dialog.findViewById<EditText>(R.id.edtSubcategoryDialogIncome)
        val edtDateDialog = dialog.findViewById<EditText>(R.id.edtDateDialogIncome)
        val edtRepeatDialog = dialog.findViewById<EditText>(R.id.edtRepeatDialogIncome)
        val edtCommentDialog = dialog.findViewById<EditText>(R.id.edtCommentDialogIncome)

        edtTitleDialog.setText(income.name)
        edtAmountDialog.setText(income.amount)
        edtCategoryDialog.setText(income.category)
        edtSubcategoryDialog.setText(income.subcategory)
        edtDateDialog.setText(income.date)
        edtRepeatDialog.setText(income.repeat)
        edtCommentDialog.setText(income.comment)

        dialog.show()
    }

    private fun filterIncome(filter: String) {
        val filteredIncome = ArrayList<Income>()

        for (income in allIncomeArrayList) {
            when (filter) {
                "recurring" -> {
                    if (income.repeat != "No") {
                        filteredIncome.add(income)
                        Log.d("Filter", "Recurring income added: ${income.name}")
                    }
                }
                "one-time" -> {
                    if (income.repeat == "No") {
                        filteredIncome.add(income)
                        Log.d("Filter", "One-time income added: ${income.name}")
                    }
                }
            }
        }

        myAdapterIncome.updateIncome(filteredIncome)
        Log.d("Filter", "Filtered income count for $filter: ${filteredIncome.size}")
    }
}