package com.ritter.smartstackbills

import android.app.Dialog
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.io.File
import java.util.Calendar
import java.util.Locale
import kotlin.collections.ArrayList

class MyIncome : AppCompatActivity(), MyAdapterIncome.OnIncomeClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var incomeArrayList: ArrayList<Income>
    private lateinit var allIncomeArrayList: ArrayList<Income>
    private lateinit var myAdapterIncome: MyAdapterIncome
    private lateinit var db: FirebaseFirestore
    private var listenerRegistration: ListenerRegistration? = null
    private lateinit var fab: FloatingActionButton
    private var userEmail: String? = null
    private lateinit var dialog: Dialog
    private lateinit var drawerLayout: DrawerLayout
    private var selectedIncome: Income? = null
    private lateinit var btnCloseDialog: Button
    private var activeFilter: String = "all income"
    private var dataLoaded = false
    private val hasPremiumAccess: Boolean
        get() = PremiumAccess.isPremiumUser(this)
    private val emptyStateConfig = EmptyStateConfig(
        preferenceKey = "income_tutorial_shown",
        imageRes = R.drawable.image_income,
        titleRes = R.string.empty_income_title,
        messageRes = R.string.empty_income_message,
        addActionRes = R.string.add_income,
        requiresPremium = true
    )



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_my_income)

        drawerLayout = findViewById(R.id.drawer_layout_income)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainIncome)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        recyclerView = findViewById(R.id.recyclerViewIncome)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)
        userEmail = intent.getStringExtra(AuthUtils.EXTRA_USER_EMAIL) ?: AuthUtils.currentUserEmail()

        incomeArrayList = ArrayList()
        allIncomeArrayList = ArrayList()
        myAdapterIncome = MyAdapterIncome(this, incomeArrayList, this)
        recyclerView.adapter = myAdapterIncome

        findViewById<TextView>(R.id.tvIncomePremiumPreview).visibility =
            if (hasPremiumAccess) View.GONE else View.VISIBLE

        fab = findViewById(R.id.fabIncome)
        fab.setOnClickListener { handleAddIncome() }

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottomNavigationViewIncome)
        bottomNavigationView.selectedItemId = R.id.Income
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.Main -> {
                    val intent = Intent(this, MainMenu::class.java)
                    intent.putExtra(AuthUtils.EXTRA_USER_EMAIL, userEmail) // Pasar el correo electrÃ³nico
                    startActivity(intent)
                    true
                }

                R.id.Bills -> {
                    val intent = Intent(this, MyBills::class.java)
                    intent.putExtra(AuthUtils.EXTRA_USER_EMAIL, userEmail)
                    startActivity(intent)
                    true
                }

                R.id.Spendings -> {
                    val intent = Intent(this, MySpendings::class.java)
                    intent.putExtra(AuthUtils.EXTRA_USER_EMAIL, userEmail)
                    startActivity(intent)
                    true
                }

                R.id.Income -> {
                    // Do nothing since we're already on this screen
                    true
                }
                R.id.Calendar -> {
                    // Intent for Calendar (assumed to be implemented)
                    val intent = Intent(this, CalendarActivity::class.java)
                    intent.putExtra(AuthUtils.EXTRA_USER_EMAIL, userEmail) // Pasar el correo electrÃ³nico
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

        // Initialize the toolbar and set it as the action bar
        val toolbar: Toolbar = findViewById(R.id.materialToolbarIncome)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        DrawerNavigation.setup(this, drawerLayout, findViewById(R.id.nav_viewIncome))

        db = FirebaseFirestore.getInstance()
        setupDialog()
        setupEventChangeListener()

        findViewById<Button>(R.id.btnRecurringIncome).setOnClickListener { filterIncome("recurring") }
        findViewById<Button>(R.id.btnOneTimeIncome).setOnClickListener { filterIncome("one-time") }
        findViewById<Button>(R.id.btnAllIncome).setOnClickListener { filterIncome("all income") }
    }
    private fun setupDialog() {
        dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_box_income)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawable(getDrawable(R.drawable.dialog_box_income_bg))
        dialog.setCancelable(false)

        val imgDeleteIncome = dialog.findViewById<ImageView>(R.id.imgDeleteIncome)

        btnCloseDialog = dialog.findViewById(R.id.btnCloseDialogIncome)

        btnCloseDialog.setOnClickListener {
            if (btnCloseDialog.text == getString(R.string.cancel)) {
                // Cancel editing
                btnCloseDialog.text = getString(R.string.close)
                dialog.findViewById<Button>(R.id.btnSaveChangesIncome).visibility = View.GONE

                dialog.findViewById<EditText>(R.id.edtTitleDialogIncome).isEnabled = false
                dialog.findViewById<EditText>(R.id.edtAmountDialogIncome).isEnabled = false
                dialog.findViewById<EditText>(R.id.edtDateDialogIncome).isEnabled = false
                dialog.findViewById<EditText>(R.id.edtRepeatDialogIncome).isEnabled = false
                dialog.findViewById<EditText>(R.id.edtCategoryDialogIncome).isEnabled = false
                dialog.findViewById<EditText>(R.id.edtSubcategoryDialogIncome).isEnabled = false
                dialog.findViewById<EditText>(R.id.edtCommentDialogIncome).isEnabled = false
                dialog.findViewById<EditText>(R.id.edtSourceDialogIncome).isEnabled = false
                configureIncomePickers(
                    dialog.findViewById(R.id.edtDateDialogIncome),
                    dialog.findViewById(R.id.edtRepeatDialogIncome),
                    dialog.findViewById(R.id.edtCategoryDialogIncome),
                    dialog.findViewById(R.id.edtSubcategoryDialogIncome),
                    false
                )
            } else {
                dialog.dismiss()
            }
        }

        imgDeleteIncome.setOnClickListener {
            if (hasPremiumAccess || !selectedIncome?.importHash.isNullOrBlank()) {
                deleteIncome()
            } else {
                showUpgradeDialog()
            }
        }
    }

    private fun setupEventChangeListener() {
        val userUid = FirebaseAuth.getInstance().currentUser?.uid
        if (userUid != null) {
            listenerRegistration = db.collection("users").document(userUid).collection("income")
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        Toast.makeText(this, getString(R.string.load_income_failed, e.message.orEmpty()), Toast.LENGTH_SHORT).show()
                        Log.e("Firestore Error", e.message.toString())
                        return@addSnapshotListener
                    }

                    if (snapshots != null) {
                        dataLoaded = true
                        incomeArrayList.clear()
                        allIncomeArrayList.clear()

                        for (document in snapshots.documents) {
                            try {
                                val income = document.toObject(Income::class.java)
                                if (income != null) {
                                    repairLegacyIncome(userUid, document.id, income)
                                    // Validate and handle the `amount` field
                                    if (document.contains("amount")) {
                                        val rawAmount = document["amount"]
                                        income.amount = when (rawAmount) {
                                            is Number -> rawAmount.toDouble()
                                            is String -> rawAmount.toDoubleOrNull() ?: 0.0
                                            else -> 0.0 // Default to 0 for invalid values
                                        }
                                    }
                                    incomeArrayList.add(income)
                                    allIncomeArrayList.add(income)
                                    Log.d("Firestore Data", "Income added: ${income.name}, ${income.date}, ${income.repeat}")
                                }
                            } catch (ex: Exception) {
                                Log.e("DataError", "Error processing document ${document.id}: ${ex.message}")
                            }
                        }
                        filterIncome(activeFilter)
                        if (allIncomeArrayList.isEmpty()) {
                            EmptyStateTutorial.showFirstTimeIfNeeded(
                                this,
                                emptyStateConfig,
                                hasPremiumAccess,
                                onAdd = ::openCreateIncome,
                                onPremium = ::showUpgradeDialog
                            )
                        }
                    } else {
                        Log.d("Firestore Data", "No income found")
                    }
                }
        } else {
            Toast.makeText(this, R.string.user_not_authenticated, Toast.LENGTH_SHORT).show()
            Log.e("Authentication Error", "User not authenticated")
        }
    }

    private fun repairLegacyIncome(userUid: String, documentId: String, income: Income) {
        val updates = mutableMapOf<String, Any>()
        if (income.incomeId.isNullOrBlank()) {
            income.incomeId = documentId
            updates["incomeId"] = documentId
        }
        if (income.parentIncomeId.isNullOrBlank()) {
            income.parentIncomeId = documentId
            updates["parentIncomeId"] = documentId
        }
        if (income.category.isNullOrBlank()) {
            income.category = FinancialEntryOptions.DEFAULT_INCOME_CATEGORY
            updates["category"] = income.category
        }
        if (income.subcategory.isNullOrBlank() || income.subcategory == "-") {
            income.subcategory = FinancialEntryOptions.normalizedIncomeSubcategory(
                this,
                income.category,
                income.subcategory
            )
            updates["subcategory"] = income.subcategory
        }
        if (income.currency.isNullOrBlank()) {
            income.currency = CurrencyPreferences.selectedCode(this)
            updates["currency"] = income.currency
        }
        if (updates.isNotEmpty()) {
            db.collection("users").document(userUid)
                .collection("income")
                .document(documentId)
                .update(updates)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        listenerRegistration?.remove()
    }

    override fun onIncomeClick(position: Int) {
        val item = myAdapterIncome.getItemAtPosition(position)
        // Check if the clicked item is an income item
        if (item is Income) {
            selectedIncome = item
            showIncomeDetailsDialog(item)
        }
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_nav, menu)
        // Find and update the unread notification count TextView on the alarm icon badge
        val menuItem = menu?.findItem(R.id.alarm)
        val actionView = menuItem?.actionView
        // Find the badge TextView
        val badgeCountTextView = actionView?.findViewById<TextView>(R.id.badge_count)

        // Update badge count with the current unread notification count
        updateUnreadCountBadge(badgeCountTextView)

        // Set click listener on the alarm icon
        actionView?.setOnClickListener {
            resetUnreadNotificationCount(badgeCountTextView)
            val intent = Intent(this, NotificationsActivity::class.java)
            startActivity(intent)  // Open the notifications activity
        }

        return true
    }

    private fun showIncomeDetailsDialog(income: Income) {
        val edtTitleDialog = dialog.findViewById<EditText>(R.id.edtTitleDialogIncome)
        val edtAmountDialog = dialog.findViewById<EditText>(R.id.edtAmountDialogIncome)
        val edtCategoryDialog = dialog.findViewById<EditText>(R.id.edtCategoryDialogIncome)
        val edtSubcategoryDialog = dialog.findViewById<EditText>(R.id.edtSubcategoryDialogIncome)
        val edtDateDialog = dialog.findViewById<EditText>(R.id.edtDateDialogIncome)
        val edtRepeatDialog = dialog.findViewById<EditText>(R.id.edtRepeatDialogIncome)
        val edtCommentDialog = dialog.findViewById<EditText>(R.id.edtCommentDialogIncome)
        val edtSourceDialog = dialog.findViewById<EditText>(R.id.edtSourceDialogIncome)
        val btnSaveChanges = dialog.findViewById<Button>(R.id.btnSaveChangesIncome)
        val btnEditChanges = dialog.findViewById<ImageView>(R.id.imgEditIncome)
        val btnDelete = dialog.findViewById<ImageView>(R.id.imgDeleteIncome)
        val attachmentView = dialog.findViewById<ImageView>(R.id.edtAttachmentDialogIncome)

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val incomeDateString = if (income.date != null) dateFormat.format(income.date.toDate()) else ""

        edtTitleDialog.setText(income.name)
        edtAmountDialog.setText(CurrencyPreferences.formatPlain(income.amount))
        edtCategoryDialog.setText(FinancialEntryOptions.displayCategory(this, income.category))
        edtSubcategoryDialog.setText(
            FinancialEntryOptions.displaySubcategory(this, income.category, income.subcategory)
        )
        edtDateDialog.setText(incomeDateString)
        edtRepeatDialog.setText(income.repeat)
        edtCommentDialog.setText(income.comment)
        edtSourceDialog.setText(income.source.orEmpty())
        if (income.attachment.isNullOrBlank()) {
            attachmentView.visibility = View.GONE
        } else {
            attachmentView.setImageURI(Uri.parse(income.attachment))
            attachmentView.visibility = View.VISIBLE
        }

        // Disable inputs initially
        edtTitleDialog.isEnabled = false
        edtAmountDialog.isEnabled = false
        edtDateDialog.isEnabled = false
        edtRepeatDialog.isEnabled = false
        edtCategoryDialog.isEnabled = false
        edtSubcategoryDialog.isEnabled = false
        edtCommentDialog.isEnabled = false
        edtSourceDialog.isEnabled = false
        configureIncomePickers(
            edtDateDialog,
            edtRepeatDialog,
            edtCategoryDialog,
            edtSubcategoryDialog,
            false
        )

        btnSaveChanges.visibility = View.GONE
        btnEditChanges.visibility = View.VISIBLE
        btnDelete.visibility =
            if (hasPremiumAccess || !income.importHash.isNullOrBlank()) View.VISIBLE else View.GONE

        dialog.show()
        styleDetailsDialogWindow(dialog)

        btnEditChanges.setOnClickListener {
            if (!hasPremiumAccess) {
                showUpgradeDialog()
                return@setOnClickListener
            }
            edtTitleDialog.isEnabled = true
            edtAmountDialog.isEnabled = true
            edtDateDialog.isEnabled = true
            edtRepeatDialog.isEnabled = true
            edtCategoryDialog.isEnabled = true
            edtSubcategoryDialog.isEnabled = true
            edtCommentDialog.isEnabled = true
            edtSourceDialog.isEnabled = true
            configureIncomePickers(
                edtDateDialog,
                edtRepeatDialog,
                edtCategoryDialog,
                edtSubcategoryDialog,
                true
            )

            btnSaveChanges.visibility = View.VISIBLE
            btnCloseDialog.text = getString(R.string.cancel)
        }

        btnSaveChanges.setOnClickListener {
            if (!hasPremiumAccess) {
                showUpgradeDialog()
                return@setOnClickListener
            }
            val userUid = FirebaseAuth.getInstance().currentUser?.uid
            val income = selectedIncome
            if (userUid != null && income != null) {
                val originalSelectedDate = income.date
                val parsedDate = try {
                    dateFormat.isLenient = false
                    dateFormat.parse(edtDateDialog.text.toString().trim())
                } catch (e: Exception) {
                    null
                }
                if (parsedDate == null) {
                    Toast.makeText(this, R.string.invalid_date_format, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val category = FinancialEntryOptions.normalizedIncomeCategory(
                    this,
                    edtCategoryDialog.text.toString()
                )
                val subcategory = FinancialEntryOptions.normalizedIncomeSubcategory(
                    this,
                    category,
                    edtSubcategoryDialog.text.toString()
                )
                income.name = edtTitleDialog.text.toString().trim().ifBlank { getString(R.string.income) }
                income.amount = CurrencyPreferences.roundToTwoDecimals(edtAmountDialog.text.toString().toDoubleOrNull() ?: 0.0)
                if (income.amount <= 0.0) {
                    Toast.makeText(this, R.string.enter_valid_amount, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                income.date = com.google.firebase.Timestamp(parsedDate)
                income.repeat = edtRepeatDialog.text.toString().trim().ifBlank { "No" }
                income.category = category
                income.subcategory = subcategory
                income.comment = edtCommentDialog.text.toString().trim()
                income.source = edtSourceDialog.text.toString().trim()

                if (!income.repeat.isNullOrBlank() && income.repeat != "No") {
                    AlertDialog.Builder(this)
                        .setTitle(R.string.edit_recurring_income)
                        .setItems(
                            arrayOf(
                                getString(R.string.edit_this_income_only),
                                getString(R.string.edit_this_and_future_income)
                            )
                        ) { _, choice ->
                            if (choice == 0) {
                                saveSingleIncomeEdit(userUid, income)
                            } else {
                                saveCurrentAndFutureIncomeEdits(userUid, income, originalSelectedDate)
                            }
                        }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
                } else {
                    saveSingleIncomeEdit(userUid, income)
                }
            } else {
                Toast.makeText(this, R.string.income_update_unavailable, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveSingleIncomeEdit(userUid: String, income: Income) {
        db.collection("users").document(userUid).collection("income")
            .document(income.incomeId)
            .set(income)
            .addOnSuccessListener {
                val index = incomeArrayList.indexOfFirst { it.incomeId == income.incomeId }
                if (index != -1) {
                    incomeArrayList[index] = income
                    myAdapterIncome.notifyItemChanged(index)
                }
                Toast.makeText(this, R.string.income_updated, Toast.LENGTH_SHORT).show()
                btnCloseDialog.text = getString(R.string.close)
                dialog.dismiss()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, getString(R.string.income_update_failed, e.message.orEmpty()), Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveCurrentAndFutureIncomeEdits(
        userUid: String,
        editedIncome: Income,
        originalSelectedDate: com.google.firebase.Timestamp?
    ) {
        val selectedDate = originalSelectedDate ?: editedIncome.date ?: return saveSingleIncomeEdit(userUid, editedIncome)
        val seriesId = editedIncome.parentIncomeId?.takeIf { it.isNotBlank() } ?: editedIncome.incomeId
        val incomeReference = db.collection("users").document(userUid).collection("income")

        incomeReference.get()
            .addOnSuccessListener { documents ->
                val batch = db.batch()
                documents.forEach { document ->
                    val occurrenceDate = document.getTimestamp("date")
                    val parentId = document.getString("parentIncomeId")
                    val belongsToSeries =
                        document.id == seriesId || parentId == seriesId
                    if (belongsToSeries && occurrenceDate != null && occurrenceDate >= selectedDate) {
                        if (document.id == editedIncome.incomeId) {
                            batch.set(document.reference, editedIncome)
                        } else {
                            val futureIncome = document.toObject(Income::class.java)
                            futureIncome.name = editedIncome.name
                            futureIncome.amount = editedIncome.amount
                            futureIncome.currency = editedIncome.currency
                            futureIncome.repeat = editedIncome.repeat
                            futureIncome.category = editedIncome.category
                            futureIncome.subcategory = editedIncome.subcategory
                            futureIncome.comment = editedIncome.comment
                            futureIncome.source = editedIncome.source
                            futureIncome.attachment = editedIncome.attachment
                            futureIncome.parentIncomeId = editedIncome.parentIncomeId
                            batch.set(document.reference, futureIncome)
                        }
                    }
                }

                batch.commit()
                    .addOnSuccessListener {
                        Toast.makeText(this, R.string.future_income_updated, Toast.LENGTH_SHORT).show()
                        btnCloseDialog.text = getString(R.string.close)
                        dialog.dismiss()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, getString(R.string.income_update_failed, e.message.orEmpty()), Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, getString(R.string.income_update_failed, e.message.orEmpty()), Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteIncome() {
        val income = selectedIncome ?: return
        if (!income.repeat.isNullOrBlank() && income.repeat != "No") {
            AlertDialog.Builder(this)
                .setTitle(R.string.delete_recurring_income)
                .setItems(
                    arrayOf(
                        getString(R.string.delete_this_income_entry),
                        getString(R.string.delete_all_recurring_income)
                    )
                ) { _, choice ->
                    if (choice == 0) deleteSingleIncome(income)
                    else deleteAllRecurringIncome(income)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        } else {
            AlertDialog.Builder(this)
                .setTitle(R.string.delete_income_entry)
                .setMessage(R.string.delete_income_confirmation)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete) { _, _ -> deleteSingleIncome(income) }
                .show()
        }
    }

    private fun deleteSingleIncome(income: Income) {
        val userUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userRef = db.collection("users").document(userUid)
        val incomeRef = userRef.collection("income").document(income.incomeId)
        val usageRef = userRef.collection("usage").document("statement_imports")
        db.runTransaction { transaction ->
            val incomeDocument = transaction.get(incomeRef)
            val usageDocument = transaction.get(usageRef)
            transaction.delete(incomeRef)
            if (!incomeDocument.getString("importHash").isNullOrBlank()) {
                val current = usageDocument.getLong("count") ?: 0L
                transaction.set(
                    usageRef,
                    mapOf(
                        "count" to (current - 1L).coerceAtLeast(0L),
                        "updatedAt" to com.google.firebase.Timestamp.now()
                    )
                )
            }
        }
            .addOnSuccessListener {
                deleteLocalAttachmentIfUnused(userUid, income.attachment)
                Toast.makeText(this, R.string.income_deleted, Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .addOnFailureListener {
                Toast.makeText(this, R.string.income_delete_failed, Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteAllRecurringIncome(selected: Income) {
        val userUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val seriesId = selected.parentIncomeId?.takeIf { it.isNotBlank() } ?: selected.incomeId
        val incomeReference = db.collection("users").document(userUid).collection("income")

        incomeReference.get()
            .addOnSuccessListener { documents ->
                val documentsToDelete = documents.filter { document ->
                    val parentId = document.getString("parentIncomeId")
                    document.id == seriesId || parentId == seriesId || document.id == selected.incomeId
                }

                if (documentsToDelete.isEmpty()) {
                    deleteSingleIncome(selected)
                    return@addOnSuccessListener
                }

                val batch = db.batch()
                documentsToDelete.forEach { batch.delete(it.reference) }
                batch.commit()
                    .addOnSuccessListener {
                        deleteLocalAttachmentIfUnused(userUid, selected.attachment)
                        Toast.makeText(this, R.string.all_recurring_income_deleted, Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, R.string.income_delete_failed, Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, R.string.income_delete_failed, Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteLocalAttachmentIfUnused(userUid: String, attachment: String?) {
        if (attachment.isNullOrBlank()) return
        db.collection("users").document(userUid).collection("income")
            .whereEqualTo("attachment", attachment)
            .limit(1)
            .get()
            .addOnSuccessListener { remaining ->
                if (remaining.isEmpty) {
                    runCatching {
                        val uri = Uri.parse(attachment)
                        if (uri.scheme == "file") File(uri.path.orEmpty()).delete()
                    }
                }
            }
    }

    private fun filterIncome(filter: String) {
        activeFilter = filter
        val filteredIncome = ArrayList<Income>()

        // Reset the button background to inactive color

        findViewById<Button>(R.id.btnRecurringIncome).setBackgroundColor(ContextCompat.getColor(this, R.color.filter_inactive))
        findViewById<Button>(R.id.btnOneTimeIncome).setBackgroundColor(ContextCompat.getColor(this, R.color.filter_inactive))
        findViewById<Button>(R.id.btnAllIncome).setBackgroundColor(ContextCompat.getColor(this, R.color.filter_inactive))


        for (income in allIncomeArrayList) {
            when (filter) {
                "recurring" -> {
                    findViewById<Button>(R.id.btnRecurringIncome).setBackgroundColor(ContextCompat.getColor(this, R.color.filter_income_active))

                        if (income.repeat != "No") {
                            filteredIncome.add(income)
                            Log.d("Filter", "Recurring income added: ${income.name}")
                        }

                }
                "one-time" -> {
                    findViewById<Button>(R.id.btnOneTimeIncome).setBackgroundColor(ContextCompat.getColor(this, R.color.filter_income_active))

                        if (income.repeat == "No") {
                            filteredIncome.add(income)
                            Log.d("Filter", "One-time income added: ${income.name}")
                        }

                }
                "all income" -> {
                    findViewById<Button>(R.id.btnAllIncome).setBackgroundColor(ContextCompat.getColor(this, R.color.filter_income_active))
                    // Add all spendings regardless of subcategory
                    filteredIncome.add(income)
                    Log.d("Filter", "All income added: ${income.name}")
                }
            }
        }

        myAdapterIncome.updateIncome(filteredIncome)
        if (dataLoaded) {
            EmptyStateTutorial.bind(
                this,
                findViewById(R.id.emptyStateIncome),
                emptyStateConfig,
                hasAnyEntries = allIncomeArrayList.isNotEmpty(),
                hasFilteredEntries = filteredIncome.isNotEmpty(),
                hasPremiumAccess = hasPremiumAccess,
                onAdd = ::openCreateIncome,
                onPremium = ::showUpgradeDialog
            )
        }
        Log.d("Filter", "Filtered income count for $filter: ${filteredIncome.size}")
    }
    private fun groupIncomeByMonth(incomeArrayList: ArrayList<Income>): ArrayList<Any> {
        val groupedIncome = LinkedHashMap<String, MutableList<Income>>()
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

        for (income in incomeArrayList) {
            val monthYear = sdf.format(income.date.toDate())
            if (!groupedIncome.containsKey(monthYear)) {
                groupedIncome[monthYear] = ArrayList()
            }
            groupedIncome[monthYear]?.add(income)
        }

        val items = ArrayList<Any>()
        for ((monthYear, income) in groupedIncome) {
            items.add(monthYear)
            items.addAll(income)
        }

        return items
    }
    // Update the unread count badge from SharedPreferences
    private fun updateUnreadCountBadge(badgeCountTextView: TextView?) {
        val unreadCount = NotificationsActivity.getUnreadNotificationCount(this)
        if (unreadCount > 0) {
            badgeCountTextView?.text = if (unreadCount > 99) "99+" else unreadCount.toString()
            badgeCountTextView?.visibility = View.VISIBLE // Show the badge
        } else {
            badgeCountTextView?.visibility = View.GONE // Hide the badge if no unread notifications
        }
    }

    // Reset unread notification count when notifications are viewed
    private fun resetUnreadNotificationCount(badgeCountTextView: TextView?) {
        NotificationsActivity.resetUnreadNotificationCount(this)
        updateUnreadCountBadge(badgeCountTextView) // Update the badge display immediately
    }
    private fun showUpgradeDialog() {
        PremiumUpgradeDialog.show(this, R.string.premium_preview_income, userEmail)
    }

    private fun handleAddIncome() {
        openCreateIncome()
    }

    private fun openCreateIncome() {
        EntryCreationFlow.show(this, EntryType.INCOME, userEmail)
    }

    private fun configureIncomePickers(
        dateField: EditText,
        repeatField: EditText,
        categoryField: EditText,
        subcategoryField: EditText,
        enabled: Boolean
    ) {
        listOf(dateField, repeatField, categoryField, subcategoryField).forEach {
            it.isEnabled = enabled
            it.isFocusable = false
            it.isFocusableInTouchMode = false
            it.isCursorVisible = false
        }
        dateField.setOnClickListener(if (enabled) View.OnClickListener { showDatePicker(dateField) } else null)
        repeatField.setOnClickListener(if (enabled) View.OnClickListener { showRepeatPicker(repeatField) } else null)
        categoryField.setOnClickListener(
            if (enabled) {
                View.OnClickListener { showIncomeCategoryPicker(categoryField, subcategoryField) }
            } else null
        )
        subcategoryField.setOnClickListener(
            if (enabled) {
                View.OnClickListener { showIncomeSubcategoryPicker(categoryField, subcategoryField) }
            } else null
        )
    }

    private fun showDatePicker(field: EditText) {
        val calendar = Calendar.getInstance()
        runCatching {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply { isLenient = false }
                .parse(field.text.toString().trim())
        }.getOrNull()?.let(calendar::setTime)
        DatePickerDialog(
            this,
            { _, year, month, day ->
                field.setText(String.format(Locale.getDefault(), "%02d/%02d/%04d", day, month + 1, year))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showRepeatPicker(field: EditText) {
        val options = arrayOf(
            "No", "Weekly", "Every 2 Weeks", "Monthly", "Every 2 Months",
            "Quarterly", "Every 6 months", "Yearly"
        )
        val selected = options.indexOf(field.text.toString()).coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle(R.string.repeat)
            .setSingleChoiceItems(options, selected) { dialog, which ->
                field.setText(options[which])
                dialog.dismiss()
            }
            .show()
    }

    private fun showIncomeCategoryPicker(categoryField: EditText, subcategoryField: EditText) {
        val options = FinancialEntryOptions.incomeCategories(this)
        val labels = options.map { it.label }.toTypedArray()
        val current = options.indexOfFirst {
            it.key == FinancialEntryOptions.normalizedIncomeCategory(this, categoryField.text.toString())
        }.coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle(R.string.category)
            .setSingleChoiceItems(labels, current) { dialog, which ->
                val selected = options[which]
                categoryField.setText(selected.label)
                subcategoryField.setText(defaultIncomeSubcategoryLabel(selected.key))
                dialog.dismiss()
            }
            .show()
    }

    private fun showIncomeSubcategoryPicker(categoryField: EditText, subcategoryField: EditText) {
        val category = FinancialEntryOptions.normalizedIncomeCategory(this, categoryField.text.toString())
        val options = FinancialEntryOptions.incomeSubcategories(this, category)
        val labels = options.map { it.label }.toTypedArray()
        val current = options.indexOfFirst {
            it.key == FinancialEntryOptions.normalizedIncomeSubcategory(this, category, subcategoryField.text.toString())
        }.coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle(R.string.subcategory)
            .setSingleChoiceItems(labels, current) { dialog, which ->
                subcategoryField.setText(options[which].label)
                dialog.dismiss()
            }
            .show()
    }

    private fun defaultIncomeSubcategoryLabel(categoryKey: String): String {
        val options = FinancialEntryOptions.incomeSubcategories(this, categoryKey)
        val defaultKey = if (categoryKey == FinancialEntryOptions.DEFAULT_INCOME_CATEGORY) {
            FinancialEntryOptions.DEFAULT_INCOME_SUBCATEGORY
        } else {
            options.firstOrNull()?.key ?: FinancialEntryOptions.DEFAULT_INCOME_SUBCATEGORY
        }
        return options.firstOrNull { it.key == defaultKey }?.label ?: defaultKey
    }

    private fun styleDetailsDialogWindow(dialog: Dialog) {
        val width = (resources.displayMetrics.widthPixels * 0.92f).toInt()
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun logoutUser() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, LogIn::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
