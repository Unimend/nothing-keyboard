/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipeTrackerTest {
    @Test
    fun `keeps remainder between move events`() {
        val tracker = SwipeTracker().apply { start(10f, 20f) }

        assertFalse(tracker.move(18f, 20f, 10f, 10f).crossedThreshold)
        val step = tracker.move(23f, 20f, 10f, 10f)

        assertEquals(1, step.countX)
        assertEquals(1, step.totalX)
    }

    @Test
    fun `tracks direction and axes independently`() {
        val tracker = SwipeTracker().apply { start(50f, 50f) }

        val step = tracker.move(29f, 75f, 10f, 10f)

        assertEquals(-2, step.countX)
        assertEquals(2, step.countY)
        assertTrue(step.crossedThreshold)
    }

    @Test
    fun `reset starts a new gesture`() {
        val tracker = SwipeTracker().apply { start(0f, 0f) }
        tracker.move(30f, 0f, 10f, 10f)

        tracker.start(100f, 100f)
        val step = tracker.move(105f, 105f, 10f, 10f)

        assertEquals(0, step.totalX)
        assertEquals(0, step.totalY)
    }
}
