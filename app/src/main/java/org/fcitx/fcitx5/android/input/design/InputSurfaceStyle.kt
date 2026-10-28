/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.design

import org.fcitx.fcitx5.android.data.theme.Theme

/**
 * Semantic layout specifications for input surfaces.
 *
 * Views select the surface they render; they must not repeat theme checks and assemble individual
 * measurements themselves. Theme selection remains at this visual-system boundary.
 */
data class CandidateSurfaceStyle(
    val showDotMarker: Boolean,
    val leadingRailDp: Int,
    val markerWidthDp: Int,
    val markerHeightDp: Int
)

data class ClipboardSurfaceStyle(
    val showDotMarker: Boolean,
    val gridGapDp: Int,
    val entryStartPaddingDp: Int,
    val entryMinimumHeightDp: Int,
    val entryCornerRadiusDp: Float,
    val entryTextSizeSp: Float?
)

data class KeyboardSurfaceStyle(
    val keyCornerRadiusDp: Float?,
    val heightScale: Float,
    val showSpaceIndicator: Boolean,
    val alternateTextSizeSp: Float,
    val primaryLabelVerticalOffsetDp: Int
)

data class InputSurfaceStyle(
    val isNothing: Boolean,
    val keyboard: KeyboardSurfaceStyle,
    val candidate: CandidateSurfaceStyle,
    val clipboard: ClipboardSurfaceStyle
)

object InputSurfaceStylePolicy {
    fun resolve(isNothing: Boolean): InputSurfaceStyle = if (isNothing) {
        InputSurfaceStyle(
            isNothing = true,
            keyboard = KeyboardSurfaceStyle(
                keyCornerRadiusDp = NothingDesignTokens.Shape.KeyCornerRadiusDp,
                heightScale = NothingDesignTokens.Keyboard.CompactHeightScale,
                showSpaceIndicator = true,
                alternateTextSizeSp = NothingTextStyle.KeyAlternate.sizeSp,
                primaryLabelVerticalOffsetDp =
                    NothingDesignTokens.Keyboard.PrimaryLabelVerticalOffsetDp
            ),
            candidate = CandidateSurfaceStyle(
                showDotMarker = true,
                leadingRailDp = NothingDesignTokens.Candidate.NothingLeadingRailDp,
                markerWidthDp = NothingDesignTokens.Candidate.MarkerWidthDp,
                markerHeightDp = NothingDesignTokens.Candidate.MarkerHeightDp
            ),
            clipboard = ClipboardSurfaceStyle(
                showDotMarker = true,
                gridGapDp = NothingDesignTokens.Clipboard.GridGapDp,
                entryStartPaddingDp = NothingDesignTokens.Clipboard.EntryStartPaddingDp,
                entryMinimumHeightDp = NothingDesignTokens.Clipboard.EntryMinimumHeightDp,
                entryCornerRadiusDp = NothingDesignTokens.Shape.ClipboardCornerRadiusDp,
                entryTextSizeSp = NothingTextStyle.ClipboardEntry.sizeSp
            )
        )
    } else {
        InputSurfaceStyle(
            isNothing = false,
            keyboard = KeyboardSurfaceStyle(
                keyCornerRadiusDp = null,
                heightScale = 1f,
                showSpaceIndicator = false,
                alternateTextSizeSp = 10.666667f,
                primaryLabelVerticalOffsetDp = 0
            ),
            candidate = CandidateSurfaceStyle(
                showDotMarker = false,
                leadingRailDp = NothingDesignTokens.Candidate.DefaultLeadingRailDp,
                markerWidthDp = 3,
                markerHeightDp = NothingDesignTokens.Candidate.MarkerHeightDp
            ),
            clipboard = ClipboardSurfaceStyle(
                showDotMarker = false,
                gridGapDp = 4,
                entryStartPaddingDp = 8,
                entryMinimumHeightDp = 30,
                entryCornerRadiusDp = Float.NaN,
                entryTextSizeSp = null
            )
        )
    }
}

val Theme.inputSurfaceStyle: InputSurfaceStyle
    get() = InputSurfaceStylePolicy.resolve(isNothingStyle)
