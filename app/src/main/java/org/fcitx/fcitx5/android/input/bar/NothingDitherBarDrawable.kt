package org.fcitx.fcitx5.android.input.bar

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

/** Three full-width vertical opacity steps from the host app into the black IME surface. */
class NothingDitherBarDrawable(density: Float) : Drawable() {
    private val paint = Paint().apply { style = Paint.Style.FILL }
    private val stepHeight = 5f * density
    private val rowAlpha = intArrayOf(48, 128, 218)
    private var drawableAlpha = 255

    override fun draw(canvas: Canvas) {
        paint.color = Color.BLACK
        rowAlpha.forEachIndexed { row, alpha ->
            paint.alpha = alpha * drawableAlpha / 255
            val top = bounds.top + row * stepHeight
            canvas.drawRect(
                bounds.left.toFloat(),
                top,
                bounds.right.toFloat(),
                top + stepHeight,
                paint
            )
        }
        paint.alpha = drawableAlpha
        val solidTop = (bounds.top + rowAlpha.size * stepHeight).coerceAtMost(bounds.bottom.toFloat())
        canvas.drawRect(bounds.left.toFloat(), solidTop, bounds.right.toFloat(), bounds.bottom.toFloat(), paint)
    }

    override fun setAlpha(alpha: Int) {
        drawableAlpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
