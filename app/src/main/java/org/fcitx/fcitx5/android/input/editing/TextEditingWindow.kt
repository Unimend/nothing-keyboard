/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.editing

import android.view.KeyEvent
import android.view.View
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.InputFeedbacks
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.input.FcitxInputMethodService
import org.fcitx.fcitx5.android.input.dependency.inputMethodService
import org.fcitx.fcitx5.android.input.dependency.inputView
import org.fcitx.fcitx5.android.input.dependency.theme
import org.fcitx.fcitx5.android.input.keyboard.CustomGestureView
import org.fcitx.fcitx5.android.input.gesture.BackspaceGestureController
import org.fcitx.fcitx5.android.input.navigation.InputNavigationCommand
import org.fcitx.fcitx5.android.input.navigation.InputNavigator
import org.fcitx.fcitx5.android.input.wm.InputWindow
import org.fcitx.fcitx5.android.input.session.InputSurface
import org.fcitx.fcitx5.android.input.session.EditorSessionSnapshot
import org.fcitx.fcitx5.android.input.session.EditorSessionChange
import org.fcitx.fcitx5.android.input.session.editorSessionOrNull
import org.mechdancer.dependency.manager.must

class TextEditingWindow : InputWindow.ExtendedInputWindow<TextEditingWindow>() {

    override val surface = InputSurface.Editing

    private val service: FcitxInputMethodService by manager.inputMethodService()
    private val inputView by manager.inputView()
    private val theme by manager.theme()

    private val hapticOnRepeat by AppPrefs.getInstance().keyboard.hapticOnRepeat

    private val buttonRipple by ThemeManager.prefs.keyRippleEffect
    private val buttonBorder by ThemeManager.prefs.keyBorder
    private val buttonRadius by ThemeManager.prefs.textEditingButtonRadius

    private val session = EditingSession()
    private val commandExecutor get() = service.editingCommands
    private val navigator: InputNavigator by manager.must()

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
            BackspaceGestureController.bind(
                backspaceButton,
                theme,
                hapticOnRepeat,
                BackspaceGestureController.Actions(
                    moveSelection = {
                        commandExecutor.execute(EditingCommand.DeletionGestureMove(it))
                    },
                    deleteSelection = {
                        commandExecutor.execute(EditingCommand.DeletionGestureRelease(it))
                        session.finishDeletionGesture()
                        updateSelection(session.hasSelection, session.userSelection)
                    },
                    clearAll = {
                        commandExecutor.execute(session.clearAll())
                        updateSelection(session.hasSelection, session.userSelection)
                    },
                    undo = { commandExecutor.execute(EditingCommand.Undo) },
                    cancel = {
                        commandExecutor.execute(EditingCommand.DeletionGestureCancel)
                        session.finishDeletionGesture()
                        updateSelection(session.hasSelection, session.userSelection)
                    }
                )
            )
            clipboardButton.setOnClickListener {
                navigator.navigate(InputNavigationCommand.ShowClipboard)
            }
            keyboardButton.setOnClickListener {
                navigator.navigate(InputNavigationCommand.ShowKeyboard())
            }
        }
    }

    override fun onCreateView(): View = ui.root

    private var selectionJob: Job? = null

    override fun onAttached() {
        service.inputSession.current.editorSessionOrNull()?.let {
            updateEditorSelection(it)
        }
        selectionJob = inputView.surfaceScope.launch {
            service.inputSession.state.map { it.selection }.distinctUntilChanged().collect {
                service.inputSession.current.editorSessionOrNull()?.let(::updateEditorSelection)
            }
        }
    }

    override fun onDetached() {
        selectionJob?.cancel()
        selectionJob = null
    }

    private fun updateEditorSelection(session: EditorSessionSnapshot) {
        this.session.updateSelection(session.selection.start, session.selection.end)
        ui.updateSelection(this.session.hasSelection, this.session.userSelection)
    }

    override val showTitle: Boolean = false
}
