package com.ritter.smartstackbills

import java.text.SimpleDateFormat
import java.util.Locale

object DocumentTextParser {
    private val amountPattern = Regex(
        """(?i)(?:USD|EUR|GBP|JPY|CNY|CAD|AUD|CHF|INR|MXN|BRL|KRW|SGD|SEK|PLN|\$|€|£|¥)?\s*(-?\d[\d\s.'’]*[.,]\d{2})"""
    )
    private val numericDatePattern = Regex(
        """\b(\d{1,4}[-/.]\d{1,2}[-/.]\d{1,4})\b"""
    )
    private val currencyCodePattern = Regex(
        """\b(USD|EUR|GBP|JPY|CNY|CAD|AUD|CHF|INR|MXN|BRL|KRW|SGD|SEK|PLN)\b""",
        RegexOption.IGNORE_CASE
    )
    private val amountPriority = listOf(
        "amount due", "balance due", "total due", "grand total", "net pay", "net amount",
        "zahlbetrag", "gesamtbetrag", "fällig", "importe total", "total a pagar",
        "total à payer", "montant net", "kwota do zapłaty", "do zapłaty",
        "valor total", "total a pagar", "netto", "total"
    )
    private val amountPenalty = listOf(
        "subtotal", "sub-total", "tax", "vat", "iva", "mwst", "ust", "discount",
        "change", "cash", "gross", "brutto", "deduction"
    )
    private val dueDateKeywords = listOf(
        "due date", "payment due", "pay by", "fällig", "zahlbar bis", "vencimiento",
        "fecha límite", "échéance", "à payer avant", "termin płatności", "vencimento"
    )
    private val genericDateKeywords = listOf(
        "date", "datum", "fecha", "data", "received", "paid", "payment", "purchase"
    )
    private val ignoredPartyWords = listOf(
        "invoice", "receipt", "statement", "tax invoice", "rechnung", "quittung",
        "factura", "recibo", "faktura", "paragon", "total", "date", "amount"
    )

    fun parse(text: String, type: EntryType): ScanPrefill {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        val amount = findAmount(lines, type)
        val date = findDate(lines, type)
        val party = findParty(lines)
        val currency = findCurrency(text)
        return ScanPrefill(
            title = party,
            amount = amount?.let { CurrencyPreferences.formatPlain(it) },
            date = date,
            party = party,
            attachmentUri = null,
            rawText = text.take(12_000),
            currency = currency
        )
    }

    private fun findCurrency(text: String): String? {
        currencyCodePattern.find(text)?.value?.uppercase(Locale.ROOT)?.let { return it }
        return when {
            text.contains('$') -> "USD"
            text.contains('€') || text.contains("â‚¬") -> "EUR"
            text.contains('£') || text.contains("Â£") -> "GBP"
            text.contains('¥') || text.contains("Â¥") -> "JPY"
            else -> null
        }
    }

    private fun findAmount(lines: List<String>, type: EntryType): Double? {
        val candidates = mutableListOf<Pair<Double, Int>>()
        lines.forEachIndexed { index, line ->
            val lower = line.lowercase(Locale.ROOT)
            amountPattern.findAll(line).forEach { match ->
                val value = parseAmount(match.groupValues[1])
                if (value != null) {
                    var score = amountPriority.indexOfFirst { lower.contains(it) }
                        .let { if (it >= 0) 120 - it else 0 }
                    if (amountPenalty.any { lower.contains(it) }) score -= 65
                    if (line.contains(Regex("""[$€£¥]"""))) score += 12
                    if (type == EntryType.INCOME && lower.contains("net")) score += 45
                    score += index.coerceAtMost(30)
                    candidates += value to score
                }
            }
        }
        return candidates.maxWithOrNull(
            compareBy<Pair<Double, Int>> { it.second }.thenBy { it.first }
        )?.first
    }

    private fun parseAmount(raw: String): Double? {
        val cleaned = raw.replace(" ", "").replace("'", "").replace("’", "")
        val lastComma = cleaned.lastIndexOf(',')
        val lastDot = cleaned.lastIndexOf('.')
        val decimalIndex = maxOf(lastComma, lastDot)
        val normalized = buildString {
            cleaned.forEachIndexed { index, char ->
                when {
                    char.isDigit() || (char == '-' && isEmpty()) -> append(char)
                    index == decimalIndex -> append('.')
                }
            }
        }
        return normalized.toDoubleOrNull()?.let { kotlin.math.abs(it) }
    }

    private fun findDate(lines: List<String>, type: EntryType): String? {
        val candidates = mutableListOf<Pair<String, Int>>()
        lines.forEachIndexed { index, line ->
            val lower = line.lowercase(Locale.ROOT)
            numericDatePattern.findAll(line).forEach { match ->
                val formatted = normalizeDate(match.value)
                if (formatted != null) {
                    var score = if (
                        type == EntryType.OPEN_PAYMENT &&
                        dueDateKeywords.any { lower.contains(it) }
                    ) 120 else 0
                    if (genericDateKeywords.any { lower.contains(it) }) score += 35
                    score -= index
                    candidates += formatted to score
                }
            }
        }
        return candidates.maxByOrNull { it.second }?.first
    }

    private fun normalizeDate(raw: String): String? {
        val formats = listOf(
            "dd/MM/yyyy", "d/M/yyyy", "dd.MM.yyyy", "d.M.yyyy",
            "dd-MM-yyyy", "d-M-yyyy", "yyyy-MM-dd", "yyyy/MM/dd",
            "MM/dd/yyyy", "M/d/yyyy"
        )
        formats.forEach { pattern ->
            val parser = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }
            runCatching { parser.parse(raw) }.getOrNull()?.let {
                if (ScanDateValidator.isReasonable(it.time)) {
                    return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it)
                }
            }
        }
        return null
    }

    private fun findParty(lines: List<String>): String? =
        lines.firstOrNull { line ->
            val lower = line.lowercase(Locale.ROOT)
            line.length in 3..55 &&
                line.any(Char::isLetter) &&
                !line.contains('@') &&
                !line.matches(Regex(""".*\d{5,}.*""")) &&
                ignoredPartyWords.none { lower == it || lower.startsWith("$it ") }
        }
}
