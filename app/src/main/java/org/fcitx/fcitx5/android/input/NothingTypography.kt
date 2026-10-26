/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input

import android.graphics.Typeface
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
