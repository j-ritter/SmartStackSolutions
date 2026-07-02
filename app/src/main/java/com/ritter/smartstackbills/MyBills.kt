package com.ritter.smartstackbills

import android.app.Dialog
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.Menu
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MyBills : AppCompatActivity(), MyAdapter.OnBillClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var billsArrayList: ArrayList<Bills>
    private lateinit var allBillsArrayList: ArrayList<Bills> // Lista para almacenar todas las facturas
    private lateinit var myAdapter: MyAdapter
    private lateinit var db: FirebaseFirestore
    private var listenerRegistration: ListenerRegistration? = null
    private lateinit var fab: FloatingActionButton
    private var userEmail: String? = null
    private lateinit var dialog: Dialog
    private var selectedBill: Bills? = null
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var btnCloseDialog: Button
    private var activeFilter: String = "all"
    private var pendingBillIdFromNotification: String? = null
    private var dataLoaded = false
    private val emptyStateConfig = EmptyStateConfig(
        preferenceKey = "open_payments_tutorial_shown",
        imageRes = R.drawable.image_openpayments,
        titleRes = R.string.empty_open_payments_title,
        messageRes = R.string.empty_open_payments_message,
        addActionRes = R.string.add_open_payment
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_my_bills)

        drawerLayout = findViewById(R.id.drawer_layout_bills)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainBills)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        recyclerView = findViewById(R.id.recyclerViewBills)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)
        userEmail = intent.getStringExtra(AuthUtils.EXTRA_USER_EMAIL) ?: AuthUtils.currentUserEmail()

        billsArrayList = ArrayList()
        allBillsArrayList = ArrayList() // Inicializamos la lista para todas las facturas
        myAdapter = MyAdapter(this, billsArrayList, this)
        recyclerView.adapter = myAdapter

        fab = findViewById(R.id.fabBills)
        fab.setOnClickListener { openCreateBill() }

        // Extract the billId passed from NotificationsActivity
        pendingBillIdFromNotification = intent.getStringExtra("BILL_ID")

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottomNavigationViewBills)
        bottomNavigationView.selectedItemId = R.id.Bills
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.Main -> {
                    val intent = Intent(this, MainMenu::class.java)
                    intent.putExtra(AuthUtils.EXTRA_USER_EMAIL, userEmail)
                    startActivity(intent)
                    true
                }
                R.id.Bills -> {
                    true
                }
                R.id.Spendings -> {
                    val intent = Intent(this, MySpendings::class.java)
                    intent.putExtra(AuthUtils.EXTRA_USER_EMAIL, userEmail)
                    startActivity(intent)
                    true
                }
                R.id.Income -> {  // New navigation option for Income
                    val intent = Intent(this, MyIncome::class.java)
                    intent.putExtra(AuthUtils.EXTRA_USER_EMAIL, userEmail)
                    startActivity(intent)
                    true
                }
                R.id.Calendar -> {
                    val intent = Intent(this, CalendarActivity::class.java)
                    intent.putExtra(AuthUtils.EXTRA_USER_EMAIL, userEmail)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
        // Initialize the toolbar and set it as the action bar
        val toolbar: Toolbar = findViewById(R.id.materialToolbarBills)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        DrawerNavigation.setup(this, drawerLayout, findViewById(R.id.nav_viewBills))

        db = FirebaseFirestore.getInstance()
        setupDialog()
        setupEventChangeListener()

        // Inicializar botones de filtro
        findViewById<Button>(R.id.btnIncoming).setOnClickListener { filterBills("incoming") }
        findViewById<Button>(R.id.btnDue).setOnClickListener { filterBills("due") }
        findViewById<Button>(R.id.btnRecurring).setOnClickListener { filterBills("recurring") }
        findViewById<Button>(R.id.btnAllBills).setOnClickListener { filterBills("all") }
    }
    private fun setupDialog() {
        dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_box_bill)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawable(getDrawable(R.drawable.dialog_box_bills_bg))
        dialog.setCancelable(false)

        val imgDeleteBill = dialog.findViewById<ImageView>(R.id.imgDeleteBill)
        val imgEditBill = dialog.findViewById<ImageView>(R.id.imgEditBill)

        // Initialize btnCloseDialog
        btnCloseDialog = dialog.findViewById(R.id.btnCloseDialog)
        btnCloseDialog.setOnClickListener {
            if (btnCloseDialog.text == getString(R.string.cancel)) {
                // Cancel editing mode
                btnCloseDialog.text = getString(R.string.close)
                dialog.findViewById<Button>(R.id.btnSaveChanges).visibility = View.GONE

                // Disable fields again
                dialog.findViewById<EditText>(R.id.edtTitleDialog).isEnabled = false
                dialog.findViewById<EditText>(R.id.edtAmountDialog).isEnabled = false
                dialog.findViewById<EditText>(R.id.edtDateDialog).isEnabled = false
                dialog.findViewById<EditText>(R.id.edtRepeatDialog).isEnabled = false
                dialog.findViewById<EditText>(R.id.edtCategoryDialog).isEnabled = false
                dialog.findViewById<EditText>(R.id.edtSubcategoryDialog).isEnabled = false
                dialog.findViewById<EditText>(R.id.edtVendorDialog).isEnabled = false
                dialog.findViewById<EditText>(R.id.edtCommentDialog).isEnabled = false
                configureBillPickers(
                    dialog.findViewById(R.id.edtDateDialog),
                    dialog.findViewById(R.id.edtRepeatDialog),
                    dialog.findViewById(R.id.edtCategoryDialog),
                    dialog.findViewById(R.id.edtSubcategoryDialog),
                    false
                )
            } else {
                dialog.dismiss()
            }
        }

        imgDeleteBill.setOnClickListener {
            deleteBill()
        }
    }

    private fun setupEventChangeListener() {
        val userUid = FirebaseAuth.getInstance().currentUser?.uid
        if (userUid != null) {
            listenerRegistration = db.collection("users").document(userUid).collection("bills")
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        Toast.makeText(this, getString(R.string.load_open_payments_failed, e.message.orEmpty()), Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }

                    if (snapshots != null) {
                        dataLoaded = true
                        billsArrayList.clear()
                        allBillsArrayList.clear() // Limpiamos la lista de todas las facturas
                        for (document in snapshots.documents) {
                            val bill = document.toObject(Bills::class.java)
                            if (bill != null) {
                                repairLegacyBill(userUid, document.id, bill)
                                PaymentNotificationScheduler.scheduleBill(this, userUid, bill)
                                billsArrayList.add(bill)
                                allBillsArrayList.add(bill) // Añadimos a la lista de todas las facturas
                            }
                        }
                        filterBills(activeFilter)
                        if (allBillsArrayList.none { !it.paid }) {
                            EmptyStateTutorial.showFirstTimeIfNeeded(
                                this,
                                emptyStateConfig,
                                hasPremiumAccess = PremiumAccess.isPremiumUser(this),
                                onAdd = ::openCreateBill,
                                onPremium = ::openCreateBill
                            )
                        }
                        pendingBillIdFromNotification?.let { billId ->
                            pendingBillIdFromNotification = null
                            findAndShowBillById(billId)
                        }
                    } else {
                    }
                }
        } else {
            Toast.makeText(this, R.string.user_not_authenticated, Toast.LENGTH_SHORT).show()
        }
    }
    private fun repairLegacyBill(userUid: String, documentId: String, bill: Bills) {
        val updates = mutableMapOf<String, Any>()

        if (bill.billId.isNullOrBlank()) {
            bill.billId = documentId
            updates["billId"] = documentId
        }
        if (bill.category.isNullOrBlank()) {
            bill.category = FinancialEntryOptions.DEFAULT_EXPENSE_CATEGORY
            updates["category"] = bill.category
        }
        if (bill.subcategory.isNullOrBlank() || bill.subcategory == "-") {
            bill.subcategory = FinancialEntryOptions.normalizedExpenseSubcategory(
                this,
                bill.category,
                bill.subcategory
            )
            updates["subcategory"] = bill.subcategory
        }
        if (bill.parentBillId.isNullOrBlank()) {
            bill.parentBillId = documentId
            updates["parentBillId"] = documentId
        }
        if (bill.currency.isNullOrBlank()) {
            bill.currency = CurrencyPreferences.selectedCode(this)
            updates["currency"] = bill.currency
        }
        if (updates.isNotEmpty()) {
            db.collection("users").document(userUid)
                .collection("bills")
                .document(documentId)
                .update(updates)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        listenerRegistration?.remove()
    }

    override fun onBillClick(position: Int) {
        val item = myAdapter.getItemAtPosition(position)
        // Check if the clicked item is a bill
        if (item is Bills) {
            selectedBill = item
            showBillDetailsDialog(item)
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

    private fun showBillDetailsDialog(bill: Bills) {
        val edtTitleDialog = dialog.findViewById<EditText>(R.id.edtTitleDialog)
        val edtAmountDialog = dialog.findViewById<EditText>(R.id.edtAmountDialog)
        val edtCategoryDialog = dialog.findViewById<EditText>(R.id.edtCategoryDialog)
        val edtSubcategoryDialog = dialog.findViewById<EditText>(R.id.edtSubcategoryDialog)
        val edtVendorDialog = dialog.findViewById<EditText>(R.id.edtVendorDialog)
        val edtDateDialog = dialog.findViewById<EditText>(R.id.edtDateDialog)
        val edtRepeatDialog = dialog.findViewById<EditText>(R.id.edtRepeatDialog)
        val edtCommentDialog = dialog.findViewById<EditText>(R.id.edtCommentDialog)
        val edtAttachmentDialog = dialog.findViewById<ImageView>(R.id.edtAttachmentDialog)
        val attachmentUri = bill.attachment
        val btnSaveChanges = dialog.findViewById<Button>(R.id.btnSaveChanges)
        val btnEditChanges = dialog.findViewById<ImageView>(R.id.imgEditBill)

        // Convertir el Timestamp a String
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val billDateString = if (bill.date != null) dateFormat.format(bill.date.toDate()) else ""

        if (attachmentUri != null) {
            edtAttachmentDialog.setImageURI(Uri.parse(attachmentUri))
            edtAttachmentDialog.visibility = View.VISIBLE
        } else {
            edtAttachmentDialog.visibility = View.GONE
        }
        dialog.show()

        edtTitleDialog.setText(bill.name)
        edtAmountDialog.setText(CurrencyPreferences.formatPlain(bill.amount))
        edtAmountDialog.setTextColor(
            ContextCompat.getColor(
                this,
                if (bill.date != null && AppDateUtils.isBeforeToday(bill.date.toDate()) && !bill.paid) {
                    R.color.red
                } else {
                    R.color.bill_color
                }
            )
        )
        edtCategoryDialog.setText(FinancialEntryOptions.displayCategory(this, bill.category))
        edtSubcategoryDialog.setText(
            FinancialEntryOptions.displaySubcategory(this, bill.category, bill.subcategory)
        )
        edtVendorDialog.setText(if (bill.vendor != "-") bill.vendor else "")
        edtRepeatDialog.setText(bill.repeat)
        edtDateDialog.setText(billDateString)
        edtCommentDialog.setText(bill.comment)

        // Initially disable fields
        edtTitleDialog.isEnabled = false
        edtAmountDialog.isEnabled = false
        edtDateDialog.isEnabled = false
        edtRepeatDialog.isEnabled = false
        edtCategoryDialog.isEnabled = false
        edtSubcategoryDialog.isEnabled = false
        edtVendorDialog.isEnabled = false
        edtCommentDialog.isEnabled = false
        configureBillPickers(
            edtDateDialog,
            edtRepeatDialog,
            edtCategoryDialog,
            edtSubcategoryDialog,
            false
        )


        // Hide save button initially
        btnSaveChanges.visibility = View.GONE

        btnEditChanges.setOnClickListener {
            edtTitleDialog.isEnabled = true
            edtAmountDialog.isEnabled = true
            edtDateDialog.isEnabled = true
            edtRepeatDialog.isEnabled = true
            edtCategoryDialog.isEnabled = true
            edtSubcategoryDialog.isEnabled = true
            edtVendorDialog.isEnabled = true
            edtCommentDialog.isEnabled = true
            configureBillPickers(
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
            val userUid = FirebaseAuth.getInstance().currentUser?.uid
            val bill = selectedBill
            if (userUid != null && bill != null) {
                val originalSelectedDate = bill.date
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
                val category = FinancialEntryOptions.normalizedExpenseCategory(
                    this,
                    edtCategoryDialog.text.toString()
                )
                val subcategory = FinancialEntryOptions.normalizedExpenseSubcategory(
                    this,
                    category,
                    edtSubcategoryDialog.text.toString()
                )
                // Update the bill object with new values
                bill.name = edtTitleDialog.text.toString().trim().ifBlank { getString(R.string.open_payment) }
                bill.amount = CurrencyPreferences.roundToTwoDecimals(edtAmountDialog.text.toString().toDoubleOrNull() ?: 0.0)
                if (bill.amount <= 0.0) {
                    Toast.makeText(this, R.string.enter_valid_amount, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                bill.date = com.google.firebase.Timestamp(parsedDate)
                bill.repeat = edtRepeatDialog.text.toString().trim().ifBlank { "No" }
                bill.category = category
                bill.subcategory = subcategory
                bill.vendor = edtVendorDialog.text.toString().trim()
                bill.comment = edtCommentDialog.text.toString().trim()

                btnSaveChanges.visibility = View.VISIBLE

                if (!bill.repeat.isNullOrBlank() && bill.repeat != "No") {
                    AlertDialog.Builder(this)
                        .setTitle(R.string.edit_recurring_payment)
                        .setItems(
                            arrayOf(
                                getString(R.string.edit_this_payment_only),
                                getString(R.string.edit_this_and_future_payments)
                            )
                        ) { _, choice ->
                            if (choice == 0) {
                                saveSingleBillEdit(userUid, bill)
                            } else {
                                saveCurrentAndFutureBillEdits(userUid, bill, originalSelectedDate)
                            }
                        }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
                } else {
                    saveSingleBillEdit(userUid, bill)
                }
            } else {
                Toast.makeText(this, R.string.open_payment_update_unavailable, Toast.LENGTH_SHORT).show()
            }
        }}

    private fun saveSingleBillEdit(userUid: String, bill: Bills) {
        db.collection("users").document(userUid).collection("bills")
            .document(bill.billId)
            .set(bill)
            .addOnSuccessListener {
                PaymentNotificationScheduler.scheduleBill(this, userUid, bill, forceReplace = true)
                val index = billsArrayList.indexOfFirst { it.billId == bill.billId }
                if (index != -1) {
                    billsArrayList[index] = bill
                    myAdapter.notifyItemChanged(index)
                }
                Toast.makeText(this, R.string.open_payment_updated, Toast.LENGTH_SHORT).show()
                btnCloseDialog.text = getString(R.string.close)
                dialog.dismiss()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, getString(R.string.open_payment_update_failed, e.message.orEmpty()), Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveCurrentAndFutureBillEdits(
        userUid: String,
        editedBill: Bills,
        originalSelectedDate: com.google.firebase.Timestamp?
    ) {
        val selectedDate = originalSelectedDate ?: editedBill.date ?: return saveSingleBillEdit(userUid, editedBill)
        val seriesId = editedBill.parentBillId?.takeIf { it.isNotBlank() } ?: editedBill.billId
        val billsReference = db.collection("users").document(userUid).collection("bills")

        billsReference.get()
            .addOnSuccessListener { documents ->
                val batch = db.batch()
                val updatedFutureBills = mutableListOf<Bills>()
                documents.forEach { document ->
                    val occurrenceDate = document.getTimestamp("date")
                    val parentId = document.getString("parentBillId")
                    val belongsToSeries =
                        document.id == seriesId || parentId == seriesId
                    if (belongsToSeries && occurrenceDate != null && occurrenceDate >= selectedDate) {
                        if (document.id == editedBill.billId) {
                            batch.set(document.reference, editedBill)
                            updatedFutureBills.add(editedBill)
                        } else {
                            val futureBill = document.toObject(Bills::class.java)
                            futureBill.name = editedBill.name
                            futureBill.amount = editedBill.amount
                            futureBill.currency = editedBill.currency
                            futureBill.repeat = editedBill.repeat
                            futureBill.category = editedBill.category
                            futureBill.subcategory = editedBill.subcategory
                            futureBill.vendor = editedBill.vendor
                            futureBill.comment = editedBill.comment
                            futureBill.attachment = editedBill.attachment
                            futureBill.parentBillId = editedBill.parentBillId
                            batch.set(document.reference, futureBill)
                            updatedFutureBills.add(futureBill)
                        }
                    }
                }

                if (updatedFutureBills.isEmpty()) {
                    saveSingleBillEdit(userUid, editedBill)
                    return@addOnSuccessListener
                }

                batch.commit()
                    .addOnSuccessListener {
                        updatedFutureBills.forEach {
                            PaymentNotificationScheduler.scheduleBill(this, userUid, it, forceReplace = true)
                        }
                        Toast.makeText(this, R.string.future_open_payments_updated, Toast.LENGTH_SHORT).show()
                        btnCloseDialog.text = getString(R.string.close)
                        dialog.dismiss()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, getString(R.string.open_payment_update_failed, e.message.orEmpty()), Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, getString(R.string.open_payment_update_failed, e.message.orEmpty()), Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteBill() {
        val bill = selectedBill ?: return
        if (!bill.repeat.isNullOrBlank() && bill.repeat != "No") {
            AlertDialog.Builder(this)
                .setTitle(R.string.delete_recurring_payment)
                .setItems(
                    arrayOf(
                        getString(R.string.delete_this_occurrence),
                        getString(R.string.delete_this_and_future)
                    )
                ) { _, choice ->
                    if (choice == 0) {
                        deleteSingleBill(bill)
                    } else {
                        deleteCurrentAndFutureBills(bill)
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        } else {
            AlertDialog.Builder(this)
                .setTitle(R.string.delete_open_payment)
                .setMessage(R.string.delete_open_payment_confirmation)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete) { _, _ -> deleteSingleBill(bill) }
                .show()
        }
    }

    private fun deleteSingleBill(bill: Bills) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.collection("users").document(uid).collection("bills")
            .document(bill.billId)
            .delete()
            .addOnSuccessListener {
                PaymentNotificationScheduler.cancelBill(this, bill.billId)
                Toast.makeText(this, R.string.open_payment_deleted, Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .addOnFailureListener {
                Toast.makeText(this, R.string.open_payment_delete_failed, Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteCurrentAndFutureBills(selected: Bills) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val selectedDate = selected.date ?: return
        val seriesId = selected.parentBillId?.takeIf { it.isNotBlank() } ?: selected.billId
        val billsReference = db.collection("users").document(uid).collection("bills")

        billsReference.get()
            .addOnSuccessListener { documents ->
                val documentsToDelete = documents.filter { document ->
                    val occurrenceDate = document.getTimestamp("date")
                    val parentId = document.getString("parentBillId")
                    val belongsToSeries =
                        document.id == seriesId || parentId == seriesId || document.id == selected.billId
                    belongsToSeries && occurrenceDate != null && occurrenceDate >= selectedDate
                }

                if (documentsToDelete.isEmpty()) {
                    deleteSingleBill(selected)
                    return@addOnSuccessListener
                }

                val batch = db.batch()
                documentsToDelete.forEach { batch.delete(it.reference) }
                batch.commit()
                    .addOnSuccessListener {
                        documentsToDelete.forEach {
                            PaymentNotificationScheduler.cancelBill(this, it.id)
                        }
                        Toast.makeText(
                            this,
                            R.string.future_open_payments_deleted,
                            Toast.LENGTH_SHORT
                        ).show()
                        dialog.dismiss()
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            this,
                            R.string.open_payment_delete_failed,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, R.string.open_payment_delete_failed, Toast.LENGTH_SHORT).show()
            }
    }

    private fun filterBills(filter: String) {
        activeFilter = filter
        val filteredBills = ArrayList<Bills>()
        findViewById<Button>(R.id.btnAllBills).setBackgroundColor(ContextCompat.getColor(this, R.color.filter_inactive))
        findViewById<Button>(R.id.btnIncoming).setBackgroundColor(ContextCompat.getColor(this, R.color.filter_inactive))
        findViewById<Button>(R.id.btnDue).setBackgroundColor(ContextCompat.getColor(this, R.color.filter_inactive))
        findViewById<Button>(R.id.btnRecurring).setBackgroundColor(ContextCompat.getColor(this, R.color.filter_inactive))

        for (bill in allBillsArrayList) { // Usamos allBillsArrayList para filtrar
            try {
                val billDate = if (bill.date != null) bill.date.toDate() else null

                when (filter) {
                    "all" -> {
                        findViewById<Button>(R.id.btnAllBills).setBackgroundColor(ContextCompat.getColor(this, R.color.filter_open_active))
                        if (!bill.paid) {
                            filteredBills.add(bill)
                        }
                    }
                    "due" -> {
                        findViewById<Button>(R.id.btnDue).setBackgroundColor(ContextCompat.getColor(this, R.color.filter_overdue_active))
                        if (billDate != null && AppDateUtils.isBeforeToday(billDate) && !bill.paid) {
                            filteredBills.add(bill)
                        }
                    }
                    "recurring" -> {
                        findViewById<Button>(R.id.btnRecurring).setBackgroundColor(ContextCompat.getColor(this, R.color.filter_open_active))
                        if (!bill.paid && bill.repeat != "No") {
                            filteredBills.add(bill)
                        }
                    }
                    "incoming" -> {
                        findViewById<Button>(R.id.btnIncoming).setBackgroundColor(ContextCompat.getColor(this, R.color.filter_open_active))
                        if (billDate != null && AppDateUtils.isTodayOrAfter(billDate) && !bill.paid) {
                            filteredBills.add(bill)
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore malformed records so one bad date does not break the list.
            }
        }

        myAdapter.updateBills(filteredBills)
        if (dataLoaded) {
            val hasOpenPayments = allBillsArrayList.any { !it.paid }
            EmptyStateTutorial.bind(
                this,
                findViewById(R.id.emptyStateBills),
                emptyStateConfig,
                hasAnyEntries = hasOpenPayments,
                hasFilteredEntries = filteredBills.isNotEmpty(),
                hasPremiumAccess = PremiumAccess.isPremiumUser(this),
                onAdd = ::openCreateBill,
                onPremium = ::openCreateBill
            )
        }
    }

    private fun openCreateBill() {
        EntryCreationFlow.show(this, EntryType.OPEN_PAYMENT, userEmail)
    }

    private fun configureBillPickers(
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
                View.OnClickListener { showExpenseCategoryPicker(categoryField, subcategoryField) }
            } else null
        )
        subcategoryField.setOnClickListener(
            if (enabled) {
                View.OnClickListener { showExpenseSubcategoryPicker(categoryField, subcategoryField) }
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

    private fun showExpenseCategoryPicker(categoryField: EditText, subcategoryField: EditText) {
        val options = FinancialEntryOptions.expenseCategories(this)
        val labels = options.map { it.label }.toTypedArray()
        val current = options.indexOfFirst {
            it.key == FinancialEntryOptions.normalizedExpenseCategory(this, categoryField.text.toString())
        }.coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle(R.string.category)
            .setSingleChoiceItems(labels, current) { dialog, which ->
                val selected = options[which]
                categoryField.setText(selected.label)
                subcategoryField.setText(defaultExpenseSubcategoryLabel(selected.key))
                dialog.dismiss()
            }
            .show()
    }

    private fun showExpenseSubcategoryPicker(categoryField: EditText, subcategoryField: EditText) {
        val category = FinancialEntryOptions.normalizedExpenseCategory(this, categoryField.text.toString())
        val options = FinancialEntryOptions.expenseSubcategories(this, category)
        val labels = options.map { it.label }.toTypedArray()
        val current = options.indexOfFirst {
            it.key == FinancialEntryOptions.normalizedExpenseSubcategory(this, category, subcategoryField.text.toString())
        }.coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle(R.string.subcategory)
            .setSingleChoiceItems(labels, current) { dialog, which ->
                subcategoryField.setText(options[which].label)
                dialog.dismiss()
            }
            .show()
    }

    private fun defaultExpenseSubcategoryLabel(categoryKey: String): String {
        val options = FinancialEntryOptions.expenseSubcategories(this, categoryKey)
        val defaultKey = if (categoryKey == FinancialEntryOptions.DEFAULT_EXPENSE_CATEGORY) {
            FinancialEntryOptions.DEFAULT_EXPENSE_SUBCATEGORY
        } else {
            options.firstOrNull()?.key ?: FinancialEntryOptions.DEFAULT_EXPENSE_SUBCATEGORY
        }
        return options.firstOrNull { it.key == defaultKey }?.label ?: defaultKey
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
    private fun findAndShowBillById(billId: String) {
        // Search for the bill in the allBillsArrayList
        val bill = allBillsArrayList.find { it.billId == billId }

        // If a matching bill is found, show its details
        if (bill != null) {
            showBillDetailsDialog(bill)
        } else {
            Toast.makeText(this, R.string.bill_not_found, Toast.LENGTH_SHORT).show()
        }
    }

    private fun logoutUser() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, LogIn::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
