package com.rogger.bp.ui.animation

import android.animation.ValueAnimator
import android.graphics.drawable.GradientDrawable
import android.view.View

class GradientAnimator(private val view: View) {

    private var animator: ValueAnimator? = null
    fun start() {
        val drawable = view.background

        if (drawable !is GradientDrawable) return

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 4000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE

            addUpdateListener {
                val value = it.animatedValue as Float

                val color1 = blendColors(0xFF008000.toInt(), 0xFF006400.toInt(), value)
                val color2 = blendColors(0xFFFFC107.toInt(), 0xFFF44336.toInt(), value)

                // 🔥 Atualiza as cores do drawable existente em vez de criar um novo
                val drawable = view.background as? GradientDrawable
                if (drawable != null) {
                    drawable.colors = intArrayOf(color1, color2)
                } else {
                    val newDrawable = GradientDrawable(
                        GradientDrawable.Orientation.LEFT_RIGHT,
                        intArrayOf(color1, color2)
                    )
                    newDrawable.cornerRadius = 0f
                    view.background = newDrawable
                }
            }
            start()
        }
    }

    fun stop() {
        animator?.cancel()
    }

    private fun blendColors(from: Int, to: Int, ratio: Float): Int {
        val r =
            (android.graphics.Color.red(to) * ratio + android.graphics.Color.red(from) * (1 - ratio)).toInt()
        val g =
            (android.graphics.Color.green(to) * ratio + android.graphics.Color.green(from) * (1 - ratio)).toInt()
        val b =
            (android.graphics.Color.blue(to) * ratio + android.graphics.Color.blue(from) * (1 - ratio)).toInt()
        return android.graphics.Color.rgb(r, g, b)
    }
}