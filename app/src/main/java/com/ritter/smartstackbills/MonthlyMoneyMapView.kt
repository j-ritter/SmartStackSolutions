package com.ritter.smartstackbills

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.max

class MonthlyMoneyMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var income = 0.0
    private var paid = 0.0
    private var open = 0.0
    private var available = 0.0
    private var target = 0.0

    fun setData(income: Double, paid: Double, open: Double, available: Double, target: Double) {
        this.income = income.coerceAtLeast(0.0)
        this.paid = paid.coerceAtLeast(0.0)
        this.open = open.coerceAtLeast(0.0)
        this.available = available.coerceAtLeast(0.0)
        this.target = target.coerceAtLeast(0.0)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val left = paddingLeft.toFloat()
        val right = width - paddingRight.toFloat()
        val labelWidth = 94f.dp
        val amountWidth = 86f.dp
        val barLeft = left + labelWidth
        val barRight = (right - amountWidth).coerceAtLeast(barLeft + 20f.dp)
        val maxValue = max(1.0, max(income, max(paid + open, max(available, target))))
        val rows = listOf(
            Row(context.getString(R.string.income), income, R.color.income_color),
            Row(context.getString(R.string.paid_expenses_short), paid, R.color.spending_color),
            Row(context.getString(R.string.still_due_short), open, R.color.bill_color),
            Row(context.getString(R.string.available_after_commitments), available, R.color.positive_balance),
            Row(context.getString(R.string.savings_target), target, R.color.savings_color)
        )

        rows.forEachIndexed { index, row ->
            val centerY = paddingTop + 19f.dp + index * 38f.dp
            drawText(canvas, row.label, left, centerY + 4f.dp, 12f.sp, color(R.color.colorSecondary), Paint.Align.LEFT, true)
            drawTrack(canvas, barLeft, centerY - 7f.dp, barRight, centerY + 7f.dp)
            val fraction = (row.value / maxValue).coerceIn(0.0, 1.0).toFloat()
            if (fraction > 0f) {
                paint.color = color(row.color)
                canvas.drawRoundRect(
                    RectF(barLeft, centerY - 7f.dp, barLeft + (barRight - barLeft) * fraction, centerY + 7f.dp),
                    7f.dp,
                    7f.dp,
                    paint
                )
            }
            drawText(
                canvas,
                CurrencyPreferences.format(context, row.value),
                right,
                centerY + 4f.dp,
                11f.sp,
                color(R.color.colorSecondary),
                Paint.Align.RIGHT,
                true
            )
        }

        val difference = available - target
        val message = when {
            target <= 0.0 -> context.getString(R.string.no_savings_target_insight)
            difference >= 0.0 -> context.getString(
                R.string.savings_target_cushion,
                CurrencyPreferences.format(context, difference)
            )
            else -> context.getString(
                R.string.savings_target_shortfall,
                CurrencyPreferences.format(context, -difference)
            )
        }
        drawText(
            canvas,
            message,
            left,
            height - paddingBottom - 4f.dp,
            12f.sp,
            color(if (difference >= 0.0) R.color.savings_color else R.color.red),
            Paint.Align.LEFT,
            true
        )
    }

    private fun drawTrack(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float) {
        paint.color = 0xFFE6ECF1.toInt()
        canvas.drawRoundRect(RectF(left, top, right, bottom), 7f.dp, 7f.dp, paint)
    }

    private fun drawText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        size: Float,
        textColor: Int,
        align: Paint.Align,
        bold: Boolean
    ) {
        paint.color = textColor
        paint.textSize = size
        paint.textAlign = align
        paint.typeface = if (bold) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
        canvas.drawText(text, x, y, paint)
    }

    private fun color(resId: Int) = ContextCompat.getColor(context, resId)
    private val Float.dp get() = this * resources.displayMetrics.density
    private val Float.sp get() = this * resources.displayMetrics.scaledDensity

    private data class Row(val label: String, val value: Double, val color: Int)
}
