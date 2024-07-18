package com.example.smartstackbills

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.res.colorResource
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class MyBills : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var billsArrayList: ArrayList<Bills>
    private lateinit var myAdapter: MyAdapter
    private lateinit var db: FirebaseFirestore
    private var listenerRegistration: ListenerRegistration? = null
    private lateinit var fab: FloatingActionButton
    private var userEmail: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_my_bills)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)
        userEmail = intent.getStringExtra("USER_EMAIL")


        billsArrayList = ArrayList()
        myAdapter = MyAdapter(this, billsArrayList)
        recyclerView.adapter = myAdapter

        fab = findViewById(R.id.fabBills)
        fab.setOnClickListener {
            val intent = Intent(this, createBill::class.java)
            intent.putExtra("USER_EMAIL", userEmail)
            startActivity(intent)
        }

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
                        for (document in snapshots.documents) {
                            val bill = document.toObject(Bills::class.java)
                            if (bill != null) {
                                billsArrayList.add(bill)
                            }
                        }
                        myAdapter.notifyDataSetChanged()
                    }
                }
        } else {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        listenerRegistration?.remove()
    }
}
