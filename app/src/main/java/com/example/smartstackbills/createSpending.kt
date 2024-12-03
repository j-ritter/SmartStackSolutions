package com.example.smartstackbills

import android.R.layout.simple_spinner_dropdown_item
import android.app.Activity
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
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import android.os.Environment
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import com.google.firebase.firestore.FieldValue
import java.io.File
import java.io.IOException
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class createSpending : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private var userEmail: String? = null
    private var userUid: String? = null
    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_IMAGE_GALLERY = 2
    private var imageUri: Uri? = null
    private var currentPhotoPath: String? = null

    // Maps for filtering purposes in MySpendings
    val subcategoryFilterMap = mapOf(
        "Rent" to "Essential",
        "Mortgage" to "Essential",
        "Home maintenance" to "Essential",
        "Utilities" to "Essential",
        "Furniture" to "Non-essential",
        "Repairs and renovations" to "Non-essential",
        "Mobile phone" to "Essential",
        "Landline phone" to "Essential",
        "Internet" to "Essential",
        "Cable/satellite TV" to "Non-essential",
        "Messaging services" to "Non-essential",
        "Health insurance" to "Essential",
        "Life insurance" to "Essential",
        "Car insurance" to "Essential",
        "Home insurance" to "Essential",
        "Travel insurance" to "Non-essential",
        "Pet insurance" to "Non-essential",
        "Magazine/newspaper subscriptions" to "Non-essential",
        "Streaming services" to "Non-essential",
        "Gym memberships" to "Non-essential",
        "Software subscriptions" to "Non-essential",
        "Clubs and associations" to "Non-essential",
        "Fuel" to "Essential",
        "Vehicle maintenance" to "Essential",
        "Public transportation" to "Essential",
        "Parking" to "Non-essential",
        "Vehicle rental" to "Non-essential",
        "Bank fees" to "Essential",
        "Investment fees" to "Non-essential",
        "Loan interest" to "Essential",
        "Credit card fees" to "Essential",
        "Brokerage fees" to "Non-essential",
        "Income tax" to "Essential",
        "Property tax" to "Essential",
        "Sales tax" to "Essential",
        "Self-employment tax" to "Essential",
        "Capital gains tax" to "Essential",
        "Doctor visits" to "Essential",
        "Dental care" to "Essential",
        "Prescription medications" to "Essential",
        "Health supplements" to "Non-essential",
        "Medical equipment" to "Essential",
        "Tuition fees" to "Essential",
        "Textbooks" to "Essential",
        "Online courses" to "Non-essential",
        "School supplies" to "Essential",
        "Extracurricular activities" to "Non-essential",

        "Clothing" to "Essential",
        "Electronics" to "Non-essential",
        "Household goods" to "Essential",
        "Personal care products" to "Essential",
        // Essential groceries
        "Groceries - Basic Food" to "Essential",
        "Groceries - Household Necessities" to "Essential",

        // Non-essential groceries
        "Groceries - Beverages" to "Non-essential",
        "Groceries - Alcoholic Beverages" to "Non-essential",
        "Groceries - Snacks and Sweets" to "Non-essential",
        "Groceries - Luxury Foods" to "Non-essential",

        "Others" to "Non-essential"
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
            "Pottery Barn", "Bed Bath & Beyond", "Ashley Furniture", "Rooms To Go", "Value City Furniture", "Others",
        ),
        "Communication" to arrayOf(
            "Comcast", "Verizon", "AT&T", "T-Mobile",
            "Vodafone", "Orange", "Telefónica", "Telcel", "Movistar",
            "Megacable", "Altice", "Bouygues Telecom", "Free Mobile", "SFR",
            "Deutsche Telekom", "O2", "TIM", "Wind Tre", "Swisscom", "A1 Telekom",
            "KPN", "Telenor", "Telia", "BT Group", "Claro",
            "Rogers Communications", "Bell Canada", "Videotron", "Virgin Mobile", "Sprint","Others",
        ),
        "Insurance" to arrayOf(
            "Geico", "State Farm", "Progressive", "Allstate",
            "AXA", "Allianz", "Zurich", "Mapfre", "GNP",
            "Generali", "AIG", "MetLife", "Liberty Mutual", "Cigna",
            "Nationwide", "Prudential", "Aviva", "Munich Re", "Swiss Re",
            "Chubb", "Hiscox", "Travelers", "Amica", "USAA",
            "Erie Insurance", "The Hartford", "Farmers Insurance", "American Family Insurance", "Mercury Insurance", "Others",
        ),
        "Subscription and Memberships" to arrayOf(
            "Netflix", "Hulu", "Spotify", "Disney+",
            "Amazon Prime", "HBO Max", "Apple Music", "DAZN", "Claro Video",
            "YouTube Premium", "Paramount+", "Showtime", "BritBox", "Crunchyroll",
            "Stitcher", "Pandora", "Audible", "Scribd", "Kindle Unlimited",
            "Deezer", "Tidal", "Xbox Game Pass", "PlayStation Plus", "Google Play Music",
            "FuboTV", "Sling TV", "Peacock", "Discovery+", "AMC+", "Others",
        ),
        "Transportation" to arrayOf(
            "Uber", "Lyft", "Delta Airlines", "American Airlines",
            "EasyJet", "Ryanair", "Vueling", "Aeroméxico",
            "British Airways", "Southwest Airlines", "LATAM", "Qantas", "Air France",
            "Lufthansa", "Emirates", "Qatar Airways", "Singapore Airlines", "KLM",
            "Turkish Airlines", "Cathay Pacific", "Alaska Airlines", "JetBlue", "Spirit Airlines",
            "ANA", "Japan Airlines", "Air Canada", "WestJet", "Viva Aerobus",
            "Aer Lingus", "Iberia", "Volaris", "Hawaiian Airlines", "Frontier Airlines", "Others",
        ),
        "Finances/Fees" to arrayOf(
            "Bank of America", "Wells Fargo", "Chase", "Citi",
            "Santander", "BBVA", "HSBC", "Scotiabank",
            "Banamex", "Goldman Sachs", "Morgan Stanley", "Barclays", "Credit Suisse",
            "Deutsche Bank", "UBS", "BNP Paribas", "Societe Generale", "ING",
            "Rabobank", "ANZ", "Westpac", "NatWest", "Lloyds Banking Group",
            "TD Bank", "Capital One", "American Express", "US Bank", "PNC Financial Services", "Others",
        ),
        "Taxes" to arrayOf(
            "TurboTax", "H&R Block",
            "KPMG", "Deloitte", "PwC", "EY",
            "Grant Thornton", "BDO", "RSM", "Mazars", "Crowe",
            "Baker Tilly", "Nexia", "Moore Stephens", "Kreston", "PKF International",
            "Ryan", "Andersen Tax", "Cherry Bekaert", "CliftonLarsonAllen", "BPM", "Others",
        ),
        "Health" to arrayOf(
            "CVS Pharmacy", "Walgreens", "Rite Aid",
            "Boots", "Superdrug", "Farmacias Benavides", "Farmacias Guadalajara",
            "Walgreens Boots Alliance", "Apoteket", "Mediq", "Phoenix Group", "McKesson",
            "Cardinal Health", "AmerisourceBergen", "Medline", "Fresenius", "Bayer",
            "Johnson & Johnson", "Roche", "Pfizer", "Sanofi", "Novartis",
            "Teva Pharmaceuticals", "Gilead Sciences", "AbbVie", "Bristol-Myers Squibb", "Merck & Co.", "Others",
        ),
        "Education" to arrayOf(
            "Coursera", "Udemy", "edX",
            "FutureLearn", "Khan Academy", "Open University",
            "LinkedIn Learning", "Skillshare", "Treehouse", "Pluralsight", "Codecademy",
            "Simplilearn", "Udacity", "Alison", "MasterClass", "Teachable",
            "CreativeLive", "Edureka", "DataCamp", "General Assembly", "Springboard",
            "Coursera for Business", "EdX for Business", "Skillsoft", "Mindvalley", "Tynker", "Others",
        ),
        "Shopping & Consumption" to arrayOf(
            "Amazon", "Walmart", "Target", "Best Buy", "Costco",
            "El Corte Inglés", "Carrefour", "Aldi", "Lidl",
            "Chedraui", "Soriana", "Bodega Aurrera", "Sam's Club", "BJ's Wholesale Club",
            "Tesco", "Sainsbury's", "Asda", "Marks & Spencer", "John Lewis",
            "Waitrose", "Co-op", "Morrisons", "Loblaws", "Metro",
            "Woolworths", "Coles", "REWE", "Edeka", "Auchan",
            "Kroger", "Publix", "Albertsons", "H-E-B", "Meijer",
            "Whole Foods Market", "Sprouts Farmers Market", "Trader Joe's", "Safeway", "ShopRite", "Others",
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_spending)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        userEmail = intent.getStringExtra("USER_EMAIL")
        userUid = FirebaseAuth.getInstance().currentUser?.uid

        val repeatValue = intent.getStringExtra("repeat") ?: "No"

        val edtDate = findViewById<EditText>(R.id.edtDateSpending)
        edtDate.inputType = InputType.TYPE_NULL  // Disable manual input
        edtDate.setOnClickListener {
            showDatePickerDialog()
        }
        edtDate.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) showDatePickerDialog()
        }

        val checkBoxPaid = findViewById<CheckBox>(R.id.checkBoxPaidSpending).apply {
            isChecked = true
            isEnabled = false
        }

        val spinnerCategories = findViewById<Spinner>(R.id.spinnerCategoriesSpending)
        val spinnerSubcategories = findViewById<Spinner>(R.id.spinnerSubcategoriesSpending)
        val autoCompleteVendors = findViewById<AutoCompleteTextView>(R.id.autoCompleteVendorSpending)
        val edtCustomVendor = findViewById<EditText>(R.id.edtCustomVendorSpending)

        // Initialize spinners with empty adapters (populated later)
        val emptyAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, arrayOf<String>())
        spinnerCategories.adapter = emptyAdapter
        spinnerSubcategories.adapter = emptyAdapter

        // Load categories when the spinner is touched
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

        // Handle vendor selection in AutoCompleteTextView
        autoCompleteVendors.setOnItemClickListener { parent, _, position, _ ->
            val selectedVendor = parent.getItemAtPosition(position).toString()
            if (selectedVendor == "Create Own Vendor") {
                edtCustomVendor.visibility = View.VISIBLE
            } else {
                edtCustomVendor.visibility = View.GONE
            }
        }

        val saveButton = findViewById<Button>(R.id.btnSaveSpending)
        saveButton.setOnClickListener { saveSpending() }

        val btnCancel = findViewById<Button>(R.id.btnCancelSpending)
        btnCancel.setOnClickListener {
            finish()}

        // Image upload handling
        findViewById<Button>(R.id.btnUploadImageSpending).setOnClickListener { handleImageUpload() }
    }
    private fun loadVendors(category: String, autoCompleteVendors: AutoCompleteTextView) {
        val vendors = vendorsMap[category] ?: emptyArray()

        // Set the adapter for the AutoCompleteTextView
        val arrayAdapterVendors = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            vendors + "Create Own Vendor"
        )
        autoCompleteVendors.setAdapter(arrayAdapterVendors)

        autoCompleteVendors.threshold = 1 // Start showing suggestions from the first character
        autoCompleteVendors.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                autoCompleteVendors.showDropDown()
            }
        }
    }

    // Handles showing the date picker
    private fun showDatePickerDialog() {
        val datePicker = DatePickerFragment { day, month, year -> onDateSelected(day, month, year) }
        datePicker.show(supportFragmentManager, "datePicker")
    }

    private fun onDateSelected(day: Int, month: Int, year: Int) {
        findViewById<EditText>(R.id.edtDateSpending).setText("$day/${month + 1}/$year")
    }

    private fun handleImageUpload() {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Upload Spending Image")
        builder.setItems(options) { dialog, which ->
            when (options[which]) {
                "Take Photo" -> dispatchTakePictureIntent()
                "Choose from Gallery" -> dispatchChooseFromGalleryIntent()
                "Cancel" -> dialog.dismiss()
            }
        }
        builder.show()
    }

    // Date validation to ensure it's in correct format
    private fun validateDateField(): Boolean {
        val edtDate = findViewById<EditText>(R.id.edtDateSpending)
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return try {
            dateFormat.isLenient = false
            dateFormat.parse(edtDate.text.toString()) != null
        } catch (e: Exception) {
            Toast.makeText(this, "Invalid date format. Please use dd/MM/yyyy", Toast.LENGTH_SHORT).show()
            false
        }
    }

    private fun loadCategories(spinnerCategories: Spinner) {
        val categories = arrayOf("Accommodation", "Communication", "Insurance", "Subscription and Memberships",
            "Transportation", "Finances/Fees", "Taxes", "Health", "Education", "Shopping & Consumption", "Others")
        val arrayAdapterCategories = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        spinnerCategories.adapter = arrayAdapterCategories
    }

    private fun loadSubcategories(category: String, spinnerSubcategories: Spinner) {
        val subcategories = when (category) {
            "Accommodation" -> arrayOf("Rent", "Mortgage", "Home maintenance", "Utilities", "Furniture", "Repairs and renovations")
            "Communication" -> arrayOf("Mobile phone", "Landline phone", "Internet", "Cable/satellite TV", "Messaging services")
            "Insurance" -> arrayOf("Health insurance", "Life insurance", "Car insurance", "Home insurance", "Travel insurance", "Pet insurance")
            "Subscription and Memberships" -> arrayOf("Streaming services", "Gym memberships", "Software subscriptions", "Magazine/newspaper subscriptions", "Clubs and associations")
            "Transportation" -> arrayOf("Fuel", "Vehicle maintenance", "Public transportation", "Parking", "Vehicle rental")
            "Finances/Fees" -> arrayOf("Bank fees", "Investment fees", "Loan interest", "Credit card fees", "Brokerage fees")
            "Taxes" -> arrayOf("Income tax", "Property tax", "Sales tax", "Self-employment tax", "Capital gains tax")
            "Health" -> arrayOf("Doctor visits", "Dental care", "Prescription medications", "Health supplements", "Medical equipment")
            "Education" -> arrayOf("Tuition fees", "Textbooks", "Online courses", "School supplies", "Extracurricular activities")
            "Shopping & Consumption" -> arrayOf("Clothing", "Electronics", "Household goods", "Personal care products")
            "Groceries" -> arrayOf("Basic food", "Household necessities", "Beverages", "Alcoholic beverages", "Snacks and sweets", "Luxury foods")
            "Others" -> arrayOf("Miscellaneous")
            else -> emptyArray()
        }
        val arrayAdapterSubcategories = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, subcategories)
        spinnerSubcategories.adapter = arrayAdapterSubcategories
    }

    private fun saveSpending() {
        if (validateMandatoryFields() && validateDateField()) {
            userUid?.let {
                val spendingName = findViewById<EditText>(R.id.edtTitleSpending).text.toString()
                val spendingAmount = findViewById<EditText>(R.id.edtAmountSpending).text.toString().toDoubleOrNull() ?: 0.0
                val spendingDateString = findViewById<EditText>(R.id.edtDateSpending).text.toString()
                val spendingCategory = findViewById<Spinner>(R.id.spinnerCategoriesSpending).selectedItem?.toString() ?: ""
                val spendingSubcategory = findViewById<Spinner>(R.id.spinnerSubcategoriesSpending).selectedItem?.toString() ?: ""
                val spendingVendor = findViewById<AutoCompleteTextView>(R.id.autoCompleteVendorSpending).text.toString()
                val customVendor = findViewById<EditText>(R.id.edtCustomVendorSpending).text.toString()
                val spendingComment = findViewById<EditText>(R.id.edtCommentSpending).text.toString()
                val spendingAttachment = imageUri?.toString()

                val spendingDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(spendingDateString)
                val timestamp = spendingDate?.let { com.google.firebase.Timestamp(it) }

                val repeatValue = intent.getStringExtra("repeat") ?: "No"

                // Generate a unique ID for the spending
                val spendingId = db.collection("users").document(it).collection("spendings").document().id

                val spending = hashMapOf(
                    "name" to spendingName,
                    "amount" to spendingAmount,
                    "date" to timestamp,
                    "category" to spendingCategory,
                    "subcategory" to spendingSubcategory,
                    "vendor" to if (spendingVendor == "Create Own Vendor") customVendor else spendingVendor,
                    "repeat" to repeatValue,
                    "comment" to spendingComment,
                    "attachment" to spendingAttachment,
                    "paid" to true
                )

                // Save spending to Firestore with specified document ID
                db.collection("users").document(it).collection("spendings").document(spendingId)
                    .set(spending)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Spending saved successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error saving spending: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } ?: run {
                Toast.makeText(this, "Error: No user ID available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validateMandatoryFields(): Boolean {
        val spendingName = findViewById<EditText>(R.id.edtTitleSpending).text.toString()
        val spendingAmount = findViewById<EditText>(R.id.edtAmountSpending).text.toString()

        if (spendingName.isEmpty()) {
            Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show()
            return false
        }

        if (spendingAmount.isEmpty()) {
            Toast.makeText(this, "Amount is required", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    // Methods for handling image capture and gallery selection
    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureIntent.resolveActivity(packageManager)?.let {
            val photoFile: File? = try {
                createImageFile()
            } catch (ex: IOException) {
                null
            }
            photoFile?.also {
                val photoURI: Uri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.provider", it)
                currentPhotoPath = it.absolutePath
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    private fun dispatchChooseFromGalleryIntent() {
        val pickPhoto = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(pickPhoto, REQUEST_IMAGE_GALLERY)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val txtImageAdded = findViewById<TextView>(R.id.txtImageAddedSpending)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    val file = File(currentPhotoPath)
                    imageUri = Uri.fromFile(file)
                    txtImageAdded.text = "Image added"
                }
                REQUEST_IMAGE_GALLERY -> {
                    imageUri = data?.data
                    txtImageAdded.text = "Image added"
                }
            }
            txtImageAdded.visibility = View.VISIBLE
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
            currentPhotoPath = absolutePath
        }
    }
}
