/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class InputLifecycle { Inactive, InputStarted, InputViewStarted }

data class SessionRange(val start: Int = -1, val end: Int = -1)

data class EditorSnapshot(
    val packageName: String = "",
    val inputType: Int = 0,
    val imeOptions: Int = 0,
    val privateImeOptions: String? = null,
    val initialSelection: SessionRange = SessionRange()
)

data class InputSessionSnapshot(
    val lifecycle: InputLifecycle = InputLifecycle.Inactive,
    val editor: EditorSnapshot? = null,
    val selection: SessionRange = SessionRange(),
    val capabilityFlags: ULong = 0u,
    val surface: InputSurface = InputSurface.None,
    val previousSurface: InputSurface = InputSurface.None,
    val restarting: Boolean = false,
    val engine: EngineState = EngineState(),
    val revision: Long = 0
)

sealed interface InputSessionEvent {
    data class InputStarted(
        val editor: EditorSnapshot,
        val capabilityFlags: ULong,
        val restarting: Boolean
    ) : InputSessionEvent

    data class InputViewStarted(
        val editor: EditorSnapshot,
        val restarting: Boolean
    ) : InputSessionEvent

    data class SelectionChanged(val start: Int, val end: Int) : InputSessionEvent
    data class SurfaceChanged(val surface: InputSurface) : InputSessionEvent
    data class EngineChanged(val event: EngineStateEvent) : InputSessionEvent
    data object InputViewFinished : InputSessionEvent
    data object InputFinished : InputSessionEvent
}

object InputSessionReducer {
    fun reduce(state: InputSessionSnapshot, event: InputSessionEvent): InputSessionSnapshot {
        val next = when (event) {
            is InputSessionEvent.InputStarted -> state.copy(
                lifecycle = InputLifecycle.InputStarted,
                editor = event.editor,
                selection = event.editor.initialSelection,
                capabilityFlags = event.capabilityFlags,
                restarting = event.restarting
            )
            is InputSessionEvent.InputViewStarted -> state.copy(
                lifecycle = InputLifecycle.InputViewStarted,
                editor = event.editor,
                restarting = event.restarting
            )
            is InputSessionEvent.SelectionChanged -> state.copy(
                selection = SessionRange(event.start, event.end)
            )
            is InputSessionEvent.SurfaceChanged -> if (state.surface == event.surface) {
                state
            } else {
                state.copy(surface = event.surface, previousSurface = state.surface)
            }
            is InputSessionEvent.EngineChanged ->
                state.copy(engine = EngineStateReducer.reduce(state.engine, event.event))
            InputSessionEvent.InputViewFinished -> state.copy(
                lifecycle = InputLifecycle.InputStarted,
                surface = InputSurface.None,
                previousSurface = state.surface
            )
            InputSessionEvent.InputFinished -> InputSessionSnapshot()
        }
        return if (next == state) state else next.copy(revision = state.revision + 1)
    }
}

/** Service-lifetime source of truth for editor, lifecycle, selection, and active input surface. */
class InputSessionStore {
    private val mutableState = MutableStateFlow(InputSessionSnapshot())
    val state = mutableState.asStateFlow()
    val current: InputSessionSnapshot get() = mutableState.value

    fun dispatch(event: InputSessionEvent) {
        mutableState.value = InputSessionReducer.reduce(mutableState.value, event)
    }
}
