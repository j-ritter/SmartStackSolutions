package com.ritter.smartstackbills

import android.content.Intent
import android.os.Bundle
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
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import android.os.Environment
import android.text.InputType
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class createSpending : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private var userEmail: String? = null
    private var userUid: String? = null
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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.feature2_constraint_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        userEmail = intent.getStringExtra(AuthUtils.EXTRA_USER_EMAIL) ?: AuthUtils.currentUserEmail()
        userUid = FirebaseAuth.getInstance().currentUser?.uid

        if (!PremiumAccess.isPremiumUser(this)) {
            showUpgradeDialog()
            return
        }

        val repeatValue = intent.getStringExtra("repeat") ?: "No"

        val edtDate = findViewById<EditText>(R.id.edtDateSpending)
        edtDate.inputType = InputType.TYPE_NULL  // Disable manual input
        edtDate.setOnClickListener {
            showDatePickerDialog()
        }
        edtDate.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) showDatePickerDialog()
        }
        setTodayIfBlank(edtDate)

        val checkBoxPaid = findViewById<CheckBox>(R.id.checkBoxPaidSpending).apply {
            isChecked = true
            isEnabled = false
        }

        val spinnerCategories = findViewById<Spinner>(R.id.spinnerCategoriesSpending)
        val spinnerSubcategories = findViewById<Spinner>(R.id.spinnerSubcategoriesSpending)
        val autoCompleteVendors = findViewById<AutoCompleteTextView>(R.id.autoCompleteVendorSpending)

        loadCategories(spinnerCategories)
        selectOptionByKey(spinnerCategories, "Other")
        spinnerCategories.selectedItem?.let {
            val selectedCategory = FinancialEntryOptions.selectedKey(it)
            loadSubcategories(selectedCategory, spinnerSubcategories)
            selectDefaultSubcategory(spinnerSubcategories, selectedCategory)
        }
        setupMoreDetailsToggle(
            R.id.tvMoreDetailsSpending,
            R.id.txtCategorySpending,
            R.id.spinnerCategoriesSpending,
            R.id.txtSubcategorySpending,
            R.id.spinnerSubcategoriesSpending,
            R.id.txtVendorSpending,
            R.id.autoCompleteVendorSpending,
            R.id.txtCommentSpending,
            R.id.edtCommentSpending,
            R.id.layoutAttachmentSpending
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
        setupTemplates(spinnerCategories, spinnerSubcategories, autoCompleteVendors)

        val saveButton = findViewById<Button>(R.id.btnSaveSpending)
        saveButton.setOnClickListener { saveSpending() }

        val btnCancel = findViewById<Button>(R.id.btnCancelSpending)
        btnCancel.setOnClickListener {
            confirmDiscardIfNeeded()
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                confirmDiscardIfNeeded()
            }
        })

        // Image upload handling
        findViewById<Button>(R.id.btnUploadImageSpending).setOnClickListener { handleImageUpload() }
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
        return text(R.id.edtTitleSpending).isNotBlank() ||
            text(R.id.edtAmountSpending).isNotBlank() ||
            findViewById<AutoCompleteTextView>(R.id.autoCompleteVendorSpending).text?.toString()?.trim().orEmpty().isNotBlank() ||
            text(R.id.edtCommentSpending).isNotBlank() ||
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
            findViewById<EditText>(R.id.edtTitleSpending).setText(it)
        }
        intent.getStringExtra(ScanPrefill.EXTRA_SCAN_AMOUNT)?.takeIf { it.isNotBlank() }?.let {
            findViewById<EditText>(R.id.edtAmountSpending).setText(it)
        }
        ScanDateValidator.sanitizeDisplayDate(intent.getStringExtra(ScanPrefill.EXTRA_SCAN_DATE))?.let {
            findViewById<EditText>(R.id.edtDateSpending).setText(it)
        }
        intent.getStringExtra(ScanPrefill.EXTRA_SCAN_PARTY)?.takeIf { it.isNotBlank() }?.let {
            findViewById<AutoCompleteTextView>(R.id.autoCompleteVendorSpending).setText(it)
        }
        intent.getStringExtra(ScanPrefill.EXTRA_SCAN_ATTACHMENT)?.takeIf { it.isNotBlank() }?.let {
            imageUri = Uri.parse(it)
            updateAttachmentStatus()
        }
        showScanCurrencyWarningIfNeeded()
    }

    private fun showScanReviewHintIfNeeded() {
        findViewById<TextView>(R.id.tvScanReviewHintSpending).visibility =
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

    private fun showUpgradeDialog() {
        PremiumUpgradeDialog.show(
            this,
            R.string.premium_upgrade_required,
            intent.getStringExtra(AuthUtils.EXTRA_USER_EMAIL),
            finishHost = true
        )
    }

    private fun loadVendors(category: String, autoCompleteVendors: AutoCompleteTextView) {
        val vendors = FinancialEntryOptions.vendorSuggestions(this, category)

        // Set the adapter for the AutoCompleteTextView
        val arrayAdapterVendors = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            vendors
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

    private fun setupTemplates(
        spinnerCategories: Spinner,
        spinnerSubcategories: Spinner,
        autoCompleteVendors: AutoCompleteTextView
    ) {
        findViewById<CheckBox>(R.id.checkSaveTemplateSpending).setOnCheckedChangeListener { _, checked ->
            findViewById<EditText>(R.id.edtTemplateNameSpending).visibility = if (checked) View.VISIBLE else View.GONE
        }

        val templates = EntryTemplateStore.templates(this, EntryTemplateStore.Type.CLOSED_PAYMENT)
        val container = findViewById<LinearLayout>(R.id.layoutTemplatesSpending)
        val chips = findViewById<LinearLayout>(R.id.templateChipsSpending)
        if (templates.isEmpty()) {
            container.visibility = View.GONE
            return
        }
        container.visibility = View.VISIBLE
        val toggle = findViewById<TextView>(R.id.tvUseTemplateSpending)
        val toggleRow = findViewById<LinearLayout>(R.id.templateToggleSpending)
        val arrow = findViewById<ImageView>(R.id.templateArrowSpending)
        val scroll = findViewById<HorizontalScrollView>(R.id.templateScrollSpending)
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
                findViewById<EditText>(R.id.edtTitleSpending).setText(template.title)
                findViewById<EditText>(R.id.edtAmountSpending).setText(
                    if (template.amount > 0.0) CurrencyPreferences.formatPlain(template.amount) else ""
                )
                selectOptionByKey(spinnerCategories, template.category)
                loadVendors(template.category, autoCompleteVendors)
                loadSubcategories(template.category, spinnerSubcategories)
                selectOptionByKey(spinnerSubcategories, template.subcategory)
                autoCompleteVendors.setText(template.vendorOrSource)
                findViewById<EditText>(R.id.edtCommentSpending).setText(template.comment)
            })
        }
    }

    private fun templateButton(label: String, onClick: () -> Unit): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            setTextColor(ContextCompat.getColor(this@createSpending, R.color.colorPrimary))
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
        val saveTemplate = findViewById<CheckBox>(R.id.checkSaveTemplateSpending).isChecked
        val templateName = findViewById<EditText>(R.id.edtTemplateNameSpending).text.toString().trim()
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
        comment: String
    ) {
        if (!findViewById<CheckBox>(R.id.checkSaveTemplateSpending).isChecked) return
        val templateName = findViewById<EditText>(R.id.edtTemplateNameSpending).text.toString().trim()
        EntryTemplateStore.save(
            this,
            EntryTemplateStore.Template(
                id = "closed_payment_${templateName.lowercase(Locale.US)}",
                type = EntryTemplateStore.Type.CLOSED_PAYMENT,
                templateName = templateName,
                title = title,
                amount = amount,
                category = category,
                subcategory = subcategory,
                vendorOrSource = vendor,
                repeat = "No",
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
                        putExtra(DocumentScanActivity.EXTRA_ENTRY_TYPE, EntryType.CLOSED_PAYMENT.wireValue)
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

    // Date validation to ensure it's in correct format
    private fun validateDateField(): Boolean {
        val edtDate = findViewById<EditText>(R.id.edtDateSpending)
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return try {
            dateFormat.isLenient = false
            dateFormat.parse(edtDate.text.toString()) != null
        } catch (e: Exception) {
            Toast.makeText(this, R.string.invalid_date_format, Toast.LENGTH_SHORT).show()
            false
        }
    }

    private fun loadCategories(spinnerCategories: Spinner) {
        val arrayAdapterCategories = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            FinancialEntryOptions.expenseCategories(this)
        )
        spinnerCategories.adapter = arrayAdapterCategories
    }

    private fun loadSubcategories(category: String, spinnerSubcategories: Spinner) {
        val subcategories = FinancialEntryOptions.expenseSubcategories(this, category)
        val arrayAdapterSubcategories = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, subcategories)
        spinnerSubcategories.adapter = arrayAdapterSubcategories
    }

    private fun saveSpending() {
        if (validateMandatoryFields() && validateDateField() && validateTemplateNameIfNeeded()) {
            userUid?.let {
                val spendingName = findViewById<EditText>(R.id.edtTitleSpending).text.toString().trim()
                    .ifBlank { getString(R.string.closed_payment) }
                val spendingAmount = CurrencyPreferences.roundToTwoDecimals(
                    findViewById<EditText>(R.id.edtAmountSpending).text.toString().toDoubleOrNull() ?: 0.0
                )
                val dateEditText = findViewById<EditText>(R.id.edtDateSpending)
                setTodayIfBlank(dateEditText)
                val spendingDateString = dateEditText.text.toString()
                val selectedSpendingCategory = FinancialEntryOptions.selectedKey(
                    findViewById<Spinner>(R.id.spinnerCategoriesSpending).selectedItem
                )
                val selectedSpendingSubcategory = FinancialEntryOptions.selectedKey(
                    findViewById<Spinner>(R.id.spinnerSubcategoriesSpending).selectedItem
                )
                val spendingCategory = FinancialEntryOptions.normalizedExpenseCategory(
                    this,
                    selectedSpendingCategory
                )
                val spendingSubcategory = FinancialEntryOptions.normalizedExpenseSubcategory(
                    this,
                    spendingCategory,
                    selectedSpendingSubcategory
                )
                val spendingVendor = findViewById<AutoCompleteTextView>(R.id.autoCompleteVendorSpending).text.toString()
                val spendingComment = findViewById<EditText>(R.id.edtCommentSpending).text.toString()
                val spendingAttachment = imageUri?.toString()

                val spendingDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(spendingDateString)
                val timestamp = spendingDate?.let { com.google.firebase.Timestamp(it) }

                val repeatValue = intent.getStringExtra("repeat") ?: "No"

                // Generate a unique ID for the spending
                val spendingId = db.collection("users").document(it).collection("spendings").document().id

                val spending = hashMapOf(
                    "spendingId" to spendingId,
                    "name" to spendingName,
                    "amount" to spendingAmount,
                    "currency" to entryCurrency(),
                    "date" to timestamp,
                    "category" to spendingCategory,
                    "subcategory" to spendingSubcategory,
                    "vendor" to spendingVendor,
                    "repeat" to repeatValue,
                    "isRecurring" to (repeatValue != "No"),
                    "comment" to spendingComment,
                    "attachment" to spendingAttachment,
                    "paid" to true
                )

                // Save spending to Firestore with specified document ID
                db.collection("users").document(it).collection("spendings").document(spendingId)
                    .set(spending)
                    .addOnSuccessListener {
                        Toast.makeText(this, R.string.closed_payment_saved, Toast.LENGTH_SHORT).show()
                        saveTemplateIfRequested(
                            spendingName, spendingAmount, spendingCategory, spendingSubcategory,
                            spendingVendor, spendingComment
                        )
                        FinancialEntryOptions.rememberVendor(this, spendingVendor)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, getString(R.string.closed_payment_save_failed, e.message.orEmpty()), Toast.LENGTH_SHORT).show()
                    }
            } ?: run {
                Toast.makeText(this, R.string.user_session_unavailable, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validateMandatoryFields(): Boolean {
        val spendingAmount = findViewById<EditText>(R.id.edtAmountSpending).text.toString()

        if (spendingAmount.toDoubleOrNull()?.let { it > 0 } != true) {
            Toast.makeText(this, R.string.enter_valid_amount, Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    // Methods for handling image capture and gallery selection
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
        findViewById<TextView>(R.id.txtImageAddedSpending).apply {
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
            val outputFile = File(storageDir, "SPENDING_${timeStamp}.jpg")
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
}
