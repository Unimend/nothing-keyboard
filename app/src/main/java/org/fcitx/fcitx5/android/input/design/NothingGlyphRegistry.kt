/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.design

import androidx.annotation.DrawableRes
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.Theme

/** Maps semantic/system keyboard icons to the Nothing pixel glyph set. */
object NothingGlyphRegistry {
    @DrawableRes
    fun resolve(@DrawableRes source: Int, theme: Theme): Int =
        if (theme.isNothingStyle) resolve(source) else source

    @DrawableRes
    fun resolve(@DrawableRes source: Int): Int = when (source) {
        R.drawable.ic_capslock_none -> R.drawable.ic_pixel_caps_none
        R.drawable.ic_capslock_once -> R.drawable.ic_pixel_caps_once
        R.drawable.ic_capslock_lock -> R.drawable.ic_pixel_caps_lock
        R.drawable.ic_baseline_backspace_24 -> R.drawable.ic_pixel_backspace
        R.drawable.ic_baseline_keyboard_return_24 -> R.drawable.ic_pixel_return
        R.drawable.ic_baseline_arrow_forward_24 -> R.drawable.ic_pixel_forward
        R.drawable.ic_baseline_search_24 -> R.drawable.ic_pixel_search
        R.drawable.ic_baseline_send_24 -> R.drawable.ic_pixel_send
        R.drawable.ic_baseline_keyboard_tab_24 -> R.drawable.ic_pixel_next
        R.drawable.ic_baseline_done_24 -> R.drawable.ic_pixel_done
        R.drawable.ic_baseline_keyboard_tab_reverse_24 -> R.drawable.ic_pixel_previous
        R.drawable.ic_baseline_language_24 -> R.drawable.ic_pixel_language
        R.drawable.ic_baseline_tag_faces_24 -> R.drawable.ic_pixel_face
        R.drawable.ic_baseline_format_quote_24 -> R.drawable.ic_pixel_quote
        R.drawable.ic_baseline_space_bar_24 -> R.drawable.ic_pixel_space
        R.drawable.ic_baseline_arrow_drop_down_24 -> R.drawable.ic_pixel_hide_keyboard
        else -> source
    }
}
