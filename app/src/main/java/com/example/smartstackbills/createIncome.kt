package com.example.smartstackbills

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
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
        "Salary" to arrayOf("Regular salary", "Overtime pay", "Bonuses or incentives", "Others"),
        "Self-Employment/Freelance Work" to arrayOf("Fees for service", "Project-based income", "Others"),
        "Rental and Lease Income" to arrayOf("Real estate", "Vehicles", "Equipment", "Others"),
        "Investment Income" to arrayOf("Dividends", "Interest earnings", "Capital gains", "Others"),
        "Business Income" to arrayOf("Profit shares", "Executive salary", "Others"),
        "Retirement and Pensions" to arrayOf("State pension", "Company pension plans", "Private pension insurance", "Other"),
        "Social Benefits" to arrayOf("Unemployment benefits", "Sickness benefits", "Parental benefits", "Others"),
        "Scholarships and Grants" to arrayOf("Educational scholarships", "Research grants", "Others"),
        "Royalties and Copyrights" to arrayOf("Patents", "Software licenses", "Book publications", "Others"),
        "Other Income" to arrayOf("Benefits in kind", "Inheritances and gifts", "Crowdfunding/Crowdinvesting returns", "Others")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_income)

        userEmail = intent.getStringExtra("USER_EMAIL")
        userUid = FirebaseAuth.getInstance().currentUser?.uid

        val edtDate = findViewById<EditText>(R.id.edtDateIncome)
        edtDate.inputType = InputType.TYPE_NULL  // Disable manual input
        edtDate.setOnClickListener {
            showDatePickerDialog()
        }
        edtDate.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                showDatePickerDialog()
            }
        }

        val spinnerCategories = findViewById<Spinner>(R.id.spinnerCategoriesIncome)
        val spinnerSubcategories = findViewById<Spinner>(R.id.spinnerSubcategoriesIncome)
        val spinnerRepeat = findViewById<Spinner>(R.id.spinnerRepeatIncome)
        val saveButton = findViewById<Button>(R.id.btnSaveIncome)

        // Set up ArrayAdapter for Spinners to match TextView textSize and style
        val arrayAdapterCategories = ArrayAdapter(this, R.layout.spinner_item, categories)
        arrayAdapterCategories.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategories.adapter = arrayAdapterCategories

        val arrayAdapterRepeat = ArrayAdapter(this, R.layout.spinner_item, repeatOptions)
        arrayAdapterRepeat.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
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
    private fun validateDateField(): Boolean {
        val edtDate = findViewById<EditText>(R.id.edtDateIncome)
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        dateFormat.isLenient = false  // This ensures that the date format is strict

        return try {
            val incomeDate = dateFormat.parse(edtDate.text.toString())
            val currentDate = Calendar.getInstance().time

            if (incomeDate != null) {
                true
            } else {
                Toast.makeText(this, "Please select a valid future date", Toast.LENGTH_SHORT).show()
                false
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Invalid date format. Please use dd/MM/yyyy", Toast.LENGTH_SHORT).show()
            false
        }
    }

    private fun saveIncome() {
        if (validateMandatoryFields() && validateDateField()) {
            if (userEmail != null && userUid != null) {
                val incomeTitle = findViewById<EditText>(R.id.edtTitleIncome).text.toString()
                val incomeAmount = findViewById<EditText>(R.id.edtAmountIncome).text.toString()
                val incomeDateString = findViewById<EditText>(R.id.edtDateIncome).text.toString()
                val incomeCategory = findViewById<Spinner>(R.id.spinnerCategoriesIncome).selectedItem.toString()
                val incomeSubcategory = findViewById<Spinner>(R.id.spinnerSubcategoriesIncome).selectedItem.toString()
                val incomeRepeat = findViewById<Spinner>(R.id.spinnerRepeatIncome).selectedItem.toString()
                val incomeComment = findViewById<EditText>(R.id.edtCommentIncome).text.toString()

                // Conversión de String a Date
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val incomeDate: Date? = try {
                    sdf.parse(incomeDateString)
                } catch (e: Exception) {
                    null
                }
                val timestamp = incomeDate?.let { com.google.firebase.Timestamp(it) }

                val income = hashMapOf(
                    "name" to incomeTitle,
                    "amount" to incomeAmount,
                    "date" to timestamp,  // Guarda el Timestamp aquí
                    "category" to incomeCategory,
                    "subcategory" to incomeSubcategory,

                    "repeat" to incomeRepeat,
                    "comment" to incomeComment,


                )



                val docRef = db.collection("users").document(userUid!!).collection("income").document()
                val incomeId = docRef.id
                income["incomeId"] = incomeId

                docRef.set(income)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Income saved successfully", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, MyIncome::class.java)
                        intent.putExtra("USER_EMAIL", userEmail)
                        startActivity(intent)
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