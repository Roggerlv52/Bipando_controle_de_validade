package com.rogger.bp.ui.animation

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View

class ScannerView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var animatedX = 0f

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 2000
        repeatCount = ValueAnimator.INFINITE
        addUpdateListener {
            animatedX = it.animatedValue as Float
            invalidate()
        }
    }

    // 🎨 cores do seu logo
    private val colors = intArrayOf(
        0xFF1DBA00.toInt(),
        0xFF5EDC00.toInt(),
        0xFFB7F000.toInt(),
        0xFFFFFF00.toInt(),
        0xFFFFC107.toInt(),
        0xFFFF9800.toInt(),
        0xFFFF5722.toInt(),
        0xFFF44336.toInt()
    )

    fun start() = animator.start()
    fun stop() = animator.cancel()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val barWidth = width / colors.size.toFloat()

        // 🟩 desenhar barras
        for (i in colors.indices) {
            paint.color = colors[i]
            canvas.drawRect(
                i * barWidth,
                0f,
                (i + 1) * barWidth,
                height.toFloat(),
                paint
            )
        }

        // 🔥 efeito scanner (luz passando)
        val scannerWidth = width / 3f
        val x = (width + scannerWidth) * animatedX - scannerWidth

        val gradient = LinearGradient(
            x, 0f,
            x + scannerWidth, 0f,
            intArrayOf(
                0x00FFFFFF,
                0xAAFFFFFF.toInt(),
                0x00FFFFFF
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )

        paint.shader = gradient
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null
    }
}