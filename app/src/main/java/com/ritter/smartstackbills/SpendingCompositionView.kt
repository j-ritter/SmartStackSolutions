package com.ritter.smartstackbills

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

class SpendingCompositionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var categories: List<CategorySlice> = emptyList()
    fun setData(values: Map<String, Double>) {
        val sorted = values.filterValues { it > 0.0 }.entries.sortedByDescending { it.value }
        val top = sorted.take(4).map { entry ->
            CategorySlice(
                entry.key,
                entry.value,
                CategoryColorPalette.colorFor(context, entry.key)
            )
        }.toMutableList()
        val other = sorted.drop(4).sumOf { it.value }
        if (other > 0.0) {
            val otherLabel = context.getString(R.string.other)
            top += CategorySlice(
                otherLabel,
                other,
                CategoryColorPalette.colorFor(context, otherLabel)
            )
        }
        categories = top
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val total = categories.sumOf { it.amount }
        val centerX = paddingLeft + 62f.dp
        val centerY = height / 2f
        val radius = 49f.dp
        val oval = RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 17f.dp
        paint.strokeCap = Paint.Cap.BUTT
        if (total <= 0.0) {
            paint.color = 0xFFE2E8EE.toInt()
            canvas.drawArc(oval, -90f, 360f, false, paint)
        } else {
            var angle = -90f
            categories.forEach { slice ->
                val sweep = (slice.amount / total * 360.0).toFloat()
                paint.color = ContextCompat.getColor(context, slice.color)
                canvas.drawArc(oval, angle, sweep, false, paint)
                angle += sweep
            }
        }
        paint.style = Paint.Style.FILL

        drawText(
            canvas,
            CurrencyPreferences.format(context, total),
            centerX,
            centerY - 2f.dp,
            11.5f.sp,
            R.color.colorSecondary,
            Paint.Align.CENTER,
            true
        )
        drawText(
            canvas,
            context.getString(R.string.paid),
            centerX,
            centerY + 17f.dp,
            10f.sp,
            R.color.textSecondary,
            Paint.Align.CENTER,
            false
        )

        val legendLeft = centerX + radius + 20f.dp
        val legendTextLeft = legendLeft + 17f.dp
        val availableLabelWidth = (width - paddingRight - legendTextLeft).coerceAtLeast(40f.dp)
        if (categories.isEmpty()) {
            drawText(
                canvas,
                context.getString(R.string.no_closed_payments_for_month),
                legendLeft,
                centerY,
                11f.sp,
                R.color.textSecondary,
                Paint.Align.LEFT,
                false
            )
            return
        }

        categories.forEachIndexed { index, slice ->
            val y = paddingTop + 23f.dp + index * 31f.dp
            paint.color = ContextCompat.getColor(context, slice.color)
            canvas.drawCircle(legendLeft + 5f.dp, y - 4f.dp, 5f.dp, paint)
            val percentage = if (total > 0.0) (slice.amount / total * 100).toInt() else 0
            drawText(
                canvas,
                fitText(slice.label, availableLabelWidth, 10.5f.sp, true),
                legendTextLeft,
                y,
                10.5f.sp,
                R.color.colorSecondary,
                Paint.Align.LEFT,
                true
            )
            drawText(
                canvas,
                "$percentage% \u00B7 ${CurrencyPreferences.format(context, slice.amount)}",
                legendTextLeft,
                y + 14f.dp,
                9.5f.sp,
                R.color.textSecondary,
                Paint.Align.LEFT,
                false
            )
        }
    }

    private fun drawText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        size: Float,
        colorRes: Int,
        align: Paint.Align,
        bold: Boolean
    ) {
        paint.style = Paint.Style.FILL
        paint.color = ContextCompat.getColor(context, colorRes)
        paint.textSize = size
        paint.textAlign = align
        paint.typeface = if (bold) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
        canvas.drawText(text, x, y, paint)
    }

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

    private val Float.dp get() = this * resources.displayMetrics.density
    private val Float.sp get() = this * resources.displayMetrics.scaledDensity
    private data class CategorySlice(val label: String, val amount: Double, val color: Int)
}
