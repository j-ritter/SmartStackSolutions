package com.ritter.smartstackbills

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

class RecurringCommitmentView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    data class Point(val label: String, val amount: Double)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var points: List<Point> = emptyList()

    fun setData(points: List<Point>) {
        this.points = points
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (points.isEmpty()) return
        val left = paddingLeft + 6f.dp
        val right = width - paddingRight - 6f.dp
        val top = paddingTop + 8f.dp
        val bottom = height - paddingBottom - 28f.dp
        val maxValue = points.maxOfOrNull { it.amount }?.coerceAtLeast(1.0) ?: 1.0
        val slot = (right - left) / points.size
        points.forEachIndexed { index, point ->
            val barWidth = (slot * .55f).coerceAtMost(26f.dp)
            val x = left + slot * index + slot / 2
            val barHeight = ((point.amount / maxValue) * (bottom - top)).toFloat()
            paint.color = ContextCompat.getColor(context, R.color.bill_color)
            canvas.drawRoundRect(RectF(x - barWidth / 2, bottom - barHeight, x + barWidth / 2, bottom), 6f.dp, 6f.dp, paint)
            text(canvas, point.label, x, height - 7f.dp, 9f.sp, R.color.textSecondary)
        }
        val first = points.first().amount
        val last = points.last().amount
        val summary = when {
            last > first -> context.getString(R.string.commitments_increased, CurrencyPreferences.format(context, last - first))
            last < first -> context.getString(R.string.commitments_decreased, CurrencyPreferences.format(context, first - last))
            else -> context.getString(R.string.commitments_steady)
        }
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 10f.sp
        paint.color = ContextCompat.getColor(context, R.color.colorSecondary)
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        canvas.drawText(summary, left, 11f.dp, paint)
    }

    private fun text(canvas: Canvas, value: String, x: Float, y: Float, size: Float, colorRes: Int) {
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = size
        paint.color = ContextCompat.getColor(context, colorRes)
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        canvas.drawText(value, x, y, paint)
    }
    private val Float.dp get() = this * resources.displayMetrics.density
    private val Float.sp get() = this * resources.displayMetrics.scaledDensity
}
