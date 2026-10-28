/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.input.platform

import android.view.inputmethod.EditorInfo
import org.fcitx.fcitx5.android.input.engine.InputContextLifecycleController
import org.fcitx.fcitx5.android.input.session.AndroidEditorCapabilities
import org.fcitx.fcitx5.android.input.session.AndroidEditorPolicy
import org.fcitx.fcitx5.android.input.session.EditorSessionSnapshot
import org.fcitx.fcitx5.android.input.session.InputLifecycleController
import org.fcitx.fcitx5.android.input.session.toEditorSnapshot

interface ImeInputLifecycleHost {
    fun notifyInputStarted(info: EditorInfo)
    fun evaluateInputView(info: EditorInfo): Boolean
    fun currentEditorSession(): EditorSessionSnapshot?
    fun resetHardwareKeys()
}

/** Owns ordering across Android input callbacks, session state and visible presentation. */
class ImeInputLifecycleCoordinator(
    private val lifecycle: InputLifecycleController,
    private val presentation: InputViewPresentationController,
    private val contextLifecycle: InputContextLifecycleController,
    private val host: ImeInputLifecycleHost
) {
    fun bind(uid: Int, packageName: String) = contextLifecycle.bind(uid, packageName)

    fun startInput(info: EditorInfo, restarting: Boolean) {
        host.notifyInputStarted(info)
        lifecycle.startInput(
            editorSnapshot = info.toEditorSnapshot(),
            capabilities = AndroidEditorCapabilities.fromAndroid(info.inputType, info.imeOptions),
            initialSelectionStart = info.initialSelStart,
            initialSelectionEnd = info.initialSelEnd,
            restarting = restarting,
            acceptsEngineFocus = AndroidEditorPolicy.acceptsEngineFocus(info)
        )
    }

    fun startInputView(info: EditorInfo, restarting: Boolean) {
        lifecycle.startInputView(info.toEditorSnapshot(), restarting)
        presentation.start(
            isVirtualKeyboard = host.evaluateInputView(info),
            session = host.currentEditorSession()
        )
    }

    fun selectionChanged(
        start: Int,
        end: Int,
        composingStart: Int,
        composingEnd: Int
    ) = lifecycle.selectionChanged(start, end, composingStart, composingEnd)

    fun finishInputView() = presentation.finish(lifecycle::finishInputView)

    fun finishInput() = lifecycle.finishInput()

    fun configurationChanged() = lifecycle.configurationChanged()

    fun unbind(uid: Int?) {
        host.resetHardwareKeys()
        lifecycle.unbindInput()
        contextLifecycle.unbind(uid)
    }
}
