package com.example.smartstackbills

import android.content.Intent
import android.os.Bundle
import android.widget.Button
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

class MyBills : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var billsArrayList: ArrayList<Bills>
    private lateinit var incomingBills: ArrayList<Bills>
    private lateinit var overdueBills: ArrayList<Bills>
    private lateinit var paidBills: ArrayList<Bills>
    private lateinit var myAdapter: MyAdapter
    private lateinit var db: FirebaseFirestore
    private var listenerRegistration: ListenerRegistration? = null
    private lateinit var fab: FloatingActionButton
    private var userEmail: String? = null
    private lateinit var btnIncoming: Button
    private lateinit var btnOverdue: Button
    private lateinit var btnPaid: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_my_bills)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)
        userEmail = intent.getStringExtra("USER_EMAIL")

        billsArrayList = ArrayList()
        incomingBills = ArrayList()
        overdueBills = ArrayList()
        paidBills = ArrayList()

        myAdapter = MyAdapter(this, incomingBills)
        recyclerView.adapter = myAdapter

        fab = findViewById(R.id.fabBills)
        fab.setOnClickListener {
            val intent = Intent(this, createBill::class.java)
            intent.putExtra("USER_EMAIL", userEmail)
            startActivity(intent)
        }

        btnIncoming = findViewById(R.id.button1)
        btnOverdue = findViewById(R.id.button2)
        btnPaid = findViewById(R.id.button4)

        btnIncoming.setOnClickListener { showIncomingBills() }
        btnOverdue.setOnClickListener { showOverdueBills() }
        btnPaid.setOnClickListener { showPaidBills() }

        db = FirebaseFirestore.getInstance()

        setupEventChangeListener()
    }

    private fun setupEventChangeListener() {
        val userUid = FirebaseAuth.getInstance().currentUser?.uid
        if (userUid != null) {
            listenerRegistration = db.collection("users").document(userUid).collection("bills")
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        Toast.makeText(this, "Error al cargar las facturas: ${e.message}", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }

                    if (snapshots != null) {
                        billsArrayList.clear()
                        incomingBills.clear()
                        overdueBills.clear()
                        paidBills.clear()

                        for (document in snapshots.documents) {
                            val bill = document.toObject(Bills::class.java)
                            if (bill != null) {
                                billsArrayList.add(bill)
                                categorizeBill(bill)
                            }
                        }
                        myAdapter.notifyDataSetChanged()
                    }
                }
        } else {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun categorizeBill(bill: Bills) {
        if (bill.paid) {
            paidBills.add(bill)
        } else {
            val today = Calendar.getInstance()
            val billDate = Calendar.getInstance()
            try {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val date = sdf.parse(bill.date)
                if (date != null) {
                    billDate.time = date
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (today.before(billDate)) {
                incomingBills.add(bill)
            } else {
                overdueBills.add(bill)
            }
        }
    }

    private fun showIncomingBills() {
        myAdapter.setBillsList(incomingBills)
    }

    private fun showOverdueBills() {
        myAdapter.setBillsList(overdueBills)
    }

    private fun showPaidBills() {
        myAdapter.setBillsList(paidBills)
    }

    override fun onDestroy() {
        super.onDestroy()
        listenerRegistration?.remove()
    }
}
