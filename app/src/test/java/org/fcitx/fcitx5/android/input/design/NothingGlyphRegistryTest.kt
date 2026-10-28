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
            NothingGlyphRegistry.resolve(R.drawable.ic_baseline_backspace_24)
        )
        assertEquals(
            R.drawable.ic_pixel_send,
            NothingGlyphRegistry.resolve(R.drawable.ic_baseline_send_24)
        )
        assertEquals(
            R.drawable.ic_pixel_expand_more,
            NothingGlyphRegistry.resolve(R.drawable.ic_baseline_expand_more_24)
        )
        assertEquals(
            R.drawable.ic_pixel_expand_less,
            NothingGlyphRegistry.resolve(R.drawable.ic_baseline_expand_less_24)
        )
    }

    @Test
    fun `preserves glyphs without a Nothing equivalent`() {
        assertEquals(
            R.drawable.ic_baseline_delete_24,
            NothingGlyphRegistry.resolve(R.drawable.ic_baseline_delete_24)
        )
    }
}
