package com.example.smartstackbills

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class createBill : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private var userEmail: String? = null
    private var userUid: String? = null

    val repeat = arrayOf(
        "No", "Daily", "Weekly", "Every 2 Weeks", "Monthly", "Yearly"
    )

    val categories = arrayOf(
        "Accommodation", "Communication", "Insurance", "Subscription and Memberships",
        "Transportation", "Finances/Fees", "Taxes", "Health", "Education", "Shopping & Consumption"
    )

    val vendors = arrayOf(
        "Amazon", "Walmart", "Target", "Best Buy", "Macy's", "Costco", "Home Depot", "Lowe's",
        "IKEA", "Wayfair", "Overstock", "Nordstrom", "JCPenney", "Sears", "Kohl's", "TJ Maxx",
        "Marshalls", "Bed Bath & Beyond", "Sam's Club", "Dollar Tree", "Dollar General",
        "Five Below", "Big Lots", "Barnes & Noble", "Apple Store", "Microsoft Store", "Staples",
        "Office Depot", "PetSmart", "Petco", "Whole Foods", "Trader Joe's", "Kroger", "Aldi",
        "Safeway", "Publix", "Wegmans", "Sprouts Farmers Market", "Albertsons", "H-E-B",
        "Hy-Vee", "Meijer", "Food Lion", "Giant Eagle", "Stop & Shop", "Hannaford", "WinCo Foods",
        "ShopRite", "Fresh Market", "Harris Teeter", "Piggly Wiggly", "Save-A-Lot", "Comcast",
        "Verizon", "AT&T", "Duke Energy", "Pacific Gas and Electric", "National Grid", "Con Edison",
        "Xcel Energy", "Southern California Edison", "Florida Power & Light", "Dominion Energy",
        "American Electric Power", "Entergy", "FirstEnergy", "PPL Electric Utilities",
        "CenterPoint Energy", "Exelon", "Consumers Energy", "NRG Energy", "CVS Pharmacy",
        "Walgreens", "Rite Aid", "Kaiser Permanente", "UnitedHealthcare", "Anthem", "Cigna",
        "Blue Cross Blue Shield", "Humana", "Aetna", "Molina Healthcare", "WellCare", "Centene",
        "Magellan Health", "CVS Health", "Uber", "Lyft", "Delta Airlines", "American Airlines",
        "United Airlines", "Southwest Airlines", "Greyhound", "Amtrak", "JetBlue", "Alaska Airlines",
        "Spirit Airlines", "Frontier Airlines", "Enterprise Rent-A-Car", "Hertz", "Avis", "Budget",
        "National Car Rental", "Thrifty Car Rental", "Dollar Rent A Car", "Coursera", "Udemy",
        "edX", "Khan Academy", "LinkedIn Learning", "Skillshare", "MasterClass", "FutureLearn",
        "Udacity", "Codecademy", "Pluralsight", "Lynda.com", "Treehouse", "Duolingo", "Rosetta Stone",
        "Netflix", "Hulu", "Spotify", "Disney+", "HBO Max", "Amazon Prime Video", "Apple Music",
        "YouTube Premium", "Pandora", "Tidal", "SiriusXM", "Peacock", "Paramount+", "Showtime",
        "Crunchyroll", "Funimation", "Deezer", "Audible", "Geico", "State Farm", "Progressive",
        "Allstate", "Liberty Mutual", "Nationwide", "USAA", "Farmers Insurance", "Travelers",
        "American Family Insurance", "Chubb", "MetLife", "AIG", "Hartford", "Erie Insurance",
        "Amica", "Safeco", "Auto-Owners Insurance", "McDonald's", "Starbucks", "Subway", "Pizza Hut",
        "Taco Bell", "Burger King", "Dunkin'", "Chipotle", "Panera Bread", "KFC", "Panda Express",
        "Olive Garden", "Red Lobster", "Chili's", "Outback Steakhouse", "Buffalo Wild Wings",
        "Applebee's", "IHOP", "Denny's", "Wendy's", "Jack in the Box", "Arby's", "Five Guys",
        "Shake Shack", "Sonic Drive-In", "Other"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_bill)

        userEmail = intent.getStringExtra("USER_EMAIL")
        userUid = FirebaseAuth.getInstance().currentUser?.uid

        val edtTitle = findViewById<EditText>(R.id.edtTitle)
        val edtAmount = findViewById<EditText>(R.id.edtAmount)
        val edtDate = findViewById<EditText>(R.id.edtDate)
        val spinnerCategories = findViewById<Spinner>(R.id.spinnerCategories)
        val spinnerVendors = findViewById<Spinner>(R.id.spinnerVendors)
        val spinnerReminder = findViewById<Spinner>(R.id.spinnerRepeat)
        val saveButton = findViewById<Button>(R.id.btnGuardar)
        val btnCancel = findViewById<Button>(R.id.btnCancel)

        val arrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        val arrayAdapterVendors = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, vendors)
        val arrayAdapterRepeat = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, repeat)

        spinnerCategories.adapter = arrayAdapter
        spinnerVendors.adapter = arrayAdapterVendors
        spinnerReminder.adapter = arrayAdapterRepeat

        spinnerCategories.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Handle category selection
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }

        spinnerVendors.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Handle vendor selection
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }

        spinnerReminder.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Handle reminder selection
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        addTooltip(edtTitle, "Enter the title of your bill (required).")
        addTooltip(edtAmount, "Enter the amount of your bill (required).")
        addTooltip(edtDate, "Enter the date of your bill (required).")
        addTooltip(spinnerCategories, "Select the category of your bill (required).")

        saveButton.setOnClickListener {
            val title = edtTitle.text.toString().trim()
            val amount = edtAmount.text.toString().trim()
            val date = edtDate.text.toString().trim()
            val category = spinnerCategories.selectedItem.toString()

            when {
                title.isEmpty() -> edtTitle.error = "Please enter the title"
                amount.isEmpty() -> edtAmount.error = "Please enter the amount"
                date.isEmpty() -> edtDate.error = "Please enter the date"
                category.isEmpty() -> {
                    val errorText = spinnerCategories.selectedView as TextView
                    errorText.error = "Please select a category"
                    errorText.requestFocus()
                }
                else -> saveBill()
            }
        }

        btnCancel.setOnClickListener {
            val intent = Intent(this, MyBills::class.java)
            startActivity(intent)
        }

        edtDate.setOnClickListener {
            showDatePickerDialog()
        }
    }

    private fun showDatePickerDialog() {
        val datePicker = DatePickerFragment { day, month, year -> onDateSelected(day, month, year) }
        datePicker.show(supportFragmentManager, "datePicker")
    }

    fun onDateSelected(day: Int, month: Int, year: Int) {
        val edtDate = findViewById<EditText>(R.id.edtDate)
        edtDate.setText("$day/$month/$year")
    }

    private fun saveBill() {
        // Ensure userEmail and userUid are not null
        if (userEmail != null && userUid != null) {
            // Get data from input fields
            val billName = findViewById<EditText>(R.id.edtTitle).text.toString()
            val billAmount = findViewById<EditText>(R.id.edtAmount).text.toString()
            val billDate = findViewById<EditText>(R.id.edtDate).text.toString()
            val billCategory = findViewById<Spinner>(R.id.spinnerCategories).selectedItem.toString()
            val billVendor = findViewById<Spinner>(R.id.spinnerVendors).selectedItem.toString()
            val billRepeat = findViewById<Spinner>(R.id.spinnerRepeat).selectedItem.toString()
            val billComment = findViewById<EditText>(R.id.edtComments).text.toString()
            val billPaid = findViewById<CheckBox>(R.id.checkBoxPaid).isChecked

            // Create a hash map with bill data
            val bill = hashMapOf(
                "name" to billName,
                "amount" to billAmount,
                "date" to billDate,
                "category" to billCategory,
                "vendor" to billVendor,
                "repeat" to billRepeat,
                "comment" to billComment,
                "Paid" to billPaid
            )

            // Save bill to the user's 'bills' subcollection in Firestore
            db.collection("users").document(userUid!!).collection("bills")
                .add(bill)
                .addOnSuccessListener {
                    Toast.makeText(this, "Bill saved successfully", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MyBills::class.java)
                    startActivity(intent)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error saving bill: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "Error: Unable to obtain user email or UID", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addTooltip(view: View, message: String) {
        view.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
