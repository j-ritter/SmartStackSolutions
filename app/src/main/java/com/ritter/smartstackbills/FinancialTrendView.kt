package com.ritter.smartstackbills

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import java.util.Currency
import kotlin.math.max
import kotlin.math.min

class FinancialTrendView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    data class Point(val label: String, val income: Double, val obligations: Double)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var points: List<Point> = emptyList()

    fun setData(points: List<Point>) {
        this.points = points
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (points.isEmpty()) return
        val left = paddingLeft + 58f.dp
        val right = width - paddingRight - 8f.dp
        val top = paddingTop + 22f.dp
        val bottom = height - paddingBottom - 30f.dp
        val balances = points.map { it.income - it.obligations }
        val rawMaximum = max(
            1.0,
            max(
                points.maxOfOrNull { max(it.income, it.obligations) } ?: 1.0,
                balances.maxOrNull() ?: 0.0
            )
        )
        val rawMinimum = min(0.0, balances.minOrNull() ?: 0.0)
        val rangePadding = (rawMaximum - rawMinimum) * 0.08
        val maximum = rawMaximum + rangePadding
        val minimum = if (rawMinimum < 0.0) rawMinimum - rangePadding else 0.0
        val range = (maximum - minimum).coerceAtLeast(1.0)
        fun yFor(value: Double): Float =
            (top + ((maximum - value) / range * (bottom - top))).toFloat()
        val step = if (points.size > 1) (right - left) / (points.size - 1) else 0f

        paint.strokeWidth = 1f.dp
        paint.color = 0xFFE1E8EE.toInt()
        repeat(5) { index ->
            val value = maximum - range * index / 4.0
            val y = yFor(value)
            canvas.drawLine(left, y, right, y, paint)
            text(
                canvas,
                axisAmount(value),
                left - 7f.dp,
                y + 3f.dp,
                8f.sp,
                R.color.textSecondary,
                Paint.Align.RIGHT,
                false
            )
        }

        val zeroY = yFor(0.0)
        paint.strokeWidth = 1.5f.dp
        paint.color = ContextCompat.getColor(context, R.color.textSecondary)
        canvas.drawLine(left, zeroY, right, zeroY, paint)

        points.forEachIndexed { index, point ->
            val balance = point.income - point.obligations
            val x = left + step * index
            val balanceY = yFor(balance)
            paint.color = color(if (balance >= 0) R.color.positive_balance else R.color.negative_balance)
            canvas.drawRoundRect(
                x - 6f.dp,
                min(zeroY, balanceY),
                x + 6f.dp,
                max(zeroY, balanceY),
                4f.dp,
                4f.dp,
                paint
            )
            text(canvas, point.label, x, height - 8f.dp, 9f.sp, R.color.textSecondary, Paint.Align.CENTER)
        }

        drawLine(canvas, points.mapIndexed { i, p ->
            (left + step * i) to yFor(p.income)
        }, R.color.income_color)
        drawLine(canvas, points.mapIndexed { i, p ->
            (left + step * i) to yFor(p.obligations)
        }, R.color.spending_color)

        text(canvas, context.getString(R.string.income), left, 11f.dp, 10f.sp, R.color.income_color, Paint.Align.LEFT)
        text(canvas, context.getString(R.string.total_obligations), left + 70f.dp, 11f.dp, 10f.sp, R.color.spending_color, Paint.Align.LEFT)
        text(canvas, context.getString(R.string.balance), right, 11f.dp, 10f.sp, R.color.positive_balance, Paint.Align.RIGHT)
    }

    private fun drawLine(canvas: Canvas, coordinates: List<Pair<Float, Float>>, colorRes: Int) {
        val path = Path()
        coordinates.forEachIndexed { index, point ->
            if (index == 0) path.moveTo(point.first, point.second) else path.lineTo(point.first, point.second)
        }
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f.dp
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = color(colorRes)
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.FILL
        coordinates.forEach { canvas.drawCircle(it.first, it.second, 4f.dp, paint) }
    }

    private fun axisAmount(value: Double): String {
        val absolute = kotlin.math.abs(value)
        val compact = when {
            absolute >= 1_000_000 -> String.format("%.1fM", absolute / 1_000_000)
            absolute >= 1_000 -> String.format("%.1fk", absolute / 1_000)
            else -> String.format("%.0f", absolute)
        }
        val symbol = Currency.getInstance(CurrencyPreferences.selectedCode(context)).symbol
        return if (value < -0.005) "-$symbol$compact" else "$symbol$compact"
    }

    private fun text(
        canvas: Canvas,
        value: String,
        x: Float,
        y: Float,
        size: Float,
        colorRes: Int,
        align: Paint.Align,
        bold: Boolean = true
    ) {
        paint.style = Paint.Style.FILL
        paint.color = color(colorRes)
        paint.textSize = size
        paint.textAlign = align
        paint.typeface = if (bold) {
            android.graphics.Typeface.DEFAULT_BOLD
        } else {
            android.graphics.Typeface.DEFAULT
        }
        canvas.drawText(value, x, y, paint)
    }

    private fun color(res: Int) = ContextCompat.getColor(context, res)
    private val Float.dp get() = this * resources.displayMetrics.density
    private val Float.sp get() = this * resources.displayMetrics.scaledDensity
}
