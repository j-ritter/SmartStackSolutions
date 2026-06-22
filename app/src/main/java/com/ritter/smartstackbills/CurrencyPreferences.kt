package com.ritter.smartstackbills

import android.content.Context
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object CurrencyPreferences {
    private const val PREFERENCES_NAME = "currency_preferences"
    private const val KEY_CURRENCY_CODE = "currency_code"
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
        "PLN"  // Polish zloty
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
    fun format(context: Context, amount: Double): String =
        NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
            currency = Currency.getInstance(selectedCode(context))
        }.format(amount)

    @JvmStatic
    fun availableCurrencies(): List<Currency> =
        supportedCurrencyCodes.map(Currency::getInstance)

    private fun defaultCurrencyCode(): String =
        runCatching { Currency.getInstance(Locale.getDefault()).currencyCode }
            .getOrDefault("USD")
            .takeIf { it in supportedCurrencyCodes }
            ?: "USD"
}
