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
    fun resolveOrKeepOriginal(@DrawableRes source: Int, theme: Theme): Int =
        if (theme.isNothingStyle) mappedOrOriginal(source) else source

    @DrawableRes
    fun resolveOrKeepOriginal(@DrawableRes source: Int): Int = mappedOrOriginal(source)

    /**
     * Resolves a project-owned control glyph. Unlike external/content icons, a Nothing control is
     * not allowed to silently fall back to its Material source.
     */
    @DrawableRes
    fun resolveRequired(@DrawableRes source: Int, theme: Theme): Int =
        if (theme.isNothingStyle) resolveRequired(source) else source

    @DrawableRes
    fun resolveRequired(@DrawableRes source: Int): Int {
        val mapped = mappedOrOriginal(source)
        require(mapped != source || source in nativePixelGlyphs) {
            "Nothing control glyph is not registered: $source"
        }
        return mapped
    }

    private val nativePixelGlyphs = setOf(
        R.drawable.ic_pixel_backspace,
        R.drawable.ic_pixel_caps_lock,
        R.drawable.ic_pixel_caps_none,
        R.drawable.ic_pixel_caps_once,
        R.drawable.ic_pixel_clipboard,
        R.drawable.ic_pixel_code,
        R.drawable.ic_pixel_code_off,
        R.drawable.ic_pixel_cursor_move,
        R.drawable.ic_pixel_done,
        R.drawable.ic_pixel_down,
        R.drawable.ic_pixel_expand_less,
        R.drawable.ic_pixel_expand_more,
        R.drawable.ic_pixel_face,
        R.drawable.ic_pixel_forward,
        R.drawable.ic_pixel_hide_keyboard,
        R.drawable.ic_pixel_history,
        R.drawable.ic_pixel_language,
        R.drawable.ic_pixel_more,
        R.drawable.ic_pixel_next,
        R.drawable.ic_pixel_number_pad,
        R.drawable.ic_pixel_keyboard,
        R.drawable.ic_pixel_pin,
        R.drawable.ic_pixel_private,
        R.drawable.ic_pixel_previous,
        R.drawable.ic_pixel_quote,
        R.drawable.ic_pixel_redo,
        R.drawable.ic_pixel_return,
        R.drawable.ic_pixel_search,
        R.drawable.ic_pixel_send,
        R.drawable.ic_pixel_space,
        R.drawable.ic_pixel_undo,
        R.drawable.ic_pixel_unpin,
        R.drawable.ic_pixel_unicode,
        R.drawable.ic_pixel_up,
        R.drawable.ic_pixel_voice,
        R.drawable.ic_pixel_add,
        R.drawable.ic_pixel_delete,
        R.drawable.ic_pixel_drag,
        R.drawable.ic_pixel_edit,
        R.drawable.ic_pixel_settings
    )

    @DrawableRes
    private fun mappedOrOriginal(@DrawableRes source: Int): Int = when (source) {
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
        R.drawable.ic_baseline_expand_more_24 -> R.drawable.ic_pixel_expand_more
        R.drawable.ic_baseline_expand_less_24 -> R.drawable.ic_pixel_expand_less
        R.drawable.ic_baseline_arrow_upward_24,
        R.drawable.ic_baseline_keyboard_arrow_up_24 -> R.drawable.ic_pixel_up
        R.drawable.ic_baseline_arrow_downward_24,
        R.drawable.ic_baseline_keyboard_arrow_down_24 -> R.drawable.ic_pixel_down
        R.drawable.ic_baseline_arrow_back_24,
        R.drawable.ic_baseline_arrow_prev_24,
        R.drawable.ic_baseline_keyboard_arrow_left_24,
        R.drawable.ic_baseline_first_page_24 -> R.drawable.ic_pixel_previous
        R.drawable.ic_baseline_arrow_next_24,
        R.drawable.ic_baseline_keyboard_arrow_right_24,
        R.drawable.ic_baseline_last_page_24 -> R.drawable.ic_pixel_next
        R.drawable.ic_baseline_push_pin_24 -> R.drawable.ic_pixel_pin
        R.drawable.ic_outline_push_pin_24 -> R.drawable.ic_pixel_unpin
        R.drawable.ic_baseline_edit_24 -> R.drawable.ic_pixel_edit
        R.drawable.ic_baseline_share_24 -> R.drawable.ic_pixel_forward
        R.drawable.ic_baseline_delete_24,
        R.drawable.ic_baseline_delete_sweep_24 -> R.drawable.ic_pixel_delete
        R.drawable.ic_baseline_content_paste_24 -> R.drawable.ic_pixel_clipboard
        R.drawable.ic_baseline_spellcheck_24 -> R.drawable.ic_pixel_done
        R.drawable.ic_baseline_text_format_24 -> R.drawable.ic_pixel_quote
        R.drawable.ic_baseline_code_24 -> R.drawable.ic_pixel_code
        R.drawable.ic_baseline_code_off_24 -> R.drawable.ic_pixel_code_off
        R.drawable.ic_baseline_sync_24 -> R.drawable.ic_pixel_redo
        R.drawable.ic_baseline_keyboard_24 -> R.drawable.ic_pixel_keyboard
        R.drawable.ic_baseline_settings_24 -> R.drawable.ic_pixel_settings
        R.drawable.ic_baseline_keyboard_voice_24 -> R.drawable.ic_pixel_voice
        R.drawable.ic_view_private -> R.drawable.ic_pixel_private
        else -> source
    }
}
