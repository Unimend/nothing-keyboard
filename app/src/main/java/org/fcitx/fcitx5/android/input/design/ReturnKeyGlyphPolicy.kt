/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.design

import androidx.annotation.DrawableRes
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.input.session.EditorAction
import org.fcitx.fcitx5.android.input.session.EditorSnapshot

/** Pure visual policy derived from the session snapshot; it owns no mutable compatibility state. */
object ReturnKeyGlyphPolicy {
    @DrawableRes
    fun resolve(editor: EditorSnapshot?, preeditEmpty: Boolean, nothingStyle: Boolean): Int {
        val source = if (!preeditEmpty || editor == null) {
            R.drawable.ic_baseline_keyboard_return_24
        } else when (editor.action) {
            EditorAction.Go -> R.drawable.ic_baseline_arrow_forward_24
            EditorAction.Search -> R.drawable.ic_baseline_search_24
            EditorAction.Send -> R.drawable.ic_baseline_send_24
            EditorAction.Next -> R.drawable.ic_baseline_keyboard_tab_24
            EditorAction.Done -> R.drawable.ic_baseline_done_24
            EditorAction.Previous -> R.drawable.ic_baseline_keyboard_tab_reverse_24
            EditorAction.Return -> R.drawable.ic_baseline_keyboard_return_24
        }
        return if (nothingStyle) NothingGlyphRegistry.resolve(source) else source
    }
}
