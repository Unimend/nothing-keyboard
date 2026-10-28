/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.gesture

import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.feedback.KeyboardFeedback
import org.fcitx.fcitx5.android.input.keyboard.CustomGestureView
import org.fcitx.fcitx5.android.input.keyboard.CustomGestureView.GestureType
import org.fcitx.fcitx5.android.input.keyboard.CustomGestureView.OnGestureListener

/** The single binding between every backspace view and the shared gesture policy. */
object BackspaceGestureController {
    data class Actions(
        val moveSelection: (Int) -> Unit,
        val deleteSelection: (Int) -> Unit,
        val clearAll: () -> Unit,
        val undo: () -> Unit,
        val cancel: () -> Unit
    )

    fun bind(
        view: CustomGestureView,
        theme: Theme,
        hapticOnRepeat: Boolean,
        actions: Actions
    ) = with(view) {
        swipeEnabled = true
        swipeRepeatEnabled = true
        val density = resources.displayMetrics.density
        swipeThresholdX = KeyboardGestureConfig.SelectionStepDp * density
        swipeThresholdY = KeyboardGestureConfig.DeleteVerticalStepDp * density
        onGestureListener = OnGestureListener { gestureView, event ->
            when (event.type) {
                GestureType.Down -> {
                    // Picker pages live inside ViewPager2. Claim the full gesture at touch-down so
                    // the pager cannot steal horizontal selection before the first threshold.
                    gestureView.parent?.requestDisallowInterceptTouchEvent(true)
                    false
                }
                GestureType.Move, GestureType.Up, GestureType.Cancel -> {
                    val phase = when (event.type) {
                        GestureType.Move -> GesturePhase.Move
                        GestureType.Cancel -> GesturePhase.Cancel
                        else -> GesturePhase.Up
                    }
                    val decision = KeyboardGesturePolicy.backspace(
                        GestureSnapshot(phase, event.countX, event.totalX, event.totalY)
                    )
                    when (val action = decision.action) {
                        is KeyboardGestureAction.MoveSelection -> {
                            actions.moveSelection(action.steps)
                            KeyboardFeedback.step(gestureView, hapticOnRepeat)
                        }
                        is KeyboardGestureAction.DeleteSelection ->
                            actions.deleteSelection(action.steps)
                        KeyboardGestureAction.ClearAllText -> {
                            KeyboardFeedback.confirmation(gestureView, theme)
                            actions.clearAll()
                        }
                        KeyboardGestureAction.Undo -> {
                            KeyboardFeedback.confirmation(gestureView, theme)
                            actions.undo()
                        }
                        KeyboardGestureAction.CancelSelection -> actions.cancel()
                        else -> Unit
                    }
                    if (event.type != GestureType.Move) {
                        gestureView.parent?.requestDisallowInterceptTouchEvent(false)
                    }
                    decision.consumed
                }
            }
        }
    }
}
