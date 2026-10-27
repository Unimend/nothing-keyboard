/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.design

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.Theme

enum class KeyboardTextKind { LatinOrNumber, Cjk, Symbol }

/** Semantic typography roles used by every input surface. */
enum class NothingTextRole {
    /** Candidates, composition and other mixed content: Doto only for Latin/digit runs. */
    AdaptiveContent,
    /** Keyboard-owned labels such as ENGLISH, PINYIN, CLIPBOARD and editing commands. */
    ControlLabel,
    /** Application/user prose such as clipboard contents and instructions. */
    SystemContent
}

object NothingTypographyPolicy {
    fun usesDoto(character: Char): Boolean =
        character in 'A'..'Z' || character in 'a'..'z' || character in '0'..'9'

    fun canUseWholeDotoTypeface(text: CharSequence): Boolean =
        text.isNotEmpty() && text.all { usesDoto(it) || it.isWhitespace() }

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

class NothingTypefaceSpan(private val newTypeface: Typeface) : MetricAffectingSpan() {
    override fun updateDrawState(textPaint: TextPaint) = apply(textPaint)
    override fun updateMeasureState(textPaint: TextPaint) = apply(textPaint)

    private fun apply(textPaint: TextPaint) {
        textPaint.typeface = newTypeface
    }
}

/**
 * Applies one semantic typography role after [TextView.setText]. Adaptive content is based on a
 * system-font baseline and receives Doto spans only over Latin letters and digits, so mixed text
 * never forces CJK or punctuation through the pixel font.
 */
fun TextView.applyNothingTextRole(
    theme: Theme,
    role: NothingTextRole,
    bold: Boolean = true
) {
    val style = if (bold) Typeface.BOLD else Typeface.NORMAL
    if (!theme.isNothingStyle) {
        typeface = Typeface.create(Typeface.DEFAULT, style)
        letterSpacing = 0f
        return
    }
    when (role) {
        NothingTextRole.ControlLabel -> {
            typeface = NothingTypefaceCache.get(this, bold)
            letterSpacing = 0.015f
        }
        NothingTextRole.SystemContent -> {
            typeface = Typeface.create(Typeface.DEFAULT, style)
            letterSpacing = 0f
        }
        NothingTextRole.AdaptiveContent -> {
            val value = text
            if (value.isEmpty()) return
            if (NothingTypographyPolicy.canUseWholeDotoTypeface(value)) {
                typeface = NothingTypefaceCache.get(this, bold)
                letterSpacing = 0.015f
                return
            }
            typeface = Typeface.create(Typeface.DEFAULT, style)
            letterSpacing = 0f
            val styled = SpannableString(value)
            styled.getSpans(0, styled.length, NothingTypefaceSpan::class.java)
                .forEach(styled::removeSpan)
            val doto = NothingTypefaceCache.get(this, bold)
            var runStart = -1
            for (index in 0..styled.length) {
                val isDoto = index < styled.length &&
                    NothingTypographyPolicy.usesDoto(styled[index])
                if (isDoto && runStart < 0) runStart = index
                if (!isDoto && runStart >= 0) {
                    styled.setSpan(
                        NothingTypefaceSpan(doto), runStart, index,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    runStart = -1
                }
            }
            text = styled
        }
    }
}
