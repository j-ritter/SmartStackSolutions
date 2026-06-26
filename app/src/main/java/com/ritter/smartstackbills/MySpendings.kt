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
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MySpendings : AppCompatActivity(), MyAdapterSpendings.OnSpendingClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var spendingsArrayList: ArrayList<Spendings>
    private lateinit var allSpendingsArrayList: ArrayList<Spendings>
    private lateinit var myAdapter: MyAdapterSpendings
    private lateinit var db: FirebaseFirestore
    private var listenerRegistration: ListenerRegistration? = null
    private lateinit var fab: FloatingActionButton
    private var userEmail: String? = null
    private lateinit var dialog: Dialog
    private lateinit var requestDisplayPermissionLauncher: ActivityResultLauncher<String>
    private var pendingSpendingForDialog: Spendings? = null
    private var pendingDialogImageView: ImageView? = null
    private var pendingDialogDetailsLayout: View? = null
    private var selectedSpending: Spendings? = null
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var btnCloseDialog: Button
    private var activeFilter: String = "all"
    private var dataLoaded = false
    private val hasPremiumAccess: Boolean
        get() = PremiumAccess.isPremiumUser(this)
    private val emptyStateConfig = EmptyStateConfig(
        preferenceKey = "closed_payments_tutorial_shown",
        imageRes = R.drawable.image_closedpayments,
        titleRes = R.string.empty_closed_payments_title,
        messageRes = R.string.empty_closed_payments_message,
        addActionRes = R.string.add_closed_payment,
        requiresPremium = true
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_my_spendings)

        drawerLayout = findViewById(R.id.drawer_layout_spendings)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainSpendings)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        recyclerView = findViewById(R.id.recyclerViewSpendings)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)
        userEmail = intent.getStringExtra(AuthUtils.EXTRA_USER_EMAIL) ?: AuthUtils.currentUserEmail()

        spendingsArrayList = ArrayList()
        allSpendingsArrayList = ArrayList()
        myAdapter = MyAdapterSpendings(this, spendingsArrayList, this)
        recyclerView.adapter = myAdapter

        findViewById<TextView>(R.id.tvSpendingsPremiumPreview).visibility =
            if (hasPremiumAccess) View.GONE else View.VISIBLE

        fab = findViewById(R.id.fabSpendings)
        fab.setOnClickListener { handleAddSpending() }

        requestDisplayPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                if (isGranted) {
                    pendingSpendingForDialog?.let { spending ->
                        pendingDialogImageView?.let { imageView ->
                            loadImageIntoView(spending.attachment, imageView, pendingDialogDetailsLayout)
                        }
                    }
                } else {
                    Toast.makeText(this, R.string.image_permission_denied, Toast.LENGTH_SHORT).show()
                    // Hide image view or show placeholder if permission is denied
                    pendingDialogImageView?.visibility = View.GONE
                    pendingDialogDetailsLayout?.visibility = View.VISIBLE // Show other details
                }
                // Clear pending items
                pendingSpendingForDialog = null
                pendingDialogImageView = null
                pendingDialogDetailsLayout = null
            }

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottomNavigationViewSpendings)
        bottomNavigationView.selectedItemId = R.id.Spendings
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
                    intent.putExtra(AuthUtils.EXTRA_USER_EMAIL, userEmail) // Pasar el correo electrÃ³nico
                    startActivity(intent)
                    true
                }
                R.id.Spendings -> {
                    true
                }
                R.id.Income -> {
                    val intent = Intent(this, MyIncome::class.java)
                    intent.putExtra(AuthUtils.EXTRA_USER_EMAIL, userEmail)
                    startActivity(intent)
                    true
                }
                R.id.Calendar -> {
                    val intent = Intent(this,CalendarActivity::class.java)
                    intent.putExtra(AuthUtils.EXTRA_USER_EMAIL, userEmail)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
        // Initialize the toolbar and set it as the action bar
        val toolbar: Toolbar = findViewById(R.id.materialToolbarSpendings)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
        DrawerNavigation.setup(this, drawerLayout, findViewById(R.id.nav_viewSpendings))

        db = FirebaseFirestore.getInstance()
        setupDialog()
        setupEventChangeListener()

        // Check if the activity was started with a specific filter
        activeFilter = intent.getStringExtra("FILTER_TYPE") ?: "all"
        filterSpendings(activeFilter)

        // Initialize filter buttons
        findViewById<Button>(R.id.btnEssential).setOnClickListener { filterSpendings("essential") }
        findViewById<Button>(R.id.btnNonEssential).setOnClickListener { filterSpendings("non-essential") }
        findViewById<Button>(R.id.btnSpendingsAll).setOnClickListener { filterSpendings("all") }
    }
    private fun setupDialog() {
        dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_box_spendings)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawable(getDrawable(R.drawable.dialog_box_spendings_bg))
        dialog.setCancelable(false)


        val imgDeleteSpending = dialog.findViewById<ImageView>(R.id.imgDeleteSpendings)
        val imgEditSpending = dialog.findViewById<ImageView>(R.id.imgEditSpendings)

        btnCloseDialog = dialog.findViewById(R.id.btnCloseDialogSpendings)
        btnCloseDialog.setOnClickListener {
            if (btnCloseDialog.text == getString(R.string.cancel)) {
                // Cancel editing mode
                btnCloseDialog.text = getString(R.string.close)
                dialog.findViewById<Button>(R.id.btnSaveChangesSpendings).visibility = View.GONE

                // Disable inputs again
                dialog.findViewById<EditText>(R.id.edtTitleDialogSpendings).isEnabled = false
                dialog.findViewById<EditText>(R.id.edtAmountDialogSpendings).isEnabled = false
                dialog.findViewById<EditText>(R.id.edtCommentDialogSpendings).isEnabled = false
            } else {
                dialog.dismiss()
            }
        }

        imgDeleteSpending.setOnClickListener {
            if (hasPremiumAccess || !selectedSpending?.importHash.isNullOrBlank()) {
                deleteSpending()
            } else {
                showUpgradeDialog()
            }
        }
    }

    private fun setupEventChangeListener() {
        val userUid = FirebaseAuth.getInstance().currentUser?.uid
        if (userUid != null) {
            listenerRegistration = db.collection("users").document(userUid).collection("spendings")
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        Toast.makeText(this, getString(R.string.load_closed_payments_failed, e.message.orEmpty()), Toast.LENGTH_SHORT).show()
                        Log.e("Firestore Error", e.message.toString())
                        return@addSnapshotListener
                    }

                    if (snapshots != null) {
                        dataLoaded = true
                        spendingsArrayList.clear()
                        allSpendingsArrayList.clear()
                        for (document in snapshots.documents) {
                            val spending = document.toObject(Spendings::class.java)
                            if (spending != null) {
                                repairLegacySpending(userUid, document.id, spending)
                                spendingsArrayList.add(spending)
                                allSpendingsArrayList.add(spending)
                                Log.d("Firestore Data", "Spending added: ${spending.name}, ${spending.date}")
                            }
                        }
                        // Notify the adapter of the updated data
                        myAdapter.updateSpendings(spendingsArrayList)

                        // Save the updated list 
                        filterSpendings(activeFilter)
                        if (allSpendingsArrayList.isEmpty()) {
                            EmptyStateTutorial.showFirstTimeIfNeeded(
                                this,
                                emptyStateConfig,
                                hasPremiumAccess,
                                onAdd = ::openCreateSpending,
                                onPremium = ::showUpgradeDialog
                            )
                        }
                    } else {
                        Log.d("Firestore Data", "No spendings found")
                    }
                }
        } else {
            Toast.makeText(this, R.string.user_not_authenticated, Toast.LENGTH_SHORT).show()
            Log.e("Authentication Error", "User not authenticated")
        }
    }
    private fun repairLegacySpending(userUid: String, documentId: String, spending: Spendings) {
        val updates = mutableMapOf<String, Any>()

        if (spending.spendingId.isNullOrBlank()) {
            spending.spendingId = documentId
            updates["spendingId"] = documentId
        }
        if (spending.subcategory == null) {
            spending.subcategory = "-"
            updates["subcategory"] = "-"
        }
        if (spending.currency.isNullOrBlank()) {
            spending.currency = CurrencyPreferences.selectedCode(this)
            updates["currency"] = spending.currency
        }
        if (updates.isNotEmpty()) {
            db.collection("users").document(userUid)
                .collection("spendings")
                .document(documentId)
                .update(updates)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        listenerRegistration?.remove()
    }

    override fun onSpendingClick(position: Int) {
        val item = myAdapter.getItemAtPosition(position)
        // Check if the clicked item is a spending
        if (item is Spendings) {
            selectedSpending = item
            showSpendingDetailsDialog(item)
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

    private fun showSpendingDetailsDialog(spending: Spendings) {
        val edtTitleDialog = dialog.findViewById<EditText>(R.id.edtTitleDialogSpendings)
        val edtAmountDialog = dialog.findViewById<EditText>(R.id.edtAmountDialogSpendings)
        val edtCategoryDialog = dialog.findViewById<EditText>(R.id.edtCategoryDialogSpendings)
        val edtSubcategoryDialog = dialog.findViewById<EditText>(R.id.edtSubcategoryDialogSpendings)
        val edtVendorDialog = dialog.findViewById<EditText>(R.id.edtVendorDialogSpendings)
        val edtDateDialog = dialog.findViewById<EditText>(R.id.edtDateDialogSpendings)
        val edtCommentDialog = dialog.findViewById<EditText>(R.id.edtCommentDialogSpendings)
        val edtAttachmentDialog = dialog.findViewById<ImageView>(R.id.edtAttachmentDialogSpendings)
        val attachmentUri = spending.attachment
        val spendingAttachmentImageView = dialog.findViewById<ImageView>(R.id.edtAttachmentDialogSpendings)
        val btnSaveChanges = dialog.findViewById<Button>(R.id.btnSaveChangesSpendings)
        val btnEditChanges = dialog.findViewById<ImageView>(R.id.imgEditSpendings)
        val btnDelete = dialog.findViewById<ImageView>(R.id.imgDeleteSpendings)

        // Convertir el Timestamp a String
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val spendingDateString = if (spending.date != null) dateFormat.format(spending.date.toDate()) else ""

        val attachmentUriString = spending.attachment
        spendingAttachmentImageView.visibility = View.GONE // Hide image view initially

        if (!attachmentUriString.isNullOrEmpty()) {
            val permissionToRequest = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                android.Manifest.permission.READ_MEDIA_IMAGES
            } else {
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            }

            if (ContextCompat.checkSelfPermission(this, permissionToRequest) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                // Permission is already granted, load the image directly
                loadImageIntoView(attachmentUriString, spendingAttachmentImageView, null /*pendingDialogDetailsLayout not needed here*/)
            } else {
                // Permission is not granted, request it.
                // Store the bill and ImageView to use in the permission result callback.
                pendingSpendingForDialog = spending
                pendingDialogImageView = spendingAttachmentImageView
                // pendingDialogDetailsLayout = null; // Or pass a relevant layout if needed by loadImageIntoView's callback part

                // You can add a rationale here if needed:
                // if (shouldShowRequestPermissionRationale(permissionToRequest)) { ... }

                requestDisplayPermissionLauncher.launch(permissionToRequest)
                // The image will be loaded by the launcher's callback if permission is granted.
                // If denied, the launcher's callback already shows a Toast.
            }
        }
        dialog.show()
        styleDetailsDialogWindow(dialog)

        edtTitleDialog.setText(spending.name)
        edtAmountDialog.setText(CurrencyPreferences.formatPlain(spending.amount))

        edtCategoryDialog.setText(FinancialEntryOptions.displayCategory(this, spending.category))
        edtSubcategoryDialog.setText(
            FinancialEntryOptions.displaySubcategory(this, spending.category, spending.subcategory)
        )
        edtVendorDialog.setText(spending.vendor ?: "-")
        edtDateDialog.setText(spendingDateString)
        edtCommentDialog.setText(spending.comment)

        // Initially disable fields
        edtTitleDialog.isEnabled = false
        edtAmountDialog.isEnabled = false
        edtCommentDialog.isEnabled = false

        // Hide save button initially
        btnSaveChanges.visibility = View.GONE
        btnEditChanges.visibility = if (hasPremiumAccess) View.VISIBLE else View.GONE
        btnDelete.visibility = View.VISIBLE

        btnEditChanges.setOnClickListener {
            if (!hasPremiumAccess) {
                showUpgradeDialog()
                return@setOnClickListener
            }
            edtTitleDialog.isEnabled = true
            edtAmountDialog.isEnabled = true
            edtCommentDialog.isEnabled = true

            btnSaveChanges.visibility = View.VISIBLE
            btnCloseDialog.text = getString(R.string.cancel)

        }
        btnSaveChanges.setOnClickListener {
            if (!hasPremiumAccess) {
                showUpgradeDialog()
                return@setOnClickListener
            }
            val userUid = FirebaseAuth.getInstance().currentUser?.uid
            val spending = selectedSpending
            if (userUid != null && spending != null) {
                // Update the bill object with new values
                spending.name = edtTitleDialog.text.toString()
                spending.amount = CurrencyPreferences.roundToTwoDecimals(edtAmountDialog.text.toString().toDoubleOrNull() ?: 0.0)

                spending.comment = edtCommentDialog.text.toString()

                btnSaveChanges.visibility = View.VISIBLE

                // Save the updated bill to Firebase
                db.collection("users").document(userUid).collection("spendings")
                    .document(spending.spendingId)
                    .set(spending)
                    .addOnSuccessListener {
                        // Update the local list
                        val index = spendingsArrayList.indexOfFirst { it.spendingId == spending.spendingId }
                        if (index != -1) {
                            spendingsArrayList[index] = spending
                            myAdapter.notifyItemChanged(index)
                        }
                        Toast.makeText(this, R.string.closed_payment_updated, Toast.LENGTH_SHORT).show()
                        btnCloseDialog.text = getString(R.string.close)
                        dialog.dismiss()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, getString(R.string.closed_payment_update_failed, e.message.orEmpty()), Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, R.string.closed_payment_update_unavailable, Toast.LENGTH_SHORT).show()
            }
        }}

    private fun deleteSpending() {
        selectedSpending?.let { spending ->
            val userUid = FirebaseAuth.getInstance().currentUser?.uid
            if (userUid != null) {
                val userRef = db.collection("users").document(userUid)
                val spendingRef = userRef.collection("spendings").document(spending.spendingId)
                val usageRef = userRef.collection("usage").document("statement_imports")
                db.runTransaction { transaction ->
                    val spendingDocument = transaction.get(spendingRef)
                    val usageDocument = transaction.get(usageRef)
                    transaction.delete(spendingRef)
                    if (!spendingDocument.getString("importHash").isNullOrBlank()) {
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
                        Toast.makeText(this, R.string.closed_payment_deleted, Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, R.string.closed_payment_delete_failed, Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, R.string.user_not_authenticated, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun filterSpendings(filter: String) {
        activeFilter = filter
        val filteredSpendings = ArrayList<Spendings>()

        // Reset button colors
        findViewById<Button>(R.id.btnSpendingsAll).setBackgroundColor(ContextCompat.getColor(this, R.color.filter_inactive))
        findViewById<Button>(R.id.btnEssential).setBackgroundColor(ContextCompat.getColor(this, R.color.filter_inactive))
        findViewById<Button>(R.id.btnNonEssential).setBackgroundColor(ContextCompat.getColor(this, R.color.filter_inactive))

        for (spending in allSpendingsArrayList) {
            val classification =
                SpendingClassification.classify(spending.category, spending.subcategory)

            when (filter) {
                "essential" -> {
                    findViewById<Button>(R.id.btnEssential).setBackgroundColor(ContextCompat.getColor(this, R.color.filter_closed_active))
                    if (classification == SpendingClassification.Type.ESSENTIAL) {
                        filteredSpendings.add(spending)
                    }
                }
                "non-essential" -> {
                    findViewById<Button>(R.id.btnNonEssential).setBackgroundColor(ContextCompat.getColor(this, R.color.filter_closed_active))
                    if (classification == SpendingClassification.Type.NON_ESSENTIAL) {
                        filteredSpendings.add(spending)
                    }
                }
                "all" -> {
                    findViewById<Button>(R.id.btnSpendingsAll).setBackgroundColor(ContextCompat.getColor(this, R.color.filter_closed_active))
                    filteredSpendings.add(spending)
                }
            }
        }

        myAdapter.updateSpendings(filteredSpendings)
        if (dataLoaded) {
            EmptyStateTutorial.bind(
                this,
                findViewById(R.id.emptyStateSpendings),
                emptyStateConfig,
                hasAnyEntries = allSpendingsArrayList.isNotEmpty(),
                hasFilteredEntries = filteredSpendings.isNotEmpty(),
                hasPremiumAccess = hasPremiumAccess,
                onAdd = ::openCreateSpending,
                onPremium = ::showUpgradeDialog
            )
        }
        Log.d("Filter", "Filtered spendings count for $filter: ${filteredSpendings.size}")
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
        PremiumUpgradeDialog.show(this, R.string.premium_preview_closed_payments, userEmail)
    }

    private fun handleAddSpending() {
        openCreateSpending()
    }

    private fun openCreateSpending() {
        EntryCreationFlow.show(this, EntryType.CLOSED_PAYMENT, userEmail)
    }

    private fun styleDetailsDialogWindow(dialog: Dialog) {
        val width = (resources.displayMetrics.widthPixels * 0.92f).toInt()
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
    private fun loadImageIntoView(attachmentUriString: String?, imageView: ImageView, detailsLayout: View?) {
        if (!attachmentUriString.isNullOrEmpty()) {
            try {
                val uri = Uri.parse(attachmentUriString)
                imageView.setImageURI(null)
                imageView.setImageURI(uri)
                imageView.visibility = View.VISIBLE
                detailsLayout?.visibility =View.VISIBLE // Or however you manage layout visibility
            } catch (e: Exception) {
                Log.e("ImageLoad", "Error loading image in loadImageIntoView", e)
                Toast.makeText(this, getString(R.string.image_display_failed, e.message.orEmpty()), Toast.LENGTH_SHORT).show()
                imageView.visibility = View.GONE
                detailsLayout?.visibility = View.VISIBLE // Still show other details
            }
        } else {
            imageView.visibility = View.GONE
            detailsLayout?.visibility = View.VISIBLE // Still show other details
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
