/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.gesture

/** Pure swipe-distance accounting. It knows nothing about Views, actions or feedback. */
class SwipeTracker {
    data class Step(
        val countX: Int,
        val countY: Int,
        val totalX: Int,
        val totalY: Int
    ) {
        val crossedThreshold: Boolean
            get() = countX != 0 || countY != 0
    }

    private var lastX = 0f
    private var lastY = 0f
    private var unconsumedX = 0f
    private var unconsumedY = 0f

    var totalX: Int = 0
        private set
    var totalY: Int = 0
        private set

    fun start(x: Float, y: Float) {
        lastX = x
        lastY = y
        unconsumedX = 0f
        unconsumedY = 0f
        totalX = 0
        totalY = 0
    }

    fun move(x: Float, y: Float, thresholdX: Float, thresholdY: Float): Step {
        val countX = consume(x - lastX + unconsumedX, thresholdX).also {
            unconsumedX = it.second
            totalX += it.first
        }.first
        val countY = consume(y - lastY + unconsumedY, thresholdY).also {
            unconsumedY = it.second
            totalY += it.first
        }.first
        lastX = x
        lastY = y
        return Step(countX, countY, totalX, totalY)
    }

    fun reset() = start(0f, 0f)

    private fun consume(distance: Float, threshold: Float): Pair<Int, Float> {
        require(threshold > 0f) { "Swipe threshold must be positive" }
        val count = (distance / threshold).toInt()
        return count to (distance % threshold)
    }
}
