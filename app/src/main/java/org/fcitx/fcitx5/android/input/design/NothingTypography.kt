/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.design

import android.graphics.Typeface
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.Theme

enum class KeyboardTextKind { LatinOrNumber, Cjk, Symbol }

object NothingTypographyPolicy {
    fun classify(text: CharSequence): KeyboardTextKind {
        var containsLatinOrNumber = false
        var containsCjk = false
        text.forEach { character ->
            when {
                character in 'A'..'Z' || character in 'a'..'z' || character in '0'..'9' ->
                    containsLatinOrNumber = true
                Character.UnicodeScript.of(character.code) in setOf(
                    Character.UnicodeScript.HAN,
                    Character.UnicodeScript.HIRAGANA,
                    Character.UnicodeScript.KATAKANA,
                    Character.UnicodeScript.HANGUL
                ) -> containsCjk = true
            }
        }
        return when {
            containsCjk -> KeyboardTextKind.Cjk
            containsLatinOrNumber -> KeyboardTextKind.LatinOrNumber
            else -> KeyboardTextKind.Symbol
        }
    }
}

private object NothingTypefaceCache {
    @Volatile private var regular: Typeface? = null
    @Volatile private var bold: Typeface? = null

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

fun TextView.applyNothingTypography(theme: Theme, bold: Boolean = true) {
    if (!theme.isNothingStyle) return
    typeface = NothingTypefaceCache.get(this, bold)
    letterSpacing = 0.015f
}

fun TextView.nothingTypeface(theme: Theme, bold: Boolean = true): Typeface? =
    if (theme.isNothingStyle) NothingTypefaceCache.get(this, bold) else null

class NothingTypefaceSpan(private val newTypeface: Typeface) : MetricAffectingSpan() {
    override fun updateDrawState(textPaint: TextPaint) = apply(textPaint)
    override fun updateMeasureState(textPaint: TextPaint) = apply(textPaint)

    private fun apply(textPaint: TextPaint) {
        textPaint.typeface = newTypeface
    }
}

/** Doto for Latin letters/numbers; Android's system font for CJK and symbols. */
fun TextView.applyNothingLegendTypography(theme: Theme, text: CharSequence, bold: Boolean = true) {
    if (theme.isNothingStyle &&
        NothingTypographyPolicy.classify(text) == KeyboardTextKind.LatinOrNumber
    ) {
        applyNothingTypography(theme, bold)
    } else {
        typeface = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
        letterSpacing = 0f
    }
}
