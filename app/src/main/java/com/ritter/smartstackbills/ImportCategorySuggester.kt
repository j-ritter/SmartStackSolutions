package com.ritter.smartstackbills

import java.text.Normalizer
import java.util.Locale

object ImportCategorySuggester {
    data class Suggestion(val category: String, val subcategory: String)

    fun suggest(type: ImportedTransactionType, title: String): Suggestion {
        val value = normalize(title)
        if (type == ImportedTransactionType.INCOME) {
            return when {
                value.containsAny("nomina", "payroll", "salary", "wage", "gehalt", "sueldo") ->
                    Suggestion("Salary & wages", "Regular salary")
                value.containsAny("refund", "reembolso", "devolucion", "erstattung") ->
                    Suggestion("Refunds & reimbursements", "Purchase refund")
                value.containsAny("interest", "interes", "zinsen", "dividend") ->
                    Suggestion("Investment income", "Interest")
                else -> Suggestion("Other income", "Miscellaneous")
            }
        }
        return when {
            value.containsAny(
                "oxxo", "walmart", "mercado", "supermercado", "panaderia",
                "tortilleria", "grocery", "supermarket", "aldi", "lidl"
            ) -> Suggestion("Groceries", "Food")
            value.containsAny(
                "gasolin", "gaservicio", "uber", "didi", "taxi",
                "parking", "estacionamiento", "toll", "peaje"
            ) -> Suggestion("Transportation", "Fuel")
            value.containsAny("netflix", "spotify", "disney", "prime video", "youtube premium") ->
                Suggestion("Subscriptions", "Streaming")
            value.containsAny("telcel", "movistar", "internet", "telefono", "phone") ->
                Suggestion("Communication", "Mobile phone")
            value.containsAny("farmacia", "pharmacy", "hospital", "clinic", "doctor") ->
                Suggestion("Health", "Medication")
            value.containsAny("renta", "rent", "mortgage", "hipoteca", "electric", "water bill") ->
                Suggestion("Housing & utilities", "Rent")
            value.containsAny("restaurant", "restaurante", "cafe", "coffee", "tacos") ->
                Suggestion("Groceries", "Dining & takeaway")
            else -> Suggestion("Other", "Miscellaneous")
        }
    }

    fun requiresReview(title: String): Boolean {
        val value = normalize(title)
        return value.containsAny(
            "transfer", "transferencia", "spei", "wire",
            "credit card payment", "pago tarjeta", "refund", "reembolso",
            "devolucion", "cash withdrawal", "retiro cajero", "atm"
        )
    }

    private fun normalize(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")
            .lowercase(Locale.ROOT)
            .replace("[^a-z0-9]+".toRegex(), " ")
            .trim()

    private fun String.containsAny(vararg candidates: String): Boolean =
        candidates.any(::contains)
}
