package com.example.smartstackbills

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import java.text.DecimalFormat

class RealTimeFormattingTextWatcher(private val editText: EditText) : TextWatcher {

    private var isFormatting: Boolean = false // To prevent recursive calls
    private var currentText: String = ""

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        // No action needed before text changes
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        // No action needed during text changes
    }

    override fun afterTextChanged(s: Editable?) {
        if (isFormatting) return

        val input = s.toString()
        if (input != currentText) {
            isFormatting = true
            try {
                // Remove unwanted characters (keep digits and decimal point)
                val cleanString = input.replace("[^\\d.]".toRegex(), "")

                if (cleanString.isNotEmpty()) {
                    val decimalParts = cleanString.split(".")
                    val integerPart = decimalParts[0].toLongOrNull() ?: 0
                    val formattedIntegerPart = DecimalFormat("#,###").format(integerPart)

                    // Rebuild the formatted string
                    val formatted = if (decimalParts.size > 1) {
                        val decimalPart = decimalParts[1].take(2) // Up to 2 decimal places
                        "$formattedIntegerPart.$decimalPart"
                    } else {
                        formattedIntegerPart
                    }

                    currentText = formatted
                    editText.setText(formatted)
                    editText.setSelection(formatted.length) // Move cursor to the end
                }
            } catch (e: Exception) {
                // Handle invalid inputs gracefully if needed
            }
            isFormatting = false
        }
    }
}
