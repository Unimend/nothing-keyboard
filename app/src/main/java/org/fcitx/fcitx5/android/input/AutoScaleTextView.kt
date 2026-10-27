/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.text.Spanned
import android.text.TextPaint
import android.text.style.CharacterStyle
import android.util.AttributeSet
import android.view.Gravity
import android.widget.TextView
import androidx.core.graphics.withSave
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@SuppressLint("AppCompatCustomView")
class AutoScaleTextView @JvmOverloads constructor(
    context: Context?,
    attributeSet: AttributeSet? = null
) : TextView(context, attributeSet) {

    enum class Mode {
        /**
         * do not scale or ellipse text, overflow when cannot fit width
         */
        None,

        /**
         * only scale in X axis, makes text looks "condensed" or "slim"
         */
        Horizontal,

        /**
         * scale both in X and Y axis, align center vertically
         */
        Proportional
    }

    var scaleMode = Mode.None

    private var needsMeasureText = true
    private val fontMetrics = Paint.FontMetrics()
    private val textBounds = Rect()

    private var needsCalculateTransform = true
    private var baselineX = 0.0f
    private var baselineY = 0.0f
    private var textScaleX = 1.0f
    private var textScaleY = 1.0f

    private fun invalidateTextMetrics() {
        needsMeasureText = true
        needsCalculateTransform = true
        requestLayout()
        invalidate()
    }

    override fun setText(charSequence: CharSequence?, bufferType: BufferType) {
        if (charSequence == null || !text.contentEquals(charSequence) || charSequence is Spanned) {
            super.setText(charSequence, bufferType)
            invalidateTextMetrics()
        }
    }

    override fun setTextSize(unit: Int, size: Float) {
        super.setTextSize(unit, size)
        invalidateTextMetrics()
    }

    override fun setTypeface(typeface: Typeface?) {
        super.setTypeface(typeface)
        invalidateTextMetrics()
    }

    override fun setTypeface(typeface: Typeface?, style: Int) {
        super.setTypeface(typeface, style)
        invalidateTextMetrics()
    }

    override fun setLetterSpacing(letterSpacing: Float) {
        super.setLetterSpacing(letterSpacing)
        invalidateTextMetrics()
    }

    override fun setTextScaleX(size: Float) {
        super.setTextScaleX(size)
        invalidateTextMetrics()
    }

    override fun setGravity(gravity: Int) {
        super.setGravity(gravity)
        needsCalculateTransform = true
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val width = measureTextBounds().width() + paddingLeft + paddingRight
        val height = ceil(fontMetrics.bottom - fontMetrics.top + paddingTop + paddingBottom).toInt()
        val maxHeight = if (maxHeight >= 0) maxHeight else Int.MAX_VALUE
        val maxWidth = if (maxWidth >= 0) maxWidth else Int.MAX_VALUE
        setMeasuredDimension(
            measure(widthMode, widthSize, min(max(width, minimumWidth), maxWidth)),
            measure(heightMode, heightSize, min(max(height, minimumHeight), maxHeight))
        )
    }

    private fun measure(specMode: Int, specSize: Int, calculatedSize: Int): Int = when (specMode) {
        MeasureSpec.EXACTLY -> specSize
        MeasureSpec.AT_MOST -> min(calculatedSize, specSize)
        else -> calculatedSize
    }

    private fun measureTextBounds(): Rect {
        if (needsMeasureText) {
            var width = 0f
            var top = Float.POSITIVE_INFINITY
            var bottom = Float.NEGATIVE_INFINITY
            forEachStyledRun { value, start, end, runPaint ->
                width += runPaint.measureText(value, start, end)
                val metrics = runPaint.fontMetrics
                top = min(top, metrics.top)
                bottom = max(bottom, metrics.bottom)
            }
            if (!top.isFinite() || !bottom.isFinite()) {
                paint.getFontMetrics(fontMetrics)
                top = fontMetrics.top
                bottom = fontMetrics.bottom
            } else {
                fontMetrics.top = top
                fontMetrics.ascent = top
                fontMetrics.descent = bottom
                fontMetrics.bottom = bottom
            }
            val codePointCount = Character.codePointCount(text, 0, text.length)
            if (codePointCount == 1) {
                // use actual text bounds when there is only one "character",
                // e.g. full-width punctuation
                val runPaint = styledPaintAt(0)
                runPaint.getTextBounds(text.toString(), 0, text.length, textBounds)
            } else {
                textBounds.set(
                    /* left = */ 0,
                    /* top = */ floor(top).toInt(),
                    /* right = */ ceil(width).toInt(),
                    /* bottom = */ ceil(bottom).toInt()
                )
            }
            needsMeasureText = false
        }
        return textBounds
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        if (needsCalculateTransform || changed) {
            calculateTransform(right - left, bottom - top)
            needsCalculateTransform = false
        }
    }

    private fun calculateTransform(viewWidth: Int, viewHeight: Int) {
        val contentWidth: Int = viewWidth - paddingLeft - paddingRight
        val contentHeight: Int = viewHeight - paddingTop - paddingBottom
        measureTextBounds()
        val textLeft: Float = textBounds.left.toFloat()
        val textWidth: Float = textBounds.width().toFloat()
        val textTop: Float = fontMetrics.top
        val textHeight: Float = fontMetrics.bottom - fontMetrics.top
        if (textWidth > contentWidth) {
            when (scaleMode) {
                Mode.None -> {
                    textScaleX = 1.0f
                    textScaleY = 1.0f
                }
                Mode.Horizontal -> {
                    textScaleX = contentWidth / textWidth
                    textScaleY = 1.0f
                }
                Mode.Proportional -> {
                    val scale = contentWidth / textWidth
                    textScaleX = scale
                    textScaleY = scale
                }
            }
        } else {
            textScaleX = 1.0f
            textScaleY = 1.0f
        }
        baselineX = calculateBaselineX(paddingLeft, contentWidth, textLeft, textWidth, textScaleX)
        baselineY = calculateBaselineY(paddingTop, contentHeight, textTop, textHeight, textScaleY)
    }

    private fun calculateBaselineX(
        contentLeft: Int,
        contentWidth: Int,
        textLeft: Float,
        textWidth: Float,
        scaleX: Float
    ): Float {
        val scaledTextLeft = textLeft * scaleX
        val scaledTextWidth = textWidth * scaleX
        val horizontalGravity =
            Gravity.getAbsoluteGravity(gravity, layoutDirection) and Gravity.HORIZONTAL_GRAVITY_MASK
        val targetLeft: Float = @SuppressLint("RtlHardcoded") when (horizontalGravity) {
            Gravity.LEFT -> contentLeft.toFloat()
            Gravity.RIGHT -> contentLeft + contentWidth - scaledTextWidth
            else -> contentLeft + (contentWidth - scaledTextWidth) / 2.0f
        }
        return targetLeft - scaledTextLeft
    }

    private fun calculateBaselineY(
        contentTop: Int,
        contentHeight: Int,
        textTop: Float,
        textHeight: Float,
        scaleY: Float
    ): Float {
        val scaledTextTop = textTop * scaleY
        val scaledTextHeight = textHeight * scaleY
        val verticalGravity = gravity and Gravity.VERTICAL_GRAVITY_MASK
        val targetTop: Float = when (verticalGravity) {
            Gravity.TOP -> contentTop.toFloat()
            Gravity.BOTTOM -> contentTop + contentHeight - scaledTextHeight
            else -> contentTop + (contentHeight - scaledTextHeight) / 2.0f
        }
        return targetTop - scaledTextTop
    }

    override fun onDraw(canvas: Canvas) {
        if (needsCalculateTransform) {
            calculateTransform(width, height)
            needsCalculateTransform = false
        }
        canvas.withSave {
            translate(scrollX.toFloat(), scrollY.toFloat())
            translate(baselineX, baselineY)
            scale(textScaleX, textScaleY)
            var runX = 0f
            forEachStyledRun { value, start, end, runPaint ->
                drawText(value, start, end, runX, 0f, runPaint)
                runX += runPaint.measureText(value, start, end)
            }
        }
    }

    private fun styledPaintAt(index: Int): TextPaint {
        val runPaint = TextPaint(paint).apply { color = currentTextColor }
        val spanned = text as? Spanned ?: return runPaint
        spanned.getSpans(index, min(index + 1, spanned.length), CharacterStyle::class.java)
            .forEach { it.updateDrawState(runPaint) }
        return runPaint
    }

    private inline fun forEachStyledRun(
        block: (value: String, start: Int, end: Int, paint: TextPaint) -> Unit
    ) {
        val value = text.toString()
        if (value.isEmpty()) return
        val spanned = text as? Spanned
        if (spanned == null) {
            block(value, 0, value.length, TextPaint(paint).apply { color = currentTextColor })
            return
        }
        var start = 0
        while (start < spanned.length) {
            val end = spanned.nextSpanTransition(start, spanned.length, CharacterStyle::class.java)
            val runPaint = TextPaint(paint).apply { color = currentTextColor }
            spanned.getSpans(start, end, CharacterStyle::class.java)
                .forEach { it.updateDrawState(runPaint) }
            block(value, start, end, runPaint)
            start = end
        }
    }

    override fun getBaseline(): Int {
        return paddingTop + baselineY.roundToInt()
    }
}
