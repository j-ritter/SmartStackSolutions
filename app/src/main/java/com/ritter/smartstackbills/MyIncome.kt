package com.ritter.smartstackbills

import android.app.Dialog
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
                dialog.findViewById<EditText>(R.id.edtCommentDialogIncome).isEnabled = false
                dialog.findViewById<EditText>(R.id.edtSourceDialogIncome).isEnabled = false
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
        if (income.subcategory == null) {
            income.subcategory = "-"
            updates["subcategory"] = "-"
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
        edtCommentDialog.isEnabled = false
        edtSourceDialog.isEnabled = false

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
            edtCommentDialog.isEnabled = true
            edtSourceDialog.isEnabled = true

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
                income.name = edtTitleDialog.text.toString()
                income.amount = CurrencyPreferences.roundToTwoDecimals(edtAmountDialog.text.toString().toDoubleOrNull() ?: 0.0)
                income.comment = edtCommentDialog.text.toString()
                income.source = edtSourceDialog.text.toString()

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
            } else {
                Toast.makeText(this, R.string.income_update_unavailable, Toast.LENGTH_SHORT).show()
            }
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
