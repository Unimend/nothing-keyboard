package org.fcitx.fcitx5.android.input.design

import android.graphics.Typeface
import android.text.Spanned
import android.text.style.*
import androidx.core.text.buildSpannedString
import org.fcitx.fcitx5.android.input.session.EngineText
import org.fcitx.fcitx5.android.input.session.EngineTextStyle

/** Shared semantic text styling for the editor composition and keyboard preedit surfaces. */
internal fun renderEngineText(text: EngineText, highlightColor: Int) = buildSpannedString {
    text.segments.forEach { segment ->
        val start = length
        append(segment.text)
        segment.styles.forEach { style ->
            val span = when (style) {
                EngineTextStyle.Underline -> UnderlineSpan()
                EngineTextStyle.Highlight -> BackgroundColorSpan(highlightColor)
                EngineTextStyle.Bold -> StyleSpan(Typeface.BOLD)
                EngineTextStyle.Italic -> StyleSpan(Typeface.ITALIC)
                EngineTextStyle.Strike -> StrikethroughSpan()
                EngineTextStyle.DontCommit -> null
            }
            if (span != null) setSpan(span, start, length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        }
    }
}
