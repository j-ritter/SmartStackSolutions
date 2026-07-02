package com.ritter.smartstackbills

import android.content.Context
import androidx.annotation.ArrayRes

object FinancialEntryOptions {
    const val DEFAULT_EXPENSE_CATEGORY = "Other"
    const val DEFAULT_EXPENSE_SUBCATEGORY = "Miscellaneous"
    const val DEFAULT_INCOME_CATEGORY = "Salary & wages"
    const val DEFAULT_INCOME_SUBCATEGORY = "Regular salary"

    data class Option(val key: String, val label: String) {
        override fun toString(): String = label
    }

    private val expenseCategoryKeys = listOf(
        "Housing & utilities", "Communication", "Insurance", "Subscriptions",
        "Transportation", "Financial obligations", "Taxes", "Health", "Education",
        "Shopping & personal", "Groceries", "Family & childcare", "Pets",
        "Travel & leisure", "Other"
    )

    private val expenseSubcategoryKeys = linkedMapOf(
        "Housing & utilities" to listOf("Rent", "Mortgage", "Utilities", "Maintenance & repairs", "Furniture", "Home services", "Other"),
        "Communication" to listOf("Mobile phone", "Internet", "TV", "Cloud storage", "Other"),
        "Insurance" to listOf("Health", "Life", "Vehicle", "Home", "Travel", "Pet", "Other"),
        "Subscriptions" to listOf("Streaming", "Music", "Software", "Memberships", "News & publications", "Other"),
        "Transportation" to listOf("Fuel", "Public transport", "Vehicle maintenance", "Parking & tolls", "Taxi & ride share", "Vehicle rental", "Other"),
        "Financial obligations" to listOf("Loan payment", "Credit card payment", "Bank fees", "Interest & charges", "Professional fees", "Other"),
        "Taxes" to listOf("Income tax", "Property tax", "Sales or VAT", "Business tax", "Other"),
        "Health" to listOf("Doctor", "Dental", "Medication", "Mental health", "Medical equipment", "Wellness", "Other"),
        "Education" to listOf("Tuition", "Books & supplies", "Courses", "Child education", "Professional development", "Other"),
        "Shopping & personal" to listOf("Clothing", "Electronics", "Household goods", "Personal care", "Gifts", "Other"),
        "Groceries" to listOf("Food", "Household necessities", "Beverages", "Dining & takeaway", "Other"),
        "Family & childcare" to listOf("Childcare", "Child support", "Baby supplies", "Family activities", "Other"),
        "Pets" to listOf("Food", "Veterinary care", "Insurance", "Supplies", "Other"),
        "Travel & leisure" to listOf("Accommodation", "Flights", "Activities", "Entertainment", "Other"),
        "Other" to listOf("Donations", "Legal", "Unexpected expense", "Miscellaneous")
    )

    private val expenseSubcategoryResources = mapOf(
        "Housing & utilities" to R.array.expense_subcategories_housing,
        "Communication" to R.array.expense_subcategories_communication,
        "Insurance" to R.array.expense_subcategories_insurance,
        "Subscriptions" to R.array.expense_subcategories_subscriptions,
        "Transportation" to R.array.expense_subcategories_transportation,
        "Financial obligations" to R.array.expense_subcategories_financial,
        "Taxes" to R.array.expense_subcategories_taxes,
        "Health" to R.array.expense_subcategories_health,
        "Education" to R.array.expense_subcategories_education,
        "Shopping & personal" to R.array.expense_subcategories_shopping,
        "Groceries" to R.array.expense_subcategories_groceries,
        "Family & childcare" to R.array.expense_subcategories_family,
        "Pets" to R.array.expense_subcategories_pets,
        "Travel & leisure" to R.array.expense_subcategories_travel,
        "Other" to R.array.expense_subcategories_other
    )

    private val incomeCategoryKeys = listOf(
        "Salary & wages", "Freelance & self-employment", "Business income",
        "Rental income", "Investment income", "Pension & retirement",
        "Benefits & support", "Scholarships & grants", "Royalties",
        "Gifts & inheritance", "Refunds & reimbursements", "Other income"
    )

    private val incomeSubcategoryKeys = linkedMapOf(
        "Salary & wages" to listOf("Regular salary", "Overtime", "Bonus", "Other"),
        "Freelance & self-employment" to listOf("Service fee", "Project income", "Commission", "Other"),
        "Business income" to listOf("Business profit", "Profit share", "Owner payment", "Other"),
        "Rental income" to listOf("Property", "Vehicle", "Equipment", "Other"),
        "Investment income" to listOf("Dividend", "Interest", "Capital gain", "Other"),
        "Pension & retirement" to listOf("State pension", "Employer pension", "Private pension", "Other"),
        "Benefits & support" to listOf("Unemployment", "Sickness", "Parental", "Child support", "Other"),
        "Scholarships & grants" to listOf("Scholarship", "Research grant", "Other"),
        "Royalties" to listOf("Books", "Music", "Software", "Patent", "Other"),
        "Gifts & inheritance" to listOf("Gift", "Inheritance", "Other"),
        "Refunds & reimbursements" to listOf("Purchase refund", "Tax refund", "Expense reimbursement", "Other"),
        "Other income" to listOf("Sale of personal item", "Crowdfunding return", "Miscellaneous")
    )

    private val incomeSubcategoryResources = mapOf(
        "Salary & wages" to R.array.income_subcategories_salary,
        "Freelance & self-employment" to R.array.income_subcategories_freelance,
        "Business income" to R.array.income_subcategories_business,
        "Rental income" to R.array.income_subcategories_rental,
        "Investment income" to R.array.income_subcategories_investment,
        "Pension & retirement" to R.array.income_subcategories_pension,
        "Benefits & support" to R.array.income_subcategories_benefits,
        "Scholarships & grants" to R.array.income_subcategories_scholarships,
        "Royalties" to R.array.income_subcategories_royalties,
        "Gifts & inheritance" to R.array.income_subcategories_gifts,
        "Refunds & reimbursements" to R.array.income_subcategories_refunds,
        "Other income" to R.array.income_subcategories_other
    )

    private val genericVendorSuggestions = mapOf(
        "Housing & utilities" to listOf("Landlord", "Utility provider", "Property manager"),
        "Communication" to listOf("Mobile provider", "Internet provider"),
        "Insurance" to listOf("Insurance provider"),
        "Subscriptions" to listOf("Subscription provider"),
        "Transportation" to listOf("Fuel station", "Transport provider"),
        "Financial obligations" to listOf("Bank", "Credit card provider", "Lender"),
        "Taxes" to listOf("Tax authority"),
        "Health" to listOf("Clinic", "Pharmacy", "Hospital"),
        "Education" to listOf("School", "University", "Course provider"),
        "Groceries" to listOf("Supermarket", "Grocery store"),
        "Family & childcare" to listOf("Childcare provider"),
        "Pets" to listOf("Veterinary clinic", "Pet store"),
        "Travel & leisure" to listOf("Travel provider")
    )

    fun expenseCategories(context: Context): List<Option> =
        options(context, expenseCategoryKeys, R.array.expense_category_labels)

    fun expenseSubcategories(context: Context, category: String): List<Option> {
        val key = canonicalCategory(context, category)
        return options(
            context,
            expenseSubcategoryKeys[key].orEmpty(),
            expenseSubcategoryResources[key]
        )
    }

    fun incomeCategories(context: Context): List<Option> =
        options(context, incomeCategoryKeys, R.array.income_category_labels)

    fun incomeSubcategories(context: Context, category: String): List<Option> {
        val key = canonicalCategory(context, category)
        return options(
            context,
            incomeSubcategoryKeys[key].orEmpty(),
            incomeSubcategoryResources[key]
        )
    }

    @JvmStatic
    fun displayCategory(context: Context, value: String?): String {
        val key = value?.trim().orEmpty()
        if (key.isBlank() || key == "-") return context.getString(R.string.uncategorized)
        return (expenseCategories(context) + incomeCategories(context))
            .firstOrNull { it.key == key || it.label == key }
            ?.label
            ?: key
    }

    @JvmStatic
    fun displaySubcategory(context: Context, category: String?, value: String?): String {
        val key = value?.trim().orEmpty()
        if (key.isBlank() || key == "-") return context.getString(R.string.uncategorized)
        val categoryKey = canonicalCategory(context, category.orEmpty())
        val categoryOptions = when {
            categoryKey in expenseSubcategoryKeys -> expenseSubcategories(context, categoryKey)
            categoryKey in incomeSubcategoryKeys -> incomeSubcategories(context, categoryKey)
            else -> emptyList()
        }
        return categoryOptions.firstOrNull { it.key == key || it.label == key }?.label ?: key
    }

    fun vendorSuggestions(context: Context, category: String): List<String> {
        val categoryKey = canonicalCategory(context, category)
        val recent = context.getSharedPreferences("recent_vendors", Context.MODE_PRIVATE)
            .getStringSet("vendors", emptySet()).orEmpty().toList().sorted()
        return (recent + genericVendorSuggestions[categoryKey].orEmpty()).distinct().take(12)
    }

    fun rememberVendor(context: Context, vendor: String) {
        val value = vendor.trim()
        if (value.isBlank()) return
        val prefs = context.getSharedPreferences("recent_vendors", Context.MODE_PRIVATE)
        val updated = (prefs.getStringSet("vendors", emptySet()).orEmpty() + value)
            .toList().takeLast(20).toSet()
        prefs.edit().putStringSet("vendors", updated).apply()
    }

    fun selectedKey(value: Any?): String =
        (value as? Option)?.key ?: value?.toString().orEmpty()

    @JvmStatic
    fun canonicalCategoryKey(context: Context, value: String): String =
        canonicalCategory(context, value)

    fun normalizedExpenseCategory(context: Context, value: String?): String =
        canonicalCategory(context, value.orEmpty())
            .takeIf { it in expenseCategoryKeys }
            ?: DEFAULT_EXPENSE_CATEGORY

    fun normalizedExpenseSubcategory(context: Context, category: String?, value: String?): String {
        val categoryKey = normalizedExpenseCategory(context, category)
        val subcategory = value?.trim().orEmpty()
        val options = expenseSubcategories(context, categoryKey)
        return options.firstOrNull { it.key == subcategory || it.label == subcategory }?.key
            ?: if (categoryKey == DEFAULT_EXPENSE_CATEGORY) {
                DEFAULT_EXPENSE_SUBCATEGORY
            } else {
                options.lastOrNull()?.key ?: DEFAULT_EXPENSE_SUBCATEGORY
            }
    }

    fun normalizedIncomeCategory(context: Context, value: String?): String =
        canonicalCategory(context, value.orEmpty())
            .takeIf { it in incomeCategoryKeys }
            ?: DEFAULT_INCOME_CATEGORY

    fun normalizedIncomeSubcategory(context: Context, category: String?, value: String?): String {
        val categoryKey = normalizedIncomeCategory(context, category)
        val subcategory = value?.trim().orEmpty()
        val options = incomeSubcategories(context, categoryKey)
        return options.firstOrNull { it.key == subcategory || it.label == subcategory }?.key
            ?: if (categoryKey == DEFAULT_INCOME_CATEGORY) {
                DEFAULT_INCOME_SUBCATEGORY
            } else {
                options.lastOrNull()?.key ?: DEFAULT_INCOME_SUBCATEGORY
            }
    }

    private fun canonicalCategory(context: Context, value: String): String {
        val normalized = value.trim()
        return (expenseCategories(context) + incomeCategories(context))
            .firstOrNull { it.key == normalized || it.label == normalized }
            ?.key
            ?: normalized
    }

    private fun options(
        context: Context,
        keys: List<String>,
        @ArrayRes labelsResource: Int?
    ): List<Option> {
        if (labelsResource == null) return emptyList()
        val labels = context.resources.getStringArray(labelsResource)
        return keys.mapIndexed { index, key ->
            Option(key, labels.getOrNull(index) ?: key)
        }
    }
}
