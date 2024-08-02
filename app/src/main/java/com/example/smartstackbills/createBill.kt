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

class createBill : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private var userEmail: String? = null
    private var userUid: String? = null
    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_IMAGE_GALLERY = 2
    private var imageUri: Uri? = null


    val repeat = arrayOf(
        "No","Daily","Weekly","Every 2 Weeks","Monthly","Yearly"
    )

    val categories = arrayOf(
        "Accommodation", "Communication", "Insurance", "Subscription and Memberships",
        "Transportation", "Finances/Fees", "Taxes", "Health", "Education", "Shopping & Consumption"
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
        "Shopping & Consumption" to arrayOf("Groceries", "Clothing", "Electronics", "Household goods", "Personal care products")
    )

    val vendorsMap = mapOf(
        "Accommodation" to arrayOf("IKEA", "Home Depot", "Lowe's", "Wayfair", "Overstock"),
        "Communication" to arrayOf("Comcast", "Verizon", "AT&T", "T-Mobile"),
        "Insurance" to arrayOf("Geico", "State Farm", "Progressive", "Allstate"),
        "Subscription and Memberships" to arrayOf("Netflix", "Hulu", "Spotify", "Disney+"),
        "Transportation" to arrayOf("Uber", "Lyft", "Delta Airlines", "American Airlines"),
        "Finances/Fees" to arrayOf("Bank of America", "Wells Fargo", "Chase", "Citi"),
        "Taxes" to arrayOf("TurboTax", "H&R Block"),
        "Health" to arrayOf("CVS Pharmacy", "Walgreens", "Rite Aid"),
        "Education" to arrayOf("Coursera", "Udemy", "edX"),
        "Shopping & Consumption" to arrayOf("Amazon", "Walmart", "Target", "Best Buy")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_bill)

        // Obtener el email y UID del Intent
        userEmail = intent.getStringExtra("USER_EMAIL")
        userUid = FirebaseAuth.getInstance().currentUser?.uid

        val edtDate = findViewById<EditText>(R.id.edtDate)
        edtDate.setOnClickListener {
            showDatePickerDialog()
        }

        val spinnerCategories = findViewById<Spinner>(R.id.spinnerCategories)
        val spinnerSubcategories = findViewById<Spinner>(R.id.spinnerSubcategories)
        val spinnerVendors = findViewById<Spinner>(R.id.spinnerVendors)
        val spinnerReminder = findViewById<Spinner>(R.id.spinnerRepeat)
        val saveButton = findViewById<Button>(R.id.btnGuardar)

        val arrayAdapterCategories = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        val arrayAdapterRepeat = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, repeat)

        spinnerCategories.adapter = arrayAdapterCategories
        spinnerReminder.adapter = arrayAdapterRepeat

        spinnerCategories.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCategory = parent?.getItemAtPosition(position).toString()


                val subcategories = subcategoriesMap[selectedCategory] ?: emptyArray()
                val arrayAdapterSubcategories = ArrayAdapter(this@createBill, android.R.layout.simple_spinner_dropdown_item, subcategories)
                spinnerSubcategories.adapter = arrayAdapterSubcategories


                val vendors = vendorsMap[selectedCategory] ?: emptyArray()
                val arrayAdapterVendors = ArrayAdapter(this@createBill, android.R.layout.simple_spinner_dropdown_item, vendors)
                spinnerVendors.adapter = arrayAdapterVendors
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }

        spinnerReminder.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedItemReminder = parent?.getItemAtPosition(position).toString()
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
                            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
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

        val btnCancel = findViewById<Button>(R.id.btnCancel)
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
        val edtDate = findViewById<EditText>(R.id.edtDate)
        edtDate.setText("$day/${month + 1}/$year")
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val txtImageAdded = findViewById<TextView>(R.id.txtImageAdded)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    val imageBitmap = data?.extras?.get("data") as Bitmap
                    val uri = saveImageToGallery(imageBitmap)
                    imageUri = uri
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



    private fun saveBill() {
        if (validateMandatoryFields()) {
            // Verificar que el email y UID no sean nulos
            // Verificar que el email y UID no sean nulos
            if (userEmail != null && userUid != null) {
                // Obtener los datos de los EditText
                val billName = findViewById<EditText>(R.id.edtTitle).text.toString()
                val billAmount = findViewById<EditText>(R.id.edtAmount).text.toString()
                val billDate = findViewById<EditText>(R.id.edtDate).text.toString()
                val billCategory =
                    findViewById<Spinner>(R.id.spinnerCategories).selectedItem.toString()
                val billSubcategory =
                    findViewById<Spinner>(R.id.spinnerSubcategories).selectedItem.toString()
                val billVendor = findViewById<Spinner>(R.id.spinnerVendors).selectedItem.toString()
                val billRepeat = findViewById<Spinner>(R.id.spinnerRepeat).selectedItem.toString()
                val billComment = findViewById<EditText>(R.id.edtComments).text.toString()
                val billPaid = findViewById<CheckBox>(R.id.checkBoxPaid).isChecked
                val billAttachment = imageUri?.toString()

                // Crear un hash map con los datos de la factura
                val bill = hashMapOf(
                    "name" to billName,
                    "amount" to billAmount,
                    "date" to billDate,
                    "category" to billCategory,
                    "subcategory" to billSubcategory,
                    "vendor" to billVendor,
                    "repeat" to billRepeat,
                    "comment" to billComment,
                    "paid" to billPaid,
                    "attachment" to billAttachment
                )

                // Guardar la factura en la subcolección 'bills' del usuario actual
                val docRef =
                    db.collection("users").document(userUid!!).collection("bills").document()
                val billId = docRef.id
                bill["billId"] = billId

                docRef.set(bill)
                    .addOnSuccessListener {
                        // Factura guardada exitosamente
                        Toast.makeText(this, "Factura guardada exitosamente", Toast.LENGTH_SHORT)
                            .show()
                        val intent = Intent(this, MyBills::class.java)
                        intent.putExtra("USER_EMAIL", userEmail) // Pasar el email de vuelta
                        startActivity(intent)
                    }
                    .addOnFailureListener { e ->
                        // Error al guardar la factura
                        Toast.makeText(
                            this,
                            "Error al guardar la factura: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            } else {
                // El email o UID es nulo
                Toast.makeText(
                    this,
                    "Error: No se pudo obtener el email o UID del usuario",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    private fun validateMandatoryFields(): Boolean {
        val billName = findViewById<EditText>(R.id.edtTitle).text.toString()
        val billAmount = findViewById<EditText>(R.id.edtAmount).text.toString()
        val billDate = findViewById<EditText>(R.id.edtDate).text.toString()
        val billCategory = findViewById<Spinner>(R.id.spinnerCategories).selectedItem.toString()


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

}
