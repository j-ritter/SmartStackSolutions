package com.example.smartstackbills

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class createIncome : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private var userEmail: String? = null
    private var userUid: String? = null

    val repeatOptions = arrayOf(
        "No", "Weekly", "Every 2 Weeks", "Monthly", "Every 2 Months", "Quarterly", "Every 6 months", "Yearly"
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

        val edtAmountIncome = findViewById<EditText>(R.id.edtAmountIncome)
        edtAmountIncome.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false
            private var currentText = ""

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isFormatting) return

                val input = s.toString()
                if (input != currentText) {
                    isFormatting = true
                    try {
                        val cleanString = input.replace("[^\\d.]".toRegex(), "")
                        if (cleanString.isNotEmpty()) {
                            val decimalParts = cleanString.split(".")
                            val integerPart = decimalParts[0].toLongOrNull() ?: 0
                            val formattedIntegerPart = DecimalFormat("#,###").format(integerPart)
                            val formatted = if (decimalParts.size > 1) {
                                val decimalPart = decimalParts[1].take(2)
                                "$formattedIntegerPart.$decimalPart"
                            } else {
                                formattedIntegerPart
                            }
                            currentText = formatted
                            edtAmountIncome.setText(formatted)
                            edtAmountIncome.setSelection(formatted.length)
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@createIncome, "Invalid input: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                    isFormatting = false
                }
            }
        })

        userEmail = intent.getStringExtra("USER_EMAIL")
        userUid = FirebaseAuth.getInstance().currentUser?.uid

        val edtDate = findViewById<EditText>(R.id.edtDateIncome)
        edtDate.inputType = InputType.TYPE_NULL  // Disable manual input
        edtDate.setOnClickListener { showDatePickerDialog() }
        edtDate.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) { showDatePickerDialog() }
        }

        val spinnerCategories = findViewById<Spinner>(R.id.spinnerCategoriesIncome)
        val spinnerSubcategories = findViewById<Spinner>(R.id.spinnerSubcategoriesIncome)
        val spinnerRepeat = findViewById<Spinner>(R.id.spinnerRepeatIncome)
        val saveButton = findViewById<Button>(R.id.btnSaveIncome)

        val arrayAdapterRepeat = ArrayAdapter(this, R.layout.spinner_item, repeatOptions)
        arrayAdapterRepeat.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRepeat.adapter = arrayAdapterRepeat

        // Initialize spinners with empty arrays (populated later)
        val emptyAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, arrayOf<String>())
        spinnerCategories.adapter = emptyAdapter
        spinnerSubcategories.adapter = emptyAdapter

        // Load categories dynamically when spinner is touched
        spinnerCategories.setOnTouchListener { _, _ ->
            loadCategories(spinnerCategories)
            false
        }

        spinnerSubcategories.setOnTouchListener { _, _ ->
            spinnerCategories.selectedItem?.let { loadSubcategories(it.toString(), spinnerSubcategories) }
            false
        }

        saveButton.setOnClickListener {
            saveIncome()
        }

        val btnCancel = findViewById<Button>(R.id.btnCancelIncome)
        btnCancel.setOnClickListener {
            val intent = Intent(this, MyIncome::class.java)
            intent.putExtra("USER_EMAIL", userEmail)
            startActivity(intent)
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
                val incomeCategory = findViewById<Spinner>(R.id.spinnerCategoriesIncome).selectedItem?.toString() ?: "-"
                val incomeSubcategory = findViewById<Spinner>(R.id.spinnerSubcategoriesIncome).selectedItem?.toString() ?: "-"
                val incomeRepeat = findViewById<Spinner>(R.id.spinnerRepeatIncome).selectedItem.toString()
                val incomeComment = findViewById<EditText>(R.id.edtCommentIncome).text.toString()

                // Convert String to Date
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
                    "date" to timestamp,  // Save the Timestamp
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
                        // Generate recurring income if necessary
                        if (incomeRepeat != "No" && incomeDate != null) {
                            generateRecurringIncome(
                                incomeTitle, incomeAmount, incomeDate, incomeCategory, incomeSubcategory,
                                incomeRepeat, incomeComment, incomeId
                            )
                        }
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
    private fun generateRecurringIncome(
        incomeTitle: String, incomeAmount: String, incomeDate: Date, incomeCategory: String, incomeSubcategory: String,
        incomeRepeat: String, incomeComment: String, parentIncomeId: String
    ) {
        val calendar = Calendar.getInstance()
        calendar.time = incomeDate

        // Define the max end date (1 year in advance)
        val endDate = Calendar.getInstance()
        endDate.add(Calendar.YEAR, 1)

        // Loop to generate future occurrences up to 1 year
        while (calendar.before(endDate)) {
            // Adjust the date based on the selected repeat value
            when (incomeRepeat) {
                "Monthly" -> calendar.add(Calendar.MONTH, 1)
                "Weekly" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                "Every 2 Weeks" -> calendar.add(Calendar.WEEK_OF_YEAR, 2)
                "Every 2 Months" -> calendar.add(Calendar.MONTH, 2)
                "Quarterly" -> calendar.add(Calendar.MONTH, 3)
                "Every 6 months" -> calendar.add(Calendar.MONTH, 6)
                "Yearly" -> calendar.add(Calendar.YEAR, 1)
            }
            // Check if the new date is within 1 year, if not, stop the loop
            if (calendar.after(endDate)) {
                break
            }
            // Create a new incomeId for the next occurrence
            val newIncomeId = db.collection("users").document(userUid!!).collection("income").document().id
            // Prepare the data for the recurring income
            val recurringIncome = hashMapOf(
                "name" to incomeTitle,
                "amount" to incomeAmount,
                "date" to com.google.firebase.Timestamp(calendar.time),
                "category" to incomeCategory,
                "subcategory" to incomeSubcategory,
                "repeat" to incomeRepeat,
                "comment" to incomeComment,
                "parentIncomeId" to parentIncomeId,  // Link to the original income
                "incomeId" to newIncomeId
            )
            // Save the next occurrence to Firebase
            db.collection("users").document(userUid!!).collection("income").document(newIncomeId)
                .set(recurringIncome)
                .addOnSuccessListener {
                    Toast.makeText(this, "Recurring income saved successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error saving recurring income: ${e.message}", Toast.LENGTH_SHORT).show()
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

    private fun loadCategories(spinnerCategories: Spinner) {
        val arrayAdapterCategories = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        spinnerCategories.adapter = arrayAdapterCategories
    }

    private fun loadSubcategories(category: String, spinnerSubcategories: Spinner) {
        val subcategories = subcategoriesMap[category] ?: emptyArray()
        val arrayAdapterSubcategories = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, subcategories)
        spinnerSubcategories.adapter = arrayAdapterSubcategories
    }
}
