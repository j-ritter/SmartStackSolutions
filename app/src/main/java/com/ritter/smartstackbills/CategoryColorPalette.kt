package com.ritter.smartstackbills

import android.content.Context

object CategoryColorPalette {
    private val categoryColors = mapOf(
        "Housing & utilities" to R.color.income_color,
        "Communication" to R.color.bill_color,
        "Insurance" to R.color.savings_color,
        "Subscriptions" to R.color.unknown_color,
        "Transportation" to R.color.spending_color,
        "Financial obligations" to R.color.income_color,
        "Taxes" to R.color.bill_color,
        "Health" to R.color.savings_color,
        "Education" to R.color.unknown_color,
        "Shopping & personal" to R.color.spending_color,
        "Groceries" to R.color.spending_color,
        "Family & childcare" to R.color.income_color,
        "Pets" to R.color.savings_color,
        "Travel & leisure" to R.color.bill_color,
        "Other" to R.color.unknown_color
    )

    fun colorFor(context: Context, category: String): Int {
        val key = FinancialEntryOptions.canonicalCategoryKey(context, category)
        return categoryColors[key] ?: R.color.unknown_color
    }
}
