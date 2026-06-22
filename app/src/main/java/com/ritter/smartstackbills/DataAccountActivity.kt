package com.ritter.smartstackbills

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class DataAccountActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private var pendingExport: String? = null
    private val collections = listOf("bills", "spendings", "income", "notifications")

    private val createExportFile = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val content = pendingExport
        if (uri != null && content != null) {
            runCatching {
                contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(content) }
            }.onSuccess {
                Toast.makeText(this, R.string.export_complete, Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(this, R.string.export_failed, Toast.LENGTH_SHORT).show()
            }
        }
        pendingExport = null
        setBusy(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_account)
        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.dataAccountToolbar)
            .setNavigationOnClickListener { finish() }
        findViewById<Button>(R.id.btnExportData).setOnClickListener { exportData() }
        findViewById<Button>(R.id.btnDeleteAccount).setOnClickListener { confirmDeletion() }
    }

    private fun exportData() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        setBusy(true)
        Thread {
            try {
                val userRef = db.collection("users").document(user.uid)
                val root = JSONObject()
                root.put("exportedAt", System.currentTimeMillis())
                root.put("account", documentToJson(Tasks.await(userRef.get())))
                collections.forEach { name ->
                    val documents = Tasks.await(userRef.collection(name).get()).documents
                    root.put(name, documentsToJson(documents))
                }
                val savings = JSONArray()
                Tasks.await(userRef.collection("savings_targets").get()).documents.forEach { target ->
                    val targetJson = documentToJson(target)
                    val monthly = Tasks.await(target.reference.collection("monthly_savings").get())
                    targetJson.put("monthly_savings", documentsToJson(monthly.documents))
                    savings.put(targetJson)
                }
                root.put("savings_targets", savings)
                runOnUiThread {
                    pendingExport = root.toString(2)
                    createExportFile.launch("SmartStack-export.json")
                }
            } catch (_: Exception) {
                runOnUiThread {
                    setBusy(false)
                    Toast.makeText(this, R.string.export_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun confirmDeletion() {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_account_title)
            .setMessage(R.string.delete_account_confirmation)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ -> deleteAccount() }
            .show()
    }

    private fun deleteAccount() {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser ?: return
        val lastSignIn = user.metadata?.lastSignInTimestamp ?: 0L
        if (System.currentTimeMillis() - lastSignIn > 4 * 60 * 1000L) {
            Toast.makeText(this, R.string.delete_account_relogin, Toast.LENGTH_LONG).show()
            auth.signOut()
            startActivity(AuthUtils.loginIntent(this))
            finishAffinity()
            return
        }
        setBusy(true)
        Thread {
            try {
                val userRef = db.collection("users").document(user.uid)
                collections.forEach {
                    deleteCollection(Tasks.await(userRef.collection(it).get()).documents)
                }
                val targets = Tasks.await(userRef.collection("savings_targets").get()).documents
                targets.forEach { target ->
                    deleteCollection(Tasks.await(target.reference.collection("monthly_savings").get()).documents)
                }
                deleteCollection(targets)
                Tasks.await(userRef.delete())
                Tasks.await(user.delete())
                getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.let { pictures ->
                    deleteLocalPictureFiles(pictures)
                }
                getSharedPreferences("AppPrefs", MODE_PRIVATE).edit().clear().apply()
                runOnUiThread {
                    startActivity(AuthUtils.loginIntent(this))
                    finishAffinity()
                }
            } catch (exception: Exception) {
                runOnUiThread {
                    setBusy(false)
                    val recentLoginRequired =
                        exception is FirebaseAuthRecentLoginRequiredException ||
                            exception.cause is FirebaseAuthRecentLoginRequiredException
                    Toast.makeText(
                        this,
                        if (recentLoginRequired) R.string.delete_account_relogin else R.string.delete_account_failed,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }

    private fun deleteCollection(documents: List<DocumentSnapshot>) {
        documents.chunked(450).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { batch.delete(it.reference) }
            Tasks.await(batch.commit())
        }
    }

    private fun documentsToJson(documents: List<DocumentSnapshot>) = JSONArray().apply {
        documents.forEach { put(documentToJson(it)) }
    }

    private fun documentToJson(document: DocumentSnapshot) = JSONObject().apply {
        put("id", document.id)
        document.data.orEmpty().forEach { (key, value) -> put(key, jsonValue(value)) }
    }

    private fun jsonValue(value: Any?): Any = when (value) {
        null -> JSONObject.NULL
        is Timestamp -> value.toDate().time
        is Map<*, *> -> JSONObject().apply {
            value.forEach { (key, nested) -> put(key.toString(), jsonValue(nested)) }
        }
        is Iterable<*> -> JSONArray().apply { value.forEach { put(jsonValue(it)) } }
        else -> value
    }

    private fun deleteLocalPictureFiles(directory: File) {
        directory.listFiles().orEmpty().forEach { file ->
            if (file.isDirectory) deleteLocalPictureFiles(file) else file.delete()
        }
    }

    private fun setBusy(busy: Boolean) {
        findViewById<ProgressBar>(R.id.dataAccountProgress).visibility =
            if (busy) View.VISIBLE else View.GONE
        findViewById<Button>(R.id.btnExportData).isEnabled = !busy
        findViewById<Button>(R.id.btnDeleteAccount).isEnabled = !busy
    }
}
