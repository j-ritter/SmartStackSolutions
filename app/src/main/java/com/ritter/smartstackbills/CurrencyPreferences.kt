package com.ritter.smartstackbills

import android.content.Context
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object CurrencyPreferences {
    private const val PREFERENCES_NAME = "currency_preferences"
    private const val KEY_CURRENCY_CODE = "currency_code"
    private const val KEY_DECIMAL_MODE = "decimal_mode"

    enum class AmountDecimalMode(val key: String) {
        TWO_DECIMALS("two_decimals"),
        NO_DECIMALS("no_decimals")
    }
    private val supportedCurrencyCodes = listOf(
        "USD", // United States dollar
        "EUR", // Euro
        "GBP", // British pound
        "JPY", // Japanese yen
        "CNY", // Chinese yuan
        "CAD", // Canadian dollar
        "AUD", // Australian dollar
        "CHF", // Swiss franc
        "INR", // Indian rupee
        "MXN", // Mexican peso
        "BRL", // Brazilian real
        "KRW", // South Korean won
        "SGD", // Singapore dollar
        "SEK", // Swedish krona
        "PLN", // Polish zloty
        "CZK", // Czech koruna
        "HUF", // Hungarian forint
        "IDR", // Indonesian rupiah
        "RON", // Romanian leu
        "TRY", // Turkish lira
        "VND"  // Vietnamese dong
    )

    @JvmStatic
    fun selectedCode(context: Context): String =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CURRENCY_CODE, null)
            ?.takeIf { it in supportedCurrencyCodes }
            ?: defaultCurrencyCode()

    @JvmStatic
    fun setSelectedCode(context: Context, code: String) {
        require(code in supportedCurrencyCodes) { "Unsupported currency: $code" }
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CURRENCY_CODE, code)
            .apply()
    }

    @JvmStatic
    fun selectedDecimalMode(context: Context): AmountDecimalMode {
        val stored = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getString(KEY_DECIMAL_MODE, AmountDecimalMode.TWO_DECIMALS.key)
        return AmountDecimalMode.values().firstOrNull { it.key == stored }
            ?: AmountDecimalMode.TWO_DECIMALS
    }

    @JvmStatic
    fun setDecimalMode(context: Context, mode: AmountDecimalMode) {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_DECIMAL_MODE, mode.key)
            .apply()
    }

    @JvmStatic
    fun format(context: Context, amount: Double): String =
        format(context, amount, selectedCode(context))

    @JvmStatic
    fun format(context: Context, amount: Double, currencyCode: String?): String =
        NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
            currency = Currency.getInstance(
                currencyCode?.takeIf { it in supportedCurrencyCodes } ?: selectedCode(context)
            )
            val digits = if (selectedDecimalMode(context) == AmountDecimalMode.NO_DECIMALS) 0 else 2
            minimumFractionDigits = digits
            maximumFractionDigits = digits
        }.format(amount)

    @JvmStatic
    fun formatPlain(amount: Double): String =
        DecimalFormat("0.00", DecimalFormatSymbols(Locale.US)).format(amount)

    @JvmStatic
    fun roundToTwoDecimals(amount: Double): Double =
        kotlin.math.round(amount * 100.0) / 100.0

    @JvmStatic
    fun availableCurrencies(): List<Currency> =
        supportedCurrencyCodes.map(Currency::getInstance)

    private fun defaultCurrencyCode(): String =
        runCatching { Currency.getInstance(Locale.getDefault()).currencyCode }
            .getOrDefault("USD")
            .takeIf { it in supportedCurrencyCodes }
            ?: "USD"
}
