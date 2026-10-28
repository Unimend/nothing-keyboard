/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.input.popup

data class PopupPlacement(val offsetX: Int, val offsetY: Int)

/** Pure edge-safe placement. Coordinates stay relative to the trigger for gesture hit testing. */
object PopupGeometry {
    fun place(
        outerLeft: Int,
        outerTop: Int,
        outerRight: Int,
        outerBottom: Int,
        triggerLeft: Int,
        triggerTop: Int,
        desiredOffsetX: Int,
        desiredOffsetY: Int,
        width: Int,
        height: Int
    ): PopupPlacement {
        val desiredLeft = triggerLeft + desiredOffsetX
        val desiredTop = triggerTop + desiredOffsetY
        val maxLeft = (outerRight - width).coerceAtLeast(outerLeft)
        val maxTop = (outerBottom - height).coerceAtLeast(outerTop)
        val left = desiredLeft.coerceIn(outerLeft, maxLeft)
        val top = desiredTop.coerceIn(outerTop, maxTop)
        return PopupPlacement(left - triggerLeft, top - triggerTop)
    }
}
