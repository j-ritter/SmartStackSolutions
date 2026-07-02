package com.ritter.smartstackbills

import android.content.Context
import com.google.firebase.Timestamp
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Calendar
import java.util.Date

object UsageLimits {
    const val FREE_ACTIVE_OPEN_PAYMENTS = 100L
    const val FREE_CLOSED_PAYMENTS = 100L
    const val PREMIUM_FUTURE_ENTRIES = 2_000L

    private val recurringValues = listOf(
        "Weekly",
        "Every 2 Weeks",
        "Monthly",
        "Every 2 Months",
        "Quarterly",
        "Every 6 months",
        "Yearly"
    )

    fun checkBillCreation(
        context: Context,
        uid: String,
        repeat: String,
        startDate: Date,
        onResult: (allowed: Boolean, messageRes: Int?) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(uid)
        val newDates = occurrenceDates(startDate, repeat)
        val futureNewEntries = newDates.count { !it.before(startOfToday()) }.toLong()

        if (PremiumAccess.isPremiumUser(context)) {
            countFutureEntries(userRef.collection("bills"), userRef.collection("income")) { current ->
                if (current == null) {
                    onResult(false, R.string.usage_limit_check_failed)
                    return@countFutureEntries
                }
                onResult(
                    current + futureNewEntries <= PREMIUM_FUTURE_ENTRIES,
                    R.string.premium_usage_limit_reached
                )
            }
            return
        }

        countActiveOpenBills(userRef.collection("bills")) { current ->
            if (current == null) {
                onResult(false, R.string.usage_limit_check_failed)
                return@countActiveOpenBills
            }
            onResult(
                current + newDates.size.toLong() <= FREE_ACTIVE_OPEN_PAYMENTS,
                R.string.free_open_payment_limit_reached
            )
        }
    }

    fun checkIncomeCreation(
        context: Context,
        uid: String,
        repeat: String,
        startDate: Date,
        onResult: (allowed: Boolean, messageRes: Int?) -> Unit
    ) {
        if (!PremiumAccess.isPremiumUser(context)) {
            onResult(false, R.string.premium_upgrade_required)
            return
        }
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(uid)
        val futureNewEntries = occurrenceDates(startDate, repeat)
            .count { !it.before(startOfToday()) }
            .toLong()
        countFutureEntries(userRef.collection("bills"), userRef.collection("income")) { current ->
            if (current == null) {
                onResult(false, R.string.usage_limit_check_failed)
                return@countFutureEntries
            }
            onResult(
                current + futureNewEntries <= PREMIUM_FUTURE_ENTRIES,
                R.string.premium_usage_limit_reached
            )
        }
    }

    @JvmStatic
    fun checkClosedPaymentCreation(
        context: Context,
        uid: String,
        newEntries: Long = 1L,
        onResult: (allowed: Boolean, messageRes: Int?) -> Unit
    ) {
        if (PremiumAccess.isPremiumUser(context)) {
            onResult(true, null)
            return
        }
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .collection("spendings")
            .count()
            .get(AggregateSource.SERVER)
            .addOnSuccessListener { current ->
                onResult(
                    current.count + newEntries <= FREE_CLOSED_PAYMENTS,
                    R.string.free_closed_payment_limit_reached
                )
            }
            .addOnFailureListener {
                onResult(false, R.string.usage_limit_check_failed)
            }
    }

    private fun countFutureEntries(
        bills: Query,
        income: Query,
        onResult: (Long?) -> Unit
    ) {
        val today = Timestamp(startOfToday())
        bills.whereGreaterThanOrEqualTo("date", today).count().get(AggregateSource.SERVER)
            .addOnSuccessListener { billCount ->
                income.whereGreaterThanOrEqualTo("date", today).count().get(AggregateSource.SERVER)
                    .addOnSuccessListener { incomeCount ->
                        onResult(billCount.count + incomeCount.count)
                    }
                    .addOnFailureListener { onResult(null) }
            }
            .addOnFailureListener { onResult(null) }
    }

    private fun countActiveOpenBills(query: Query, onResult: (Long?) -> Unit) {
        query.count()
            .get(AggregateSource.SERVER)
            .addOnSuccessListener { onResult(it.count) }
            .addOnFailureListener { onResult(null) }
    }

    private fun countMatchingRepeats(
        query: Query,
        fromDate: Timestamp?,
        onResult: (Long?) -> Unit
    ) {
        var remaining = recurringValues.size
        var total = 0L
        var failed = false
        recurringValues.forEach { repeat ->
            var recurringQuery = query.whereEqualTo("repeat", repeat)
            if (fromDate != null) {
                recurringQuery = recurringQuery.whereGreaterThanOrEqualTo("date", fromDate)
            }
            recurringQuery
                .count()
                .get(AggregateSource.SERVER)
                .addOnSuccessListener {
                    total += it.count
                    remaining -= 1
                    if (remaining == 0) onResult(if (failed) null else total)
                }
                .addOnFailureListener {
                    failed = true
                    remaining -= 1
                    if (remaining == 0) onResult(null)
                }
        }
    }

    private fun occurrenceDates(startDate: Date, repeat: String): List<Date> {
        val dates = mutableListOf(startDate)
        if (repeat == "No") return dates

        val calendar = Calendar.getInstance().apply { time = startDate }
        val endDate = Calendar.getInstance().apply { add(Calendar.YEAR, 1) }
        while (calendar.before(endDate)) {
            when (repeat) {
                "Weekly" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                "Every 2 Weeks" -> calendar.add(Calendar.WEEK_OF_YEAR, 2)
                "Monthly" -> calendar.add(Calendar.MONTH, 1)
                "Every 2 Months" -> calendar.add(Calendar.MONTH, 2)
                "Quarterly" -> calendar.add(Calendar.MONTH, 3)
                "Every 6 months" -> calendar.add(Calendar.MONTH, 6)
                "Yearly" -> calendar.add(Calendar.YEAR, 1)
                else -> return dates
            }
            if (calendar.after(endDate)) break
            dates += calendar.time
        }
        return dates
    }

    private fun startOfToday(): Date = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.time
}
