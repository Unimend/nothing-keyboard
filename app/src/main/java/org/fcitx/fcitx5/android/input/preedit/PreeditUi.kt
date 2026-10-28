/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2025 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.preedit

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface.BOLD
import android.graphics.Typeface.ITALIC
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.text.Spanned
import android.text.SpannedString
import android.text.style.DynamicDrawableSpan
import android.text.style.BackgroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.text.buildSpannedString
import org.fcitx.fcitx5.android.core.TextFormatFlag
import org.fcitx.fcitx5.android.core.hasFlag
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.design.NothingTextRole
import org.fcitx.fcitx5.android.input.design.applyNothingTextRole
import org.fcitx.fcitx5.android.input.session.EngineInputPanel
import org.fcitx.fcitx5.android.input.session.EngineText
import splitties.dimensions.dp
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.add
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.textView
import splitties.views.dsl.core.verticalLayout

open class PreeditUi(
    override val ctx: Context,
    private val theme: Theme,
    private val setupTextView: (TextView.() -> Unit)? = null
) : Ui {

    class CursorSpan(ctx: Context, @ColorInt color: Int, metrics: Paint.FontMetricsInt) :
        DynamicDrawableSpan() {
        private val drawable = ShapeDrawable(RectShape()).apply {
            paint.color = color
            setBounds(0, metrics.ascent, ctx.dp(1), metrics.bottom)
        }

        override fun getDrawable() = drawable
    }

    private val cursorSpan by lazy {
        CursorSpan(ctx, theme.keyTextColor, upView.paint.fontMetricsInt)
    }

    private fun createTextView() = textView {
        setTextColor(theme.keyTextColor)
        textSize = 16f
        setupTextView?.invoke(this)
    }

    private val upView = createTextView()

    private val downView = createTextView()

    var visible = false
        private set

    override val root: View = verticalLayout {
        add(upView, lParams())
        add(downView, lParams())
    }

    private fun updateTextView(view: TextView, str: CharSequence, visible: Boolean) {
        view.text = str
        view.applyNothingTextRole(theme, NothingTextRole.AdaptiveContent)
        view.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun EngineText.toSpannedString(highlightColor: Int) = buildSpannedString {
        segments.forEach { segment ->
            val start = length
            append(segment.text)
            val end = length
            val flags = segment.formatFlags
            if (flags.hasFlag(TextFormatFlag.Underline)) setSpan(UnderlineSpan(), start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            if (flags.hasFlag(TextFormatFlag.HighLight)) setSpan(BackgroundColorSpan(highlightColor), start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            if (flags.hasFlag(TextFormatFlag.Bold)) setSpan(StyleSpan(BOLD), start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            if (flags.hasFlag(TextFormatFlag.Strike)) setSpan(StrikethroughSpan(), start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            if (flags.hasFlag(TextFormatFlag.Italic)) setSpan(StyleSpan(ITALIC), start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        }
    }

    fun update(inputPanel: EngineInputPanel) {
        val activeBkg = theme.genericActiveBackgroundColor
        val upString: SpannedString
        val upCursor: Int
        if (inputPanel.auxiliaryUp.isEmpty) {
            upString = inputPanel.preedit.toSpannedString(activeBkg)
            upCursor = inputPanel.preedit.cursor
        } else {
            upString = buildSpannedString {
                append(inputPanel.auxiliaryUp.toSpannedString(activeBkg))
                append(inputPanel.preedit.toSpannedString(activeBkg))
            }
            upCursor = inputPanel.preedit.cursor.let {
                if (it < 0) it
                else inputPanel.auxiliaryUp.plainText.length + it
            }
        }
        val downString = inputPanel.auxiliaryDown.toSpannedString(activeBkg)
        val hasUp = upString.isNotEmpty()
        val hasDown = downString.isNotEmpty()
        visible = hasUp || hasDown
        if (!visible) {
            updateTextView(upView, "", false)
            updateTextView(downView, "", false)
            return
        }
        val upStringWithCursor = if (upCursor < 0 || upCursor == upString.length) {
            upString
        } else buildSpannedString {
            if (upCursor > 0) append(upString, 0, upCursor)
            append('|')
            setSpan(cursorSpan, upCursor, upCursor + 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            append(upString, upCursor, upString.length)
        }
        updateTextView(upView, upStringWithCursor, hasUp)
        updateTextView(downView, downString, hasDown)
    }
}
