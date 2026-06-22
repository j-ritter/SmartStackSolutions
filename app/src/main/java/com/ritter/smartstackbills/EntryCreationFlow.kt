package com.ritter.smartstackbills

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog

enum class EntryType(val wireValue: String) {
    OPEN_PAYMENT("open_payment"),
    CLOSED_PAYMENT("closed_payment"),
    INCOME("income");

    companion object {
        fun from(value: String?): EntryType =
            values().firstOrNull { it.wireValue == value } ?: OPEN_PAYMENT
    }
}

object EntryCreationFlow {
    fun show(
        activity: AppCompatActivity,
        type: EntryType,
        userEmail: String? = AuthUtils.currentUserEmail()
    ) {
        if (type != EntryType.OPEN_PAYMENT && !PremiumAccess.isPremiumUser(activity)) {
            PremiumUpgradeDialog.show(
                activity,
                if (type == EntryType.INCOME) {
                    R.string.premium_preview_income
                } else {
                    R.string.premium_preview_closed_payments
                },
                userEmail
            )
            return
        }

        val dialog = BottomSheetDialog(activity)
        val content = LayoutInflater.from(activity)
            .inflate(R.layout.bottom_sheet_entry_method, null, false)
        content.findViewById<TextView>(R.id.entryMethodTitle).text = activity.getString(
            when (type) {
                EntryType.OPEN_PAYMENT -> R.string.entry_method_open_payment
                EntryType.CLOSED_PAYMENT -> R.string.entry_method_closed_payment
                EntryType.INCOME -> R.string.entry_method_income
            }
        )
        content.findViewById<View>(R.id.entryMethodScan).setOnClickListener {
            dialog.dismiss()
            activity.startActivity(Intent(activity, DocumentScanActivity::class.java).apply {
                putExtra(DocumentScanActivity.EXTRA_ENTRY_TYPE, type.wireValue)
                putExtra(AuthUtils.EXTRA_USER_EMAIL, userEmail)
            })
        }
        content.findViewById<View>(R.id.entryMethodManual).setOnClickListener {
            dialog.dismiss()
            launchForm(activity, type, userEmail)
        }
        dialog.setContentView(content)
        dialog.show()
    }

    fun launchForm(
        activity: AppCompatActivity,
        type: EntryType,
        userEmail: String?,
        prefill: ScanPrefill? = null
    ) {
        val destination = when (type) {
            EntryType.OPEN_PAYMENT -> createBill::class.java
            EntryType.CLOSED_PAYMENT -> createSpending::class.java
            EntryType.INCOME -> createIncome::class.java
        }
        activity.startActivity(Intent(activity, destination).apply {
            putExtra(AuthUtils.EXTRA_USER_EMAIL, userEmail)
            prefill?.writeTo(this)
        })
    }
}

data class ScanPrefill(
    val title: String?,
    val amount: String?,
    val date: String?,
    val party: String?,
    val attachmentUri: String?,
    val rawText: String?
) {
    fun writeTo(intent: Intent) {
        intent.putExtra(EXTRA_SCAN_TITLE, title)
        intent.putExtra(EXTRA_SCAN_AMOUNT, amount)
        intent.putExtra(EXTRA_SCAN_DATE, date)
        intent.putExtra(EXTRA_SCAN_PARTY, party)
        intent.putExtra(EXTRA_SCAN_ATTACHMENT, attachmentUri)
        intent.putExtra(EXTRA_SCAN_RAW_TEXT, rawText)
    }

    companion object {
        const val EXTRA_SCAN_TITLE = "scan_prefill_title"
        const val EXTRA_SCAN_AMOUNT = "scan_prefill_amount"
        const val EXTRA_SCAN_DATE = "scan_prefill_date"
        const val EXTRA_SCAN_PARTY = "scan_prefill_party"
        const val EXTRA_SCAN_ATTACHMENT = "scan_prefill_attachment"
        const val EXTRA_SCAN_RAW_TEXT = "scan_prefill_raw_text"
    }
}
