/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.design

import org.fcitx.fcitx5.android.data.theme.Theme

/**
 * The single boundary for selecting the custom Nothing visual language.
 *
 * Keeping the theme identity here prevents UI components from learning how a style is selected.
 * It can later move from a theme-name check to a persisted style id without touching every view.
 */
object NothingStyle {
    const val ThemeName = "NothingDark"

    fun isActive(themeName: String): Boolean = themeName == ThemeName
}

val Theme.isNothingStyle: Boolean
    get() = NothingStyle.isActive(name)
