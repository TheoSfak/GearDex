package com.geardex.app.ui.garage

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import com.geardex.app.R

class HealthGaugeView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var score = 0
    private var animatedSweep = 0f
    private val maxSweep = 270f // 3/4 circle

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 28f
        strokeCap = Paint.Cap.ROUND
        color = ContextCompat.getColor(context, R.color.surface_elevated)
    }

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 28f
        strokeCap = Paint.Cap.ROUND
    }

    private val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 72f * resources.displayMetrics.density / 2.5f
        isFakeBoldText = true
        color = ContextCompat.getColor(context, R.color.text_primary)
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 14f * resources.displayMetrics.density
        color = ContextCompat.getColor(context, R.color.text_secondary)
    }

    private val arcRect = RectF()

    fun setScore(value: Int, animate: Boolean = true) {
        score = value.coerceIn(0, 100)
        arcPaint.color = ContextCompat.getColor(context, getScoreColor(score))

        if (animate) {
            val target = (score / 100f) * maxSweep
            ValueAnimator.ofFloat(0f, target).apply {
                duration = 1200
                interpolator = DecelerateInterpolator(2f)
                addUpdateListener {
                    animatedSweep = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        } else {
            animatedSweep = (score / 100f) * maxSweep
            invalidate()
        }
    }

    private fun getScoreColor(s: Int): Int = when {
        s >= 70 -> R.color.score_good
        s >= 40 -> R.color.score_fair
        else -> R.color.score_poor
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = minOf(cx, cy) - trackPaint.strokeWidth
        arcRect.set(cx - radius, cy - radius, cx + radius, cy + radius)

        // Start at 135° (bottom-left), sweep 270° for full track
        canvas.drawArc(arcRect, 135f, maxSweep, false, trackPaint)
        canvas.drawArc(arcRect, 135f, animatedSweep, false, arcPaint)

        // Score text
        canvas.drawText(score.toString(), cx, cy + scorePaint.textSize / 3f, scorePaint)
        canvas.drawText(context.getString(R.string.health_out_of_100), cx, cy + scorePaint.textSize / 3f + labelPaint.textSize + 8f, labelPaint)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = MeasureSpec.getSize(widthMeasureSpec).coerceAtMost(MeasureSpec.getSize(heightMeasureSpec))
        setMeasuredDimension(size, size)
    }
}
