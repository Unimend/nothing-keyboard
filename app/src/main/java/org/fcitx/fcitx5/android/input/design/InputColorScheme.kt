/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.design

import androidx.annotation.ColorInt
import org.fcitx.fcitx5.android.data.theme.Theme

/** Semantic colors consumed by input surfaces, independent of the persisted theme schema. */
data class InputColorScheme(
    @ColorInt val surface: Int,
    @ColorInt val barSurface: Int,
    @ColorInt val keyContainer: Int,
    @ColorInt val keyContent: Int,
    @ColorInt val alternateKeyContainer: Int,
    @ColorInt val alternateKeyContent: Int,
    @ColorInt val accentContainer: Int,
    @ColorInt val accentContent: Int,
    @ColorInt val pressed: Int,
    @ColorInt val shadow: Int,
    @ColorInt val divider: Int,
    @ColorInt val candidateContent: Int,
    @ColorInt val candidateMarker: Int,
    @ColorInt val candidateComment: Int,
    @ColorInt val clipboardContainer: Int,
    @ColorInt val activeContainer: Int,
    @ColorInt val activeContent: Int
)

/** The only adapter from the legacy/persisted [Theme] color schema into input visual roles. */
fun Theme.toInputColorScheme(): InputColorScheme = InputColorScheme(
    surface = if (isNothingStyle) NothingDesignTokens.Color.Surface else keyboardColor,
    barSurface = if (isNothingStyle) NothingDesignTokens.Color.Surface else barColor,
    keyContainer = keyBackgroundColor,
    keyContent = keyTextColor,
    alternateKeyContainer = altKeyBackgroundColor,
    alternateKeyContent = altKeyTextColor,
    accentContainer = accentKeyBackgroundColor,
    accentContent = accentKeyTextColor,
    pressed = keyPressHighlightColor,
    shadow = keyShadowColor,
    divider = dividerColor,
    candidateContent = candidateTextColor,
    candidateMarker = candidateLabelColor,
    candidateComment = candidateCommentColor,
    clipboardContainer = if (isNothingStyle) {
        NothingDesignTokens.Color.RaisedSurface
    } else clipboardEntryColor,
    activeContainer = genericActiveBackgroundColor,
    activeContent = genericActiveForegroundColor
)
