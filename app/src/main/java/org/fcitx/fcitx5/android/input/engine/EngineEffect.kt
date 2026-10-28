/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.engine

import org.fcitx.fcitx5.android.input.session.EngineInputMethod
import org.fcitx.fcitx5.android.input.session.EngineText

enum class EngineVirtualKey { Backspace, Return, ArrowLeft, ArrowRight, Unicode, Unhandled }

enum class EngineSwitchReason {
    Trigger, Deactivate, AlternateTrigger, Activate, Enumerate, GroupChange,
    CapabilityChanged, Other
}

sealed interface EngineEffect {
    data class CommitText(val text: String, val cursor: Int) : EngineEffect
    data class UpdateClientPreedit(val text: EngineText) : EngineEffect
    data class DeleteSurrounding(val before: Int, val after: Int) : EngineEffect
    data class InputMethodChanged(val inputMethod: EngineInputMethod) : EngineEffect
    data class InputMethodSwitched(val reason: EngineSwitchReason) : EngineEffect

    data class VirtualKey(
        val key: EngineVirtualKey,
        val unicode: Int,
        val rawSymbol: Int
    ) : EngineEffect

    data class ForwardKey(
        val timestamp: Int,
        val androidKeyCode: Int,
        val androidMetaState: Int,
        val unicode: Int,
        val keyUp: Boolean
    ) : EngineEffect
}
