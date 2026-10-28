/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.wm

import android.view.View
import android.view.inputmethod.CursorAnchorInfo
import org.fcitx.fcitx5.android.input.CandidatesView

data class CandidateCharacterBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

data class CandidateInsertionMarker(
    val horizontal: Float,
    val top: Float,
    val bottom: Float
)

object CandidateAnchorPolicy {
    fun rawAnchor(
        character: CandidateCharacterBounds?,
        insertion: CandidateInsertionMarker,
        isRtl: Boolean
    ): FloatArray {
        val horizontal = if (character == null) insertion.horizontal
            else if (isRtl) character.right else character.left
        val bottom = character?.bottom ?: insertion.bottom
        val top = character?.top ?: insertion.top
        return floatArrayOf(horizontal, bottom, horizontal, top)
    }

    fun isValid(anchor: FloatArray): Boolean =
        anchor.size == 4 && anchor.all(Float::isFinite)
}

/** Owns Android cursor geometry and keeps it out of the input-service lifecycle coordinator. */
class AndroidCandidateAnchorController(
    private val contentView: () -> View,
    private val decorView: () -> View,
    private val candidatesView: () -> CandidatesView?
) {
    private val contentSize = floatArrayOf(0f, 0f)
    private val decorLocation = floatArrayOf(0f, 0f)
    private val decorLocationInt = intArrayOf(0, 0)
    private var decorLocationUpdated = false

    fun invalidateWindowGeometry() {
        decorLocationUpdated = false
    }

    fun showFallback() {
        ensureWindowGeometry()
        candidatesView()?.updateCursorAnchor(contentSize)
    }

    fun update(info: CursorAnchorInfo) {
        val target = candidatesView() ?: return
        val bounds = info.getCharacterBounds(0)?.let {
            CandidateCharacterBounds(it.left, it.top, it.right, it.bottom)
        }
        val anchor = CandidateAnchorPolicy.rawAnchor(
            bounds,
            CandidateInsertionMarker(
                info.insertionMarkerHorizontal,
                info.insertionMarkerTop,
                info.insertionMarkerBottom
            ),
            target.layoutDirection == View.LAYOUT_DIRECTION_RTL
        )
        ensureWindowGeometry()
        if (!CandidateAnchorPolicy.isValid(anchor)) {
            target.updateCursorAnchor(contentSize)
            return
        }
        info.matrix.mapPoints(anchor)
        if (!CandidateAnchorPolicy.isValid(anchor)) {
            target.updateCursorAnchor(contentSize)
            return
        }
        val (xOffset, yOffset) = decorLocation
        anchor[0] -= xOffset
        anchor[1] -= yOffset
        anchor[2] -= xOffset
        anchor[3] -= yOffset
        target.updateCursorAnchor(anchor, contentSize)
    }

    private fun ensureWindowGeometry() {
        if (decorLocationUpdated) return
        val content = contentView()
        val decor = decorView()
        contentSize[0] = content.width.toFloat()
        contentSize[1] = content.height.toFloat()
        decor.getLocationOnScreen(decorLocationInt)
        decorLocation[0] = decorLocationInt[0].toFloat()
        decorLocation[1] = decorLocationInt[1].toFloat()
        // The first onStartInputView can run before measurement. Retry until dimensions are real.
        decorLocationUpdated = contentSize[0] > 0f && contentSize[1] > 0f
    }
}
