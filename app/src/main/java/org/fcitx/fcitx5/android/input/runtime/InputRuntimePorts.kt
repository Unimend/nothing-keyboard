/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.runtime

import android.view.inputmethod.InputMethodSubtype
import android.view.inputmethod.EditorInfo
import org.fcitx.fcitx5.android.input.editing.EditingCommandPort
import org.fcitx.fcitx5.android.input.engine.EngineCommandPort
import org.fcitx.fcitx5.android.input.engine.InputEngineQueries
import org.fcitx.fcitx5.android.input.engine.PlatformInputMethodPort
import org.fcitx.fcitx5.android.input.session.InputSessionReader
import org.mechdancer.dependency.UniqueComponent

class InputSessionAccess(val reader: InputSessionReader) : UniqueComponent<InputSessionAccess>()

class InputEngineAccess(
    val commands: EngineCommandPort,
    val queries: InputEngineQueries
) : UniqueComponent<InputEngineAccess>()

class InputEditingAccess(val commands: EditingCommandPort) : UniqueComponent<InputEditingAccess>()

class EditorInfoAccess(private val currentEditorInfo: () -> EditorInfo) :
    UniqueComponent<EditorInfoAccess>() {
    fun current(): EditorInfo = currentEditorInfo()
}

class ImePlatformAccess(
    val inputMethods: PlatformInputMethodPort,
    private val hideKeyboardAction: () -> Unit,
    private val switchToNextInputMethodAction: () -> Unit,
    private val switchToInputMethodAction: (String, InputMethodSubtype) -> Unit
) : UniqueComponent<ImePlatformAccess>() {
    fun hideKeyboard() = hideKeyboardAction()
    fun switchToNextInputMethod() = switchToNextInputMethodAction()
    fun switchToInputMethod(id: String, subtype: InputMethodSubtype) =
        switchToInputMethodAction(id, subtype)
}
