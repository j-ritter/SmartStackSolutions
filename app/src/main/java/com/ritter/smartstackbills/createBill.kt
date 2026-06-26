package com.ritter.smartstackbills

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.*
import androidx.activity.OnBackPressedCallback
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
import androidx.core.content.ContextCompat
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
        setTodayIfBlank(edtDate)

        val spinnerCategories = findViewById<Spinner>(R.id.spinnerCategoriesBill)
        val spinnerSubcategories = findViewById<Spinner>(R.id.spinnerSubcategoriesBill)
        val autoCompleteVendors = findViewById<AutoCompleteTextView>(R.id.autoCompleteVendorBill)
        val spinnerRepeat = findViewById<Spinner>(R.id.spinnerRepeatBill)

        val arrayAdapterRepeat = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, repeat)
        spinnerRepeat.adapter = arrayAdapterRepeat

        loadCategories(spinnerCategories)
        selectOptionByKey(spinnerCategories, "Other")
        spinnerCategories.selectedItem?.let {
            val selectedCategory = FinancialEntryOptions.selectedKey(it)
            loadSubcategories(selectedCategory, spinnerSubcategories)
            selectDefaultSubcategory(spinnerSubcategories, selectedCategory)
        }
        setupMoreDetailsToggle(
            R.id.tvMoreDetailsBill,
            R.id.txtRepeatBill,
            R.id.spinnerRepeatBill,
            R.id.tvRecurrenceNoteBill,
            R.id.txtCategoryBill,
            R.id.spinnerCategoriesBill,
            R.id.txtSubcategoryBill,
            R.id.spinnerSubcategoriesBill,
            R.id.txtVendorBill,
            R.id.autoCompleteVendorBill,
            R.id.txtCommentBill,
            R.id.edtCommentBill,
            R.id.layoutAttachmentBill
        )

        // Load vendors based on selected category
        spinnerCategories.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCategory = FinancialEntryOptions.selectedKey(spinnerCategories.selectedItem)
                loadVendors(selectedCategory, autoCompleteVendors)
                loadSubcategories(selectedCategory, spinnerSubcategories)
                selectDefaultSubcategory(spinnerSubcategories, selectedCategory)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Handle case where no category is selected
            }
        }
        setupTemplates(spinnerCategories, spinnerSubcategories, autoCompleteVendors, spinnerRepeat)

        val saveButton = findViewById<Button>(R.id.btnSaveBill)
        saveButton.setOnClickListener { saveBill() }

        val btnCancel = findViewById<Button>(R.id.btnCancelBill)
        btnCancel.setOnClickListener {
            confirmDiscardIfNeeded()
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                confirmDiscardIfNeeded()
            }
        })
        // Image upload handling
        findViewById<Button>(R.id.btnUploadImageBill).setOnClickListener { handleImageUpload() }
        applyScanPrefill()
    }

    private fun confirmDiscardIfNeeded() {
        if (!hasUnsavedInput()) {
            finish()
            return
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.unsaved_changes_title)
            .setMessage(R.string.unsaved_changes_message)
            .setNegativeButton(R.string.keep_editing, null)
            .setPositiveButton(R.string.discard_changes) { _, _ -> finish() }
            .show()
    }

    private fun hasUnsavedInput(): Boolean {
        fun text(id: Int): String = findViewById<EditText>(id).text?.toString()?.trim().orEmpty()
        return text(R.id.edtTitleBill).isNotBlank() ||
            text(R.id.edtAmountBill).isNotBlank() ||
            findViewById<AutoCompleteTextView>(R.id.autoCompleteVendorBill).text?.toString()?.trim().orEmpty().isNotBlank() ||
            text(R.id.edtCommentBill).isNotBlank() ||
            imageUri != null ||
            hasScanPrefill()
    }

    private fun hasScanPrefill(): Boolean =
        listOf(
            ScanPrefill.EXTRA_SCAN_TITLE,
            ScanPrefill.EXTRA_SCAN_AMOUNT,
            ScanPrefill.EXTRA_SCAN_DATE,
            ScanPrefill.EXTRA_SCAN_PARTY,
            ScanPrefill.EXTRA_SCAN_ATTACHMENT
        ).any { intent.getStringExtra(it).isNullOrBlank().not() }

    private fun applyScanPrefill() {
        showScanReviewHintIfNeeded()
        intent.getStringExtra(ScanPrefill.EXTRA_SCAN_TITLE)?.takeIf { it.isNotBlank() }?.let {
            findViewById<EditText>(R.id.edtTitleBill).setText(it)
        }
        intent.getStringExtra(ScanPrefill.EXTRA_SCAN_AMOUNT)?.takeIf { it.isNotBlank() }?.let {
            findViewById<EditText>(R.id.edtAmountBill).setText(it)
        }
        ScanDateValidator.sanitizeDisplayDate(intent.getStringExtra(ScanPrefill.EXTRA_SCAN_DATE))?.let {
            findViewById<EditText>(R.id.edtDateBill).setText(it)
        }
        intent.getStringExtra(ScanPrefill.EXTRA_SCAN_PARTY)?.takeIf { it.isNotBlank() }?.let {
            findViewById<AutoCompleteTextView>(R.id.autoCompleteVendorBill).setText(it)
        }
        intent.getStringExtra(ScanPrefill.EXTRA_SCAN_ATTACHMENT)?.takeIf { it.isNotBlank() }?.let {
            imageUri = Uri.parse(it)
            updateAttachmentStatus()
        }
        showScanCurrencyWarningIfNeeded()
    }

    private fun showScanReviewHintIfNeeded() {
        findViewById<TextView>(R.id.tvScanReviewHintBill).visibility =
            if (hasScanPrefill()) View.VISIBLE else View.GONE
    }

    private fun entryCurrency(): String =
        CurrencyPreferences.selectedCode(this)

    private fun showScanCurrencyWarningIfNeeded() {
        val scannedCurrency = intent.getStringExtra(ScanPrefill.EXTRA_SCAN_CURRENCY)
            ?.takeIf { it.isNotBlank() }
            ?: return
        val appCurrency = CurrencyPreferences.selectedCode(this)
        if (scannedCurrency != appCurrency) {
            Toast.makeText(
                this,
                getString(R.string.scan_currency_mismatch, scannedCurrency, appCurrency),
                Toast.LENGTH_LONG
            ).show()
        }
    }
    // Load categories dynamically (not pre-selected)
    private fun loadCategories(spinnerCategories: Spinner) {
        val arrayAdapterCategories = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            FinancialEntryOptions.expenseCategories(this)
        )
        spinnerCategories.adapter = arrayAdapterCategories
    }

    // Load subcategories based on selected category
    private fun loadSubcategories(category: String, spinnerSubcategories: Spinner) {
        val subcategories = FinancialEntryOptions.expenseSubcategories(this, category)
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

    private fun setTodayIfBlank(editText: EditText) {
        if (editText.text.isNullOrBlank()) {
            editText.setText(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()))
        }
    }

    private fun setupMoreDetailsToggle(toggleId: Int, vararg detailIds: Int) {
        val toggle = findViewById<TextView>(toggleId)
        var expanded = false
        fun applyState() {
            detailIds.forEach { id -> findViewById<View>(id).visibility = if (expanded) View.VISIBLE else View.GONE }
            toggle.text = getString(if (expanded) R.string.hide_details else R.string.show_details)
        }
        toggle.setOnClickListener {
            expanded = !expanded
            applyState()
        }
        applyState()
    }

    private fun selectOptionByKey(spinner: Spinner, key: String) {
        val adapter = spinner.adapter ?: return
        for (index in 0 until adapter.count) {
            val option = adapter.getItem(index) as? FinancialEntryOptions.Option
            if (option?.key == key) {
                spinner.setSelection(index)
                return
            }
        }
    }

    private fun selectDefaultSubcategory(spinner: Spinner, category: String) {
        if (category == "Other") {
            selectOptionByKey(spinner, "Miscellaneous")
        }
    }

    private fun selectOptionByText(spinner: Spinner, value: String) {
        val adapter = spinner.adapter ?: return
        for (index in 0 until adapter.count) {
            if (adapter.getItem(index)?.toString() == value) {
                spinner.setSelection(index)
                return
            }
        }
    }

    private fun setupTemplates(
        spinnerCategories: Spinner,
        spinnerSubcategories: Spinner,
        autoCompleteVendors: AutoCompleteTextView,
        spinnerRepeat: Spinner
    ) {
        findViewById<CheckBox>(R.id.checkSaveTemplateBill).setOnCheckedChangeListener { _, checked ->
            findViewById<EditText>(R.id.edtTemplateNameBill).visibility = if (checked) View.VISIBLE else View.GONE
        }

        val templates = EntryTemplateStore.templates(this, EntryTemplateStore.Type.OPEN_PAYMENT)
        val container = findViewById<LinearLayout>(R.id.layoutTemplatesBill)
        val chips = findViewById<LinearLayout>(R.id.templateChipsBill)
        if (templates.isEmpty()) {
            container.visibility = View.GONE
            return
        }
        container.visibility = View.VISIBLE
        val toggle = findViewById<TextView>(R.id.tvUseTemplateBill)
        val toggleRow = findViewById<LinearLayout>(R.id.templateToggleBill)
        val arrow = findViewById<ImageView>(R.id.templateArrowBill)
        val scroll = findViewById<HorizontalScrollView>(R.id.templateScrollBill)
        scroll.visibility = View.GONE
        toggle.text = getString(R.string.use_template)
        arrow.rotation = 0f
        toggleRow.setOnClickListener {
            val expanded = scroll.visibility != View.VISIBLE
            scroll.visibility = if (expanded) View.VISIBLE else View.GONE
            toggle.text = getString(if (expanded) R.string.hide_templates else R.string.use_template)
            arrow.animate().rotation(if (expanded) 180f else 0f).setDuration(180L).start()
        }
        chips.removeAllViews()
        templates.forEach { template ->
            chips.addView(templateButton(template.templateName) {
                findViewById<EditText>(R.id.edtTitleBill).setText(template.title)
                findViewById<EditText>(R.id.edtAmountBill).setText(
                    if (template.amount > 0.0) CurrencyPreferences.formatPlain(template.amount) else ""
                )
                selectOptionByKey(spinnerCategories, template.category)
                loadVendors(template.category, autoCompleteVendors)
                loadSubcategories(template.category, spinnerSubcategories)
                selectOptionByKey(spinnerSubcategories, template.subcategory)
                autoCompleteVendors.setText(template.vendorOrSource)
                selectOptionByText(spinnerRepeat, template.repeat)
                findViewById<EditText>(R.id.edtCommentBill).setText(template.comment)
            })
        }
    }

    private fun templateButton(label: String, onClick: () -> Unit): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            setTextColor(ContextCompat.getColor(this@createBill, R.color.colorPrimary))
            setBackgroundResource(R.drawable.create_upload_button)
            setPadding(20, 0, 20, 0)
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                (38 * resources.displayMetrics.density).toInt()
            ).apply {
                marginEnd = (8 * resources.displayMetrics.density).toInt()
            }
        }

    private fun validateTemplateNameIfNeeded(): Boolean {
        val saveTemplate = findViewById<CheckBox>(R.id.checkSaveTemplateBill).isChecked
        val templateName = findViewById<EditText>(R.id.edtTemplateNameBill).text.toString().trim()
        if (saveTemplate && templateName.isBlank()) {
            Toast.makeText(this, R.string.template_name_required, Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun saveTemplateIfRequested(
        title: String,
        amount: Double,
        category: String,
        subcategory: String,
        vendor: String,
        repeat: String,
        comment: String
    ) {
        if (!findViewById<CheckBox>(R.id.checkSaveTemplateBill).isChecked) return
        val templateName = findViewById<EditText>(R.id.edtTemplateNameBill).text.toString().trim()
        EntryTemplateStore.save(
            this,
            EntryTemplateStore.Template(
                id = "open_payment_${templateName.lowercase(Locale.US)}",
                type = EntryTemplateStore.Type.OPEN_PAYMENT,
                templateName = templateName,
                title = title,
                amount = amount,
                category = category,
                subcategory = subcategory,
                vendorOrSource = vendor,
                repeat = repeat,
                comment = comment
            )
        )
        Toast.makeText(this, R.string.template_saved, Toast.LENGTH_SHORT).show()
    }
    private fun handleImageUpload() {
        val options = arrayOf(
            getString(R.string.scan_and_prefill),
            getString(R.string.take_photo),
            getString(R.string.choose_from_gallery),
            getString(R.string.cancel)
        )
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.add_attachment)
        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> {
                    startActivity(Intent(this, DocumentScanActivity::class.java).apply {
                        putExtra(DocumentScanActivity.EXTRA_ENTRY_TYPE, EntryType.OPEN_PAYMENT.wireValue)
                        putExtra(AuthUtils.EXTRA_USER_EMAIL, userEmail)
                    })
                    finish()
                }
                1 -> dispatchTakePictureIntent()
                2 -> chooseImageLauncher.launch("image/*")
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
        val billName = findViewById<EditText>(R.id.edtTitleBill).text.toString().trim()
            .ifBlank { getString(R.string.open_payment) }
        val billAmount = CurrencyPreferences.roundToTwoDecimals(
            findViewById<EditText>(R.id.edtAmountBill).text.toString().toDoubleOrNull() ?: 0.0
        )
        val dateEditText = findViewById<EditText>(R.id.edtDateBill)
        setTodayIfBlank(dateEditText)
        val billDateString = dateEditText.text.toString()
        val billCategory = FinancialEntryOptions.selectedKey(
            findViewById<Spinner>(R.id.spinnerCategoriesBill).selectedItem
        ).ifBlank { "Other" }
        val billSubcategory = FinancialEntryOptions.selectedKey(
            findViewById<Spinner>(R.id.spinnerSubcategoriesBill).selectedItem
        ).ifBlank { "Miscellaneous" }
        val billVendor = findViewById<AutoCompleteTextView>(R.id.autoCompleteVendorBill).text.toString()
        val billRepeat = findViewById<Spinner>(R.id.spinnerRepeatBill).selectedItem?.toString() ?: "-"
        val billComment = findViewById<EditText>(R.id.edtCommentBill).text.toString()
        val billPaid = findViewById<CheckBox>(R.id.checkBoxPaidBill).isChecked
        val billAttachment = imageUri?.toString()

        // Validate mandatory fields
        if (billAmount <= 0) {
            Toast.makeText(this, R.string.enter_valid_amount, Toast.LENGTH_SHORT).show()
            return
        }
        if (!validateTemplateNameIfNeeded()) return

        // Convert date string to Timestamp
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val billDate: Date? = try {
            sdf.parse(billDateString)
        } catch (e: Exception) {
            null
        }
        val timestamp = billDate?.let { Timestamp(it) }

        val uid = userUid
        if (uid != null && billDate != null) {
            val saveButton = findViewById<Button>(R.id.btnSaveBill)
            saveButton.isEnabled = false
            UsageLimits.checkBillCreation(this, uid, billRepeat, billDate) { allowed, messageRes ->
                if (!allowed) {
                    saveButton.isEnabled = true
                    Toast.makeText(
                        this,
                        messageRes ?: R.string.usage_limit_check_failed,
                        Toast.LENGTH_LONG
                    ).show()
                    return@checkBillCreation
                }

                val entryCurrency = entryCurrency()
                val bill = hashMapOf(
                    "name" to billName,
                    "amount" to billAmount,
                    "currency" to entryCurrency,
                    "date" to timestamp,
                    "category" to billCategory,
                    "subcategory" to billSubcategory,
                    "vendor" to billVendor,
                    "repeat" to billRepeat,
                    "isRecurring" to (billRepeat != "No"),
                    "comment" to billComment,
                    "attachment" to billAttachment,
                    "paid" to billPaid
                )
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
                    this.currency = entryCurrency
                    this.date = timestamp
                    this.paid = billPaid
                }

                if (billRepeat != "No") {
                    addRecurringBillsToBatch(
                        batch, billsToSchedule, uid, billName, billAmount, billDate,
                        billCategory, billSubcategory, billVendor, billRepeat,
                        billComment, billAttachment, billId, entryCurrency
                    )
                }

                batch.commit()
                    .addOnSuccessListener {
                        Toast.makeText(this, R.string.open_payment_saved, Toast.LENGTH_SHORT).show()
                        saveTemplateIfRequested(
                            billName, billAmount, billCategory, billSubcategory,
                            billVendor, billRepeat, billComment
                        )
                        FinancialEntryOptions.rememberVendor(this, billVendor)
                        billsToSchedule.forEach {
                            PaymentNotificationScheduler.scheduleBill(this, uid, it)
                        }
                        finish()
                    }
                    .addOnFailureListener { e ->
                        saveButton.isEnabled = true
                        Toast.makeText(this, getString(R.string.open_payment_save_failed, e.message.orEmpty()), Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun addRecurringBillsToBatch(
        batch: com.google.firebase.firestore.WriteBatch,
        billsToSchedule: MutableList<Bills>,
        uid: String, billTitle: String, billAmount: Double, startDate: Date, billCategory: String,
        billSubcategory: String, billVendor: String, billRepeat: String, billComment: String,
        billAttachment: String?, parentBillId: String, entryCurrency: String
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
                "currency" to entryCurrency,
                "date" to com.google.firebase.Timestamp(occurrenceDate),
                "category" to billCategory,
                "subcategory" to billSubcategory,
                "vendor" to billVendor,
                "repeat" to billRepeat,
                "isRecurring" to true,
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
                this.currency = entryCurrency
                this.date = Timestamp(occurrenceDate)
                this.paid = false
            }
        }
    }
}
