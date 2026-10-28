/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.design

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InputSurfaceStylePolicyTest {
    @Test
    fun nothingStyleResolvesCompleteSemanticSurfaceSpecs() {
        val style = InputSurfaceStylePolicy.resolve(isNothing = true)

        assertTrue(style.isNothing)
        assertEquals(NothingDesignTokens.Shape.KeyCornerRadiusDp, style.keyboard.keyCornerRadiusDp)
        assertTrue(style.keyboard.showSpaceIndicator)
        assertEquals(NothingDesignTokens.Bar.ControlSizeDp, style.toolbar.controlSizeDp)
        assertEquals(
            NothingDesignTokens.Picker.DenseSymbolTextSizeSp,
            style.picker.denseSymbolTextSizeSp
        )
        assertTrue(style.candidate.showDotMarker)
        assertEquals(NothingDesignTokens.Candidate.NothingLeadingRailDp, style.candidate.leadingRailDp)
        assertTrue(style.clipboard.showDotMarker)
        assertEquals(NothingTextStyle.ClipboardEntry.sizeSp, style.clipboard.entryTextSizeSp)
    }

    @Test
    fun nonNothingStyleKeepsLegacySurfaceGeometry() {
        val style = InputSurfaceStylePolicy.resolve(isNothing = false)

        assertFalse(style.isNothing)
        assertNull(style.keyboard.keyCornerRadiusDp)
        assertFalse(style.keyboard.showSpaceIndicator)
        assertNull(style.picker.denseSymbolTextSizeSp)
        assertFalse(style.candidate.showDotMarker)
        assertEquals(NothingDesignTokens.Candidate.DefaultLeadingRailDp, style.candidate.leadingRailDp)
        assertFalse(style.clipboard.showDotMarker)
        assertNull(style.clipboard.entryTextSizeSp)
        assertTrue(style.clipboard.entryCornerRadiusDp.isNaN())
    }
}
