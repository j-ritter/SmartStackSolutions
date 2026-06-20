package com.ritter.smartstackbills

import java.util.Locale

object SpendingClassification {
    enum class Type { ESSENTIAL, NON_ESSENTIAL, UNCLASSIFIED }

    private val nonEssentialCategories = setOf(
        "Subscriptions", "Subscription and Memberships", "Travel & leisure", "Others"
    ).normalized()
    private val nonEssentialSubcategories = setOf(
        "Furniture", "Streaming", "Music", "Memberships", "News & publications",
        "Wellness", "Entertainment", "Family activities", "Activities"
    ).normalized()
    private val essentialCategories = setOf(
        "Housing & utilities", "Communication", "Insurance", "Transportation",
        "Financial obligations", "Taxes", "Health", "Education", "Groceries",
        "Family & childcare", "Pets", "Accommodation", "Finances/Fees"
    ).normalized()

    fun classify(category: String?, subcategory: String?): Type {
        val normalizedSubcategory = subcategory.normalizedValue()
        if (normalizedSubcategory in nonEssentialSubcategories) return Type.NON_ESSENTIAL
        val normalizedCategory = category.normalizedValue()
        return when {
            normalizedCategory in nonEssentialCategories -> Type.NON_ESSENTIAL
            normalizedCategory in essentialCategories -> Type.ESSENTIAL
            normalizedCategory == "shopping & personal" ||
                normalizedCategory == "shopping & consumption" ||
                normalizedCategory == "other" ->
                Type.UNCLASSIFIED
            else -> Type.UNCLASSIFIED
        }
    }

    private fun Set<String>.normalized() = mapTo(mutableSetOf()) { it.lowercase(Locale.ROOT) }
    private fun String?.normalizedValue() = this?.trim()?.lowercase(Locale.ROOT).orEmpty()
}
