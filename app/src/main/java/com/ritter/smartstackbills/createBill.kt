package com.ritter.smartstackbills

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.text.InputType
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.Timestamp
import java.io.File
import java.io.IOException

class createBill : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private var userEmail: String? = null
    private var userUid: String? = null
    private lateinit var edtAmountBill: EditText
    private var imageUri: Uri? = null
    private var currentPhotoPath: String? = null
    private var pendingCameraFile: File? = null

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
            val file = pendingCameraFile
            imageUri = if (saved && file?.exists() == true) Uri.fromFile(file) else null
            updateAttachmentStatus()
        }

    private val chooseImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { sourceUri ->
            imageUri = sourceUri?.let { copyImageToAppStorage(it) }
            updateAttachmentStatus()
        }

    val repeat = arrayOf(
        "No", "Weekly", "Every 2 Weeks", "Monthly", "Every 2 Months",
        "Quarterly", "Every 6 months", "Yearly"
    )

    val categories = arrayOf(
        "Accommodation", "Communication", "Insurance", "Subscription and Memberships",
        "Transportation", "Finances/Fees", "Taxes", "Health", "Education",
        "Shopping & Consumption", "Groceries", "Others"
    )
    val subcategoriesMap = mapOf(
        "Accommodation" to arrayOf(
            "Rent", "Mortgage", "Home maintenance", "Utilities", "Furniture",
            "Repairs and renovations", "Property management", "Home security"
        ),
        "Communication" to arrayOf(
            "Mobile phone", "Landline phone", "Internet", "Cable/satellite TV",
            "Messaging services", "Cloud storage", "VPN services", "VOIP services"
        ),
        "Insurance" to arrayOf(
            "Health insurance", "Life insurance", "Car insurance", "Home insurance",
            "Travel insurance", "Pet insurance", "Disability insurance", "Business insurance"
        ),
        "Subscription and Memberships" to arrayOf(
            "Streaming services", "Gym memberships", "Software subscriptions",
            "Magazine/newspaper subscriptions", "Clubs and associations", "Music services",
            "Educational memberships", "Loyalty programs"
        ),
        "Transportation" to arrayOf(
            "Fuel", "Vehicle maintenance", "Public transportation", "Parking",
            "Vehicle rental", "Tolls", "Car lease", "Ride-sharing services"
        ),
        "Finances/Fees" to arrayOf(
            "Bank fees", "Investment fees", "Loan interest", "Credit card fees",
            "Brokerage fees", "Financial advisor fees", "ATM withdrawal fees", "Foreign transaction fees"
        ),
        "Taxes" to arrayOf(
            "Income tax", "Property tax", "Sales tax", "Self-employment tax",
            "Capital gains tax", "VAT (Value Added Tax)", "Import tax", "Luxury tax"
        ),
        "Health" to arrayOf(
            "Doctor visits", "Dental care", "Prescription medications", "Health supplements",
            "Medical equipment", "Mental health services", "Alternative medicine", "Vaccinations"
        ),
        "Education" to arrayOf(
            "Tuition fees", "Textbooks", "Online courses", "School supplies",
            "Extracurricular activities", "Tutoring", "Professional development", "Educational software"
        ),
        "Shopping & Consumption" to arrayOf(
            "Clothing", "Electronics", "Household goods", "Personal care products",
            "Beauty & cosmetics", "Luxury goods", "Office supplies", "Gifts", "Movies"
        ),
        "Groceries" to arrayOf(
            "Basic food", "Household necessities", "Beverages", "Alcoholic beverages",
            "Snacks and sweets", "Luxury foods", "Frozen foods", "Organic products"
        ),
        "Others" to arrayOf(
            "Miscellaneous", "Donations", "Gambling", "Unexpected expenses",
            "Legal fees", "Lottery tickets", "Pet expenses", "Festivals & events"
        )
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

        userEmail = intent.getStringExtra(AuthUtils.EXTRA_USER_EMAIL) ?: AuthUtils.currentUserEmail()
        userUid = FirebaseAuth.getInstance().currentUser?.uid

        val edtDate = findViewById<EditText>(R.id.edtDateBill)
        edtDate.inputType = InputType.TYPE_NULL
        edtDate.setOnClickListener { showDatePickerDialog() }
        edtDate.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) showDatePickerDialog() }

        val spinnerCategories = findViewById<Spinner>(R.id.spinnerCategoriesBill)
        val spinnerSubcategories = findViewById<Spinner>(R.id.spinnerSubcategoriesBill)
        val autoCompleteVendors = findViewById<AutoCompleteTextView>(R.id.autoCompleteVendorBill)
        val spinnerRepeat = findViewById<Spinner>(R.id.spinnerRepeatBill)

        val arrayAdapterRepeat = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, repeat)
        spinnerRepeat.adapter = arrayAdapterRepeat

        loadCategories(spinnerCategories)
        spinnerCategories.selectedItem?.let { loadSubcategories(it.toString(), spinnerSubcategories) }

        // Load vendors based on selected category
        spinnerCategories.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCategory = spinnerCategories.selectedItem.toString()
                loadVendors(selectedCategory, autoCompleteVendors)
                loadSubcategories(selectedCategory, spinnerSubcategories)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Handle case where no category is selected
            }
        }

        val saveButton = findViewById<Button>(R.id.btnSaveBill)
        saveButton.setOnClickListener { saveBill() }

        val btnCancel = findViewById<Button>(R.id.btnCancelBill)
        btnCancel.setOnClickListener {
            finish()
        }
        // Image upload handling
        findViewById<Button>(R.id.btnUploadImageBill).setOnClickListener { handleImageUpload() }
    }
    // Load categories dynamically (not pre-selected)
    private fun loadCategories(spinnerCategories: Spinner) {
        val arrayAdapterCategories = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, FinancialEntryOptions.expenseCategories)
        spinnerCategories.adapter = arrayAdapterCategories
    }

    // Load subcategories based on selected category
    private fun loadSubcategories(category: String, spinnerSubcategories: Spinner) {
        val subcategories = FinancialEntryOptions.expenseSubcategories[category] ?: emptyArray()
        val arrayAdapterSubcategories = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, subcategories)
        spinnerSubcategories.adapter = arrayAdapterSubcategories
    }

    // Load vendors for the selected category
    private fun loadVendors(category: String, autoCompleteVendors: AutoCompleteTextView) {
        val vendors = FinancialEntryOptions.vendorSuggestions(this, category)

        // Set the adapter for the AutoCompleteTextView
        val arrayAdapterVendors = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            vendors
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
    private fun handleImageUpload() {
        val options = arrayOf(
            getString(R.string.take_photo),
            getString(R.string.choose_from_gallery),
            getString(R.string.cancel)
        )
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.add_attachment)
        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> dispatchTakePictureIntent()
                1 -> chooseImageLauncher.launch("image/*")
                else -> dialog.dismiss()
            }
        }
        builder.show()
    }

    private fun dispatchTakePictureIntent() {
        val photoFile = try {
            createImageFile()
        } catch (exception: IOException) {
            null
        }
        if (photoFile == null) {
            updateAttachmentStatus()
            return
        }
        pendingCameraFile = photoFile
        val photoUri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.provider",
            photoFile
        )
        takePictureLauncher.launch(photoUri)
    }

    private fun updateAttachmentStatus() {
        findViewById<TextView>(R.id.txtImageAddedBill).apply {
            text = getString(
                if (imageUri != null) R.string.attachment_added else R.string.attachment_failed
            )
            visibility = View.VISIBLE
        }
    }

    private fun copyImageToAppStorage(sourceUri: Uri): Uri? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: return null
            val outputFile = File(storageDir, "BILL_${timeStamp}.jpg")
            contentResolver.openInputStream(sourceUri)?.use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            Uri.fromFile(outputFile)
        } catch (exception: Exception) {
            null
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: throw IOException("Pictures directory unavailable")
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun saveBill() {
        // Retrieve values from input fields
        val billName = findViewById<EditText>(R.id.edtTitleBill).text.toString()
        val billAmount = findViewById<EditText>(R.id.edtAmountBill).text.toString().toDoubleOrNull() ?: 0.0
        val billDateString = findViewById<EditText>(R.id.edtDateBill).text.toString()
        val billCategory = findViewById<Spinner>(R.id.spinnerCategoriesBill).selectedItem?.toString() ?: "-"
        val billSubcategory = findViewById<Spinner>(R.id.spinnerSubcategoriesBill).selectedItem?.toString() ?: "-"
        val billVendor = findViewById<AutoCompleteTextView>(R.id.autoCompleteVendorBill).text.toString()
        val billRepeat = findViewById<Spinner>(R.id.spinnerRepeatBill).selectedItem?.toString() ?: "-"
        val billComment = findViewById<EditText>(R.id.edtCommentBill).text.toString()
        val billPaid = findViewById<CheckBox>(R.id.checkBoxPaidBill).isChecked
        val billAttachment = imageUri?.toString()

        // Validate mandatory fields
        if (billName.isBlank()) {
            Toast.makeText(this, R.string.enter_open_payment_name, Toast.LENGTH_SHORT).show()
            return
        }
        if (billAmount <= 0) {
            Toast.makeText(this, R.string.enter_valid_amount, Toast.LENGTH_SHORT).show()
            return
        }
        if (billDateString.isBlank()) {
            Toast.makeText(this, R.string.select_valid_due_date, Toast.LENGTH_SHORT).show()
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
            "subcategory" to billSubcategory,
            "vendor" to billVendor,
            "repeat" to billRepeat,
            "comment" to billComment,
            "attachment" to billAttachment,
            "paid" to billPaid
        )

        // Save bill to Firebase
        val uid = userUid
        if (uid != null && billDate != null) {
            val documentReference = db.collection("users").document(uid).collection("bills").document()
            val billId = documentReference.id
            bill["billId"] = billId
            bill["parentBillId"] = billId

            val batch = db.batch()
            val billsToSchedule = mutableListOf<Bills>()
            batch.set(documentReference, bill)
            billsToSchedule += Bills().apply {
                this.billId = billId
                this.name = billName
                this.amount = billAmount
                this.date = timestamp
                this.paid = billPaid
            }

            if (billRepeat != "No") {
                addRecurringBillsToBatch(
                    batch, billsToSchedule, uid, billName, billAmount, billDate,
                    billCategory, billSubcategory, billVendor, billRepeat,
                    billComment, billAttachment, billId
                )
            }

            batch.commit()
                .addOnSuccessListener {
                    Toast.makeText(this, R.string.open_payment_saved, Toast.LENGTH_SHORT).show()
                    FinancialEntryOptions.rememberVendor(this, billVendor)
                    billsToSchedule.forEach {
                        PaymentNotificationScheduler.scheduleBill(this, uid, it)
                    }
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, getString(R.string.open_payment_save_failed, e.message.orEmpty()), Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun addRecurringBillsToBatch(
        batch: com.google.firebase.firestore.WriteBatch,
        billsToSchedule: MutableList<Bills>,
        uid: String, billTitle: String, billAmount: Double, startDate: Date, billCategory: String,
        billSubcategory: String, billVendor: String, billRepeat: String, billComment: String,
        billAttachment: String?, parentBillId: String
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

            val newBillId = db.collection("users").document(uid).collection("bills").document().id
            val occurrenceDate = calendar.time
            val recurringBill = hashMapOf(
                "name" to billTitle,
                "amount" to billAmount,
                "date" to com.google.firebase.Timestamp(occurrenceDate),
                "category" to billCategory,
                "subcategory" to billSubcategory,
                "vendor" to billVendor,
                "repeat" to billRepeat,
                "comment" to billComment,
                "attachment" to billAttachment,
                "parentBillId" to parentBillId,
                "billId" to newBillId,
                "paid" to false
            )

            val recurringRef = db.collection("users").document(uid)
                .collection("bills").document(newBillId)
            batch.set(recurringRef, recurringBill)
            billsToSchedule += Bills().apply {
                this.billId = newBillId
                this.name = billTitle
                this.amount = billAmount
                this.date = Timestamp(occurrenceDate)
                this.paid = false
            }
        }
    }
}
