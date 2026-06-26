package com.ritter.smartstackbills

import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DocumentScanActivity : AppCompatActivity() {
    private val entryType: EntryType
        get() = EntryType.from(intent.getStringExtra(EXTRA_ENTRY_TYPE))
    private val userEmail: String?
        get() = intent.getStringExtra(AuthUtils.EXTRA_USER_EMAIL) ?: AuthUtils.currentUserEmail()

    private val scannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        if (activityResult.resultCode != RESULT_OK) {
            finish()
            return@registerForActivityResult
        }
        val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(activityResult.data)
        val imageUri = scanResult?.pages?.firstOrNull()?.imageUri
        if (imageUri == null) {
            showScanFailure()
        } else {
            recognizeDocument(imageUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_document_scan)
        val root = findViewById<android.view.View>(R.id.documentScanRoot)
        val baseLeft = root.paddingLeft
        val baseTop = root.paddingTop
        val baseRight = root.paddingRight
        val baseBottom = root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                baseLeft + systemBars.left,
                baseTop + systemBars.top,
                baseRight + systemBars.right,
                baseBottom + systemBars.bottom
            )
            insets
        }
        startScanner()
    }

    private fun startScanner() {
        findViewById<TextView>(R.id.documentScanStatus)
            .setText(R.string.preparing_document_scanner)
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(1)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
        GmsDocumentScanning.getClient(options)
            .getStartScanIntent(this)
            .addOnSuccessListener { intentSender ->
                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener { showScanFailure() }
    }

    private fun recognizeDocument(sourceUri: Uri) {
        findViewById<TextView>(R.id.documentScanStatus)
            .setText(R.string.reading_document)
        val localUri = copyToPrivateStorage(sourceUri)
        if (localUri == null) {
            showScanFailure()
            return
        }
        val image = runCatching { InputImage.fromFilePath(this, localUri) }.getOrNull()
        if (image == null) {
            showScanFailure()
            return
        }
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            .process(image)
            .addOnSuccessListener { result ->
                val parsed = DocumentTextParser.parse(result.text, entryType).copy(
                    attachmentUri = localUri.toString()
                )
                EntryCreationFlow.launchForm(this, entryType, userEmail, parsed)
                finish()
            }
            .addOnFailureListener {
                EntryCreationFlow.launchForm(
                    this,
                    entryType,
                    userEmail,
                    ScanPrefill(null, null, null, null, localUri.toString(), null)
                )
                Toast.makeText(this, R.string.document_text_not_recognized, Toast.LENGTH_LONG).show()
                finish()
            }
    }

    private fun copyToPrivateStorage(sourceUri: Uri): Uri? = runCatching {
        val directory = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: return@runCatching null
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val output = File(directory, "SCAN_${entryType.wireValue}_$stamp.jpg")
        contentResolver.openInputStream(sourceUri)?.use { input ->
            output.outputStream().use { target -> input.copyTo(target) }
        } ?: return@runCatching null
        Uri.fromFile(output)
    }.getOrNull()

    private fun showScanFailure() {
        if (isFinishing || isDestroyed) return
        AlertDialog.Builder(this)
            .setTitle(R.string.document_scan_failed_title)
            .setMessage(R.string.document_scan_failed_message)
            .setPositiveButton(R.string.try_again) { _, _ -> startScanner() }
            .setNegativeButton(R.string.enter_manually) { _, _ ->
                EntryCreationFlow.launchForm(this, entryType, userEmail)
                finish()
            }
            .setOnCancelListener { finish() }
            .show()
    }

    companion object {
        const val EXTRA_ENTRY_TYPE = "entry_type"
    }
}
