/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input

import android.graphics.Typeface
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.Theme

private object NothingTypefaceCache {
    @Volatile
    private var regular: Typeface? = null

    @Volatile
    private var bold: Typeface? = null

    fun get(textView: TextView, useBold: Boolean): Typeface {
        val cached = if (useBold) bold else regular
        if (cached != null) return cached
        return synchronized(this) {
            val secondCheck = if (useBold) bold else regular
            secondCheck ?: run {
                val base = ResourcesCompat.getFont(textView.context, R.font.doto)
                    ?: Typeface.create("sans-serif-medium", Typeface.NORMAL)
                Typeface.create(base, if (useBold) Typeface.BOLD else Typeface.NORMAL).also {
                    if (useBold) bold = it else regular = it
                }
            }
        }
    }
}

/** Keeps every text surface in the Nothing theme on the same font stack. */
fun TextView.applyNothingTypography(theme: Theme, bold: Boolean = true) {
    if (theme.name != "NothingDark") return
    typeface = NothingTypefaceCache.get(this, bold)
    letterSpacing = 0.015f
}

fun TextView.nothingTypeface(theme: Theme, bold: Boolean = true): Typeface? =
    if (theme.name == "NothingDark") NothingTypefaceCache.get(this, bold) else null

class NothingTypefaceSpan(private val newTypeface: Typeface) : MetricAffectingSpan() {
    override fun updateDrawState(textPaint: TextPaint) = apply(textPaint)
    override fun updateMeasureState(textPaint: TextPaint) = apply(textPaint)

    private fun apply(textPaint: TextPaint) {
        textPaint.typeface = newTypeface
    }
}

/** Doto for Latin letters/numbers; Android's system font for punctuation and symbols. */
fun TextView.applyNothingLegendTypography(theme: Theme, text: CharSequence, bold: Boolean = true) {
    val useDoto = text.any { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' }
    if (theme.name == "NothingDark" && useDoto) {
        applyNothingTypography(theme, bold)
    } else {
        typeface = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
        letterSpacing = 0f
    }
}
