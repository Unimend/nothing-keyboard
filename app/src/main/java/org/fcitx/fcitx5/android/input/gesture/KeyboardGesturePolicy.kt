/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.gesture

import kotlin.math.absoluteValue

enum class GesturePhase { Move, Up, Cancel }

data class GestureSnapshot(
    val phase: GesturePhase,
    val countX: Int,
    val totalX: Int,
    val totalY: Int
)

sealed interface KeyboardGestureAction {
    data class MoveCursor(val steps: Int) : KeyboardGestureAction
    data class MoveSelection(val steps: Int) : KeyboardGestureAction
    data class DeleteSelection(val steps: Int) : KeyboardGestureAction
    data object OpenEditingPanel : KeyboardGestureAction
    data object ClearAllText : KeyboardGestureAction
    data object Undo : KeyboardGestureAction
    data object CancelSelection : KeyboardGestureAction
}

data class GestureDecision(
    val action: KeyboardGestureAction? = null,
    val consumed: Boolean = false
)

object KeyboardGesturePolicy {
    fun space(snapshot: GestureSnapshot): GestureDecision = when (snapshot.phase) {
        GesturePhase.Cancel -> GestureDecision(consumed = true)
        GesturePhase.Move -> when {
            snapshot.totalY.absoluteValue > snapshot.totalX.absoluteValue ->
                GestureDecision(consumed = true)
            snapshot.countX != 0 ->
                GestureDecision(KeyboardGestureAction.MoveCursor(snapshot.countX), consumed = true)
            else -> GestureDecision()
        }
        GesturePhase.Up -> if (
            snapshot.totalY < 0 &&
            snapshot.totalY.absoluteValue >= snapshot.totalX.absoluteValue
        ) {
            GestureDecision(KeyboardGestureAction.OpenEditingPanel, consumed = true)
        } else {
            GestureDecision()
        }
    }

    fun backspace(snapshot: GestureSnapshot): GestureDecision = when (snapshot.phase) {
        GesturePhase.Cancel -> GestureDecision(KeyboardGestureAction.CancelSelection, consumed = true)
        GesturePhase.Move -> when {
            snapshot.totalY != 0 &&
                snapshot.totalY.absoluteValue >= snapshot.totalX.absoluteValue ->
                GestureDecision(consumed = true)
            snapshot.countX != 0 ->
                GestureDecision(
                    KeyboardGestureAction.MoveSelection(snapshot.countX),
                    consumed = true
                )
            else -> GestureDecision()
        }
        GesturePhase.Up -> if (
            snapshot.totalY != 0 &&
            snapshot.totalY.absoluteValue >= snapshot.totalX.absoluteValue
        ) {
            val action = when {
                snapshot.totalY <= -2 -> KeyboardGestureAction.ClearAllText
                snapshot.totalY > 0 -> KeyboardGestureAction.Undo
                else -> null
            }
            GestureDecision(action, consumed = true)
        } else {
            // Preserve click handling when there was no horizontal swipe.
            GestureDecision(
                KeyboardGestureAction.DeleteSelection(snapshot.totalX),
                consumed = false
            )
        }
    }
}
