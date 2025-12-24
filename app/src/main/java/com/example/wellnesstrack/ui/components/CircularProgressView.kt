package com.example.wellnesstrack.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.graphics.Color

/**
 * Simple circular progress view that draws a background arc and a foreground arc.
 * progress is 0f..1f
 */
class CircularProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val fgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val rect = RectF()
    private var strokeWidthPx = 12f
    private var progress = 0f // 0..1
    private var progressColor = Color.parseColor("#2F9BFF")
    private var bgColor = 0xFFDDDDDD.toInt()

    init {
        // default stroke in dp -> px
        val density = resources.displayMetrics.density
        strokeWidthPx = 8f * density
        bgPaint.strokeWidth = strokeWidthPx
        fgPaint.strokeWidth = strokeWidthPx
        fgPaint.color = progressColor
        bgPaint.color = bgColor
        fgPaint.strokeCap = Paint.Cap.ROUND
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = MeasureSpec.getSize(heightMeasureSpec)
        // keep square
        val size = minOf(w, h)
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val half = width / 2f
        val left = strokeWidthPx / 2f
        val top = strokeWidthPx / 2f
        val right = width - strokeWidthPx / 2f
        val bottom = height - strokeWidthPx / 2f
        rect.set(left, top, right, bottom)

        // draw background full circle
        canvas.drawArc(rect, -90f, 360f, false, bgPaint)

        // draw foreground arc for progress
        val sweep = (progress.coerceIn(0f, 1f)) * 360f
        canvas.drawArc(rect, -90f, sweep, false, fgPaint)
    }

    fun setProgressFraction(f: Float) {
        progress = f.coerceIn(0f, 1f)
        invalidate()
    }

    fun setColors(foreground: Int, background: Int) {
        progressColor = foreground
        fgPaint.color = progressColor
        bgColor = background
        bgPaint.color = bgColor
        invalidate()
    }

    fun setStroke(dp: Float) {
        val px = dp * resources.displayMetrics.density
        strokeWidthPx = px
        fgPaint.strokeWidth = px
        bgPaint.strokeWidth = px
        invalidate()
    }
}
