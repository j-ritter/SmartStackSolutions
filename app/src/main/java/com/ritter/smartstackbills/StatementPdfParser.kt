package com.ritter.smartstackbills

import android.graphics.Rect
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class OcrStatementElement(
    val page: Int,
    val text: String,
    val bounds: Rect,
    val pageWidth: Int,
    val pageHeight: Int
) {
    val centerX: Float get() = bounds.centerX().toFloat() / pageWidth
}

object StatementPdfParser {
    private const val MAX_IMPORTED_ROWS = 200
    private val dateRegex = Regex(
        """\b(\d{1,2})[-/ ](JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC|ENE|ABR|AGO|DIC|\d{1,2})[-/ ](\d{2,4})\b""",
        RegexOption.IGNORE_CASE
    )

    fun parse(elements: List<OcrStatementElement>): StatementImportResult {
        val allText = elements.joinToString(" ") { it.text }.uppercase(Locale.ROOT)
        val isSantanderMexico = allText.contains("SANTANDER") &&
            (allText.contains("RETIRO") || allText.contains("RETIROS")) &&
            (allText.contains("DEPOSITO") || allText.contains("DEPÓSITO"))
        val currency = when {
            allText.contains("MXN") || allText.contains("BANCO SANTANDER MEXICO") ||
                allText.contains("BANCO SANTANDER MÉXICO") -> "MXN"
            allText.contains(" USD ") -> "USD"
            allText.contains(" EUR ") -> "EUR"
            else -> null
        }
        val transactions = if (isSantanderMexico) {
            parseSantanderMexico(elements)
        } else {
            parseGeneric(elements)
        }
        val warnings = buildList {
            if (!isSantanderMexico) {
                add("This PDF format is not yet recognized; all detected rows require review.")
            }
            if (transactions.size >= MAX_IMPORTED_ROWS) {
                add("Only the first $MAX_IMPORTED_ROWS transactions are shown.")
            }
        }
        return StatementImportResult(transactions.take(MAX_IMPORTED_ROWS), currency, warnings)
    }

    private fun parseSantanderMexico(elements: List<OcrStatementElement>): List<ImportedTransaction> {
        val result = mutableListOf<ImportedTransaction>()
        var previousBalance: Double? = null
        elements.groupBy { it.page }.toSortedMap().forEach { (page, pageElements) ->
            val sorted = pageElements.sortedWith(compareBy({ it.bounds.top }, { it.bounds.left }))
            val dateElements = sorted.filter { element ->
                element.centerX < 0.14f && extractDate(element.text) != null
            }
            dateElements.forEachIndexed { index, dateElement ->
                val date = extractDate(dateElement.text) ?: return@forEachIndexed
                val nextTop = dateElements.getOrNull(index + 1)?.bounds?.top ?: dateElement.pageHeight
                val rowTop = dateElement.bounds.top - 5
                val rowElements = sorted.filter {
                    it.bounds.centerY() >= rowTop && it.bounds.centerY() < nextTop
                }
                val deposit = rowElements
                    .filter { it.centerX in 0.585f..0.705f }
                    .mapNotNull { parseAmount(it.text) }
                    .firstOrNull { it > 0.0 }
                val withdrawal = rowElements
                    .filter { it.centerX in 0.705f..0.815f }
                    .mapNotNull { parseAmount(it.text) }
                    .firstOrNull { it > 0.0 }
                val balance = rowElements
                    .filter { it.centerX in 0.815f..0.97f }
                    .mapNotNull { parseAmount(it.text) }
                    .firstOrNull()
                val amount = withdrawal ?: deposit ?: return@forEachIndexed
                val type = if (withdrawal != null) {
                    ImportedTransactionType.CLOSED_PAYMENT
                } else {
                    ImportedTransactionType.INCOME
                }
                val description = rowElements
                    .filter { it.centerX in 0.185f..0.585f }
                    .sortedWith(compareBy({ it.bounds.top }, { it.bounds.left }))
                    .joinToString(" ") { it.text }
                    .cleanDescription()
                if (description.isBlank()) return@forEachIndexed
                val reference = rowElements
                    .filter { it.centerX in 0.13f..0.185f && it !== dateElement }
                    .joinToString("") { it.text }
                    .filter(Char::isLetterOrDigit)
                    .ifBlank { null }
                val suggestion = ImportCategorySuggester.suggest(type, description)
                val balanceMismatch = previousBalance?.let { prior ->
                    balance?.let { current ->
                        val expected = prior + (deposit ?: 0.0) - (withdrawal ?: 0.0)
                        kotlin.math.abs(expected - current) > 0.02
                    }
                } ?: false
                result += ImportedTransaction(
                    sourceRow = (page * 1000) + index,
                    reference = reference,
                    title = description,
                    amount = CurrencyPreferences.roundToTwoDecimals(amount),
                    date = date,
                    type = type,
                    category = suggestion.category,
                    subcategory = suggestion.subcategory,
                    needsReview = balanceMismatch ||
                        ImportCategorySuggester.requiresReview(description)
                )
                if (balance != null) previousBalance = balance
            }
        }
        return result
    }

    private fun parseGeneric(elements: List<OcrStatementElement>): List<ImportedTransaction> {
        val result = mutableListOf<ImportedTransaction>()
        elements.groupBy { it.page }.toSortedMap().forEach { (page, pageElements) ->
            val sorted = pageElements.sortedWith(compareBy({ it.bounds.top }, { it.bounds.left }))
            val dateElements = sorted.filter { extractDate(it.text) != null }
            dateElements.forEachIndexed { index, dateElement ->
                val date = extractDate(dateElement.text) ?: return@forEachIndexed
                val nextTop = dateElements.getOrNull(index + 1)?.bounds?.top ?: dateElement.pageHeight
                val row = sorted.filter {
                    it.bounds.centerY() >= dateElement.bounds.top - 5 &&
                        it.bounds.centerY() < nextTop
                }
                val amountElements = row.mapNotNull { element ->
                    parseAmount(element.text)?.let { amount -> element to amount }
                }.filter { it.second != 0.0 }
                val candidate = amountElements
                    .filter { it.first.centerX > 0.55f }
                    .minByOrNull { it.first.centerX }
                    ?: return@forEachIndexed
                val signedAmount = candidate.second
                val description = row
                    .filter { it.centerX in 0.12f..0.65f && it !== dateElement }
                    .joinToString(" ") { it.text }
                    .cleanDescription()
                if (description.isBlank()) return@forEachIndexed
                val type = if (signedAmount < 0) {
                    ImportedTransactionType.CLOSED_PAYMENT
                } else {
                    ImportedTransactionType.INCOME
                }
                val suggestion = ImportCategorySuggester.suggest(type, description)
                result += ImportedTransaction(
                    sourceRow = (page * 1000) + index,
                    reference = null,
                    title = description,
                    amount = CurrencyPreferences.roundToTwoDecimals(kotlin.math.abs(signedAmount)),
                    date = date,
                    type = type,
                    category = suggestion.category,
                    subcategory = suggestion.subcategory,
                    needsReview = true
                )
            }
        }
        return result
    }

    private fun extractDate(value: String): Date? {
        val match = dateRegex.find(
            normalize(value).uppercase(Locale.ROOT)
        ) ?: return null
        val day = match.groupValues[1].toIntOrNull() ?: return null
        val monthValue = match.groupValues[2]
        val year = match.groupValues[3].toIntOrNull()?.let {
            if (it < 100) 2000 + it else it
        } ?: return null
        val month = monthValue.toIntOrNull() ?: mapOf(
            "JAN" to 1, "ENE" to 1, "FEB" to 2, "MAR" to 3,
            "APR" to 4, "ABR" to 4, "MAY" to 5, "JUN" to 6,
            "JUL" to 7, "AUG" to 8, "AGO" to 8, "SEP" to 9,
            "OCT" to 10, "NOV" to 11, "DEC" to 12, "DIC" to 12
        )[monthValue] ?: return null
        return runCatching {
            SimpleDateFormat("dd/MM/yyyy", Locale.US).apply {
                isLenient = false
            }.parse(String.format(Locale.US, "%02d/%02d/%04d", day, month, year))
        }.getOrNull()
    }

    private fun parseAmount(value: String): Double? {
        val match = Regex("""[-+]?\$?\s*\d[\d,.]*\d|[-+]?\$?\s*\d""")
            .find(value.replace(" ", ""))?.value ?: return null
        var cleaned = match.replace("$", "")
        val lastComma = cleaned.lastIndexOf(',')
        val lastDot = cleaned.lastIndexOf('.')
        cleaned = when {
            lastComma > lastDot -> cleaned.replace(".", "").replace(',', '.')
            else -> cleaned.replace(",", "")
        }
        return cleaned.toDoubleOrNull()
    }

    private fun normalize(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")

    private fun String.cleanDescription(): String =
        replace("\\s+".toRegex(), " ")
            .replace(Regex("""\b\d{1,2}(?:JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC|ENE|ABR|AGO|DIC)\d{2,4}\b""", RegexOption.IGNORE_CASE), "")
            .trim()
            .take(180)
}
