/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.design

import org.fcitx.fcitx5.android.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
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
    }

    @Test
    fun `explicit optional resolution preserves external glyphs without an equivalent`() {
        assertEquals(
            R.drawable.ic_baseline_delete_24,
            NothingGlyphRegistry.resolveOrKeepOriginal(R.drawable.ic_baseline_delete_24)
        )
    }

    @Test
    fun `required project controls cannot silently retain Material glyphs`() {
        assertThrows(IllegalArgumentException::class.java) {
            NothingGlyphRegistry.resolveRequired(R.drawable.ic_baseline_delete_24)
        }
    }
}
