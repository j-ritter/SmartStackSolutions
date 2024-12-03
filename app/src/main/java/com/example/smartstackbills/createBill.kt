package com.example.smartstackbills

import android.os.Bundle
import android.text.Editable
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.text.InputType
import android.text.TextWatcher
import androidx.work.WorkManager
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.Timestamp
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit

class createBill : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private var userEmail: String? = null
    private var userUid: String? = null
    private lateinit var edtAmountBill: EditText

    val repeat = arrayOf(
        "No", "Weekly", "Every 2 Weeks", "Monthly", "Every 2 Months",
        "Quarterly", "Every 6 months", "Yearly"
    )

    val categories = arrayOf(
        "Accommodation", "Communication", "Insurance", "Subscription and Memberships",
        "Transportation", "Finances/Fees", "Taxes", "Health", "Education",
        "Shopping & Consumption", "Others"
    )
    val subcategoriesMap = mapOf(
        "Accommodation" to arrayOf("Rent", "Mortgage", "Home maintenance", "Utilities", "Furniture", "Repairs and renovations"),
        "Communication" to arrayOf("Mobile phone", "Landline phone", "Internet", "Cable/satellite TV", "Messaging services"),
        "Insurance" to arrayOf("Health insurance", "Life insurance", "Car insurance", "Home insurance", "Travel insurance", "Pet insurance"),
        "Subscription and Memberships" to arrayOf("Streaming services", "Gym memberships", "Software subscriptions", "Magazine/newspaper subscriptions", "Clubs and associations"),
        "Transportation" to arrayOf("Fuel", "Vehicle maintenance", "Public transportation", "Parking", "Vehicle rental"),
        "Finances/Fees" to arrayOf("Bank fees", "Investment fees", "Loan interest", "Credit card fees", "Brokerage fees"),
        "Taxes" to arrayOf("Income tax", "Property tax", "Sales tax", "Self-employment tax", "Capital gains tax"),
        "Health" to arrayOf("Doctor visits", "Dental care", "Prescription medications", "Health supplements", "Medical equipment"),
        "Education" to arrayOf("Tuition fees", "Textbooks", "Online courses", "School supplies", "Extracurricular activities"),
        "Shopping & Consumption" to arrayOf("Clothing", "Electronics", "Household goods", "Personal care products"),
        "Groceries" to arrayOf("Basic food", "Household necessities", "Beverages", "Alcoholic beverages", "Snacks and sweets", "Luxury foods"),
        "Others" to arrayOf("Miscellaneous")
    )

    val vendorsMap = mapOf(
        "Accommodation" to arrayOf(
            "IKEA", "Home Depot", "Lowe's", "Wayfair", "Overstock",
            "Conforama", "Maisons du Monde", "Alinéa", "Sears", "Liverpool",
            "Falabella", "Ripley", "Habitat", "Castorama", "B&Q", "OBI",
            "Brico Depot", "Bauhaus", "Hornbach", "Mr. Bricolage",
            "Leroy Merlin", "Kingfisher", "Travis Perkins", "Wickes", "Ace Hardware",
            "Rona", "Canadian Tire", "Menards", "Crate & Barrel", "West Elm",
            "Pottery Barn", "Bed Bath & Beyond", "Ashley Furniture", "Rooms To Go",
            "Value City Furniture"
        ),
        "Communication" to arrayOf(
            "Comcast", "Verizon", "AT&T", "T-Mobile",
            "Vodafone", "Orange", "Telefónica", "Telcel", "Movistar",
            "Megacable", "Altice", "Bouygues Telecom", "Free Mobile", "SFR",
            "Deutsche Telekom", "O2", "TIM", "Wind Tre", "Swisscom", "A1 Telekom",
            "KPN", "Telenor", "Telia", "BT Group", "Claro",
            "Rogers Communications", "Bell Canada", "Videotron", "Virgin Mobile", "Sprint"
        ),
        "Insurance" to arrayOf(
            "Geico", "State Farm", "Progressive", "Allstate",
            "AXA", "Allianz", "Zurich", "Mapfre", "GNP",
            "Generali", "AIG", "MetLife", "Liberty Mutual", "Cigna",
            "Nationwide", "Prudential", "Aviva", "Munich Re", "Swiss Re",
            "Chubb", "Hiscox", "Travelers", "Amica", "USAA",
            "Erie Insurance", "The Hartford", "Farmers Insurance", "American Family Insurance", "Mercury Insurance"
        ),
        "Subscription and Memberships" to arrayOf(
            "Netflix", "Hulu", "Spotify", "Disney+",
            "Amazon Prime", "HBO Max", "Apple Music", "DAZN", "Claro Video",
            "YouTube Premium", "Paramount+", "Showtime", "BritBox", "Crunchyroll",
            "Stitcher", "Pandora", "Audible", "Scribd", "Kindle Unlimited",
            "Deezer", "Tidal", "Xbox Game Pass", "PlayStation Plus", "Google Play Music",
            "FuboTV", "Sling TV", "Peacock", "Discovery+", "AMC+"
        ),
        "Transportation" to arrayOf(
            "Uber", "Lyft", "Delta Airlines", "American Airlines",
            "EasyJet", "Ryanair", "Vueling", "Aeroméxico",
            "British Airways", "Southwest Airlines", "LATAM", "Qantas", "Air France",
            "Lufthansa", "Emirates", "Qatar Airways", "Singapore Airlines", "KLM",
            "Turkish Airlines", "Cathay Pacific", "Alaska Airlines", "JetBlue", "Spirit Airlines",
            "ANA", "Japan Airlines", "Air Canada", "WestJet", "Viva Aerobus",
            "Aer Lingus", "Iberia", "Volaris", "Hawaiian Airlines", "Frontier Airlines"
        ),
        "Finances/Fees" to arrayOf(
            "Bank of America", "Wells Fargo", "Chase", "Citi",
            "Santander", "BBVA", "HSBC", "Scotiabank",
            "Banamex", "Goldman Sachs", "Morgan Stanley", "Barclays", "Credit Suisse",
            "Deutsche Bank", "UBS", "BNP Paribas", "Societe Generale", "ING",
            "Rabobank", "ANZ", "Westpac", "NatWest", "Lloyds Banking Group",
            "TD Bank", "Capital One", "American Express", "US Bank", "PNC Financial Services"
        ),
        "Taxes" to arrayOf(
            "TurboTax", "H&R Block",
            "KPMG", "Deloitte", "PwC", "EY",
            "Grant Thornton", "BDO", "RSM", "Mazars", "Crowe",
            "Baker Tilly", "Nexia", "Moore Stephens", "Kreston", "PKF International",
            "Ryan", "Andersen Tax", "Cherry Bekaert", "CliftonLarsonAllen", "BPM"
        ),
        "Health" to arrayOf(
            "CVS Pharmacy", "Walgreens", "Rite Aid",
            "Boots", "Superdrug", "Farmacias Benavides", "Farmacias Guadalajara",
            "Walgreens Boots Alliance", "Apoteket", "Mediq", "Phoenix Group", "McKesson",
            "Cardinal Health", "AmerisourceBergen", "Medline", "Fresenius", "Bayer",
            "Johnson & Johnson", "Roche", "Pfizer", "Sanofi", "Novartis",
            "Teva Pharmaceuticals", "Gilead Sciences", "AbbVie", "Bristol-Myers Squibb", "Merck & Co."
        ),
        "Education" to arrayOf(
            "Coursera", "Udemy", "edX",
            "FutureLearn", "Khan Academy", "Open University",
            "LinkedIn Learning", "Skillshare", "Treehouse", "Pluralsight", "Codecademy",
            "Simplilearn", "Udacity", "Alison", "MasterClass", "Teachable",
            "CreativeLive", "Edureka", "DataCamp", "General Assembly", "Springboard",
            "Coursera for Business", "edX for Business", "Skillsoft", "Mindvalley", "Tynker"
        ),
        "Shopping & Consumption" to arrayOf(
            "Amazon", "Walmart", "Target", "Best Buy", "Costco",
            "El Corte Inglés", "Carrefour", "Aldi", "Lidl",
            "Chedraui", "Soriana", "Bodega Aurrera", "Sam's Club", "BJ's Wholesale Club",
            "Tesco", "Sainsbury's", "Asda", "Marks & Spencer", "John Lewis",
            "Waitrose", "Co-op", "Morrisons", "Loblaws", "Metro",
            "Woolworths", "Coles", "REWE", "Edeka", "Auchan",
            "Kroger", "Publix", "Albertsons", "H-E-B", "Meijer",
            "Whole Foods Market", "Sprouts Farmers Market", "Trader Joe's", "Safeway", "ShopRite"
        ),
        "Others" to arrayOf(
            "Other Vendor 1", "Other Vendor 2", "Other Vendor 3", "Other Vendor 4", "Other Vendor 5"
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_bill)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        userEmail = intent.getStringExtra("USER_EMAIL")
        userUid = FirebaseAuth.getInstance().currentUser?.uid

        val edtDate = findViewById<EditText>(R.id.edtDateBill)
        edtDate.inputType = InputType.TYPE_NULL
        edtDate.setOnClickListener { showDatePickerDialog() }
        edtDate.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) showDatePickerDialog() }

        val spinnerCategories = findViewById<Spinner>(R.id.spinnerCategoriesBill)
        val spinnerSubcategories = findViewById<Spinner>(R.id.spinnerSubcategoriesBill)
        val autoCompleteVendors = findViewById<AutoCompleteTextView>(R.id.autoCompleteVendorBill)
        val edtCustomVendor = findViewById<EditText>(R.id.edtCustomVendorBill)
        val spinnerRepeat = findViewById<Spinner>(R.id.spinnerRepeatBill)

        val arrayAdapterRepeat = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, repeat)
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

        // Load subcategories based on selected category
        spinnerSubcategories.setOnTouchListener { _, _ ->
            spinnerCategories.selectedItem?.let { loadSubcategories(it.toString(), spinnerSubcategories) }
            false
        }

        // Load vendors based on selected category
        spinnerCategories.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCategory = spinnerCategories.selectedItem.toString()
                loadVendors(selectedCategory, autoCompleteVendors)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Handle case where no category is selected
            }
        }

        // Handle vendor selection and custom vendor input
        autoCompleteVendors.setOnItemClickListener { parent, _, position, _ ->
            val selectedVendor = parent.getItemAtPosition(position).toString()
            if (selectedVendor == "Create Own Vendor") {
                edtCustomVendor.visibility = View.VISIBLE
            } else {
                edtCustomVendor.visibility = View.GONE
            }
        }

        val saveButton = findViewById<Button>(R.id.btnSaveBill)
        saveButton.setOnClickListener { saveBill() }

        val btnCancel = findViewById<Button>(R.id.btnCancelBill)
        btnCancel.setOnClickListener {
            finish()
        }
    }
    // Load categories dynamically (not pre-selected)
    private fun loadCategories(spinnerCategories: Spinner) {
        val arrayAdapterCategories = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        spinnerCategories.adapter = arrayAdapterCategories
    }

    // Load subcategories based on selected category
    private fun loadSubcategories(category: String, spinnerSubcategories: Spinner) {
        val subcategories = subcategoriesMap[category] ?: emptyArray()
        val arrayAdapterSubcategories = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, subcategories)
        spinnerSubcategories.adapter = arrayAdapterSubcategories
    }

    // Load vendors for the selected category
    private fun loadVendors(category: String, autoCompleteVendors: AutoCompleteTextView) {
        val vendors = vendorsMap[category] ?: emptyArray()

        // Set the adapter for the AutoCompleteTextView
        val arrayAdapterVendors = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            vendors + "Create Own Vendor"
        )
        autoCompleteVendors.setAdapter(arrayAdapterVendors)

        // Show the dropdown immediately when the field is clicked
        autoCompleteVendors.threshold = 1 // Start showing suggestions from the first character
        autoCompleteVendors.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                autoCompleteVendors.showDropDown()
            }
        }
    }

    private fun showDatePickerDialog() {
        val datePicker = DatePickerFragment { day, month, year -> onDateSelected(day, month, year) }
        datePicker.show(supportFragmentManager, "datePicker")
    }

    private fun onDateSelected(day: Int, month: Int, year: Int) {
        val edtDate = findViewById<EditText>(R.id.edtDateBill)
        edtDate.setText("$day/${month + 1}/$year")
    }

    private fun saveBill() {
        // Retrieve values from input fields
        val billName = findViewById<EditText>(R.id.edtTitleBill).text.toString()
        val billAmount = findViewById<EditText>(R.id.edtAmountBill).text.toString().toDoubleOrNull() ?: 0.0
        val billDateString = findViewById<EditText>(R.id.edtDateBill).text.toString()
        val billCategory = findViewById<Spinner>(R.id.spinnerCategoriesBill).selectedItem?.toString() ?: "-"
        val billVendor = findViewById<AutoCompleteTextView>(R.id.autoCompleteVendorBill).text.toString()
        val customVendor = findViewById<EditText>(R.id.edtCustomVendorBill).text.toString()
        val billRepeat = findViewById<Spinner>(R.id.spinnerRepeatBill).selectedItem?.toString() ?: "-"
        val billComment = findViewById<EditText>(R.id.edtCommentBill).text.toString()
        val billPaid = findViewById<CheckBox>(R.id.checkBoxPaidBill).isChecked

        // Validate mandatory fields
        if (billName.isBlank()) {
            Toast.makeText(this, "Please enter a name for the bill", Toast.LENGTH_SHORT).show()
            return
        }
        if (billAmount == null || billAmount <= 0) {
            Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }
        if (billDateString == null) {
            Toast.makeText(this, "Please select a valid due date for the bill", Toast.LENGTH_SHORT).show()
            return
        }

        // Convert date string to Timestamp
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val billDate: Date? = try {
            sdf.parse(billDateString)
        } catch (e: Exception) {
            null
        }
        val timestamp = billDate?.let { Timestamp(it) }

        // Prepare the bill data
        val bill = hashMapOf(
            "name" to billName,
            "amount" to billAmount,
            "date" to timestamp,
            "category" to billCategory,
            "vendor" to if (billVendor == "Create Own Vendor") customVendor else billVendor,
            "repeat" to billRepeat,
            "comment" to billComment,
            "paid" to billPaid
        )

        // Save bill to Firebase
        if (userEmail != null && userUid != null && billDate != null) {
            db.collection("users").document(userUid!!).collection("bills")
                .add(bill)
                .addOnSuccessListener { documentReference ->
                    Toast.makeText(this, "Bill saved successfully", Toast.LENGTH_SHORT).show()

                    // Get the newly generated document ID for billId
                    val billId = documentReference.id
                    documentReference.update("billId", billId)

                    // Trigger immediate notification using WorkManager
                    val workManager = androidx.work.WorkManager.getInstance(this)
                    // Calculate delay for 72 hours (3 days) before the due date
                    val delayMillis = (billDate?.time ?: 0L) - System.currentTimeMillis() - TimeUnit.HOURS.toMillis(72)

                    if (delayMillis > 0) {
                        val workRequest = androidx.work.OneTimeWorkRequestBuilder<NotificationWorker>()
                            .setInitialDelay(delayMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
                            .setInputData(
                                androidx.work.workDataOf(
                                    "title" to billName,
                                    "amount" to billAmount,
                                    "billId" to billId,
                                    "dueDateMillis" to (billDate?.time ?: 0L),
                                    "createdAt" to (timestamp?.toDate()?.time ?: 0L)
                                )
                            )
                            .build()
                        WorkManager.getInstance(this).enqueue(workRequest)
                    } else {
                        Toast.makeText(this, "Bill is due or overdue; no notification scheduled.", Toast.LENGTH_SHORT).show()
                    }

                    // Generate recurring bills if necessary
                    if (billRepeat != "No") {
                        generateRecurringBills(
                            billName, billAmount, billDate, billCategory,
                            if (billVendor == "Create Own Vendor") customVendor else billVendor,
                            billRepeat, billComment, billId
                        )
                    }

                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error saving bill: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun generateRecurringBills(
        billTitle: String, billAmount: Double, startDate: Date, billCategory: String,
        billVendor: String, billRepeat: String, billComment: String, parentBillId: String
    ) {
        val calendar = Calendar.getInstance()
        calendar.time = startDate
        val endDate = Calendar.getInstance().apply { add(Calendar.YEAR, 1) } // Generate bills up to 1 year ahead

        while (calendar.before(endDate)) {
            when (billRepeat) {
                "Weekly" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                "Every 2 Weeks" -> calendar.add(Calendar.WEEK_OF_YEAR, 2)
                "Monthly" -> calendar.add(Calendar.MONTH, 1)
                "Every 2 Months" -> calendar.add(Calendar.MONTH, 2)
                "Quarterly" -> calendar.add(Calendar.MONTH, 3)
                "Every 6 months" -> calendar.add(Calendar.MONTH, 6)
                "Yearly" -> calendar.add(Calendar.YEAR, 1)
            }
            if (calendar.after(endDate)) break

            val newBillId = db.collection("users").document(userUid!!).collection("bills").document().id
            val recurringBill = hashMapOf(
                "name" to billTitle,
                "amount" to billAmount,
                "date" to com.google.firebase.Timestamp(calendar.time),
                "category" to billCategory,
                "vendor" to billVendor,
                "repeat" to billRepeat,
                "comment" to billComment,
                "parentBillId" to parentBillId,
                "billId" to newBillId
            )

            db.collection("users").document(userUid!!).collection("bills").document(newBillId)
                .set(recurringBill)
                .addOnSuccessListener {
                    Toast.makeText(this, "Recurring bill saved successfully", Toast.LENGTH_SHORT).show()

                    // Schedule notification for the recurring bill
                    val delayMillis = calendar.timeInMillis - System.currentTimeMillis() - TimeUnit.HOURS.toMillis(72)
                    if (delayMillis > 0) {
                        val workRequest = androidx.work.OneTimeWorkRequestBuilder<NotificationWorker>()
                            .setInitialDelay(delayMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
                            .setInputData(
                                androidx.work.workDataOf(
                                    "title" to billTitle,
                                    "amount" to billAmount,
                                    "billId" to newBillId,
                                    "dueDateMillis" to calendar.timeInMillis
                                )
                            )
                            .build()
                        WorkManager.getInstance(this).enqueue(workRequest)
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error saving recurring bill: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
