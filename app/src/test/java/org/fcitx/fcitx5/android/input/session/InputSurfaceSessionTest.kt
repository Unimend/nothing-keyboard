/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.session

import org.junit.Assert.assertEquals
import org.junit.Test

class InputSurfaceSessionTest {
    @Test
    fun `records current previous and revision`() {
        val session = InputSurfaceSession()

        session.transitionTo(InputSurface.Keyboard)
        session.transitionTo(InputSurface.Editing)

        assertEquals(InputSurface.Editing, session.state.value.current)
        assertEquals(InputSurface.Keyboard, session.state.value.previous)
        assertEquals(2L, session.state.value.revision)
    }

    @Test
    fun `same surface transition is ignored`() {
        val session = InputSurfaceSession()
        session.transitionTo(InputSurface.Picker("Symbol"))
        val revision = session.state.value.revision

        session.transitionTo(InputSurface.Picker("Symbol"))

        assertEquals(revision, session.state.value.revision)
    }
}
