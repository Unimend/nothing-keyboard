/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.design

/**
 * Single source of truth for the Nothing keyboard's visual measurements.
 *
 * Components should consume semantic tokens from here instead of embedding unrelated dp/sp
 * literals. Values remain deliberately small and platform-neutral so density conversion stays at
 * the View boundary.
 */
object NothingDesignTokens {
    object Color {
        const val Surface = 0xff000000.toInt()
        const val RaisedSurface = 0xff0d0d0d.toInt()
        const val AccentRed = 0xffd71921.toInt()
    }

    object Shape {
        const val KeyCornerRadiusDp = 8f
        const val SpaceCornerRadiusDp = 3f
        const val ClipboardCornerRadiusDp = 5f
    }

    object Motion {
        const val InstantFeedbackMs = 100L
        const val QuickFeedbackMs = 120L
        const val DotStepMs = 35L
        const val WindowTransitionMs = 100L
        const val CapsFeedbackMs = 110L
        const val ConfirmationActionDelayMs = 140L
    }

    object GestureFeedback {
        const val Columns = 1
        const val Rows = 4
        const val DotDiameterDp = 4f
        const val DotGapDp = 3f
        const val EdgePaddingDp = 7f
        const val InactiveAlpha = 0.16f
    }

    object Bar {
        const val HeightDp = 50
        const val ControlSizeDp = 40
        const val IconPaddingDp = 10
        const val LabelTextSizeSp = 24f
        const val TitleSpacingDp = 8
        const val ExtensionEndInsetDp = 5
        const val PressRadiusDp = 20
    }

    object Editing {
        const val ShadowWidthDp = 1
        const val ContentInsetDp = 4
        const val DividerWidthDp = 1
    }

    object Picker {
        const val DenseSymbolTextSizeSp = 21f
        const val PunctuationTextSizeSp = 23f
        const val PaginationHeightDp = 2
        const val EmbeddedKeyboardHeightFraction = 0.25f
    }

    object Keyboard {
        const val CompactHeightScale = 0.9f
        const val SpaceIndicatorColumns = 5
        const val SpaceIndicatorDotDiameterDp = 2
        const val SpaceIndicatorDotGapDp = 3
        const val SpaceIndicatorWidthDp = 30
        const val SpaceIndicatorHeightDp = 4
        const val PrimaryLabelVerticalOffsetDp = 2
    }

    object Clipboard {
        const val GridGapDp = 6
        const val EntryStartPaddingDp = 22
        const val EntryMinimumHeightDp = 44
        const val MarkerWidthDp = 5
        const val MarkerHeightDp = 24
        const val MarkerRows = 4
        const val MarkerDotDiameterDp = 3
        const val MarkerDotGapDp = 2
    }

    object Candidate {
        const val ExpandedRowHeightDp = 48
        const val MinimumItemWidthDp = 48
        const val HorizontalPaddingDp = 14
        const val NothingLeadingRailDp = 16
        const val DefaultLeadingRailDp = 12
        const val MarkerWidthDp = 4
        const val MarkerHeightDp = 18
        const val MarkerRows = 4
        const val MarkerDotDiameterDp = 2
        const val MarkerDotGapDp = 2
    }
}
