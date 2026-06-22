package com.ritter.smartstackbills

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.abs

class CategoryChangeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    data class Change(
        val label: String,
        val amount: Double,
        val percent: Double?,
        val colorRes: Int
    )

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var changes: List<Change> = emptyList()

    fun setData(changes: List<Change>) {
        this.changes = changes.take(4)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (changes.isEmpty()) {
            text(
                canvas,
                context.getString(R.string.not_enough_category_data),
                paddingLeft.toFloat(),
                height / 2f,
                12f.sp,
                R.color.textSecondary,
                Paint.Align.LEFT
            )
            return
        }

        val maxAmount = changes.maxOfOrNull { it.amount }?.coerceAtLeast(1.0) ?: 1.0
        changes.forEachIndexed { index, change ->
            val rowTop = paddingTop + index * 48f.dp
            val textY = rowTop + 14f.dp
            val delta = change.percent
            val deltaText = when {
                delta == null -> "\u2014"
                delta > 0 -> "\u2191 ${abs(delta).toInt()}%"
                delta < 0 -> "\u2193 ${abs(delta).toInt()}%"
                else -> "0%"
            }
            val summary = "${CurrencyPreferences.format(context, change.amount)} \u00B7 $deltaText"

            paint.textSize = 10f.sp
            paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
            val summaryWidth = paint.measureText(summary)
            val labelWidth = (
                width - paddingRight - paddingLeft - summaryWidth - 14f.dp
            ).coerceAtLeast(56f.dp)

            text(
                canvas,
                fitText(change.label, labelWidth, 10.5f.sp),
                paddingLeft.toFloat(),
                textY,
                10.5f.sp,
                R.color.colorSecondary,
                Paint.Align.LEFT
            )
            text(
                canvas,
                summary,
                width - paddingRight.toFloat(),
                textY,
                10f.sp,
                when {
                    delta == null -> R.color.textSecondary
                    delta > 0 -> R.color.red
                    delta < 0 -> R.color.savings_color
                    else -> R.color.textSecondary
                },
                Paint.Align.RIGHT
            )

            val barLeft = paddingLeft.toFloat()
            val barRight = width - paddingRight.toFloat()
            val barTop = rowTop + 23f.dp
            val barBottom = barTop + 11f.dp
            paint.color = 0xFFE5EBF0.toInt()
            canvas.drawRoundRect(RectF(barLeft, barTop, barRight, barBottom), 6f.dp, 6f.dp, paint)
            paint.color = color(change.colorRes)
            canvas.drawRoundRect(
                RectF(
                    barLeft,
                    barTop,
                    barLeft + (barRight - barLeft) * (change.amount / maxAmount).toFloat(),
                    barBottom
                ),
                6f.dp,
                6f.dp,
                paint
            )
        }
    }

    private fun text(
        canvas: Canvas,
        value: String,
        x: Float,
        y: Float,
        size: Float,
        colorRes: Int,
        align: Paint.Align
    ) {
        paint.style = Paint.Style.FILL
        paint.color = color(colorRes)
        paint.textSize = size
        paint.textAlign = align
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        canvas.drawText(value, x, y, paint)
    }

    private fun fitText(value: String, maxWidth: Float, size: Float): String {
        paint.textSize = size
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        if (paint.measureText(value) <= maxWidth) return value
        val ellipsis = "\u2026"
        var end = value.length
        while (end > 1 && paint.measureText(value.substring(0, end) + ellipsis) > maxWidth) {
            end--
        }
        return value.substring(0, end).trimEnd() + ellipsis
    }

    private fun color(res: Int) = ContextCompat.getColor(context, res)
    private val Float.dp get() = this * resources.displayMetrics.density
    private val Float.sp get() = this * resources.displayMetrics.scaledDensity
}
