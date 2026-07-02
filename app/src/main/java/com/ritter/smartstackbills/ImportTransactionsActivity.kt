package com.ritter.smartstackbills

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.SetOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class ImportTransactionsActivity : AppCompatActivity() {
    private companion object {
        const val MAX_PDF_BYTES = 25 * 1024 * 1024
        const val FREE_IMPORTS_PER_FILE = 100
        const val FREE_TOTAL_IMPORTED_TRANSACTIONS = 100L
    }
    private val transactions = mutableListOf<ImportedTransaction>()
    private val db = FirebaseFirestore.getInstance()
    private val executor = Executors.newSingleThreadExecutor()
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private lateinit var adapter: ImportTransactionsAdapter
    private lateinit var progress: ProgressBar
    private lateinit var status: TextView
    private lateinit var summary: TextView
    private lateinit var currencyStatus: TextView
    private lateinit var fileNameView: TextView
    private lateinit var blockingReason: TextView
    private lateinit var allowanceStatus: TextView
    private lateinit var importButton: Button
    private lateinit var chooseFileButton: Button
    private var sourceFileName = ""
    private var detectedCurrency: String? = null
    private var existingFreeImportCount: Long? = null
    private var isLoading = false

    private val filePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            sourceFileName = displayName(uri)
            if (::fileNameView.isInitialized) {
                fileNameView.text = getString(R.string.selected_file, sourceFileName)
            }
            if (::chooseFileButton.isInitialized) {
                chooseFileButton.text = getString(R.string.change_file)
            }
            readStatement(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_import_transactions)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.importTransactionsRoot)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<MaterialToolbar>(R.id.importTransactionsToolbar)
            .setNavigationOnClickListener { confirmDiscardIfNeeded() }
        progress = findViewById(R.id.importTransactionsProgress)
        status = findViewById(R.id.importTransactionsStatus)
        summary = findViewById(R.id.importTransactionsSummary)
        currencyStatus = findViewById(R.id.importTransactionsCurrency)
        fileNameView = findViewById(R.id.importTransactionsFileName)
        blockingReason = findViewById(R.id.importTransactionsBlockingReason)
        allowanceStatus = findViewById(R.id.importTransactionsAllowance)
        importButton = findViewById(R.id.importTransactionsConfirm)
        chooseFileButton = findViewById(R.id.importTransactionsChooseFile)
        adapter = ImportTransactionsAdapter(transactions, ::updateSummary)
        findViewById<RecyclerView>(R.id.importTransactionsList).apply {
            layoutManager = LinearLayoutManager(this@ImportTransactionsActivity)
            adapter = this@ImportTransactionsActivity.adapter
        }
        chooseFileButton.setOnClickListener {
            openPicker()
        }
        importButton.setOnClickListener { saveTransactions() }
        chooseFileButton.text = getString(R.string.choose_file)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                confirmDiscardIfNeeded()
            }
        })
    }

    private fun confirmDiscardIfNeeded() {
        if (transactions.isEmpty() && !isLoading) {
            finish()
            return
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.unsaved_changes_title)
            .setMessage(R.string.unsaved_import_message)
            .setNegativeButton(R.string.keep_editing, null)
            .setPositiveButton(R.string.discard_changes) { _, _ -> finish() }
            .show()
    }

    override fun onDestroy() {
        recognizer.close()
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun openPicker() {
        filePicker.launch(
            arrayOf(
                "application/pdf",
                "text/csv",
                "text/comma-separated-values",
                "application/csv",
                "text/plain"
            )
        )
    }

    private fun readStatement(uri: Uri) {
        setLoading(true, getString(R.string.reading_statement))
        val type = contentResolver.getType(uri).orEmpty()
        if (type == "application/pdf" || sourceFileName.endsWith(".pdf", true)) {
            readPdf(uri)
        } else {
            executor.execute {
                val result = runCatching {
                    contentResolver.openInputStream(uri)?.use(StatementCsvParser::parse)
                        ?: StatementImportResult(emptyList(), null)
                }
                runOnUiThread {
                    result.fold(::showParsedResult) {
                        showImportError(it.message)
                    }
                }
            }
        }
    }

    private fun readPdf(uri: Uri) {
        executor.execute {
            val localCopy = copyPdfToCache(uri)
            if (localCopy == null) {
                runOnUiThread { showImportError(null) }
                return@execute
            }
            runOnUiThread { processPdf(localCopy) }
        }
    }

    private fun processPdf(file: File) {
        val descriptor = runCatching {
            android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
        }.getOrNull()
        if (descriptor == null) {
            showImportError(null)
            return
        }
        val renderer = runCatching { PdfRenderer(descriptor) }.getOrElse {
            descriptor.close()
            showImportError(it.message)
            return
        }
        val elements = mutableListOf<OcrStatementElement>()
        val pageCount = renderer.pageCount.coerceAtMost(20)

        fun finishPdf(result: StatementImportResult? = null, error: Throwable? = null) {
            runCatching { renderer.close() }
            runCatching { descriptor.close() }
            file.delete()
            if (result != null) showParsedResult(result) else showImportError(error?.message)
        }

        fun processPage(index: Int) {
            if (index >= pageCount) {
                finishPdf(StatementPdfParser.parse(elements))
                return
            }
            status.text = getString(R.string.reading_statement_page, index + 1, pageCount)
            executor.execute {
                val rendered = runCatching {
                    renderer.openPage(index).use { page ->
                        val scale = (2200f / page.width).coerceIn(1.8f, 3f)
                        val bitmap = Bitmap.createBitmap(
                            (page.width * scale).toInt(),
                            (page.height * scale).toInt(),
                            Bitmap.Config.ARGB_8888
                        )
                        bitmap.eraseColor(android.graphics.Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap
                    }
                }
                runOnUiThread {
                    val bitmap = rendered.getOrElse {
                        finishPdf(error = it)
                        return@runOnUiThread
                    }
                    recognizer.process(InputImage.fromBitmap(bitmap, 0))
                        .addOnSuccessListener { text ->
                            text.textBlocks.forEach { block ->
                                block.lines.forEach { line ->
                                    line.elements.forEach { element ->
                                        val bounds = element.boundingBox ?: line.boundingBox ?: Rect()
                                        elements += OcrStatementElement(
                                            page = index + 1,
                                            text = element.text,
                                            bounds = Rect(bounds),
                                            pageWidth = bitmap.width,
                                            pageHeight = bitmap.height
                                        )
                                    }
                                }
                            }
                            bitmap.recycle()
                            processPage(index + 1)
                        }
                        .addOnFailureListener {
                            bitmap.recycle()
                            finishPdf(error = it)
                        }
                }
            }
        }
        processPage(0)
    }

    private fun showParsedResult(result: StatementImportResult) {
        detectedCurrency = result.detectedCurrency
        val importCurrency = CurrencyPreferences.selectedCode(this)
        transactions.clear()
        transactions.addAll(result.transactions.onEach { it.currency = importCurrency })
        if (transactions.isEmpty()) {
            setLoading(false, getString(R.string.no_transactions_detected))
            Toast.makeText(this, R.string.no_transactions_detected, Toast.LENGTH_LONG).show()
            return
        }
        setLoading(true, getString(R.string.checking_duplicates))
        checkDuplicates {
            adapter.notifyDataSetChanged()
            setLoading(false, result.warnings.joinToString("\n"))
            updateSummary()
        }
    }

    private fun checkDuplicates(onComplete: () -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null || transactions.isEmpty()) {
            onComplete()
            return
        }
        val minDate = transactions.minOf { it.date.time } - 86_400_000L
        val maxDate = transactions.maxOf { it.date.time } + 86_400_000L
        val user = db.collection("users").document(uid)
        val spendingTask = user.collection("spendings")
            .whereGreaterThanOrEqualTo("date", Timestamp(Date(minDate)))
            .whereLessThanOrEqualTo("date", Timestamp(Date(maxDate)))
            .get()
        val incomeTask = user.collection("income")
            .whereGreaterThanOrEqualTo("date", Timestamp(Date(minDate)))
            .whereLessThanOrEqualTo("date", Timestamp(Date(maxDate)))
            .get()
        spendingTask.addOnCompleteListener { spendingResult ->
            incomeTask.addOnCompleteListener { incomeResult ->
                val exact = mutableSetOf<String>()
                val core = mutableSetOf<String>()
                listOfNotNull(
                    spendingResult.takeIf { it.isSuccessful }?.result,
                    incomeResult.takeIf { it.isSuccessful }?.result
                ).forEach { snapshot ->
                    snapshot.documents.forEach { document ->
                        document.getString("importFingerprint")?.let(exact::add)
                        existingCoreFingerprint(document)?.let(core::add)
                    }
                }
                transactions.forEach { transaction ->
                    transaction.duplicate =
                        transaction.fingerprint() in exact || transaction.coreFingerprint() in core
                    if (transaction.duplicate) transaction.included = false
                }
                loadFreeImportUsage(onComplete)
            }
        }
    }

    private fun loadFreeImportUsage(onComplete: () -> Unit) {
        if (PremiumAccess.isPremiumUser(this)) {
            existingFreeImportCount = 0L
            onComplete()
            return
        }
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            existingFreeImportCount = null
            onComplete()
            return
        }
        statementImportUsageRef(uid).get()
            .addOnSuccessListener {
                if (it.exists()) {
                    existingFreeImportCount = it.getLong("count") ?: 0L
                    onComplete()
                } else {
                    initializeImportUsage(uid, onComplete)
                }
            }
            .addOnFailureListener {
                existingFreeImportCount = null
                onComplete()
            }
    }

    private fun initializeImportUsage(uid: String, onComplete: () -> Unit) {
        val user = db.collection("users").document(uid)
        user.collection("spendings")
            .whereGreaterThan("importHash", "")
            .count().get(AggregateSource.SERVER)
            .addOnSuccessListener { spendingCount ->
                user.collection("income")
                    .whereGreaterThan("importHash", "")
                    .count().get(AggregateSource.SERVER)
                    .addOnSuccessListener { incomeCount ->
                        val total = spendingCount.count + incomeCount.count
                        statementImportUsageRef(uid).set(
                            mapOf(
                                "count" to total,
                                "updatedAt" to Timestamp.now()
                            )
                        ).addOnCompleteListener {
                            existingFreeImportCount = if (it.isSuccessful) total else null
                            onComplete()
                        }
                    }
                    .addOnFailureListener {
                        existingFreeImportCount = null
                        onComplete()
                    }
            }
            .addOnFailureListener {
                existingFreeImportCount = null
                onComplete()
            }
    }

    private fun existingCoreFingerprint(document: DocumentSnapshot): String? {
        val name = document.getString("name") ?: return null
        val amount = CurrencyPreferences.roundToTwoDecimals(document.getDouble("amount") ?: return null)
        val date = document.getTimestamp("date")?.toDate() ?: return null
        val currency = document.getString("currency") ?: CurrencyPreferences.selectedCode(this)
        val type = if (document.reference.parent.id == "income") {
            ImportedTransactionType.INCOME
        } else {
            ImportedTransactionType.CLOSED_PAYMENT
        }
        return ImportedTransaction(
            sourceRow = 0,
            reference = null,
            title = name,
            amount = amount,
            date = date,
            type = type,
            category = "",
            subcategory = "",
            currency = currency
        ).coreFingerprint()
    }

    private fun updateSummary() {
        val closed = transactions.count {
            it.included && it.type == ImportedTransactionType.CLOSED_PAYMENT
        }
        val income = transactions.count {
            it.included && it.type == ImportedTransactionType.INCOME
        }
        val excluded = transactions.count { !it.included }
        summary.text = getString(R.string.import_summary, closed, income, excluded)
        val selectedCurrency = CurrencyPreferences.selectedCode(this)
        val detected = detectedCurrency
        val mismatch = detected != null && detected != selectedCurrency
        currencyStatus.visibility = View.VISIBLE
        currencyStatus.text = if (mismatch) {
            getString(R.string.import_currency_mismatch, detected, selectedCurrency)
        } else if (detected == null) {
            getString(R.string.import_currency_not_detected, selectedCurrency)
        } else {
            getString(R.string.import_currency_detected, detected)
        }
        currencyStatus.setTextColor(
            getColor(if (mismatch) R.color.red else R.color.colorPrimary)
        )
        val selected = closed + income
        importButton.text = resources.getQuantityString(
            R.plurals.import_selected_transactions,
            selected,
            selected
        )
        val invalidEntry = transactions.any {
            it.included && (it.title.isBlank() || it.amount <= 0.0)
        }
        val isPremium = PremiumAccess.isPremiumUser(this)
        val currentFreeCount = existingFreeImportCount
        allowanceStatus.text = if (isPremium) {
            getString(R.string.import_premium_allowance, 200)
        } else if (currentFreeCount != null) {
            getString(
                R.string.import_free_allowance,
                FREE_IMPORTS_PER_FILE,
                FREE_TOTAL_IMPORTED_TRANSACTIONS,
                (FREE_TOTAL_IMPORTED_TRANSACTIONS - currentFreeCount).coerceAtLeast(0L)
            )
        } else {
            getString(
                R.string.import_free_allowance_pending,
                FREE_IMPORTS_PER_FILE,
                FREE_TOTAL_IMPORTED_TRANSACTIONS
            )
        }
        val reason = when {
            isLoading -> null
            selected == 0 -> getString(R.string.import_blocked_no_selection)
            invalidEntry -> getString(R.string.import_blocked_invalid_entry)
            !isPremium && selected > FREE_IMPORTS_PER_FILE ->
                getString(R.string.import_blocked_free_per_file, FREE_IMPORTS_PER_FILE)
            !isPremium && currentFreeCount == null ->
                getString(R.string.import_blocked_limit_check)
            !isPremium &&
                currentFreeCount != null &&
                currentFreeCount + selected > FREE_TOTAL_IMPORTED_TRANSACTIONS ->
                getString(
                    R.string.import_blocked_free_total,
                    FREE_TOTAL_IMPORTED_TRANSACTIONS,
                    (FREE_TOTAL_IMPORTED_TRANSACTIONS - currentFreeCount).coerceAtLeast(0L)
                )
            else -> null
        }
        blockingReason.visibility = if (reason.isNullOrBlank()) View.GONE else View.VISIBLE
        blockingReason.text = reason
        importButton.isEnabled = !isLoading && reason == null
    }

    private fun saveTransactions() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val selected = transactions.filter { it.included }
        if (selected.isEmpty()) return
        importButton.isEnabled = false
        val selectedClosed = selected.count { it.type == ImportedTransactionType.CLOSED_PAYMENT }.toLong()
        if (!PremiumAccess.isPremiumUser(this) && selectedClosed > 0L) {
            db.collection("users")
                .document(uid)
                .collection("spendings")
                .count()
                .get(AggregateSource.SERVER)
                .addOnSuccessListener { current ->
                    if (current.count + selectedClosed > UsageLimits.FREE_CLOSED_PAYMENTS) {
                        importButton.isEnabled = true
                        Toast.makeText(this, R.string.free_closed_payment_limit_reached, Toast.LENGTH_LONG).show()
                    } else {
                        saveSelectedTransactions(uid, selected)
                    }
                }
                .addOnFailureListener {
                    importButton.isEnabled = true
                    Toast.makeText(this, R.string.usage_limit_check_failed, Toast.LENGTH_LONG).show()
                }
            return
        }
        saveSelectedTransactions(uid, selected)
    }

    private fun saveSelectedTransactions(
        uid: String,
        selected: List<ImportedTransaction>
    ) {
        setLoading(true, getString(R.string.importing_transactions))
        val user = db.collection("users").document(uid)
        val usageRef = statementImportUsageRef(uid)
        db.runTransaction { firestoreTransaction ->
            val existingCount = firestoreTransaction.get(usageRef).getLong("count") ?: 0L
            if (!PremiumAccess.isPremiumUser(this)) {
                require(selected.size <= FREE_IMPORTS_PER_FILE) {
                    getString(R.string.import_blocked_free_per_file, FREE_IMPORTS_PER_FILE)
                }
                require(existingCount + selected.size <= FREE_TOTAL_IMPORTED_TRANSACTIONS) {
                    getString(
                        R.string.import_blocked_free_total,
                        FREE_TOTAL_IMPORTED_TRANSACTIONS,
                        (FREE_TOTAL_IMPORTED_TRANSACTIONS - existingCount).coerceAtLeast(0L)
                    )
                }
            }
            selected.forEach { imported ->
                val ref = if (imported.type == ImportedTransactionType.INCOME) {
                    user.collection("income").document()
                } else {
                    user.collection("spendings").document()
                }
                firestoreTransaction.set(ref, transactionData(imported, ref.id))
            }
            firestoreTransaction.set(
                usageRef,
                mapOf(
                    "count" to existingCount + selected.size,
                    "updatedAt" to Timestamp.now()
                ),
                SetOptions.merge()
            )
            existingCount + selected.size
        }
            .addOnSuccessListener {
                existingFreeImportCount = it
                setLoading(false, "")
                AlertDialog.Builder(this)
                    .setTitle(R.string.import_complete)
                    .setMessage(resources.getQuantityString(
                        R.plurals.transactions_imported,
                        selected.size,
                        selected.size
                    ))
                    .setPositiveButton(android.R.string.ok) { _, _ -> finish() }
                    .setCancelable(false)
                    .show()
            }
            .addOnFailureListener {
                setLoading(false, "")
                Toast.makeText(
                    this,
                    it.message?.takeIf(String::isNotBlank) ?: getString(R.string.import_failed),
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun transactionData(
        imported: ImportedTransaction,
        documentId: String
    ): HashMap<String, Any?> {
        val category = if (imported.type == ImportedTransactionType.INCOME) {
            FinancialEntryOptions.normalizedIncomeCategory(this, imported.category)
        } else {
            FinancialEntryOptions.normalizedExpenseCategory(this, imported.category)
        }
        val subcategory = if (imported.type == ImportedTransactionType.INCOME) {
            FinancialEntryOptions.normalizedIncomeSubcategory(this, category, imported.subcategory)
        } else {
            FinancialEntryOptions.normalizedExpenseSubcategory(this, category, imported.subcategory)
        }
        val fingerprint = imported.fingerprint()
        val importHash = MessageDigest.getInstance("SHA-256")
            .digest(fingerprint.toByteArray())
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        val data = hashMapOf<String, Any?>(
            "name" to imported.title,
            "amount" to CurrencyPreferences.roundToTwoDecimals(imported.amount),
            "currency" to CurrencyPreferences.selectedCode(this),
            "date" to Timestamp(imported.date),
            "category" to category,
            "subcategory" to subcategory,
            "repeat" to "No",
            "isRecurring" to false,
            "comment" to getString(R.string.imported_from_statement),
            "attachment" to null,
            "importFingerprint" to fingerprint,
            "importHash" to importHash,
            "importReference" to imported.reference,
            "importSourceFormat" to sourceFileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        )
        if (imported.type == ImportedTransactionType.INCOME) {
            data["incomeId"] = documentId
            data["parentIncomeId"] = documentId
            data["source"] = imported.title
        } else {
            data["spendingId"] = documentId
            data["vendor"] = imported.title
            data["paid"] = true
        }
        return data
    }

    private fun statementImportUsageRef(uid: String) =
        db.collection("users").document(uid)
            .collection("usage").document("statement_imports")

    private fun setLoading(loading: Boolean, message: String) {
        isLoading = loading
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        status.visibility = if (message.isBlank()) View.GONE else View.VISIBLE
        status.text = message
        updateSummary()
    }

    private fun showImportError(detail: String?) {
        setLoading(false, getString(R.string.statement_read_failed))
        Toast.makeText(
            this,
            detail?.takeIf(String::isNotBlank) ?: getString(R.string.statement_read_failed),
            Toast.LENGTH_LONG
        ).show()
    }

    private fun copyPdfToCache(uri: Uri): File? = runCatching {
        val file = File.createTempFile("statement_", ".pdf", cacheDir)
        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output ->
                val buffer = ByteArray(8192)
                var total = 0
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    require(total <= MAX_PDF_BYTES) { "The PDF file is too large." }
                    output.write(buffer, 0, read)
                }
            }
        } ?: return null
        file
    }.getOrNull()

    private fun displayName(uri: Uri): String {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return cursor.getString(0).orEmpty()
                }
            }
        return uri.lastPathSegment.orEmpty()
    }
}
