/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.input.session

import org.fcitx.fcitx5.android.input.editing.EditingCommand
import org.fcitx.fcitx5.android.input.editing.EditingCommandPort
import org.fcitx.fcitx5.android.input.engine.EngineCommand
import org.fcitx.fcitx5.android.input.engine.EngineCommandPort

interface InputLifecycleEditor {
    fun startInput(start: Int, end: Int)
    fun finishInputView()
    fun unbindInput()
}

interface InputLifecycleSessionSink {
    fun inputStarted(editor: EditorSnapshot, capabilities: Set<EditorCapability>, restarting: Boolean)
    fun inputViewStarted(editor: EditorSnapshot, restarting: Boolean)
    fun inputViewFinished()
    fun inputFinished()
}

/** Owns ordered state transitions shared by every Android IME lifecycle callback. */
class InputLifecycleController(
    private val editor: InputLifecycleEditor,
    private val editingCommands: EditingCommandPort,
    private val engineCommands: EngineCommandPort,
    private val session: InputLifecycleSessionSink
) {
    fun startInput(
        editorSnapshot: EditorSnapshot,
        capabilities: Set<EditorCapability>,
        initialSelectionStart: Int,
        initialSelectionEnd: Int,
        restarting: Boolean,
        acceptsEngineFocus: Boolean
    ) {
        editingCommands.execute(EditingCommand.ResetDeletionGesture)
        editor.startInput(initialSelectionStart, initialSelectionEnd)
        session.inputStarted(editorSnapshot, capabilities, restarting)
        if (restarting) engineCommands.submit(EngineCommand.Focus(false))
        engineCommands.submit(EngineCommand.SetCapabilities(capabilities))
        if (acceptsEngineFocus) engineCommands.submit(EngineCommand.Focus(true))
    }

    fun startInputView(editorSnapshot: EditorSnapshot, restarting: Boolean) {
        session.inputViewStarted(editorSnapshot, restarting)
        engineCommands.submit(EngineCommand.Focus(true))
    }

    fun finishInputView() {
        session.inputViewFinished()
        editor.finishInputView()
        engineCommands.submit(EngineCommand.FocusOutIn)
    }

    fun finishInput() {
        session.inputFinished()
        engineCommands.submit(EngineCommand.Focus(false))
    }

    fun unbindInput() = editor.unbindInput()
}
