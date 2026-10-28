/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.view.Gravity
import androidx.annotation.ColorInt

/** A deliberately tiny dot grid used as a Nothing-style state accent. */
class NothingDotMatrixDrawable(
    @ColorInt color: Int,
    private val columns: Int,
    private val rows: Int,
    private val dotDiameter: Float,
    private val dotGap: Float,
    private val gravity: Int = Gravity.CENTER,
    private val horizontalPadding: Float = 0f,
    private val verticalPadding: Float = 0f,
    initialActiveDots: Int = columns * rows,
    private val inactiveAlpha: Float = 1f
) : Drawable() {

    private var activeDots = initialActiveDots.coerceIn(0, columns * rows)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }

    override fun draw(canvas: Canvas) {
        val gridWidth = columns * dotDiameter + (columns - 1) * dotGap
        val gridHeight = rows * dotDiameter + (rows - 1) * dotGap
        val startX = when {
            gravity and Gravity.END == Gravity.END -> bounds.right - horizontalPadding - gridWidth
            gravity and Gravity.START == Gravity.START -> bounds.left + horizontalPadding
            else -> bounds.exactCenterX() - gridWidth / 2f
        }
        val startY = when {
            gravity and Gravity.BOTTOM == Gravity.BOTTOM -> bounds.bottom - verticalPadding - gridHeight
            gravity and Gravity.TOP == Gravity.TOP -> bounds.top + verticalPadding
            else -> bounds.exactCenterY() - gridHeight / 2f
        }
        val radius = dotDiameter / 2f
        repeat(rows) { row ->
            repeat(columns) { column ->
                val index = row * columns + column
                paint.alpha = if (index < activeDots) 255 else (255 * inactiveAlpha).toInt()
                canvas.drawCircle(
                    startX + radius + column * (dotDiameter + dotGap),
                    startY + radius + row * (dotDiameter + dotGap),
                    radius,
                    paint
                )
            }
        }
    }

    fun setActiveDots(count: Int) {
        val next = count.coerceIn(0, columns * rows)
        if (next == activeDots) return
        activeDots = next
        invalidateSelf()
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Android")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
