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
    private var trendPoints: List<TrendPoint> = emptyList()

    fun setData(income: Double, paid: Double, open: Double, available: Double, target: Double) {
        this.income = income.coerceAtLeast(0.0)
        this.paid = paid.coerceAtLeast(0.0)
        this.open = open.coerceAtLeast(0.0)
        this.available = available.coerceAtLeast(0.0)
        this.target = target.coerceAtLeast(0.0)
        this.trendPoints = emptyList()
        invalidate()
    }

    fun setTrendData(points: List<TrendPoint>) {
        trendPoints = points
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (trendPoints.isNotEmpty()) {
            drawTrend(canvas)
            return
        }
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

    private fun drawTrend(canvas: Canvas) {
        val left = paddingLeft.toFloat()
        val right = width - paddingRight.toFloat()
        val top = paddingTop + 28f.dp
        val bottom = height - paddingBottom - 48f.dp
        val chartHeight = (bottom - top).coerceAtLeast(40f.dp)
        val maxValue = trendPoints
            .flatMap { listOf(it.income, it.paid, it.open, it.target) }
            .maxOrNull()
            ?.coerceAtLeast(1.0)
            ?: 1.0

        val legend = listOf(
            Legend(context.getString(R.string.income), R.color.income_color),
            Legend(context.getString(R.string.paid_expenses_short), R.color.spending_color),
            Legend(context.getString(R.string.still_due_short), R.color.bill_color),
            Legend(context.getString(R.string.savings_target), R.color.savings_color)
        )
        var legendX = left
        legend.forEach { item ->
            paint.color = color(item.color)
            canvas.drawCircle(legendX + 4f.dp, paddingTop + 10f.dp, 4f.dp, paint)
            drawText(canvas, item.label, legendX + 11f.dp, paddingTop + 14f.dp, 9.5f.sp, color(R.color.textSecondary), Paint.Align.LEFT, false)
            legendX += (paint.measureText(item.label) + 28f.dp).coerceAtLeast(58f.dp)
        }

        paint.color = 0xFFE6ECF1.toInt()
        paint.strokeWidth = 1f.dp
        canvas.drawLine(left, bottom, right, bottom, paint)

        val monthWidth = (right - left) / trendPoints.size.coerceAtLeast(1)
        val barWidth = (monthWidth / 6.2f).coerceIn(3f.dp, 10f.dp)
        trendPoints.forEachIndexed { index, point ->
            val centerX = left + monthWidth * index + monthWidth / 2f
            val values = listOf(
                point.income to R.color.income_color,
                point.paid to R.color.spending_color,
                point.open to R.color.bill_color,
                point.target to R.color.savings_color
            )
            val startX = centerX - barWidth * 2f - 2f.dp
            values.forEachIndexed { valueIndex, (value, colorRes) ->
                val barHeight = (value.coerceAtLeast(0.0) / maxValue * chartHeight).toFloat()
                val barLeft = startX + valueIndex * (barWidth + 2f.dp)
                paint.color = color(colorRes)
                canvas.drawRoundRect(
                    RectF(barLeft, bottom - barHeight, barLeft + barWidth, bottom),
                    3f.dp,
                    3f.dp,
                    paint
                )
            }
            drawText(canvas, point.label, centerX, height - paddingBottom - 27f.dp, 9.5f.sp, color(R.color.textSecondary), Paint.Align.CENTER, false)
        }

        val totals = trendPoints.fold(TrendPoint("", 0.0, 0.0, 0.0, 0.0)) { acc, point ->
            TrendPoint(
                "",
                acc.income + point.income,
                acc.paid + point.paid,
                acc.open + point.open,
                acc.target + point.target
            )
        }
        val balance = totals.income - totals.paid - totals.open - totals.target
        val message = context.getString(
            if (balance >= 0.0) R.string.period_balance_positive else R.string.period_balance_negative,
            CurrencyPreferences.format(context, kotlin.math.abs(balance))
        )
        drawText(
            canvas,
            fitText(message, (right - left).coerceAtLeast(40f.dp), 10.5f.sp, false),
            left,
            height - paddingBottom - 6f.dp,
            10.5f.sp,
            color(R.color.textSecondary),
            Paint.Align.LEFT,
            false
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

    private fun fitText(text: String, maxWidth: Float, size: Float, bold: Boolean): String {
        paint.textSize = size
        paint.typeface = if (bold) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
        if (paint.measureText(text) <= maxWidth) return text
        val ellipsis = "\u2026"
        var end = text.length
        while (end > 1 && paint.measureText(text.substring(0, end) + ellipsis) > maxWidth) {
            end--
        }
        return text.substring(0, end).trimEnd() + ellipsis
    }

    private data class Row(val label: String, val value: Double, val color: Int)
    private data class Legend(val label: String, val color: Int)

    data class TrendPoint(
        val label: String,
        val income: Double,
        val paid: Double,
        val open: Double,
        val target: Double
    )
}
