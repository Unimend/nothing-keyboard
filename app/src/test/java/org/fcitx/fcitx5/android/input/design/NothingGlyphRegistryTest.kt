/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.design

import org.fcitx.fcitx5.android.R
import org.junit.Assert.assertEquals
import org.junit.Test

class NothingGlyphRegistryTest {
    @Test
    fun `maps keyboard actions to pixel glyphs`() {
        assertEquals(
            R.drawable.ic_pixel_backspace,
            NothingGlyphRegistry.resolveRequired(R.drawable.ic_baseline_backspace_24)
        )
        assertEquals(
            R.drawable.ic_pixel_send,
            NothingGlyphRegistry.resolveRequired(R.drawable.ic_baseline_send_24)
        )
        assertEquals(
            R.drawable.ic_pixel_expand_more,
            NothingGlyphRegistry.resolveRequired(R.drawable.ic_baseline_expand_more_24)
        )
        assertEquals(
            R.drawable.ic_pixel_expand_less,
            NothingGlyphRegistry.resolveRequired(R.drawable.ic_baseline_expand_less_24)
        )
        assertEquals(
            R.drawable.ic_pixel_up,
            NothingGlyphRegistry.resolveRequired(R.drawable.ic_baseline_arrow_upward_24)
        )
        assertEquals(
            R.drawable.ic_pixel_down,
            NothingGlyphRegistry.resolveRequired(R.drawable.ic_baseline_arrow_downward_24)
        )
        assertEquals(
            R.drawable.ic_pixel_previous,
            NothingGlyphRegistry.resolveRequired(R.drawable.ic_baseline_keyboard_arrow_left_24)
        )
        assertEquals(
            R.drawable.ic_pixel_next,
            NothingGlyphRegistry.resolveRequired(R.drawable.ic_baseline_keyboard_arrow_right_24)
        )
    }

    @Test
    fun `legacy project controls resolve to pixel glyphs even through the optional boundary`() {
        assertEquals(
            R.drawable.ic_pixel_delete,
            NothingGlyphRegistry.resolveOrKeepOriginal(R.drawable.ic_baseline_delete_24)
        )
    }

    @Test
    fun `required project controls resolve every registered legacy glyph`() {
        assertEquals(
            R.drawable.ic_pixel_delete,
            NothingGlyphRegistry.resolveRequired(R.drawable.ic_baseline_delete_24)
        )
        assertEquals(
            R.drawable.ic_pixel_pin,
            NothingGlyphRegistry.resolveRequired(R.drawable.ic_baseline_push_pin_24)
        )
        assertEquals(
            R.drawable.ic_pixel_keyboard,
            NothingGlyphRegistry.resolveRequired(R.drawable.ic_baseline_keyboard_24)
        )
    }
}
