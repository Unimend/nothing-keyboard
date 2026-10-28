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

/** Complete semantic text styles. Components choose a meaning, never a font or raw size. */
enum class NothingTextStyle(
    val role: NothingTextRole,
    val sizeSp: Float,
    val bold: Boolean
) {
    KeyPrimary(NothingTextRole.AdaptiveContent, 23f, false),
    KeyAlternate(NothingTextRole.AdaptiveContent, 10.5f, true),
    PopupKey(NothingTextRole.AdaptiveContent, 23f, true),
    Candidate(NothingTextRole.AdaptiveContent, 20f, true),
    CandidateTab(NothingTextRole.AdaptiveContent, 16f, false),
    Preedit(NothingTextRole.AdaptiveContent, 16f, true),
    PickerTab(NothingTextRole.AdaptiveContent, 16f, true),
    StatusIcon(NothingTextRole.AdaptiveContent, 20f, true),
    StatusLabel(NothingTextRole.ControlLabel, 12f, true),
    ControlLabel(NothingTextRole.ControlLabel, 16f, true),
    ClipboardEntry(NothingTextRole.SystemContent, 15f, false),
    Body(NothingTextRole.SystemContent, 14f, false)
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
                isCjk(character) -> containsCjk = true
            }
        }
        return when {
            containsCjk -> KeyboardTextKind.Cjk
            containsLatinOrNumber -> KeyboardTextKind.LatinOrNumber
            else -> KeyboardTextKind.Symbol
        }
    }

    /** API 23-safe BMP ranges used by Android's CJK system fonts. */
    private fun isCjk(character: Char): Boolean = character.code in 0x3040..0x30FF ||
        character.code in 0x3130..0x318F ||
        character.code in 0x31F0..0x31FF ||
        character.code in 0x3400..0x4DBF ||
        character.code in 0x4E00..0x9FFF ||
        character.code in 0x1100..0x11FF ||
        character.code in 0xA960..0xA97F ||
        character.code in 0xAC00..0xD7AF ||
        character.code in 0xD7B0..0xD7FF ||
        character.code in 0xF900..0xFAFF
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
private fun TextView.applyNothingTextRole(
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

fun TextView.applyNothingTextStyle(
    theme: Theme,
    style: NothingTextStyle,
    sizeSp: Float? = style.sizeSp,
    bold: Boolean = style.bold
) {
    if (sizeSp != null) textSize = sizeSp
    applyNothingTextRole(theme, style.role, bold)
}

/** Sets dynamic content and reapplies its complete semantic style in the required order. */
fun TextView.setNothingText(
    value: CharSequence,
    theme: Theme,
    style: NothingTextStyle,
    sizeSp: Float? = style.sizeSp,
    bold: Boolean = style.bold
) {
    text = value
    applyNothingTextStyle(theme, style, sizeSp, bold)
}
