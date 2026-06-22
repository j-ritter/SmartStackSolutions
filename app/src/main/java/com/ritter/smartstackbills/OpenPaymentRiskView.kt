package com.ritter.smartstackbills

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

class OpenPaymentRiskView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var overdue = Segment()
    private var dueSoon = Segment()
    private var later = Segment()

    fun setData(
        overdueAmount: Double,
        overdueCount: Int,
        dueSoonAmount: Double,
        dueSoonCount: Int,
        laterAmount: Double,
        laterCount: Int
    ) {
        overdue = Segment(overdueAmount, overdueCount)
        dueSoon = Segment(dueSoonAmount, dueSoonCount)
        later = Segment(laterAmount, laterCount)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val gap = 8f.dp
        val usable = width - paddingLeft - paddingRight - gap * 2
        val cardWidth = usable / 3f
        val top = paddingTop.toFloat()
        val bottom = height - paddingBottom.toFloat()
        drawSegment(canvas, paddingLeft.toFloat(), top, cardWidth, bottom, overdue, R.string.overdue, R.color.red)
        drawSegment(canvas, paddingLeft + cardWidth + gap, top, cardWidth, bottom, dueSoon, R.string.due_next_seven_days, R.color.colorPrimary)
        drawSegment(canvas, paddingLeft + (cardWidth + gap) * 2, top, cardWidth, bottom, later, R.string.later_this_month, R.color.bill_color)
    }

    private fun drawSegment(
        canvas: Canvas,
        left: Float,
        top: Float,
        cardWidth: Float,
        bottom: Float,
        segment: Segment,
        labelRes: Int,
        colorRes: Int
    ) {
        val accent = ContextCompat.getColor(context, colorRes)
        paint.color = accent.withAlpha(22)
        canvas.drawRoundRect(RectF(left, top, left + cardWidth, bottom), 12f.dp, 12f.dp, paint)
        paint.color = accent
        canvas.drawRoundRect(RectF(left, top, left + cardWidth, top + 5f.dp), 12f.dp, 12f.dp, paint)

        drawCentered(canvas, context.getString(labelRes), left + cardWidth / 2, top + 30f.dp, 10f.sp, accent, true)
        drawCentered(
            canvas,
            CurrencyPreferences.format(context, segment.amount),
            left + cardWidth / 2,
            top + 60f.dp,
            13f.sp,
            ContextCompat.getColor(context, R.color.colorSecondary),
            true
        )
        drawCentered(
            canvas,
            resources.getQuantityString(R.plurals.payment_count, segment.count, segment.count),
            left + cardWidth / 2,
            top + 84f.dp,
            11f.sp,
            0xFF647383.toInt(),
            false
        )
    }

    private fun drawCentered(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        size: Float,
        color: Int,
        bold: Boolean
    ) {
        paint.color = color
        paint.textSize = size
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = if (bold) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
        canvas.drawText(text, x, y, paint)
    }

    private fun Int.withAlpha(alpha: Int) = (this and 0x00FFFFFF) or (alpha shl 24)
    private val Float.dp get() = this * resources.displayMetrics.density
    private val Float.sp get() = this * resources.displayMetrics.scaledDensity
    private data class Segment(val amount: Double = 0.0, val count: Int = 0)
}
