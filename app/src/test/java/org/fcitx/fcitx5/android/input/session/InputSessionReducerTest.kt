/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.session

import org.junit.Assert.assertEquals
import org.junit.Test

class InputSessionReducerTest {
    @Test
    fun readerObservesUpdatesWithoutExposingTheMutableStore() {
        val store = InputSessionStore()
        val reader = store.reader
        org.junit.Assert.assertFalse((reader as Any) is InputSessionStore)
        store.dispatch(InputSessionEvent.SelectionChanged(2, 2))
        assertEquals(SessionRange(2, 2), reader.current.selection)
        assertEquals(reader.current, reader.state.value)
    }

    @Test
    fun inputStartCopiesCallerOwnedCapabilitySet() {
        val capabilities = mutableSetOf(EditorCapability.Password)
        val store = InputSessionStore()
        store.dispatch(InputSessionEvent.InputStarted(editor, capabilities, false))
        capabilities.clear()
        assertEquals(setOf(EditorCapability.Password), store.reader.current.capabilities)
    }

    private val editor = EditorSnapshot(
        packageName = "notes.example",
        inputType = 1,
        imeOptions = 2,
        initialSelection = SessionRange(4, 4)
    )

    @Test
    fun lifecycleAndSelectionShareOneSnapshot() {
        var state = InputSessionSnapshot()
        state = InputSessionReducer.reduce(
            state,
            InputSessionEvent.InputStarted(
                editor, capabilities = setOf(EditorCapability.Password), restarting = false
            )
        )
        state = InputSessionReducer.reduce(
            state,
            InputSessionEvent.InputViewStarted(editor, restarting = false)
        )
        state = InputSessionReducer.reduce(state, InputSessionEvent.SelectionChanged(8, 10))
        state = InputSessionReducer.reduce(
            state,
            InputSessionEvent.SurfaceChanged(InputSurface.Clipboard)
        )

        assertEquals(InputLifecycle.InputViewStarted, state.lifecycle)
        assertEquals(SessionRange(8, 10), state.selection)
        assertEquals(InputSurface.Clipboard, state.surface)
        assertEquals(setOf(EditorCapability.Password), state.capabilities)
        assertEquals(true, state.password)
        assertEquals(
            EditorSessionSnapshot(editor, SessionRange(8, 10), false, true),
            state.editorSessionOrNull()
        )
    }

    @Test
    fun finishViewKeepsEditorButFinishInputClearsSession() {
        var state = InputSessionReducer.reduce(
            InputSessionSnapshot(),
            InputSessionEvent.InputStarted(editor, capabilities = emptySet(), restarting = true)
        )
        state = InputSessionReducer.reduce(
            state,
            InputSessionEvent.SurfaceChanged(InputSurface.Keyboard)
        )
        state = InputSessionReducer.reduce(state, InputSessionEvent.InputViewFinished)
        assertEquals(InputLifecycle.InputStarted, state.lifecycle)
        assertEquals(editor, state.editor)
        assertEquals(InputSurface.None, state.surface)

        state = InputSessionReducer.reduce(state, InputSessionEvent.InputFinished)
        assertEquals(InputLifecycle.Inactive, state.lifecycle)
        assertEquals(null, state.editor)
        assertEquals(InputSurface.None, state.surface)
    }
}
