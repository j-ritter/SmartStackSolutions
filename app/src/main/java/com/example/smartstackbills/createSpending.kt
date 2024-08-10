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
import androidx.core.app.ActivityCompat.startActivityForResult
import com.google.firebase.firestore.FieldValue
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class createSpending : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private var userEmail: String? = null
    private var userUid: String? = null
    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_IMAGE_GALLERY = 2
    private var imageUri: Uri? = null
    private var currentPhotoPath: String? = null


    // Separate map for filtering purposes in MySpendings
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
        "Groceries" to "Essential",
        "Clothing" to "Essential",
        "Electronics" to "Non-essential",
        "Household goods" to "Essential",
        "Personal care products" to "Essential",
        "Others" to "Non-essential"
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

        userEmail = intent.getStringExtra("USER_EMAIL")
        userUid = FirebaseAuth.getInstance().currentUser?.uid

        val edtDate = findViewById<EditText>(R.id.edtDateSpending)
        edtDate.setOnClickListener {
            showDatePickerDialog()
        }

        val spinnerCategories = findViewById<Spinner>(R.id.spinnerCategoriesSpending)
        val spinnerSubcategories = findViewById<Spinner>(R.id.spinnerSubcategoriesSpending)
        val spinnerVendors = findViewById<Spinner>(R.id.spinnerVendorsSpending)
        val edtCustomVendor = findViewById<EditText>(R.id.edtCustomVendorSpending)

        val saveButton = findViewById<Button>(R.id.btnSaveSpending)

        // Reference the array from arrays.xml
        val arrayAdapterCategories = ArrayAdapter.createFromResource(
            this, R.array.shared_categories, simple_spinner_dropdown_item
        )
        spinnerCategories.adapter = arrayAdapterCategories


        spinnerCategories.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCategory = parent?.getItemAtPosition(position).toString()
                // Load the corresponding subcategories array based on the selected category
                val subcategoriesArrayId = when (selectedCategory) {
                    "Accommodation" -> R.array.accommodation_subcategories
                    "Communication" -> R.array.communication_subcategories
                    "Insurance" -> R.array.insurance_subcategories
                    "Subscription and Memberships" -> R.array.subscription_memberships_subcategories
                    "Transportation" -> R.array.transportation_subcategories
                    "Finances/Fees" -> R.array.finances_fees_subcategories
                    "Taxes" -> R.array.taxes_subcategories
                    "Health" -> R.array.health_subcategories
                    "Education" -> R.array.education_subcategories
                    "Shopping & Consumption" -> R.array.shopping_consumption_subcategories
                    else -> R.array.others_subcategories
                }

                val arrayAdapterSubcategories = ArrayAdapter.createFromResource(
                    this@createSpending, subcategoriesArrayId, android.R.layout.simple_spinner_dropdown_item
                )
                spinnerSubcategories.adapter = arrayAdapterSubcategories

                val vendors = vendorsMap[selectedCategory] ?: emptyArray()
                val arrayAdapterVendors = ArrayAdapter(this@createSpending, simple_spinner_dropdown_item, vendors + "Create Own Vendor")
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

        val btnUpload = findViewById<Button>(R.id.btnUploadImageSpending)
        btnUpload.setOnClickListener {
            val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Upload Spending Image")
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
            saveSpending()
        }

        val btnCancel = findViewById<Button>(R.id.btnCancelSpending)
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
        val edtDate = findViewById<EditText>(R.id.edtDateSpending)
        edtDate.setText("$day/${month + 1}/$year")
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


    private fun saveSpending() {
        if (validateMandatoryFields()) {
            if (userEmail != null && userUid != null) {
                val spendingName = findViewById<EditText>(R.id.edtTitleSpending).text.toString()
                val spendingAmount = findViewById<EditText>(R.id.edtAmountSpending).text.toString()
                val spendingDate = findViewById<EditText>(R.id.edtDateSpending).text.toString()
                val spendingCategory = findViewById<Spinner>(R.id.spinnerCategoriesSpending).selectedItem.toString()
                val spendingSubcategory = findViewById<Spinner>(R.id.spinnerSubcategoriesSpending).selectedItem.toString()
                val spinnerVendors = findViewById<Spinner>(R.id.spinnerVendorsSpending)
                val edtCustomVendor = findViewById<EditText>(R.id.edtCustomVendorSpending)
                val spendingVendor = if (spinnerVendors.selectedItem.toString() == "Create Own Vendor") {
                    val newVendor = edtCustomVendor.text.toString()

                    newVendor
                } else {
                    spinnerVendors.selectedItem.toString()
                }
                val spendingComment = findViewById<EditText>(R.id.edtCommentSpending).text.toString()
                val spendingAttachment = imageUri?.toString()

                val spending = hashMapOf(
                    "name" to spendingName,
                    "amount" to spendingAmount,
                    "date" to spendingDate,
                    "category" to spendingCategory,
                    "subcategory" to spendingSubcategory,
                    "vendor" to spendingVendor,
                    "comment" to spendingComment,
                    "attachment" to spendingAttachment
                )


                val docRef = db.collection("users").document(userUid!!).collection("spendings").document()
                val spendingId = docRef.id
                spending["spendingId"] = spendingId

                docRef.set(spending)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Gasto guardado exitosamente", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, MySpendings::class.java)
                        intent.putExtra("USER_EMAIL", userEmail)
                        startActivity(intent)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error al guardar el gasto: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Error: No se pudo obtener el email o UID del usuario", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validateMandatoryFields(): Boolean {
        val spendingName = findViewById<EditText>(R.id.edtTitleSpending).text.toString()
        val spendingAmount = findViewById<EditText>(R.id.edtAmountSpending).text.toString()
        val spendingDate = findViewById<EditText>(R.id.edtDateSpending).text.toString()
        val spendingCategory = findViewById<Spinner>(R.id.spinnerCategoriesSpending).selectedItem.toString()
        val spendingSubcategory = findViewById<Spinner>(R.id.spinnerSubcategoriesSpending).selectedItem.toString()

        if (spendingName.isEmpty()) {
            Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show()
            return false
        }

        if (spendingAmount.isEmpty()) {
            Toast.makeText(this, "Amount is required", Toast.LENGTH_SHORT).show()
            return false
        }

        if (spendingDate.isEmpty()) {
            Toast.makeText(this, "Date is required", Toast.LENGTH_SHORT).show()
            return false
        }

        if (spendingCategory.isEmpty() || spendingCategory == "Select Category") {
            Toast.makeText(this, "Category is required", Toast.LENGTH_SHORT).show()
            return false
        }

        if (spendingSubcategory.isEmpty() || spendingSubcategory == "Select Subcategory") {
            Toast.makeText(this, "Subcategory is required", Toast.LENGTH_SHORT).show()
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
}