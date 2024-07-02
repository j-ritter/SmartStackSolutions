package com.example.smartstackbills

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.time.Year

class createBill : AppCompatActivity() {
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
        val edtDate = findViewById<EditText>(R.id.edtDate)
        edtDate.setOnClickListener {
            showDatePickerDialog()
        }





        // Encuentra el Spinner en el layout
        val spinnerCategories = findViewById<Spinner>(R.id.spinnerCategories)
        val spinnerVendors = findViewById<Spinner>(R.id.spinnerVendors)


        // Crea un ArrayAdapter usando el array de categorías y un layout de spinner por defecto
        val arrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        val arrayAdapterVendors = ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item, vendors)

        // Asigna el adapter al spinner
        spinnerCategories.adapter = arrayAdapter
        spinnerVendors.adapter = arrayAdapterVendors

        // Define el listener del spinner
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




        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun showDatePickerDialog() {
        val datePicker = DatePickerFragment{day,month,year -> onDateSelected(day,month, year)}
        datePicker.show(supportFragmentManager, "datePicker")
    }

    fun onDateSelected(day:Int,month:Int,year:Int){
    val edtDate = findViewById<EditText>(R.id.edtDate)
        edtDate.setText("$day/$month/$year")
    }
}