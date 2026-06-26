package com.ritter.smartstackbills

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class createIncome : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private var userEmail: String? = null
    private var userUid: String? = null
    private var imageUri: Uri? = null
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

    val repeatOptions = arrayOf(
        "No", "Weekly", "Every 2 Weeks", "Monthly", "Every 2 Months", "Quarterly", "Every 6 months", "Yearly"
    )

    val categories = arrayOf(
        "Salary", "Self-Employment/Freelance Work", "Rental and Lease Income",
        "Investment Income", "Business Income", "Retirement and Pensions",
        "Social Benefits", "Scholarships and Grants", "Royalties and Copyrights",
        "Other Income", "Create new type of income"
    )

    val subcategoriesMap = mapOf(
        "Salary" to arrayOf("Regular salary", "Overtime pay", "Bonuses or incentives", "Others"),
        "Self-Employment/Freelance Work" to arrayOf("Fees for service", "Project-based income", "Others"),
        "Rental and Lease Income" to arrayOf("Real estate", "Vehicles", "Equipment", "Others"),
        "Investment Income" to arrayOf("Dividends", "Interest earnings", "Capital gains", "Others"),
        "Business Income" to arrayOf("Profit shares", "Executive salary", "Others"),
        "Retirement and Pensions" to arrayOf("State pension", "Company pension plans", "Private pension insurance", "Other"),
        "Social Benefits" to arrayOf("Unemployment benefits", "Sickness benefits", "Parental benefits", "Others"),
        "Scholarships and Grants" to arrayOf("Educational scholarships", "Research grants", "Others"),
        "Royalties and Copyrights" to arrayOf("Patents", "Software licenses", "Book publications", "Others"),
        "Other Income" to arrayOf("Benefits in kind", "Inheritances and gifts", "Crowdfunding/Crowdinvesting returns", "Others")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_income)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.feature3_constraint_layout)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        userEmail = intent.getStringExtra(AuthUtils.EXTRA_USER_EMAIL) ?: AuthUtils.currentUserEmail()
        userUid = FirebaseAuth.getInstance().currentUser?.uid

        if (!PremiumAccess.isPremiumUser(this)) {
            showUpgradeDialog()
            return
        }

        val edtDate = findViewById<EditText>(R.id.edtDateIncome)
        edtDate.inputType = InputType.TYPE_NULL  // Disable manual input
        edtDate.setOnClickListener { showDatePickerDialog() }
        edtDate.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) { showDatePickerDialog() }
        }
        setTodayIfBlank(edtDate)

        val spinnerCategories = findViewById<Spinner>(R.id.spinnerCategoriesIncome)
        val spinnerSubcategories = findViewById<Spinner>(R.id.spinnerSubcategoriesIncome)
        val spinnerRepeat = findViewById<Spinner>(R.id.spinnerRepeatIncome)
        val saveButton = findViewById<Button>(R.id.btnSaveIncome)

        val arrayAdapterRepeat = ArrayAdapter(this, R.layout.spinner_item, repeatOptions)
        arrayAdapterRepeat.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRepeat.adapter = arrayAdapterRepeat

        loadCategories(spinnerCategories)
        selectOptionByKey(spinnerCategories, "Other income")
        spinnerCategories.selectedItem?.let {
            loadSubcategories(FinancialEntryOptions.selectedKey(it), spinnerSubcategories)
        }
        setupMoreDetailsToggle(
            R.id.tvMoreDetailsIncome,
            R.id.txtSourceIncome,
            R.id.edtSourceIncome,
            R.id.txtCategoryIncome,
            R.id.spinnerCategoriesIncome,
            R.id.txtSubcategoryIncome,
            R.id.spinnerSubcategoriesIncome,
            R.id.txtRepeatIncome,
            R.id.spinnerRepeatIncome,
            R.id.tvRecurrenceNoteIncome,
            R.id.txtCommentIncome,
            R.id.edtCommentIncome,
            R.id.layoutAttachmentIncome
        )
        spinnerCategories.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                spinnerCategories.selectedItem?.let {
                    loadSubcategories(FinancialEntryOptions.selectedKey(it), spinnerSubcategories)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        setupTemplates(spinnerCategories, spinnerSubcategories, spinnerRepeat)

        saveButton.setOnClickListener {
            saveIncome()
        }

        val btnCancel = findViewById<Button>(R.id.btnCancelIncome)
        btnCancel.setOnClickListener {
            confirmDiscardIfNeeded()
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                confirmDiscardIfNeeded()
            }
        })
        findViewById<Button>(R.id.btnUploadImageIncome).setOnClickListener {
            handleImageUpload()
        }
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
        return text(R.id.edtTitleIncome).isNotBlank() ||
            text(R.id.edtAmountIncome).isNotBlank() ||
            text(R.id.edtSourceIncome).isNotBlank() ||
            text(R.id.edtCommentIncome).isNotBlank() ||
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
            findViewById<EditText>(R.id.edtTitleIncome).setText(it)
        }
        intent.getStringExtra(ScanPrefill.EXTRA_SCAN_AMOUNT)?.takeIf { it.isNotBlank() }?.let {
            findViewById<EditText>(R.id.edtAmountIncome).setText(it)
        }
        ScanDateValidator.sanitizeDisplayDate(intent.getStringExtra(ScanPrefill.EXTRA_SCAN_DATE))?.let {
            findViewById<EditText>(R.id.edtDateIncome).setText(it)
        }
        intent.getStringExtra(ScanPrefill.EXTRA_SCAN_PARTY)?.takeIf { it.isNotBlank() }?.let {
            findViewById<EditText>(R.id.edtSourceIncome).setText(it)
        }
        intent.getStringExtra(ScanPrefill.EXTRA_SCAN_ATTACHMENT)?.takeIf { it.isNotBlank() }?.let {
            imageUri = Uri.parse(it)
            updateAttachmentStatus()
        }
        showScanCurrencyWarningIfNeeded()
    }

    private fun showScanReviewHintIfNeeded() {
        findViewById<TextView>(R.id.tvScanReviewHintIncome).visibility =
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

    private fun showDatePickerDialog() {
        val datePicker = DatePickerFragment { day, month, year -> onDateSelected(day, month, year) }
        datePicker.show(supportFragmentManager, "datePicker")
    }

    fun onDateSelected(day: Int, month: Int, year: Int) {
        val edtDate = findViewById<EditText>(R.id.edtDateIncome)
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
        spinnerRepeat: Spinner
    ) {
        findViewById<CheckBox>(R.id.checkSaveTemplateIncome).setOnCheckedChangeListener { _, checked ->
            findViewById<EditText>(R.id.edtTemplateNameIncome).visibility = if (checked) View.VISIBLE else View.GONE
        }

        val templates = EntryTemplateStore.templates(this, EntryTemplateStore.Type.INCOME)
        val container = findViewById<LinearLayout>(R.id.layoutTemplatesIncome)
        val chips = findViewById<LinearLayout>(R.id.templateChipsIncome)
        if (templates.isEmpty()) {
            container.visibility = View.GONE
            return
        }
        container.visibility = View.VISIBLE
        val toggle = findViewById<TextView>(R.id.tvUseTemplateIncome)
        val toggleRow = findViewById<LinearLayout>(R.id.templateToggleIncome)
        val arrow = findViewById<ImageView>(R.id.templateArrowIncome)
        val scroll = findViewById<HorizontalScrollView>(R.id.templateScrollIncome)
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
                findViewById<EditText>(R.id.edtTitleIncome).setText(template.title)
                findViewById<EditText>(R.id.edtAmountIncome).setText(
                    if (template.amount > 0.0) CurrencyPreferences.formatPlain(template.amount) else ""
                )
                selectOptionByKey(spinnerCategories, template.category)
                loadSubcategories(template.category, spinnerSubcategories)
                selectOptionByKey(spinnerSubcategories, template.subcategory)
                findViewById<EditText>(R.id.edtSourceIncome).setText(template.vendorOrSource)
                selectOptionByText(spinnerRepeat, template.repeat)
                findViewById<EditText>(R.id.edtCommentIncome).setText(template.comment)
            })
        }
    }

    private fun templateButton(label: String, onClick: () -> Unit): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            setTextColor(ContextCompat.getColor(this@createIncome, R.color.colorPrimary))
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
        val saveTemplate = findViewById<CheckBox>(R.id.checkSaveTemplateIncome).isChecked
        val templateName = findViewById<EditText>(R.id.edtTemplateNameIncome).text.toString().trim()
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
        source: String,
        repeat: String,
        comment: String
    ) {
        if (!findViewById<CheckBox>(R.id.checkSaveTemplateIncome).isChecked) return
        val templateName = findViewById<EditText>(R.id.edtTemplateNameIncome).text.toString().trim()
        EntryTemplateStore.save(
            this,
            EntryTemplateStore.Template(
                id = "income_${templateName.lowercase(Locale.US)}",
                type = EntryTemplateStore.Type.INCOME,
                templateName = templateName,
                title = title,
                amount = amount,
                category = category,
                subcategory = subcategory,
                vendorOrSource = source,
                repeat = repeat,
                comment = comment
            )
        )
        Toast.makeText(this, R.string.template_saved, Toast.LENGTH_SHORT).show()
    }

    private fun validateDateField(): Boolean {
        val edtDate = findViewById<EditText>(R.id.edtDateIncome)
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        dateFormat.isLenient = false  // This ensures that the date format is strict

        return try {
            val incomeDate = dateFormat.parse(edtDate.text.toString())
            if (incomeDate != null) {
                true
            } else {
                Toast.makeText(this, R.string.invalid_date_format, Toast.LENGTH_SHORT).show()
                false
            }
        } catch (e: Exception) {
            Toast.makeText(this, R.string.invalid_date_format, Toast.LENGTH_SHORT).show()
            false
        }
    }

    private fun saveIncome() {
        if (validateMandatoryFields() && validateDateField() && validateTemplateNameIfNeeded()) {
            val uid = userUid
            if (uid != null) {
                val incomeTitle = findViewById<EditText>(R.id.edtTitleIncome).text.toString().trim()
                    .ifBlank { getString(R.string.income) }
                val incomeAmount = CurrencyPreferences.roundToTwoDecimals(
                    findViewById<EditText>(R.id.edtAmountIncome).text.toString().toDoubleOrNull() ?: 0.0
                )
                val incomeSource = findViewById<EditText>(R.id.edtSourceIncome).text.toString().trim()
                val dateEditText = findViewById<EditText>(R.id.edtDateIncome)
                setTodayIfBlank(dateEditText)
                val incomeDateString = dateEditText.text.toString()
                val incomeCategory = FinancialEntryOptions.selectedKey(
                    findViewById<Spinner>(R.id.spinnerCategoriesIncome).selectedItem
                ).ifBlank { "Other income" }
                val incomeSubcategory = FinancialEntryOptions.selectedKey(
                    findViewById<Spinner>(R.id.spinnerSubcategoriesIncome).selectedItem
                ).ifBlank { "Miscellaneous" }
                val incomeRepeat = findViewById<Spinner>(R.id.spinnerRepeatIncome).selectedItem.toString()
                val incomeComment = findViewById<EditText>(R.id.edtCommentIncome).text.toString()
                val incomeAttachment = imageUri?.toString()

                // Convert String to Date
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val incomeDate: Date? = try {
                    sdf.parse(incomeDateString)
                } catch (e: Exception) {
                    null
                }
                val timestamp = incomeDate?.let { com.google.firebase.Timestamp(it) }

                if (incomeDate != null) {
                    val saveButton = findViewById<Button>(R.id.btnSaveIncome)
                    saveButton.isEnabled = false
                    UsageLimits.checkIncomeCreation(this, uid, incomeRepeat, incomeDate) { allowed, messageRes ->
                        if (!allowed) {
                            saveButton.isEnabled = true
                            Toast.makeText(
                                this,
                                messageRes ?: R.string.usage_limit_check_failed,
                                Toast.LENGTH_LONG
                            ).show()
                            return@checkIncomeCreation
                        }

                        val entryCurrency = entryCurrency()
                        val income = hashMapOf(
                            "name" to incomeTitle,
                            "amount" to incomeAmount,
                            "currency" to entryCurrency,
                            "date" to timestamp,
                            "category" to incomeCategory,
                            "subcategory" to incomeSubcategory,
                            "repeat" to incomeRepeat,
                            "isRecurring" to (incomeRepeat != "No"),
                            "comment" to incomeComment,
                            "source" to incomeSource,
                            "attachment" to incomeAttachment,
                        )

                        val docRef = db.collection("users").document(uid).collection("income").document()
                        val incomeId = docRef.id
                        income["incomeId"] = incomeId
                        income["parentIncomeId"] = incomeId

                        val batch = db.batch()
                        batch.set(docRef, income)
                        if (incomeRepeat != "No") {
                            addRecurringIncomeToBatch(
                                batch, uid, incomeTitle, incomeAmount, incomeDate,
                                incomeCategory, incomeSubcategory, incomeRepeat,
                                incomeComment, incomeSource, incomeAttachment, incomeId, entryCurrency
                            )
                        }

                        batch.commit()
                            .addOnSuccessListener {
                                Toast.makeText(this, R.string.income_saved, Toast.LENGTH_SHORT).show()
                                saveTemplateIfRequested(
                                    incomeTitle, incomeAmount, incomeCategory, incomeSubcategory,
                                    incomeSource, incomeRepeat, incomeComment
                                )
                                val intent = Intent(this, MyIncome::class.java)
                                intent.putExtra(AuthUtils.EXTRA_USER_EMAIL, userEmail)
                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener { e ->
                                saveButton.isEnabled = true
                                Toast.makeText(this, getString(R.string.income_save_failed, e.message.orEmpty()), Toast.LENGTH_SHORT).show()
                            }
                    }
                }
            } else {
                Toast.makeText(this, R.string.user_session_unavailable, Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun addRecurringIncomeToBatch(
        batch: com.google.firebase.firestore.WriteBatch,
        uid: String, incomeTitle: String, incomeAmount: Double, incomeDate: Date, incomeCategory: String, incomeSubcategory: String,
        incomeRepeat: String, incomeComment: String, incomeSource: String,
        incomeAttachment: String?, parentIncomeId: String, entryCurrency: String
    ) {
        val calendar = Calendar.getInstance()
        calendar.time = incomeDate

        // Define the max end date (1 year in advance)
        val endDate = Calendar.getInstance()
        endDate.add(Calendar.YEAR, 1)

        // Loop to generate future occurrences up to 1 year
        while (calendar.before(endDate)) {
            // Adjust the date based on the selected repeat value
            when (incomeRepeat) {
                "Monthly" -> calendar.add(Calendar.MONTH, 1)
                "Weekly" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                "Every 2 Weeks" -> calendar.add(Calendar.WEEK_OF_YEAR, 2)
                "Every 2 Months" -> calendar.add(Calendar.MONTH, 2)
                "Quarterly" -> calendar.add(Calendar.MONTH, 3)
                "Every 6 months" -> calendar.add(Calendar.MONTH, 6)
                "Yearly" -> calendar.add(Calendar.YEAR, 1)
            }
            // Check if the new date is within 1 year, if not, stop the loop
            if (calendar.after(endDate)) {
                break
            }
            // Create a new incomeId for the next occurrence
            val newIncomeId = db.collection("users").document(uid).collection("income").document().id
            // Prepare the data for the recurring income
            val recurringIncome = hashMapOf(
                "name" to incomeTitle,
                "amount" to incomeAmount,
                "currency" to entryCurrency,
                "date" to com.google.firebase.Timestamp(calendar.time),
                "category" to incomeCategory,
                "subcategory" to incomeSubcategory,
                "repeat" to incomeRepeat,
                "isRecurring" to true,
                "comment" to incomeComment,
                "source" to incomeSource,
                "attachment" to incomeAttachment,
                "parentIncomeId" to parentIncomeId,  // Link to the original income
                "incomeId" to newIncomeId
            )
            val recurringRef = db.collection("users").document(uid)
                .collection("income").document(newIncomeId)
            batch.set(recurringRef, recurringIncome)
        }
    }

    private fun validateMandatoryFields(): Boolean {
        val incomeAmount = findViewById<EditText>(R.id.edtAmountIncome).text.toString()

        if (incomeAmount.toDoubleOrNull()?.let { it > 0 } != true) {
            Toast.makeText(this, R.string.enter_valid_amount, Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun loadCategories(spinnerCategories: Spinner) {
        val arrayAdapterCategories = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            FinancialEntryOptions.incomeCategories(this)
        )
        spinnerCategories.adapter = arrayAdapterCategories
    }

    private fun loadSubcategories(category: String, spinnerSubcategories: Spinner) {
        val subcategories = FinancialEntryOptions.incomeSubcategories(this, category)
        val arrayAdapterSubcategories = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, subcategories)
        spinnerSubcategories.adapter = arrayAdapterSubcategories
    }

    private fun handleImageUpload() {
        AlertDialog.Builder(this)
            .setTitle(R.string.add_attachment)
            .setItems(
                arrayOf(
                    getString(R.string.scan_and_prefill),
                    getString(R.string.take_photo),
                    getString(R.string.choose_from_gallery),
                    getString(R.string.cancel)
                )
            ) { dialog, which ->
                when (which) {
                    0 -> {
                        startActivity(Intent(this, DocumentScanActivity::class.java).apply {
                            putExtra(DocumentScanActivity.EXTRA_ENTRY_TYPE, EntryType.INCOME.wireValue)
                            putExtra(AuthUtils.EXTRA_USER_EMAIL, userEmail)
                        })
                        finish()
                    }
                    1 -> dispatchTakePictureIntent()
                    2 -> chooseImageLauncher.launch("image/*")
                    else -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun dispatchTakePictureIntent() {
        val photoFile = try {
            createImageFile()
        } catch (_: IOException) {
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
        findViewById<TextView>(R.id.txtImageAddedIncome).apply {
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
            val outputFile = File(storageDir, "INCOME_${timeStamp}.jpg")
            contentResolver.openInputStream(sourceUri)?.use { input ->
                outputFile.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            Uri.fromFile(outputFile)
        } catch (_: Exception) {
            null
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: throw IOException("Pictures directory unavailable")
        return File.createTempFile("INCOME_${timeStamp}_", ".jpg", storageDir)
    }
}
