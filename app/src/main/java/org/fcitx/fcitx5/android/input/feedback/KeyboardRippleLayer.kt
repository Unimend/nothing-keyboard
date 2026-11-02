/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.feedback

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.SystemClock
import android.view.View
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/** A keyboard-wide, non-interactive light field rendered below the key labels. */
class KeyboardRippleLayer(context: Context) : View(context) {
    private val density = resources.displayMetrics.density
    private val dotPaint = Paint().apply { style = Paint.Style.FILL }
    private val dotSize = density * 2.4f
    private data class Ripple(
        val centerX: Float,
        val centerY: Float,
        val startedAt: Long,
        val radiusScale: Float,
        var progress: Float = 0f
    )

    private val ripples = mutableListOf<Ripple>()
    private var frameScheduled = false
    private val frameTicker = object : Runnable {
        override fun run() {
            val now = SystemClock.uptimeMillis()
            ripples.forEach { ripple ->
                ripple.progress = ((now - ripple.startedAt).toFloat() / DurationMs)
                    .coerceIn(0f, 1f)
            }
            ripples.removeAll { it.progress >= 1f }
            invalidate()
            if (ripples.isNotEmpty()) {
                postOnAnimation(this)
            } else {
                frameScheduled = false
            }
        }
    }

    init {
        isClickable = false
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    fun pulse(x: Float, y: Float) {
        // Real elapsed time deliberately ignores Android's global animator-duration scale.
        ripples += Ripple(
            x,
            y,
            SystemClock.uptimeMillis(),
            Random.nextFloat() * (MaxRadiusScale - MinRadiusScale) + MinRadiusScale
        )
        if (!frameScheduled) {
            frameScheduled = true
            postOnAnimation(frameTicker)
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (ripples.isEmpty() || width == 0 || height == 0) return
        val maxRadius = min(width, height) * 0.29f
        ripples.toList().forEach { ripple ->
            val eased = 1f - (1f - ripple.progress) * (1f - ripple.progress)
            val radius = maxRadius * ripple.radiusScale * (0.20f + eased * 0.80f)
            val fade = (1f - ripple.progress).coerceAtLeast(0f)
            dotPaint.color = Color.argb((168 * fade).toInt(), 255, 255, 255)
            val halfDot = dotSize / 2f
            repeat(DotCount) { index ->
                val angle = 2.0 * PI * index / DotCount
                val x = ripple.centerX + cos(angle).toFloat() * radius
                val y = ripple.centerY + sin(angle).toFloat() * radius
                canvas.drawRect(
                    x - halfDot,
                    y - halfDot,
                    x + halfDot,
                    y + halfDot,
                    dotPaint
                )
            }
        }
    }

    private fun cancelAll() {
        ripples.clear()
        removeCallbacks(frameTicker)
        frameScheduled = false
        invalidate()
    }

    override fun onDetachedFromWindow() {
        cancelAll()
        super.onDetachedFromWindow()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility != VISIBLE) cancelAll()
    }

    private companion object {
        const val DurationMs = 760f
        const val DotCount = 36
        const val MinRadiusScale = 0.72f
        const val MaxRadiusScale = 1.16f
    }
}
