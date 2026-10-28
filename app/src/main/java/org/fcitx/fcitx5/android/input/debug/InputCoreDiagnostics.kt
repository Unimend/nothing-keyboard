/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.debug

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.fcitx.fcitx5.android.input.session.InputSessionSnapshot

data class InputCoreDiagnosticsSnapshot(
    val session: InputSessionSnapshot = InputSessionSnapshot(),
    val engineEventCount: Long = 0,
    val lastEngineEvent: String = "waiting",
    val engineEffectCount: Long = 0,
    val lastEngineEffect: String = "waiting",
    val engineCommandCount: Long = 0,
    val lastEngineCommand: String = "waiting",
    val engineQueryCount: Long = 0,
    val lastEngineQuery: String = "waiting",
    val navigationCount: Long = 0,
    val lastNavigation: String = "waiting",
    val windowLifecycleCount: Long = 0,
    val lastWindowLifecycle: String = "waiting"
)

sealed interface InputDiagnosticEvent {
    data class SessionPublished(val session: InputSessionSnapshot) : InputDiagnosticEvent
    data class EngineEvent(val name: String) : InputDiagnosticEvent
    data class EngineEffect(val name: String) : InputDiagnosticEvent
    data class EngineCommand(val name: String) : InputDiagnosticEvent
    data class EngineQuery(val name: String) : InputDiagnosticEvent
    data class Navigation(val name: String) : InputDiagnosticEvent
    data class WindowLifecycle(val name: String) : InputDiagnosticEvent
}

/** Pure diagnostic reducer; concurrent producers never mutate counters themselves. */
fun reduceInputDiagnostics(
    state: InputCoreDiagnosticsSnapshot,
    event: InputDiagnosticEvent
): InputCoreDiagnosticsSnapshot = when (event) {
    is InputDiagnosticEvent.SessionPublished -> state.copy(session = event.session)
    is InputDiagnosticEvent.EngineEvent -> state.copy(
        engineEventCount = state.engineEventCount + 1, lastEngineEvent = event.name
    )
    is InputDiagnosticEvent.EngineEffect -> state.copy(
        engineEffectCount = state.engineEffectCount + 1, lastEngineEffect = event.name
    )
    is InputDiagnosticEvent.EngineCommand -> state.copy(
        engineCommandCount = state.engineCommandCount + 1, lastEngineCommand = event.name
    )
    is InputDiagnosticEvent.EngineQuery -> state.copy(
        engineQueryCount = state.engineQueryCount + 1, lastEngineQuery = event.name
    )
    is InputDiagnosticEvent.Navigation -> state.copy(
        navigationCount = state.navigationCount + 1, lastNavigation = event.name
    )
    is InputDiagnosticEvent.WindowLifecycle -> state.copy(
        windowLifecycleCount = state.windowLifecycleCount + 1, lastWindowLifecycle = event.name
    )
}

object InputCoreDiagnostics {
    private val mutableState = MutableStateFlow(InputCoreDiagnosticsSnapshot())
    val state = mutableState.asStateFlow()

    private fun publish(event: InputDiagnosticEvent) {
        mutableState.update { reduceInputDiagnostics(it, event) }
    }

    fun publishSession(session: InputSessionSnapshot) {
        publish(InputDiagnosticEvent.SessionPublished(session))
    }

    fun recordEngineEvent(name: String) {
        publish(InputDiagnosticEvent.EngineEvent(name))
    }

    fun recordEngineEffect(name: String) {
        publish(InputDiagnosticEvent.EngineEffect(name))
    }

    fun recordEngineCommand(name: String) {
        publish(InputDiagnosticEvent.EngineCommand(name))
    }

    fun recordEngineQuery(name: String) {
        publish(InputDiagnosticEvent.EngineQuery(name))
    }

    fun recordNavigation(name: String) {
        publish(InputDiagnosticEvent.Navigation(name))
    }

    fun recordWindowLifecycle(name: String) {
        publish(InputDiagnosticEvent.WindowLifecycle(name))
    }
}
