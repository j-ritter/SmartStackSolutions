package com.example.smartstackbills

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.text.InputType
import java.text.SimpleDateFormat
import java.util.*

class createBill : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private var userEmail: String? = null
    private var userUid: String? = null

    val repeat = arrayOf(
        "No", "Weekly", "Every 2 Weeks", "Monthly", "Every 2 Months",
        "Quarterly", "Every 6 months", "Yearly"
    )

    val categories = arrayOf(
        "Accommodation", "Communication", "Insurance", "Subscription and Memberships",
        "Transportation", "Finances/Fees", "Taxes", "Health", "Education",
        "Shopping & Consumption", "Others"
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

        // Initialize spinner for categories
        val arrayAdapterCategories = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        spinnerCategories.adapter = arrayAdapterCategories

        // Load vendors based on selected category
        spinnerCategories.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                val selectedCategory = spinnerCategories.selectedItem.toString()
                loadVendors(selectedCategory, autoCompleteVendors)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Handle case where no category is selected
            }
        }

        // Handle vendor selection from AutoCompleteTextView
        autoCompleteVendors.setOnItemClickListener { parent, _, position, _ ->
            val selectedVendor = parent.getItemAtPosition(position).toString()
            if (selectedVendor == "Create Own Vendor") {
                // Show the custom vendor EditText if "Create Own Vendor" is selected
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
        val billAmount = findViewById<EditText>(R.id.edtAmountBill).text.toString()
        val billDateString = findViewById<EditText>(R.id.edtDateBill).text.toString()
        val billCategory = findViewById<Spinner>(R.id.spinnerCategoriesBill).selectedItem?.toString() ?: "-"
        val billVendor = findViewById<AutoCompleteTextView>(R.id.autoCompleteVendorBill).text.toString()
        val customVendor = findViewById<EditText>(R.id.edtCustomVendorBill).text.toString()
        val billRepeat = findViewById<Spinner>(R.id.spinnerRepeatBill).selectedItem?.toString() ?: "-"
        val billComment = findViewById<EditText>(R.id.edtCommentBill).text.toString()
        val billPaid = findViewById<CheckBox>(R.id.checkBoxPaidBill).isChecked

        // Convert date string to Timestamp
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val billDate: Date? = try {
            sdf.parse(billDateString)
        } catch (e: Exception) {
            null
        }
        val timestamp = billDate?.let { com.google.firebase.Timestamp(it) }

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
        if (userEmail != null && userUid != null) {
            db.collection("users").document(userUid!!).collection("bills")
                .add(bill)
                .addOnSuccessListener {
                    Toast.makeText(this, "Bill saved successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error saving bill: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
