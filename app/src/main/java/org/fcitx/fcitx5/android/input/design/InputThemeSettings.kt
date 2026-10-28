/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.input.design

import org.fcitx.fcitx5.android.data.theme.ThemeManager

/**
 * The sole persisted-theme preference gateway for input systems.
 *
 * Renderers consume semantic visual settings from here; they must not retain access to the legacy
 * ThemeManager preference graph, whose storage shape is an application concern.
 */
object InputThemeSettings {
    private val persisted get() = ThemeManager.prefs

    val keyBorder get() = persisted.keyBorder
    val keyBorderStroke get() = persisted.keyBorderStroke
    val keyRippleEffect get() = persisted.keyRippleEffect
    val keyHorizontalMargin get() = persisted.keyHorizontalMargin
    val keyHorizontalMarginLandscape get() = persisted.keyHorizontalMarginLandscape
    val keyVerticalMargin get() = persisted.keyVerticalMargin
    val keyVerticalMarginLandscape get() = persisted.keyVerticalMarginLandscape
    val keyRadius get() = persisted.keyRadius
    val textEditingButtonRadius get() = persisted.textEditingButtonRadius
    val clipboardEntryRadius get() = persisted.clipboardEntryRadius
    val punctuationPosition get() = persisted.punctuationPosition
    val navbarBackground get() = persisted.navbarBackground
}
