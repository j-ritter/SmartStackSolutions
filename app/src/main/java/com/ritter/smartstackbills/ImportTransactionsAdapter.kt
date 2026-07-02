package com.ritter.smartstackbills

import android.app.DatePickerDialog
import android.graphics.Color
import android.graphics.Typeface
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ImportTransactionsAdapter(
    private val transactions: MutableList<ImportedTransaction>,
    private val onChanged: () -> Unit
) : RecyclerView.Adapter<ImportTransactionsAdapter.TransactionViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_import_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(transactions[position])
    }

    override fun getItemCount(): Int = transactions.size

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val typeSpinner: Spinner = itemView.findViewById(R.id.importTransactionType)
        private val included: CheckBox = itemView.findViewById(R.id.importTransactionIncluded)
        private val title: EditText = itemView.findViewById(R.id.importTransactionTitle)
        private val amount: EditText = itemView.findViewById(R.id.importTransactionAmount)
        private val currency: TextView = itemView.findViewById(R.id.importTransactionCurrency)
        private val date: TextView = itemView.findViewById(R.id.importTransactionDate)
        private val category: Spinner = itemView.findViewById(R.id.importTransactionCategory)
        private val warning: TextView = itemView.findViewById(R.id.importTransactionWarning)
        private var titleWatcher: TextWatcher? = null
        private var amountWatcher: TextWatcher? = null
        private var binding = false

        fun bind(transaction: ImportedTransaction) {
            binding = true
            titleWatcher?.let(title::removeTextChangedListener)
            amountWatcher?.let(amount::removeTextChangedListener)

            val typeOptions = listOf(
                itemView.context.getString(R.string.closed_payments),
                itemView.context.getString(R.string.income)
            )
            typeSpinner.adapter = compactSpinnerAdapter(typeOptions)
            typeSpinner.setSelection(
                when (transaction.type) {
                    ImportedTransactionType.CLOSED_PAYMENT -> 0
                    ImportedTransactionType.INCOME -> 1
                },
                false
            )
            included.setOnCheckedChangeListener(null)
            included.isChecked = transaction.included
            included.setOnCheckedChangeListener { _, checked ->
                transaction.included = checked
                updateEnabledState(transaction)
                onChanged()
            }

            title.setText(transaction.title)
            amount.setText(CurrencyPreferences.formatPlain(transaction.amount))
            currency.text = transaction.currency ?: CurrencyPreferences.selectedCode(itemView.context)
            date.text = dateFormat.format(transaction.date)
            bindCategories(transaction)
            updateEnabledState(transaction)
            warning.visibility = if (transaction.duplicate || transaction.needsReview) View.VISIBLE else View.GONE
            warning.text = when {
                transaction.duplicate -> itemView.context.getString(R.string.possible_duplicate)
                else -> itemView.context.getString(R.string.import_row_needs_review)
            }

            typeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, selected: Int, id: Long) {
                    if (binding) return
                    transaction.type = when (selected) {
                        0 -> ImportedTransactionType.CLOSED_PAYMENT
                        else -> ImportedTransactionType.INCOME
                    }
                    amount.setTextColor(
                        if (transaction.type == ImportedTransactionType.INCOME) {
                            itemView.context.getColor(R.color.income_color)
                        } else {
                            itemView.context.getColor(R.color.spending_color)
                        }
                    )
                    if (transaction.type == ImportedTransactionType.CLOSED_PAYMENT) {
                        transaction.category = FinancialEntryOptions.DEFAULT_EXPENSE_CATEGORY
                        transaction.subcategory = FinancialEntryOptions.DEFAULT_EXPENSE_SUBCATEGORY
                    } else {
                        transaction.category = FinancialEntryOptions.DEFAULT_INCOME_CATEGORY
                        transaction.subcategory = FinancialEntryOptions.DEFAULT_INCOME_SUBCATEGORY
                    }
                    bindCategories(transaction)
                    updateEnabledState(transaction)
                    onChanged()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }

            titleWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (!binding) {
                        transaction.title = s?.toString().orEmpty().trim()
                        onChanged()
                    }
                }
                override fun afterTextChanged(s: Editable?) = Unit
            }.also(title::addTextChangedListener)

            amountWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (!binding) {
                        transaction.amount = CurrencyPreferences.roundToTwoDecimals(
                            s?.toString()?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
                        )
                        onChanged()
                    }
                }
                override fun afterTextChanged(s: Editable?) = Unit
            }.also(amount::addTextChangedListener)

            date.setOnClickListener {
                if (!transaction.included) return@setOnClickListener
                val calendar = Calendar.getInstance().apply { time = transaction.date }
                DatePickerDialog(
                    itemView.context,
                    { _, year, month, day ->
                        calendar.set(year, month, day, 12, 0, 0)
                        calendar.set(Calendar.MILLISECOND, 0)
                        transaction.date = calendar.time
                        date.text = dateFormat.format(transaction.date)
                        onChanged()
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
            binding = false
        }

        private fun bindCategories(transaction: ImportedTransaction) {
            val incomeOptions = FinancialEntryOptions.incomeCategories(itemView.context)
            val expenseOptions = FinancialEntryOptions.expenseCategories(itemView.context)
            val options = when (transaction.type) {
                ImportedTransactionType.INCOME -> incomeOptions
                ImportedTransactionType.CLOSED_PAYMENT -> expenseOptions
            }
            category.adapter = compactSpinnerAdapter(options)
            val selected = options.indexOfFirst { it.key == transaction.category }
                .takeIf { it >= 0 }
                ?: options.indexOfFirst {
                    it.key == if (transaction.type == ImportedTransactionType.INCOME) {
                        FinancialEntryOptions.DEFAULT_INCOME_CATEGORY
                    } else {
                        FinancialEntryOptions.DEFAULT_EXPENSE_CATEGORY
                    }
                }.coerceAtLeast(0)
            options.getOrNull(selected)?.let { selectedOption ->
                if (transaction.category != selectedOption.key) {
                    transaction.category = selectedOption.key
                    transaction.subcategory = when {
                        options === incomeOptions ->
                            FinancialEntryOptions.incomeSubcategories(itemView.context, selectedOption.key)
                                .lastOrNull()?.key ?: FinancialEntryOptions.DEFAULT_INCOME_SUBCATEGORY
                        else ->
                            FinancialEntryOptions.expenseSubcategories(itemView.context, selectedOption.key)
                                .lastOrNull()?.key ?: FinancialEntryOptions.DEFAULT_EXPENSE_SUBCATEGORY
                    }
                }
            }
            category.setSelection(selected, false)
            category.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (binding || !transaction.included) return
                    val option = options.getOrNull(position) ?: return
                    transaction.category = option.key
                    transaction.subcategory = when (transaction.type) {
                        ImportedTransactionType.INCOME ->
                            FinancialEntryOptions.incomeSubcategories(itemView.context, option.key)
                                .lastOrNull()?.key ?: FinancialEntryOptions.DEFAULT_INCOME_SUBCATEGORY
                        else ->
                            FinancialEntryOptions.expenseSubcategories(itemView.context, option.key)
                                .lastOrNull()?.key ?: FinancialEntryOptions.DEFAULT_EXPENSE_SUBCATEGORY
                    }
                    onChanged()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
        }

        private fun updateEnabledState(transaction: ImportedTransaction) {
            val enabled = transaction.included
            title.isEnabled = enabled
            amount.isEnabled = enabled
            currency.isEnabled = enabled
            date.isEnabled = enabled
            category.isEnabled = enabled
            itemView.alpha = if (enabled) 1f else 0.55f
            amount.setTextColor(
                if (!enabled) {
                    Color.GRAY
                } else if (transaction.type == ImportedTransactionType.INCOME) {
                    itemView.context.getColor(R.color.income_color)
                } else {
                    itemView.context.getColor(R.color.spending_color)
                }
            )
            currency.setTextColor(if (enabled) Color.BLACK else Color.GRAY)
        }

        private fun <T> compactSpinnerAdapter(options: List<T>): ArrayAdapter<T> {
            return object : ArrayAdapter<T>(
                itemView.context,
                android.R.layout.simple_spinner_item,
                options
            ) {
                init {
                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }

                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    return super.getView(position, convertView, parent).also(::styleTextView)
                }

                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                    return super.getDropDownView(position, convertView, parent).also(::styleTextView)
                }

                private fun styleTextView(view: View) {
                    (view as? TextView)?.apply {
                        textSize = 12f
                        setTextColor(Color.BLACK)
                        setTypeface(typeface, Typeface.BOLD)
                        includeFontPadding = false
                    }
                }
            }
        }
    }
}
