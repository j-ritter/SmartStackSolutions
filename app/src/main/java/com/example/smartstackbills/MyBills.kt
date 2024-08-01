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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_my_bills)

        recyclerView = findViewById(R.id.recyclerView)
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

        db = FirebaseFirestore.getInstance()
        setupDialog()
        setupEventChangeListener()

        // Inicializar botones de filtro
        findViewById<Button>(R.id.btnIncoming).setOnClickListener { filterBills("incoming") }
        findViewById<Button>(R.id.btnDue).setOnClickListener { filterBills("due") }
        findViewById<Button>(R.id.btnRecurring).setOnClickListener { filterBills("recurring") }
        findViewById<Button>(R.id.btnPaid).setOnClickListener { filterBills("paid") }
    }

    private fun setupDialog() {
        dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_box_bill)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawable(getDrawable(R.drawable.dialog_box_bills_bg))
        dialog.setCancelable(false)

        val btnCloseDialog = dialog.findViewById<Button>(R.id.btnCloseDialog)
        btnCloseDialog.setOnClickListener {
            dialog.dismiss()
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
                        filterBills("incoming")
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
        val bill = billsArrayList[position]
        showBillDetailsDialog(bill)
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
        edtDateDialog.setText(bill.date)
        edtCommentDialog.setText(bill.comment)

        dialog.show()
    }

    private fun filterBills(filter: String) {
        val filteredBills = ArrayList<Bills>()
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val currentDate = Calendar.getInstance().time

        for (bill in allBillsArrayList) { // Usamos allBillsArrayList para filtrar
            try {
                val billDate = sdf.parse(bill.date)

                when (filter) {
                    "paid" -> {
                        if (bill.paid) {
                            filteredBills.add(bill)
                            Log.d("Filter", "Paid bill added: ${bill.name}")
                        }
                    }
                    "due" -> {
                        if (billDate != null && billDate.before(currentDate)) {
                            filteredBills.add(bill)
                            Log.d("Filter", "Due bill added: ${bill.name}")
                        }
                    }
                    "recurring" -> {
                        if (bill.repeat != "No") {
                            filteredBills.add(bill)
                            Log.d("Filter", "Recurring bill added: ${bill.name}")
                        }
                    }
                    "incoming" -> {
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
    }
}
