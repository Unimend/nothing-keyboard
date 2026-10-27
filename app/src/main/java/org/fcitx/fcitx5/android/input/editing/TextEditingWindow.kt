/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.editing

import android.view.KeyEvent
import android.view.View
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.InputFeedbacks
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.input.FcitxInputMethodService
import org.fcitx.fcitx5.android.input.broadcast.InputBroadcastReceiver
import org.fcitx.fcitx5.android.input.clipboard.ClipboardWindow
import org.fcitx.fcitx5.android.input.dependency.inputMethodService
import org.fcitx.fcitx5.android.input.dependency.theme
import org.fcitx.fcitx5.android.input.keyboard.CustomGestureView
import org.fcitx.fcitx5.android.input.keyboard.CustomGestureView.GestureType
import org.fcitx.fcitx5.android.input.keyboard.CustomGestureView.OnGestureListener
import org.fcitx.fcitx5.android.input.keyboard.KeyboardWindow
import org.fcitx.fcitx5.android.input.gesture.GesturePhase
import org.fcitx.fcitx5.android.input.gesture.GestureSnapshot
import org.fcitx.fcitx5.android.input.gesture.KeyboardGestureAction
import org.fcitx.fcitx5.android.input.gesture.KeyboardGestureConfig
import org.fcitx.fcitx5.android.input.gesture.KeyboardGesturePolicy
import org.fcitx.fcitx5.android.input.wm.InputWindow
import org.fcitx.fcitx5.android.input.wm.InputWindowManager
import org.mechdancer.dependency.manager.must
import splitties.dimensions.dp

class TextEditingWindow : InputWindow.ExtendedInputWindow<TextEditingWindow>(),
    InputBroadcastReceiver {

    private val service: FcitxInputMethodService by manager.inputMethodService()
    private val windowManager: InputWindowManager by manager.must()
    private val theme by manager.theme()

    private val hapticOnRepeat by AppPrefs.getInstance().keyboard.hapticOnRepeat

    private val buttonRipple by ThemeManager.prefs.keyRippleEffect
    private val buttonBorder by ThemeManager.prefs.keyBorder
    private val buttonRadius by ThemeManager.prefs.textEditingButtonRadius

    private val session = EditingSession()
    private val commandExecutor by lazy { AndroidEditingCommandExecutor(service) }

    private fun sendDirectionKey(keyEventCode: Int) {
        commandExecutor.execute(session.move(keyEventCode))
    }

    private val ui by lazy {
        TextEditingUi(context, theme, buttonRipple, buttonBorder, buttonRadius.toFloat()).apply {
            fun CustomGestureView.onClickWithRepeating(block: () -> Unit) {
                setOnClickListener { block() }
                repeatEnabled = true
                onRepeatListener = {
                    block()
                    if (hapticOnRepeat) InputFeedbacks.hapticFeedback(this)
                }
            }

            leftButton.onClickWithRepeating { sendDirectionKey(KeyEvent.KEYCODE_DPAD_LEFT) }

            upButton.onClickWithRepeating { sendDirectionKey(KeyEvent.KEYCODE_DPAD_UP) }

            downButton.onClickWithRepeating { sendDirectionKey(KeyEvent.KEYCODE_DPAD_DOWN) }

            rightButton.onClickWithRepeating { sendDirectionKey(KeyEvent.KEYCODE_DPAD_RIGHT) }

            homeButton.setOnClickListener { sendDirectionKey(KeyEvent.KEYCODE_MOVE_HOME) }
            endButton.setOnClickListener { sendDirectionKey(KeyEvent.KEYCODE_MOVE_END) }
            selectButton.setOnClickListener {
                session.toggleSelection()?.let(commandExecutor::execute)
                updateSelection(session.hasSelection, session.userSelection)
            }
            selectAllButton.setOnClickListener {
                commandExecutor.execute(session.selectAll())
            }
            cutButton.setOnClickListener {
                commandExecutor.execute(session.cut())
            }
            copyButton.setOnClickListener {
                commandExecutor.execute(session.copy())
            }
            pasteButton.setOnClickListener {
                commandExecutor.execute(session.paste())
            }
            backspaceButton.onClickWithRepeating {
                commandExecutor.execute(session.backspace())
            }
            backspaceButton.apply {
                swipeEnabled = true
                swipeRepeatEnabled = true
                // Horizontal range selection belongs to the main keyboard. The editing panel's
                // delete key only shares its vertical clear/undo gestures.
                swipeThresholdX = dp(KeyboardGestureConfig.DisabledAxisDp)
                swipeThresholdY = dp(KeyboardGestureConfig.DeleteVerticalStepDp)
                onGestureListener = OnGestureListener { view, event ->
                    val phase = when (event.type) {
                        GestureType.Move -> GesturePhase.Move
                        GestureType.Up -> GesturePhase.Up
                        else -> return@OnGestureListener false
                    }
                    val decision = KeyboardGesturePolicy.backspace(
                        GestureSnapshot(phase, event.countX, event.totalX, event.totalY)
                    )
                    when (decision.action) {
                        KeyboardGestureAction.ClearAllText -> {
                            InputFeedbacks.hapticFeedback(view, longPress = true)
                            commandExecutor.execute(session.clearAll())
                            updateSelection(session.hasSelection, session.userSelection)
                        }
                        KeyboardGestureAction.Undo -> {
                            InputFeedbacks.hapticFeedback(view, longPress = true)
                            commandExecutor.execute(EditingCommand.Undo)
                        }
                        else -> Unit
                    }
                    decision.consumed
                }
            }
            clipboardButton.setOnClickListener {
                windowManager.attachWindow(ClipboardWindow())
            }
            keyboardButton.setOnClickListener {
                windowManager.attachWindow(KeyboardWindow)
            }
        }
    }

    override fun onCreateView(): View = ui.root

    override fun onAttached() {
        val range = service.currentInputSelection
        onSelectionUpdate(range.start, range.end)
    }

    override fun onDetached() {}

    override fun onSelectionUpdate(start: Int, end: Int) {
        session.updateSelection(start, end)
        ui.updateSelection(session.hasSelection, session.userSelection)
    }

    override val showTitle: Boolean = false
}
