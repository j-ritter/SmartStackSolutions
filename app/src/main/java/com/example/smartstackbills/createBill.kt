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
        "No","Daily","Weekly","Every 2 Weeks","Monthly","Yearly"
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
        "Shake Shack", "Sonic Drive-In","Other"
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
        val spinnerVendors = findViewById<Spinner>(R.id.spinnerVendors)
        val spinnerReminder = findViewById<Spinner>(R.id.spinnerRepeat)
        val saveButton = findViewById<Button>(R.id.btnGuardar)

        val arrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        val arrayAdapterVendors = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, vendors)
        val arrayAdapterRepeat = ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,repeat)

        spinnerCategories.adapter = arrayAdapter
        spinnerVendors.adapter = arrayAdapterVendors
        spinnerReminder.adapter = arrayAdapterRepeat

        spinnerCategories.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedItemCategories = parent?.getItemAtPosition(position).toString()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Este método se llama cuando no se selecciona ningún elemento
            }
        }

        spinnerVendors.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedItemVendors = parent?.getItemAtPosition(position).toString()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Este método se llama cuando no se selecciona ningún elemento
            }
        }

        spinnerReminder.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedItemReminder = parent?.getItemAtPosition(position).toString()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Este método se llama cuando no se selecciona ningún elemento
            }
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
        edtDate.setText("$day/$month/$year")
    }

    private fun saveBill() {
        // Verificar que el email y UID no sean nulos
        if (userEmail != null && userUid != null) {
            // Obtener los datos de los EditText
            val billName = findViewById<EditText>(R.id.edtTitle).text.toString()
            val billAmount = findViewById<EditText>(R.id.edtAmount).text.toString()
            val billDate = findViewById<EditText>(R.id.edtDate).text.toString()
            val billCategory = findViewById<Spinner>(R.id.spinnerCategories).selectedItem.toString()
            val billVendor = findViewById<Spinner>(R.id.spinnerVendors).selectedItem.toString()
            val billRepeat = findViewById<Spinner>(R.id.spinnerRepeat).selectedItem.toString()
            val billComment = findViewById<EditText>(R.id.edtComments).text.toString()
            val billPaid = findViewById<CheckBox>(R.id.checkBoxPaid).isChecked

            // Crear un hash map con los datos de la factura
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

            // Guardar la factura en la subcolección 'bills' del usuario actual
            db.collection("users").document(userUid!!).collection("bills")
                .add(bill)
                .addOnSuccessListener {
                    // Factura guardada exitosamente
                    Toast.makeText(this, "Factura guardada exitosamente", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MyBills::class.java)
                    intent.putExtra("USER_EMAIL", userEmail) // Pasar el email de vuelta
                    startActivity(intent)
                }
                .addOnFailureListener { e ->
                    // Error al guardar la factura
                    Toast.makeText(this, "Error al guardar la factura: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            // El email o UID es nulo
            Toast.makeText(this, "Error: No se pudo obtener el email o UID del usuario", Toast.LENGTH_SHORT).show()
        }
    }
}