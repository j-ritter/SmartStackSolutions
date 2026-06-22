package com.ritter.smartstackbills

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

class SavingsConsistencyView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    data class Month(val label: String, val hasGoal: Boolean, val achieved: Boolean)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var months: List<Month> = emptyList()

    fun setData(months: List<Month>) {
        this.months = months
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (months.isEmpty()) return
        val left = paddingLeft + 10f.dp
        val right = width - paddingRight - 10f.dp
        val slot = (right - left) / months.size
        val y = 48f.dp
        months.forEachIndexed { index, month ->
            val x = left + slot * index + slot / 2
            paint.color = ContextCompat.getColor(
                context,
                when {
                    !month.hasGoal -> R.color.light_gray
                    month.achieved -> R.color.savings_color
                    else -> R.color.negative_balance
                }
            )
            canvas.drawCircle(x, y, 10f.dp, paint)
            text(canvas, month.label, x, y + 30f.dp, 9f.sp, R.color.textSecondary)
        }
        val goalMonths = months.filter { it.hasGoal }
        val achieved = goalMonths.count { it.achieved }
        val summary = if (goalMonths.isEmpty()) {
            context.getString(R.string.no_savings_goals_in_period)
        } else {
            context.getString(R.string.savings_goals_achieved, achieved, goalMonths.size)
        }
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 12f.sp
        paint.color = ContextCompat.getColor(context, R.color.colorSecondary)
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        canvas.drawText(summary, width / 2f, height - 8f.dp, paint)
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
