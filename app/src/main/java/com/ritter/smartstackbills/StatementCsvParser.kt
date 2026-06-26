package com.ritter.smartstackbills

import java.io.InputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StatementCsvParser {
    private const val MAX_IMPORTED_ROWS = 200
    private const val MAX_CSV_BYTES = 10 * 1024 * 1024

    private val dateHeaders = setOf("date", "fecha", "datum", "data")
    private val descriptionHeaders = setOf(
        "description", "descripcion", "descripción", "concepto", "details",
        "detalle", "merchant", "payee", "beneficiary", "memo", "beschreibung"
    )
    private val debitHeaders = setOf(
        "debit", "withdrawal", "withdrawals", "retiro", "retiros", "cargo",
        "cargos", "charge", "charges", "debe", "ausgabe", "belastung"
    )
    private val creditHeaders = setOf(
        "credit", "deposit", "deposits", "deposito", "depósito", "depositos",
        "depósitos", "abono", "abonos", "haber", "income", "eingang", "gutschrift"
    )
    private val amountHeaders = setOf("amount", "importe", "monto", "betrag", "valor")
    private val referenceHeaders = setOf(
        "reference", "referencia", "folio", "transaction id", "id transaccion",
        "id transacción", "beleg", "referenz"
    )
    private val currencyHeaders = setOf("currency", "moneda", "wahrung", "währung", "devise")
    private val typeHeaders = setOf("type", "tipo", "transaction type", "movement", "movimiento")

    fun parse(input: InputStream): StatementImportResult {
        val bytes = readLimited(input)
        val text = decode(bytes)
        val delimiter = detectDelimiter(text)
        val rows = parseRows(text, delimiter)
            .map { row -> row.map { it.trim() } }
            .filter { row -> row.any(String::isNotBlank) }
        if (rows.isEmpty()) return StatementImportResult(emptyList(), null)

        val headerIndex = rows.take(20).indexOfFirst(::looksLikeHeader)
        if (headerIndex < 0) {
            return StatementImportResult(
                emptyList(),
                null,
                listOf("No supported transaction header was found.")
            )
        }

        val headers = rows[headerIndex].map(::normalizeHeader)
        val dateIndex = findIndex(headers, dateHeaders)
        val descriptionIndex = findIndex(headers, descriptionHeaders)
        val debitIndex = findIndex(headers, debitHeaders)
        val creditIndex = findIndex(headers, creditHeaders)
        val amountIndex = findIndex(headers, amountHeaders)
        val referenceIndex = findIndex(headers, referenceHeaders)
        val currencyIndex = findIndex(headers, currencyHeaders)
        val typeIndex = findIndex(headers, typeHeaders)

        if (dateIndex < 0 || descriptionIndex < 0 ||
            (debitIndex < 0 && creditIndex < 0 && amountIndex < 0)
        ) {
            return StatementImportResult(
                emptyList(),
                null,
                listOf("The date, description, or amount columns could not be identified.")
            )
        }

        var currency: String? = null
        val transactions = mutableListOf<ImportedTransaction>()
        rows.drop(headerIndex + 1).forEachIndexed { index, row ->
            if (transactions.size >= MAX_IMPORTED_ROWS) return@forEachIndexed
            val date = parseDate(row.valueAt(dateIndex)) ?: return@forEachIndexed
            val description = row.valueAt(descriptionIndex).cleanDescription()
            if (description.isBlank()) return@forEachIndexed

            currency = currency ?: currencyFrom(row.valueAt(currencyIndex))
            val debit = parseAmount(row.valueAt(debitIndex))
            val credit = parseAmount(row.valueAt(creditIndex))
            val signedAmount = parseAmount(row.valueAt(amountIndex))
            val typeText = normalizeHeader(row.valueAt(typeIndex))

            val suggestedType: ImportedTransactionType
            val amount: Double
            var classificationNeedsReview = false
            when {
                debit != null && debit != 0.0 -> {
                    suggestedType = ImportedTransactionType.CLOSED_PAYMENT
                    amount = kotlin.math.abs(debit)
                }
                credit != null && credit != 0.0 -> {
                    suggestedType = ImportedTransactionType.INCOME
                    amount = kotlin.math.abs(credit)
                }
                signedAmount != null && signedAmount < 0 -> {
                    suggestedType = ImportedTransactionType.CLOSED_PAYMENT
                    amount = kotlin.math.abs(signedAmount)
                }
                signedAmount != null && signedAmount > 0 -> {
                    suggestedType = when {
                        typeText.containsAny(debitHeaders) -> ImportedTransactionType.CLOSED_PAYMENT
                        typeText.containsAny(creditHeaders) -> ImportedTransactionType.INCOME
                        else -> {
                            classificationNeedsReview = true
                            ImportedTransactionType.INCOME
                        }
                    }
                    amount = kotlin.math.abs(signedAmount)
                }
                else -> return@forEachIndexed
            }

            val suggestion = ImportCategorySuggester.suggest(suggestedType, description)
            transactions += ImportedTransaction(
                sourceRow = headerIndex + index + 2,
                reference = row.valueAt(referenceIndex).ifBlank { null },
                title = description,
                amount = CurrencyPreferences.roundToTwoDecimals(amount),
                date = date,
                type = suggestedType,
                category = suggestion.category,
                subcategory = suggestion.subcategory,
                needsReview = classificationNeedsReview ||
                    ImportCategorySuggester.requiresReview(description)
            )
        }

        val warning = if (transactions.size >= MAX_IMPORTED_ROWS) {
            listOf("Only the first $MAX_IMPORTED_ROWS transactions are shown.")
        } else {
            emptyList()
        }
        return StatementImportResult(transactions, currency, warning)
    }

    private fun readLimited(input: InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            require(total <= MAX_CSV_BYTES) { "The CSV file is too large." }
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private fun decode(bytes: ByteArray): String {
        val utf8 = bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF")
        return if (utf8.count { it == '\uFFFD' } <= 2) {
            utf8
        } else {
            bytes.toString(Charset.forName("windows-1252"))
        }
    }

    private fun detectDelimiter(text: String): Char {
        val candidates = listOf(';', ',', '\t', '|')
        val lines = text.lineSequence().filter(String::isNotBlank).take(10).toList()
        return candidates.maxByOrNull { delimiter ->
            lines.sumOf { countOutsideQuotes(it, delimiter) }
        } ?: ','
    }

    private fun countOutsideQuotes(line: String, delimiter: Char): Int {
        var quoted = false
        var count = 0
        line.forEach { char ->
            when {
                char == '"' -> quoted = !quoted
                char == delimiter && !quoted -> count++
            }
        }
        return count
    }

    private fun parseRows(text: String, delimiter: Char): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var index = 0
        while (index < text.length) {
            val char = text[index]
            when {
                char == '"' && quoted && index + 1 < text.length && text[index + 1] == '"' -> {
                    field.append('"')
                    index++
                }
                char == '"' -> quoted = !quoted
                char == delimiter && !quoted -> {
                    row += field.toString()
                    field.clear()
                }
                (char == '\n' || char == '\r') && !quoted -> {
                    if (char == '\r' && index + 1 < text.length && text[index + 1] == '\n') index++
                    row += field.toString()
                    field.clear()
                    rows += row
                    row = mutableListOf()
                }
                else -> field.append(char)
            }
            index++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row += field.toString()
            rows += row
        }
        return rows
    }

    private fun looksLikeHeader(row: List<String>): Boolean {
        val normalized = row.map(::normalizeHeader)
        return findIndex(normalized, dateHeaders) >= 0 &&
            findIndex(normalized, descriptionHeaders) >= 0 &&
            (
                findIndex(normalized, debitHeaders) >= 0 ||
                    findIndex(normalized, creditHeaders) >= 0 ||
                    findIndex(normalized, amountHeaders) >= 0
                )
    }

    private fun findIndex(headers: List<String>, candidates: Set<String>): Int =
        headers.indexOfFirst { header ->
            candidates.any { candidate ->
                header == normalizeHeader(candidate) ||
                    header.contains(normalizeHeader(candidate))
            }
        }

    private fun normalizeHeader(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")
            .lowercase(Locale.ROOT)
            .replace("[^a-z0-9]+".toRegex(), " ")
            .trim()

    private fun parseDate(value: String): Date? {
        val cleaned = value.trim()
        val formats = listOf(
            "dd/MM/yyyy", "d/M/yyyy", "MM/dd/yyyy", "M/d/yyyy",
            "yyyy-MM-dd", "dd-MM-yyyy", "d-M-yyyy",
            "dd MMM yyyy", "d MMM yyyy", "dd-MMM-yyyy", "d-MMM-yyyy"
        )
        val locales = listOf(Locale.getDefault(), Locale.US, Locale("es"), Locale.GERMAN)
        formats.forEach { format ->
            locales.forEach { locale ->
                runCatching {
                    SimpleDateFormat(format, locale).apply { isLenient = false }.parse(cleaned)
                }.getOrNull()?.let { return it }
            }
        }
        return null
    }

    private fun parseAmount(value: String): Double? {
        var cleaned = value.trim()
            .replace("\u00A0", "")
            .replace(Regex("[^0-9,().+\\-]"), "")
        if (cleaned.isBlank()) return null
        val negativeParentheses = cleaned.startsWith("(") && cleaned.endsWith(")")
        cleaned = cleaned.removePrefix("(").removeSuffix(")")
        val lastComma = cleaned.lastIndexOf(',')
        val lastDot = cleaned.lastIndexOf('.')
        cleaned = when {
            lastComma >= 0 && lastDot >= 0 && lastComma > lastDot ->
                cleaned.replace(".", "").replace(',', '.')
            lastComma >= 0 && lastDot >= 0 ->
                cleaned.replace(",", "")
            lastComma >= 0 && cleaned.length - lastComma - 1 in 1..2 ->
                cleaned.replace(".", "").replace(',', '.')
            lastDot >= 0 && lastComma < 0 && cleaned.length - lastDot - 1 == 3 ->
                cleaned.replace(".", "")
            else -> cleaned.replace(",", "")
        }
        val parsed = cleaned.toDoubleOrNull() ?: return null
        return if (negativeParentheses) -kotlin.math.abs(parsed) else parsed
    }

    private fun currencyFrom(value: String): String? {
        val normalized = value.uppercase(Locale.ROOT)
        return listOf("USD", "EUR", "GBP", "JPY", "CNY", "CAD", "AUD", "CHF", "INR", "MXN", "BRL", "KRW", "SGD", "SEK", "PLN")
            .firstOrNull { normalized.contains(it) }
    }

    private fun String.cleanDescription(): String =
        replace("\\s+".toRegex(), " ").trim().take(180)

    private fun List<String>.valueAt(index: Int): String =
        if (index in indices) this[index] else ""

    private fun String.containsAny(candidates: Set<String>): Boolean =
        candidates.any { contains(normalizeHeader(it)) }
}
