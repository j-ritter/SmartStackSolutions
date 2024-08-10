package com.example.smartstackbills

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class createIncome : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private var userEmail: String? = null
    private var userUid: String? = null

    val repeatOptions = arrayOf(
        "No","Daily","Weekly","Every 2 Weeks","Monthly","Every 2 Months", "Every 6 months", "Yearly"
    )

    val categories = arrayOf(
        "Salary", "Self-Employment/Freelance Work", "Rental and Lease Income",
        "Investment Income", "Business Income", "Retirement and Pensions",
        "Social Benefits", "Scholarships and Grants", "Royalties and Copyrights",
        "Other Income", "Create new type of income"
    )

    val subcategoriesMap = mapOf(
        "Salary" to arrayOf("Regular salary", "Overtime pay", "Bonuses or incentives"),
        "Self-Employment/Freelance Work" to arrayOf("Fees for service", "Project-based income"),
        "Rental and Lease Income" to arrayOf("Real estate", "Vehicles", "Equipment"),
        "Investment Income" to arrayOf("Dividends", "Interest earnings", "Capital gains"),
        "Business Income" to arrayOf("Profit shares", "Executive salary"),
        "Retirement and Pensions" to arrayOf("State pension", "Company pension plans", "Private pension insurance"),
        "Social Benefits" to arrayOf("Unemployment benefits", "Sickness benefits", "Parental benefits"),
        "Scholarships and Grants" to arrayOf("Educational scholarships", "Research grants"),
        "Royalties and Copyrights" to arrayOf("Patents", "Software licenses", "Book publications"),
        "Other Income" to arrayOf("Benefits in kind", "Inheritances and gifts", "Crowdfunding/Crowdinvesting returns")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_income)

        userEmail = intent.getStringExtra("USER_EMAIL")
        userUid = FirebaseAuth.getInstance().currentUser?.uid

        val edtDate = findViewById<EditText>(R.id.edtDateIncome)
        edtDate.setOnClickListener {
            showDatePickerDialog()
        }

        val spinnerCategories = findViewById<Spinner>(R.id.spinnerCategoriesIncome)
        val spinnerSubcategories = findViewById<Spinner>(R.id.spinnerSubcategoriesIncome)
        val spinnerRepeat = findViewById<Spinner>(R.id.spinnerRepeatIncome)
        val saveButton = findViewById<Button>(R.id.btnSaveIncome)

        val arrayAdapterCategories = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        val arrayAdapterRepeat = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, repeatOptions)

        spinnerCategories.adapter = arrayAdapterCategories
        spinnerRepeat.adapter = arrayAdapterRepeat

        spinnerCategories.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCategory = parent?.getItemAtPosition(position).toString()

                val subcategories = subcategoriesMap[selectedCategory] ?: emptyArray()
                val arrayAdapterSubcategories = ArrayAdapter(this@createIncome, android.R.layout.simple_spinner_dropdown_item, subcategories)
                spinnerSubcategories.adapter = arrayAdapterSubcategories
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // No action needed
            }
        }

        saveButton.setOnClickListener {
            saveIncome()
        }

        val btnCancel = findViewById<Button>(R.id.btnCancelIncome)
        btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun showDatePickerDialog() {
        val datePicker = DatePickerFragment { day, month, year -> onDateSelected(day, month, year) }
        datePicker.show(supportFragmentManager, "datePicker")
    }

    fun onDateSelected(day: Int, month: Int, year: Int) {
        val edtDate = findViewById<EditText>(R.id.edtDateIncome)
        edtDate.setText("$day/${month + 1}/$year")
    }

    private fun saveIncome() {
        if (validateMandatoryFields()) {
            if (userEmail != null && userUid != null) {
                val incomeTitle = findViewById<EditText>(R.id.edtTitleIncome).text.toString()
                val incomeAmount = findViewById<EditText>(R.id.edtAmountIncome).text.toString()
                val incomeDate = findViewById<EditText>(R.id.edtDateIncome).text.toString()
                val incomeCategory = findViewById<Spinner>(R.id.spinnerCategoriesIncome).selectedItem.toString()
                val incomeSubcategory = findViewById<Spinner>(R.id.spinnerSubcategoriesIncome).selectedItem.toString()
                val incomeRepeat = findViewById<Spinner>(R.id.spinnerRepeatIncome).selectedItem.toString()
                val incomeComment = findViewById<EditText>(R.id.edtCommentIncome).text.toString()

                val income = Income(incomeTitle, incomeDate, incomeComment, incomeCategory, incomeAmount, incomeRepeat, incomeSubcategory)

                val docRef = db.collection("users").document(userUid!!).collection("income").document()
                val incomeId = docRef.id

                docRef.set(income)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Income saved successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error saving income: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Error: Unable to retrieve user email or UID", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validateMandatoryFields(): Boolean {
        val incomeTitle = findViewById<EditText>(R.id.edtTitleIncome).text.toString()
        val incomeAmount = findViewById<EditText>(R.id.edtAmountIncome).text.toString()
        val incomeDate = findViewById<EditText>(R.id.edtDateIncome).text.toString()

        if (incomeTitle.isEmpty()) {
            Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show()
            return false
        }

        if (incomeAmount.isEmpty()) {
            Toast.makeText(this, "Amount is required", Toast.LENGTH_SHORT).show()
            return false
        }

        if (incomeDate.isEmpty()) {
            Toast.makeText(this, "Date is required", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }
}
