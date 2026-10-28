/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.session

data class InputPresentationState(
    val preeditEmpty: Boolean = true,
    val candidatesEmpty: Boolean = true,
    val showCandidates: Boolean = false
)

object InputPresentationReducer {
    fun reduce(engine: EngineState): InputPresentationState {
        val preeditEmpty = engine.clientPreedit.isEmpty && engine.inputPanel.preedit.isEmpty
        val candidatesEmpty = engine.candidates.items.isEmpty()
        return InputPresentationState(
            preeditEmpty = preeditEmpty,
            candidatesEmpty = candidatesEmpty,
            showCandidates = !candidatesEmpty
        )
    }
}
