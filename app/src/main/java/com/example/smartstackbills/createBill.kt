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
import com.google.firebase.firestore.FieldValue
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
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
        "No", "Daily", "Weekly", "Every 2 Weeks", "Monthly", "Every 2 Months", "Every 6 months", "Yearly"
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
        "Accommodation" to arrayOf("IKEA", "Home Depot", "Lowe's", "Wayfair", "Overstock", "Create Own Vendor"),
        "Communication" to arrayOf("Comcast", "Verizon", "AT&T", "T-Mobile", "Create Own Vendor"),
        "Insurance" to arrayOf("Geico", "State Farm", "Progressive", "Allstate", "Create Own Vendor"),
        "Subscription and Memberships" to arrayOf("Netflix", "Hulu", "Spotify", "Disney+", "Create Own Vendor"),
        "Transportation" to arrayOf("Uber", "Lyft", "Delta Airlines", "American Airlines", "Create Own Vendor"),
        "Finances/Fees" to arrayOf("Bank of America", "Wells Fargo", "Chase", "Citi", "Create Own Vendor"),
        "Taxes" to arrayOf("TurboTax", "H&R Block", "KPMG", "Deloitte", "Create Own Vendor"),
        "Health" to arrayOf("CVS Pharmacy", "Walgreens", "Rite Aid", "Boots", "Create Own Vendor"),
        "Education" to arrayOf("Coursera", "Udemy", "edX", "FutureLearn", "Create Own Vendor"),
        "Shopping & Consumption" to arrayOf("Amazon", "Walmart", "Target", "Best Buy", "Create Own Vendor"),
        "Others" to arrayOf("Create Own Vendor")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_bill)

        userEmail = intent.getStringExtra("USER_EMAIL")
        userUid = FirebaseAuth.getInstance().currentUser?.uid

        val edtDate = findViewById<EditText>(R.id.edtDate)
        edtDate.setOnClickListener {
            showDatePickerDialog()
        }

        val spinnerCategories = findViewById<Spinner>(R.id.spinnerCategories)
        val spinnerSubcategories = findViewById<Spinner>(R.id.spinnerSubcategories)
        val spinnerVendors = findViewById<Spinner>(R.id.spinnerVendors)
        val edtCustomVendor = findViewById<EditText>(R.id.edtCustomVendor)
        val spinnerReminder = findViewById<Spinner>(R.id.spinnerRepeat)
        val saveButton = findViewById<Button>(R.id.btnGuardar)

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

        val btnUpload = findViewById<Button>(R.id.btnImage)
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
            val selectedCategory = spinnerCategories.selectedItem.toString()
            val selectedSubcategory = spinnerSubcategories.selectedItem.toString()
            val selectedVendor = if (spinnerVendors.selectedItem.toString() == "Create Own Vendor") edtCustomVendor.text.toString() else spinnerVendors.selectedItem.toString()

            if (selectedVendor.isBlank()) {
                Toast.makeText(this, "Please enter a vendor name.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveBillToDatabase(selectedCategory, selectedSubcategory, selectedVendor)

            if (spinnerVendors.selectedItem.toString() == "Create Own Vendor") {
                saveNewVendorToDatabase(selectedCategory, selectedVendor)
            }
        }
    }

    private fun loadVendors(category: String) {
        db.collection("users").document(userUid ?: return)
            .collection("vendors")
            .document(category)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val customVendors = document.get("vendors") as? List<String> ?: emptyList()
                    val vendors = vendorsMap[category]?.toMutableList() ?: mutableListOf()
                    vendors.addAll(customVendors)
                    vendors.add("Create Own Vendor")

                    val arrayAdapterVendors = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, vendors)
                    findViewById<Spinner>(R.id.spinnerVendors).adapter = arrayAdapterVendors
                }
            }
    }

    private fun saveBillToDatabase(category: String, subcategory: String, vendor: String) {
        val billData = mapOf(
            "category" to category,
            "subcategory" to subcategory,
            "vendor" to vendor,
            "timestamp" to FieldValue.serverTimestamp(),
            // Agrega aquí otros campos necesarios, como fecha de la factura, monto, etc.
        )
        db.collection("users").document(userUid ?: return)
            .collection("bills")
            .add(billData)
            .addOnSuccessListener {
                Toast.makeText(this, "Bill saved successfully.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error saving bill: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveNewVendorToDatabase(category: String, vendor: String) {
        db.collection("users").document(userUid ?: return)
            .collection("vendors")
            .document(category)
            .update("vendors", FieldValue.arrayUnion(vendor))
            .addOnSuccessListener {
                Toast.makeText(this, "Vendor saved successfully.", Toast.LENGTH_SHORT).show()
                loadVendors(category)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error saving vendor: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun managePaidCheckboxState(selectedItem: String) {
        // Your existing code to manage the paid checkbox state
    }

    private fun showDatePickerDialog() {
        // Your existing code to show the date picker dialog
    }

    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
            currentPhotoPath = absolutePath
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    imageUri = Uri.fromFile(File(currentPhotoPath))
                }
                REQUEST_IMAGE_GALLERY -> {
                    imageUri = data?.data
                }
            }
        }
    }
}
