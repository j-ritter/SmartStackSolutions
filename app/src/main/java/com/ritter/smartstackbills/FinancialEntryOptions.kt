package com.ritter.smartstackbills

import android.content.Context

object FinancialEntryOptions {
    val expenseCategories = arrayOf(
        "Housing & utilities", "Communication", "Insurance", "Subscriptions",
        "Transportation", "Financial obligations", "Taxes", "Health", "Education",
        "Shopping & personal", "Groceries", "Family & childcare", "Pets",
        "Travel & leisure", "Other"
    )

    val expenseSubcategories = mapOf(
        "Housing & utilities" to arrayOf("Rent", "Mortgage", "Utilities", "Maintenance & repairs", "Furniture", "Home services", "Other"),
        "Communication" to arrayOf("Mobile phone", "Internet", "TV", "Cloud storage", "Other"),
        "Insurance" to arrayOf("Health", "Life", "Vehicle", "Home", "Travel", "Pet", "Other"),
        "Subscriptions" to arrayOf("Streaming", "Music", "Software", "Memberships", "News & publications", "Other"),
        "Transportation" to arrayOf("Fuel", "Public transport", "Vehicle maintenance", "Parking & tolls", "Taxi & ride share", "Vehicle rental", "Other"),
        "Financial obligations" to arrayOf("Loan payment", "Credit card payment", "Bank fees", "Interest & charges", "Professional fees", "Other"),
        "Taxes" to arrayOf("Income tax", "Property tax", "Sales or VAT", "Business tax", "Other"),
        "Health" to arrayOf("Doctor", "Dental", "Medication", "Mental health", "Medical equipment", "Wellness", "Other"),
        "Education" to arrayOf("Tuition", "Books & supplies", "Courses", "Child education", "Professional development", "Other"),
        "Shopping & personal" to arrayOf("Clothing", "Electronics", "Household goods", "Personal care", "Gifts", "Other"),
        "Groceries" to arrayOf("Food", "Household necessities", "Beverages", "Dining & takeaway", "Other"),
        "Family & childcare" to arrayOf("Childcare", "Child support", "Baby supplies", "Family activities", "Other"),
        "Pets" to arrayOf("Food", "Veterinary care", "Insurance", "Supplies", "Other"),
        "Travel & leisure" to arrayOf("Accommodation", "Flights", "Activities", "Entertainment", "Other"),
        "Other" to arrayOf("Donations", "Legal", "Unexpected expense", "Miscellaneous")
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

    fun vendorSuggestions(context: Context, category: String): List<String> {
        val recent = context.getSharedPreferences("recent_vendors", Context.MODE_PRIVATE)
            .getStringSet("vendors", emptySet()).orEmpty().toList().sorted()
        return (recent + genericVendorSuggestions[category].orEmpty()).distinct().take(12)
    }

    fun rememberVendor(context: Context, vendor: String) {
        val value = vendor.trim()
        if (value.isBlank()) return
        val prefs = context.getSharedPreferences("recent_vendors", Context.MODE_PRIVATE)
        val updated = (prefs.getStringSet("vendors", emptySet()).orEmpty() + value)
            .toList().takeLast(20).toSet()
        prefs.edit().putStringSet("vendors", updated).apply()
    }

    val incomeCategories = arrayOf(
        "Salary & wages", "Freelance & self-employment", "Business income",
        "Rental income", "Investment income", "Pension & retirement",
        "Benefits & support", "Scholarships & grants", "Royalties",
        "Gifts & inheritance", "Refunds & reimbursements", "Other income"
    )

    val incomeSubcategories = mapOf(
        "Salary & wages" to arrayOf("Regular salary", "Overtime", "Bonus", "Other"),
        "Freelance & self-employment" to arrayOf("Service fee", "Project income", "Commission", "Other"),
        "Business income" to arrayOf("Business profit", "Profit share", "Owner payment", "Other"),
        "Rental income" to arrayOf("Property", "Vehicle", "Equipment", "Other"),
        "Investment income" to arrayOf("Dividend", "Interest", "Capital gain", "Other"),
        "Pension & retirement" to arrayOf("State pension", "Employer pension", "Private pension", "Other"),
        "Benefits & support" to arrayOf("Unemployment", "Sickness", "Parental", "Child support", "Other"),
        "Scholarships & grants" to arrayOf("Scholarship", "Research grant", "Other"),
        "Royalties" to arrayOf("Books", "Music", "Software", "Patent", "Other"),
        "Gifts & inheritance" to arrayOf("Gift", "Inheritance", "Other"),
        "Refunds & reimbursements" to arrayOf("Purchase refund", "Tax refund", "Expense reimbursement", "Other"),
        "Other income" to arrayOf("Sale of personal item", "Crowdfunding return", "Miscellaneous")
    )
}
