package com.ritter.smartstackbills

import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ImportedTransactionType {
    CLOSED_PAYMENT,
    INCOME
}

data class ImportedTransaction(
    val sourceRow: Int,
    val reference: String?,
    var title: String,
    var amount: Double,
    var date: Date,
    var type: ImportedTransactionType,
    var category: String,
    var subcategory: String,
    var currency: String? = null,
    var included: Boolean = true,
    var duplicate: Boolean = false,
    var needsReview: Boolean = false
) {
    fun coreFingerprint(): String {
        val dateValue = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
        val normalizedTitle = Normalizer.normalize(title, Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")
            .lowercase(Locale.ROOT)
            .replace("[^a-z0-9]+".toRegex(), " ")
            .trim()
        return listOf(
            type.name,
            dateValue,
            currency.orEmpty(),
            String.format(Locale.US, "%.2f", amount),
            normalizedTitle
        ).joinToString("|")
    }

    fun fingerprint(): String =
        "${coreFingerprint()}|${reference.orEmpty().trim().lowercase(Locale.ROOT)}"
}

data class StatementImportResult(
    val transactions: List<ImportedTransaction>,
    val detectedCurrency: String?,
    val warnings: List<String> = emptyList()
)
