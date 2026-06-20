package com.ritter.smartstackbills

import android.content.Context
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object CurrencyPreferences {
    private const val PREFERENCES_NAME = "currency_preferences"
    private const val KEY_CURRENCY_CODE = "currency_code"

    @JvmStatic
    fun selectedCode(context: Context): String =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CURRENCY_CODE, null)
            ?.takeIf { code -> runCatching { Currency.getInstance(code) }.isSuccess }
            ?: defaultCurrencyCode()

    @JvmStatic
    fun setSelectedCode(context: Context, code: String) {
        Currency.getInstance(code)
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
        Currency.getAvailableCurrencies().sortedBy { it.currencyCode }

    private fun defaultCurrencyCode(): String =
        runCatching { Currency.getInstance(Locale.getDefault()).currencyCode }
            .getOrDefault("USD")
}
