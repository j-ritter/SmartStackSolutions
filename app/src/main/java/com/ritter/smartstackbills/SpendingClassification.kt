package com.ritter.smartstackbills

import android.content.Context
import java.text.Normalizer
import java.util.Locale

object SpendingClassification {
    enum class Type { ESSENTIAL, NON_ESSENTIAL, UNCLASSIFIED }

    private val essentialCategories = setOf(
        "Housing & utilities",
        "Communication",
        "Insurance",
        "Transportation",
        "Financial obligations",
        "Taxes",
        "Health",
        "Education",
        "Groceries",
        "Family & childcare",
        "Pets",
        "Finances/Fees",
        // Localized labels that may exist in older/user-created documents.
        "Wohnen & Nebenkosten",
        "Kommunikation",
        "Versicherungen",
        "Transport",
        "Finanzielle Verpflichtungen",
        "Steuern",
        "Gesundheit",
        "Bildung",
        "Lebensmittel",
        "Familie & Kinderbetreuung",
        "Haustiere"
    ).normalized()

    private val nonEssentialCategories = setOf(
        "Subscriptions",
        "Subscription and Memberships",
        "Shopping & personal",
        "Shopping & consumption",
        "Travel & leisure",
        "Other",
        "Others",
        // Localized labels that may exist in older/user-created documents.
        "Abonnements",
        "Einkaufen & Persönliches",
        "Reisen & Freizeit",
        "Sonstiges",
        "Otros",
        "Autre",
        "Autres",
        "Outros",
        "Inne"
    ).normalized()

    private val nonEssentialSubcategoryOverrides = setOf(
        "Furniture",
        "Home services",
        "TV",
        "Cloud storage",
        "Travel",
        "Pet",
        "Parking & tolls",
        "Taxi & ride share",
        "Vehicle rental",
        "Professional fees",
        "Wellness",
        "Courses",
        "Professional development",
        "Beverages",
        "Dining & takeaway",
        "Family activities",
        "Supplies",
        "Streaming",
        "Music",
        "Software",
        "Memberships",
        "News & publications",
        "Flights",
        "Activities",
        "Entertainment",
        "Donations",
        "Legal",
        "Unexpected expense",
        "Miscellaneous",
        // German labels that may exist in current localized documents.
        "Möbel",
        "Haushaltsdienste",
        "Fernsehen",
        "Cloud-Speicher",
        "Reise",
        "Haustier",
        "Parken & Maut",
        "Taxi & Fahrdienste",
        "Fahrzeugmiete",
        "Honorare",
        "Kurse",
        "Berufliche Weiterbildung",
        "Getränke",
        "Restaurant & Mitnahme",
        "Familienaktivitäten",
        "Bedarf",
        "Spenden",
        "Rechtliches",
        "Unerwartete Ausgabe",
        "Verschiedenes"
    ).normalized()

    fun classify(category: String?, subcategory: String?): Type {
        val normalizedCategory = category.normalizedValue()
        val normalizedSubcategory = subcategory.normalizedValue()

        return when {
            normalizedCategory in nonEssentialCategories -> Type.NON_ESSENTIAL
            normalizedCategory in essentialCategories &&
                normalizedSubcategory in nonEssentialSubcategoryOverrides ->
                Type.NON_ESSENTIAL
            normalizedCategory in essentialCategories -> Type.ESSENTIAL
            normalizedSubcategory in nonEssentialSubcategoryOverrides -> Type.NON_ESSENTIAL
            normalizedCategory.isBlank() && normalizedSubcategory.isBlank() -> Type.UNCLASSIFIED
            else -> Type.NON_ESSENTIAL
        }
    }

    fun classify(context: Context, category: String?, subcategory: String?): Type {
        val normalizedCategory = FinancialEntryOptions.normalizedExpenseCategory(context, category)
        val normalizedSubcategory = FinancialEntryOptions.normalizedExpenseSubcategory(
            context,
            normalizedCategory,
            subcategory
        )
        return classify(normalizedCategory, normalizedSubcategory)
    }

    private fun Set<String>.normalized() = mapTo(mutableSetOf()) { it.normalizedValue() }

    private fun String?.normalizedValue(): String {
        val value = this?.trim()?.lowercase(Locale.ROOT).orEmpty()
        return Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
    }
}
