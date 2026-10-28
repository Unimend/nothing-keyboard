/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.input.popup

import org.junit.Assert.assertEquals
import org.junit.Test

class PopupGeometryTest {
    @Test
    fun `keeps desired placement when it fits`() {
        assertEquals(
            PopupPlacement(-20, -60),
            PopupGeometry.place(0, 0, 300, 200, 100, 100, -20, -60, 120, 48)
        )
    }

    @Test
    fun `clamps both horizontal screen edges`() {
        assertEquals(
            PopupPlacement(-10, -40),
            PopupGeometry.place(0, 0, 300, 200, 10, 100, -80, -40, 120, 48)
        )
        assertEquals(
            PopupPlacement(-110, -40),
            PopupGeometry.place(0, 0, 300, 200, 290, 100, 20, -40, 120, 48)
        )
    }

    @Test
    fun `oversized popup anchors to outer origin`() {
        assertEquals(
            PopupPlacement(-100, -80),
            PopupGeometry.place(0, 0, 100, 60, 100, 80, -20, -20, 150, 90)
        )
    }
}
