/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.debug

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val lastNavigation: String = "waiting"
)

object InputCoreDiagnostics {
    private val mutableState = MutableStateFlow(InputCoreDiagnosticsSnapshot())
    val state = mutableState.asStateFlow()

    fun publishSession(session: InputSessionSnapshot) {
        mutableState.value = mutableState.value.copy(session = session)
    }

    fun recordEngineEvent(name: String) {
        mutableState.value = mutableState.value.copy(
            engineEventCount = mutableState.value.engineEventCount + 1,
            lastEngineEvent = name
        )
    }

    fun recordEngineEffect(name: String) {
        mutableState.value = mutableState.value.copy(
            engineEffectCount = mutableState.value.engineEffectCount + 1,
            lastEngineEffect = name
        )
    }

    fun recordEngineCommand(name: String) {
        mutableState.value = mutableState.value.copy(
            engineCommandCount = mutableState.value.engineCommandCount + 1,
            lastEngineCommand = name
        )
    }

    fun recordEngineQuery(name: String) {
        mutableState.value = mutableState.value.copy(
            engineQueryCount = mutableState.value.engineQueryCount + 1,
            lastEngineQuery = name
        )
    }

    fun recordNavigation(name: String) {
        mutableState.value = mutableState.value.copy(
            navigationCount = mutableState.value.navigationCount + 1,
            lastNavigation = name
        )
    }
}
