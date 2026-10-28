/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.input.editing

import org.fcitx.fcitx5.android.input.session.EngineState

sealed interface DeletionGestureEffect {
    data class Extend(val steps: Int) : DeletionGestureEffect
    data object Delete : DeletionGestureEffect
    data object ResetComposition : DeletionGestureEffect
    data object CollapseSelection : DeletionGestureEffect
}

/** One logical deletion gesture for every surface. Rendering state is never consulted. */
class DeletionGestureSession {
    private enum class State { Idle, Selecting, ResetComposition }
    private var state = State.Idle

    fun move(steps: Int, engine: EngineState): DeletionGestureEffect? {
        if (steps == 0) return null
        if (state == State.Idle) {
            val composing = !engine.clientPreedit.isEmpty || !engine.inputPanel.preedit.isEmpty
            val candidates = engine.candidates.total > 0 || engine.candidates.items.isNotEmpty()
            state = if (composing || candidates) State.ResetComposition else State.Selecting
        }
        return if (state == State.Selecting) DeletionGestureEffect.Extend(steps) else null
    }

    fun release(totalSteps: Int): DeletionGestureEffect? {
        val previous = state
        reset()
        if (totalSteps == 0) return null
        return when (previous) {
            State.Idle -> null
            State.Selecting -> DeletionGestureEffect.Delete
            State.ResetComposition -> DeletionGestureEffect.ResetComposition
        }
    }

    fun cancel(): DeletionGestureEffect? {
        val selecting = state == State.Selecting
        reset()
        return if (selecting) DeletionGestureEffect.CollapseSelection else null
    }

    fun reset() { state = State.Idle }
}
