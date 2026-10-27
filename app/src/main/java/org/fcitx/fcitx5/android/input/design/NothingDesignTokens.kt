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
    object Shape {
        const val KeyCornerRadiusDp = 8f
        const val SpaceCornerRadiusDp = 3f
        const val ClipboardCornerRadiusDp = 5f
    }

    object Typography {
        const val KeyAltLegendSp = 10.5f
        const val ClipboardEntrySp = 15f
    }

    object Motion {
        const val InstantFeedbackMs = 100L
        const val QuickFeedbackMs = 120L
    }

    object Bar {
        const val HeightDp = 50
    }

    object Candidate {
        const val TextSp = 20f
        const val ExpandedTabTextSp = 16f
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
