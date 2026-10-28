/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.design

import org.fcitx.fcitx5.android.data.theme.ThemePreset
import org.junit.Assert.assertEquals
import org.junit.Test

class InputColorSchemeTest {
    @Test
    fun nothingThemeUsesVisualSystemSurfaceRoles() {
        val colors = ThemePreset.NothingDark.toInputColorScheme()

        assertEquals(NothingDesignTokens.Color.Surface, colors.surface)
        assertEquals(NothingDesignTokens.Color.RaisedSurface, colors.clipboardContainer)
        assertEquals(ThemePreset.NothingDark.keyPressHighlightColor, colors.pressed)
    }

    @Test
    fun legacyThemePreservesPersistedSurfaceColors() {
        val theme = ThemePreset.MaterialDark
        val colors = theme.toInputColorScheme()

        assertEquals(theme.keyboardColor, colors.surface)
        assertEquals(theme.barColor, colors.barSurface)
        assertEquals(theme.clipboardEntryColor, colors.clipboardContainer)
    }
}
