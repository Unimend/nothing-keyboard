/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.session

import org.junit.Assert.assertEquals
import org.junit.Test

class InputSurfaceSessionTest {
    @Test
    fun `records current previous and revision`() {
        val session = InputSessionStore()

        session.dispatch(InputSessionEvent.SurfaceChanged(InputSurface.Keyboard))
        session.dispatch(InputSessionEvent.SurfaceChanged(InputSurface.Editing))

        assertEquals(InputSurface.Editing, session.state.value.surface)
        assertEquals(InputSurface.Keyboard, session.state.value.previousSurface)
        assertEquals(2L, session.state.value.revision)
    }

    @Test
    fun `same surface transition is ignored`() {
        val session = InputSessionStore()
        session.dispatch(InputSessionEvent.SurfaceChanged(InputSurface.Picker("Symbol")))
        val revision = session.state.value.revision

        session.dispatch(InputSessionEvent.SurfaceChanged(InputSurface.Picker("Symbol")))

        assertEquals(revision, session.state.value.revision)
    }
}
