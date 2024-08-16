package com.example.smartstackbills

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
import android.text.InputType
import com.google.firebase.firestore.FieldValue
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class createBill : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private var userEmail: String? = null
    private var userUid: String? = null
    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_IMAGE_GALLERY = 2
    private var imageUri: Uri? = null
    private var currentPhotoPath: String? = null



    val repeat = arrayOf(
        "No","Daily","Weekly","Every 2 Weeks","Monthly","Every 2 Months", "Every 6 months", "Yearly"
    )

    val categories = arrayOf(
        "Accommodation", "Communication", "Insurance", "Subscription and Memberships",
        "Transportation", "Finances/Fees", "Taxes", "Health", "Education", "Shopping & Consumption", "Others"
    )

    val subcategoriesMap = mapOf(
        "Accommodation" to arrayOf("Rent", "Mortgage", "Home maintenance", "Utilities", "Furniture", "Repairs and renovations"),
        "Communication" to arrayOf("Mobile phone", "Landline phone", "Internet", "Cable/satellite TV", "Messaging services"),
        "Insurance" to arrayOf("Health insurance", "Life insurance", "Car insurance", "Home insurance", "Travel insurance", "Pet insurance"),
        "Subscription and Memberships" to arrayOf("Magazine/newspaper subscriptions", "Streaming services", "Gym memberships", "Software subscriptions", "Clubs and associations"),
        "Transportation" to arrayOf("Fuel", "Vehicle maintenance", "Public transportation", "Parking", "Vehicle rental"),
        "Finances/Fees" to arrayOf("Bank fees", "Investment fees", "Loan interest", "Credit card fees", "Brokerage fees"),
        "Taxes" to arrayOf("Income tax", "Property tax", "Sales tax", "Self-employment tax", "Capital gains tax"),
        "Health" to arrayOf("Doctor visits", "Dental care", "Prescription medications", "Health supplements", "Medical equipment"),
        "Education" to arrayOf("Tuition fees", "Textbooks", "Online courses", "School supplies", "Extracurricular activities"),
        "Shopping & Consumption" to arrayOf("Groceries", "Clothing", "Electronics", "Household goods", "Personal care products"),
        "Others" to arrayOf("Others")
    )

    val vendorsMap = mapOf(
        "Accommodation" to arrayOf(
            "IKEA", "Home Depot", "Lowe's", "Wayfair", "Overstock",
            "Conforama", "Maisons du Monde", "Alinéa", "Sears", "Liverpool",
            "Falabella", "Ripley", "Habitat", "Castorama", "B&Q", "OBI",
            "Brico Depot", "Bauhaus", "Hornbach", "Mr. Bricolage",
            "Leroy Merlin", "Kingfisher", "Travis Perkins", "Wickes", "Ace Hardware",
            "Rona", "Canadian Tire", "Menards", "Crate & Barrel", "West Elm",
            "Pottery Barn", "Bed Bath & Beyond", "Ashley Furniture", "Rooms To Go", "Value City Furniture"
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
            "Coursera for Business", "EdX for Business", "Skillsoft", "Mindvalley", "Tynker"
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
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_bill)

        userEmail = intent.getStringExtra("USER_EMAIL")
        userUid = FirebaseAuth.getInstance().currentUser?.uid

        val edtDate = findViewById<EditText>(R.id.edtDateBill)
        edtDate.inputType = InputType.TYPE_NULL  // Disable manual input
        edtDate.setOnClickListener {
            showDatePickerDialog()
        }
        edtDate.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                showDatePickerDialog()
            }
        }

        val spinnerCategories = findViewById<Spinner>(R.id.spinnerCategoriesBill)
        val spinnerSubcategories = findViewById<Spinner>(R.id.spinnerSubcategoriesBill)
        val spinnerVendors = findViewById<Spinner>(R.id.spinnerVendorsBill)
        val edtCustomVendor = findViewById<EditText>(R.id.edtCustomVendorBill)
        val spinnerReminder = findViewById<Spinner>(R.id.spinnerRepeatBill)
        val saveButton = findViewById<Button>(R.id.btnSaveBill)

        val arrayAdapterCategories = ArrayAdapter(this, R.layout.spinner_item, categories)
        arrayAdapterCategories.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategories.adapter = arrayAdapterCategories

        val arrayAdapterRepeat = ArrayAdapter(this, R.layout.spinner_item, repeat)
        arrayAdapterRepeat.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerReminder.adapter = arrayAdapterRepeat


        spinnerCategories.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCategory = parent?.getItemAtPosition(position).toString()
                loadVendors(selectedCategory)

                val subcategories = subcategoriesMap[selectedCategory] ?: emptyArray()
                val arrayAdapterSubcategories = ArrayAdapter(this@createBill, android.R.layout.simple_spinner_dropdown_item, subcategories)
                spinnerSubcategories.adapter = arrayAdapterSubcategories

                val vendors = vendorsMap[selectedCategory] ?: emptyArray()
                val arrayAdapterVendors = ArrayAdapter(this@createBill, android.R.layout.simple_spinner_dropdown_item, vendors + "Create Own Vendor")
                spinnerVendors.adapter = arrayAdapterVendors
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // No action needed
            }
        }

        spinnerVendors.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedVendor = parent?.getItemAtPosition(position).toString()
                edtCustomVendor.visibility = if (selectedVendor == "Create Own Vendor") View.VISIBLE else View.GONE
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // No action needed
            }
        }

        spinnerReminder.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedItemReminder = parent?.getItemAtPosition(position).toString()
                managePaidCheckboxState(selectedItemReminder)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        val btnUpload = findViewById<Button>(R.id.btnUploadImageBill)
        btnUpload.setOnClickListener {
            val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Upload Bill Image")
            builder.setItems(options) { dialog, which ->
                when (options[which]) {
                    "Take Photo" -> {
                        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        if (takePictureIntent.resolveActivity(packageManager) != null) {
                            val photoFile: File? = try {
                                createImageFile()
                            } catch (ex: IOException) {
                                null
                            }
                            photoFile?.also {
                                val photoURI: Uri = FileProvider.getUriForFile(
                                    this,
                                    "${applicationContext.packageName}.provider",
                                    it
                                )
                                currentPhotoPath = it.absolutePath
                                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                            }
                        }
                    }
                    "Choose from Gallery" -> {
                        val pickPhoto = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        startActivityForResult(pickPhoto, REQUEST_IMAGE_GALLERY)
                    }
                    "Cancel" -> dialog.dismiss()
                }
            }
            builder.show()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        saveButton.setOnClickListener {
            saveBill()
        }

        val btnCancel = findViewById<Button>(R.id.btnCancelBill)
        btnCancel.setOnClickListener {
            val intent = Intent(this, MyBills::class.java)
            intent.putExtra("USER_EMAIL", userEmail)
            startActivity(intent)
        }
    }

    private fun showDatePickerDialog() {
        val datePicker = DatePickerFragment { day, month, year -> onDateSelected(day, month, year) }
        datePicker.show(supportFragmentManager, "datePicker")
    }

    fun onDateSelected(day: Int, month: Int, year: Int) {
        val edtDate = findViewById<EditText>(R.id.edtDateBill)
        edtDate.setText("$day/${month + 1}/$year")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val txtImageAdded = findViewById<TextView>(R.id.txtImageAddedBill)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    val file = File(currentPhotoPath)
                    imageUri = Uri.fromFile(file)
                    txtImageAdded.text = "Image added"
                    txtImageAdded.visibility = View.VISIBLE
                }
                REQUEST_IMAGE_GALLERY -> {
                    imageUri = data?.data
                    txtImageAdded.text = "Image added"
                    txtImageAdded.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun saveImageToGallery(bitmap: Bitmap): Uri? {
        val path = MediaStore.Images.Media.insertImage(contentResolver, bitmap, "Title", null)
        return Uri.parse(path)

    }

    private fun managePaidCheckboxState(selectedRepeat: String) {
        val checkBoxPaid = findViewById<CheckBox>(R.id.checkBoxPaid)
        if (selectedRepeat != "No") {
            checkBoxPaid.isChecked = false
            checkBoxPaid.isEnabled = false
        } else {
            checkBoxPaid.isEnabled = true
        }
    }
    private fun validateDateField(): Boolean {
        val edtDate = findViewById<EditText>(R.id.edtDateBill)
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        dateFormat.isLenient = false  // This ensures that the date format is strict

        return try {
            val billDate = dateFormat.parse(edtDate.text.toString())
            val currentDate = Calendar.getInstance().time

            if (billDate != null && !billDate.before(currentDate)) {
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


    private fun saveBill() {
        if (validateMandatoryFields() && validateDateField()) {
            if (userEmail != null && userUid != null) {
                val billName = findViewById<EditText>(R.id.edtTitleBill).text.toString()
                val billAmount = findViewById<EditText>(R.id.edtAmountBill).text.toString()
                val billDateString = findViewById<EditText>(R.id.edtDateBill).text.toString()
                val billCategory = findViewById<Spinner>(R.id.spinnerCategoriesBill).selectedItem.toString()
                val billSubcategory = findViewById<Spinner>(R.id.spinnerSubcategoriesBill).selectedItem.toString()
                val spinnerVendors = findViewById<Spinner>(R.id.spinnerVendorsBill)
                val edtCustomVendor = findViewById<EditText>(R.id.edtCustomVendorBill)
                val billVendor = if (spinnerVendors.selectedItem.toString() == "Create Own Vendor") {
                    val newVendor = edtCustomVendor.text.toString()
                    loadVendors(billCategory)  // Call loadVendors to update the list after adding new vendor
                    updateVendorsList(billCategory, newVendor)
                    newVendor
                } else {
                    spinnerVendors.selectedItem.toString()
                }
                val billRepeat = findViewById<Spinner>(R.id.spinnerRepeatBill).selectedItem.toString()
                val billComment = findViewById<EditText>(R.id.edtCommentBill).text.toString()
                val billPaid = findViewById<CheckBox>(R.id.checkBoxPaid).isChecked
                val billAttachment = imageUri?.toString()

                // Conversión de String a Date
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val billDate: Date? = try {
                    sdf.parse(billDateString)
                } catch (e: Exception) {
                    null
                }
                val timestamp = billDate?.let { com.google.firebase.Timestamp(it) }

                val bill = hashMapOf(
                    "name" to billName,
                    "amount" to billAmount,
                    "date" to timestamp,  // Guarda el Timestamp aquí
                    "category" to billCategory,
                    "subcategory" to billSubcategory,
                    "vendor" to billVendor,
                    "repeat" to billRepeat,
                    "comment" to billComment,
                    "paid" to billPaid,
                    "attachment" to billAttachment
                )

                val docRef = db.collection("users").document(userUid!!).collection("bills").document()
                val billId = docRef.id
                bill["billId"] = billId

                docRef.set(bill)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Factura guardada exitosamente", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, MyBills::class.java)
                        intent.putExtra("USER_EMAIL", userEmail)
                        startActivity(intent)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error al guardar la factura: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Error: No se pudo obtener el email o UID del usuario", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun validateMandatoryFields(): Boolean {
        val billName = findViewById<EditText>(R.id.edtTitleBill).text.toString()
        val billAmount = findViewById<EditText>(R.id.edtAmountBill).text.toString()
        val billDate = findViewById<EditText>(R.id.edtDateBill).text.toString()
        val billCategory = findViewById<Spinner>(R.id.spinnerCategoriesBill).selectedItem.toString()

        if (billName.isEmpty()) {
            Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show()
            return false
        }

        if (billAmount.isEmpty()) {
            Toast.makeText(this, "Amount is required", Toast.LENGTH_SHORT).show()
            return false
        }

        if (billDate.isEmpty()) {
            Toast.makeText(this, "Date is required", Toast.LENGTH_SHORT).show()
            return false
        }

        if (billCategory.isEmpty() || billCategory == "Select Category") {
            Toast.makeText(this, "Category is required", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            currentPhotoPath = absolutePath
        }
    }
    private fun updateVendorsList(category: String, newVendor: String) {
        val categoryVendorsRef = db.collection("categories").document(category)
        categoryVendorsRef.update("vendors", FieldValue.arrayUnion(newVendor))
            .addOnSuccessListener {
                // Successfully added the new vendor to the list in Firebase
            }
            .addOnFailureListener { e ->
                // Handle any errors
                Toast.makeText(this, "Error adding vendor: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    private fun loadVendors(category: String) {
        val categoryVendorsRef = db.collection("categories").document(category)
        categoryVendorsRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val vendors = document.get("vendors") as? List<String> ?: emptyList()
                    val arrayAdapterVendors = ArrayAdapter(this@createBill, android.R.layout.simple_spinner_dropdown_item, vendors + "Create Own Vendor")
                    val spinnerVendors = findViewById<Spinner>(R.id.spinnerVendorsBill)
                    spinnerVendors.adapter = arrayAdapterVendors
                }
            }
            .addOnFailureListener { e ->
                // Handle any errors
                Toast.makeText(this, "Error loading vendors: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}