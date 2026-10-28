/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.input.session

import android.text.InputType
import android.view.inputmethod.EditorInfo
import splitties.bitflags.hasFlag

sealed interface EditorReturnBehavior {
    data object SendEnterKey : EditorReturnBehavior
    data class PerformAction(val actionId: Int) : EditorReturnBehavior
}

/** The sole Android EditorInfo compatibility policy used by lifecycle and editor operations. */
object AndroidEditorPolicy {
    fun isRawEditor(info: EditorInfo): Boolean =
        info.inputType and InputType.TYPE_MASK_CLASS == InputType.TYPE_NULL

    fun acceptsEngineFocus(info: EditorInfo): Boolean = !isRawEditor(info)

    fun shouldSimulateBackspace(info: EditorInfo, directDeleteOption: String): Boolean =
        info.privateImeOptions != directDeleteOption || isRawEditor(info)

    fun useKeyEventsForArrows(info: EditorInfo): Boolean {
        val type = info.inputType and InputType.TYPE_MASK_CLASS
        val variation = info.inputType and InputType.TYPE_MASK_VARIATION
        return type == InputType.TYPE_NULL ||
            type == InputType.TYPE_CLASS_TEXT && variation == InputType.TYPE_TEXT_VARIATION_URI
    }

    fun returnBehavior(info: EditorInfo): EditorReturnBehavior {
        if (isRawEditor(info) || info.imeOptions.hasFlag(EditorInfo.IME_FLAG_NO_ENTER_ACTION)) {
            return EditorReturnBehavior.SendEnterKey
        }
        if (info.actionLabel?.isNotEmpty() == true &&
            info.actionId != EditorInfo.IME_ACTION_UNSPECIFIED) {
            return EditorReturnBehavior.PerformAction(info.actionId)
        }
        val action = info.imeOptions and EditorInfo.IME_MASK_ACTION
        return if (action == EditorInfo.IME_ACTION_UNSPECIFIED || action == EditorInfo.IME_ACTION_NONE) {
            EditorReturnBehavior.SendEnterKey
        } else {
            EditorReturnBehavior.PerformAction(action)
        }
    }
}
