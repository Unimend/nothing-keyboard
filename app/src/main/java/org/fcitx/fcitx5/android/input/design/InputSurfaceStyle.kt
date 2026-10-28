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

data class ToolbarSurfaceStyle(
    val controlSizeDp: Int,
    val iconPaddingDp: Int,
    val labelTextSizeSp: Float,
    val titleSpacingDp: Int,
    val extensionEndInsetDp: Int,
    val pressRadiusDp: Int
)

data class EditingSurfaceStyle(
    val shadowWidthDp: Int,
    val contentInsetDp: Int,
    val dividerWidthDp: Int
)

data class PickerSurfaceStyle(
    val denseSymbolTextSizeSp: Float?,
    val punctuationTextSizeSp: Float,
    val paginationHeightDp: Int,
    val embeddedKeyboardHeightFraction: Float
)

data class InputSurfaceStyle(
    val isNothing: Boolean,
    val keyboard: KeyboardSurfaceStyle,
    val toolbar: ToolbarSurfaceStyle,
    val editing: EditingSurfaceStyle,
    val picker: PickerSurfaceStyle,
    val candidate: CandidateSurfaceStyle,
    val clipboard: ClipboardSurfaceStyle
)

object InputSurfaceStylePolicy {
    private val nothingStyle =
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
            toolbar = ToolbarSurfaceStyle(
                controlSizeDp = NothingDesignTokens.Bar.ControlSizeDp,
                iconPaddingDp = NothingDesignTokens.Bar.IconPaddingDp,
                labelTextSizeSp = NothingDesignTokens.Bar.LabelTextSizeSp,
                titleSpacingDp = NothingDesignTokens.Bar.TitleSpacingDp,
                extensionEndInsetDp = NothingDesignTokens.Bar.ExtensionEndInsetDp,
                pressRadiusDp = NothingDesignTokens.Bar.PressRadiusDp
            ),
            editing = EditingSurfaceStyle(
                shadowWidthDp = NothingDesignTokens.Editing.ShadowWidthDp,
                contentInsetDp = NothingDesignTokens.Editing.ContentInsetDp,
                dividerWidthDp = NothingDesignTokens.Editing.DividerWidthDp
            ),
            picker = PickerSurfaceStyle(
                denseSymbolTextSizeSp = NothingDesignTokens.Picker.DenseSymbolTextSizeSp,
                punctuationTextSizeSp = NothingDesignTokens.Picker.PunctuationTextSizeSp,
                paginationHeightDp = NothingDesignTokens.Picker.PaginationHeightDp,
                embeddedKeyboardHeightFraction =
                    NothingDesignTokens.Picker.EmbeddedKeyboardHeightFraction
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

    private val defaultStyle =
        InputSurfaceStyle(
            isNothing = false,
            keyboard = KeyboardSurfaceStyle(
                keyCornerRadiusDp = null,
                heightScale = 1f,
                showSpaceIndicator = false,
                alternateTextSizeSp = 10.666667f,
                primaryLabelVerticalOffsetDp = 0
            ),
            toolbar = ToolbarSurfaceStyle(
                controlSizeDp = 40,
                iconPaddingDp = 10,
                labelTextSizeSp = 24f,
                titleSpacingDp = 8,
                extensionEndInsetDp = 5,
                pressRadiusDp = 20
            ),
            editing = EditingSurfaceStyle(
                shadowWidthDp = 1,
                contentInsetDp = 4,
                dividerWidthDp = 1
            ),
            picker = PickerSurfaceStyle(
                denseSymbolTextSizeSp = null,
                punctuationTextSizeSp = 23f,
                paginationHeightDp = 2,
                embeddedKeyboardHeightFraction = 0.25f
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

    fun resolve(isNothing: Boolean): InputSurfaceStyle =
        if (isNothing) nothingStyle else defaultStyle
}

val Theme.inputSurfaceStyle: InputSurfaceStyle
    get() = InputSurfaceStylePolicy.resolve(isNothingStyle)
