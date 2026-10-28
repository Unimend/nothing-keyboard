/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.engine

import org.fcitx.fcitx5.android.core.FcitxEvent
import org.fcitx.fcitx5.android.core.FcitxKeyMapping

object FcitxEngineEffectAdapter {
    fun map(event: FcitxEvent<*>): EngineEffect? = when (event) {
        is FcitxEvent.CommitStringEvent ->
            EngineEffect.CommitText(event.data.text, event.data.cursor)
        is FcitxEvent.ClientPreeditEvent -> EngineEffect.UpdateClientPreedit(
            FcitxEngineStateAdapter.text(event.data)
        )
        is FcitxEvent.DeleteSurroundingEvent ->
            EngineEffect.DeleteSurrounding(event.data.before, event.data.after)
        is FcitxEvent.IMChangeEvent -> EngineEffect.InputMethodChanged(
            FcitxEngineStateAdapter.inputMethod(event.data)
        )
        is FcitxEvent.SwitchInputMethodEvent -> EngineEffect.InputMethodSwitched(
            when (event.data.reason) {
                FcitxEvent.SwitchInputMethodEvent.Reason.Trigger -> EngineSwitchReason.Trigger
                FcitxEvent.SwitchInputMethodEvent.Reason.Deactivate -> EngineSwitchReason.Deactivate
                FcitxEvent.SwitchInputMethodEvent.Reason.AltTrigger -> EngineSwitchReason.AlternateTrigger
                FcitxEvent.SwitchInputMethodEvent.Reason.Activate -> EngineSwitchReason.Activate
                FcitxEvent.SwitchInputMethodEvent.Reason.Enumerate -> EngineSwitchReason.Enumerate
                FcitxEvent.SwitchInputMethodEvent.Reason.GroupChange -> EngineSwitchReason.GroupChange
                FcitxEvent.SwitchInputMethodEvent.Reason.CapabilityChanged -> EngineSwitchReason.CapabilityChanged
                FcitxEvent.SwitchInputMethodEvent.Reason.Other -> EngineSwitchReason.Other
            }
        )
        is FcitxEvent.KeyEvent -> event.data.let {
            if (it.states.virtual) {
                EngineEffect.VirtualKey(
                    key = when (it.sym.sym) {
                        FcitxKeyMapping.FcitxKey_BackSpace -> EngineVirtualKey.Backspace
                        FcitxKeyMapping.FcitxKey_Return -> EngineVirtualKey.Return
                        FcitxKeyMapping.FcitxKey_Left -> EngineVirtualKey.ArrowLeft
                        FcitxKeyMapping.FcitxKey_Right -> EngineVirtualKey.ArrowRight
                        else -> if (it.unicode > 0) EngineVirtualKey.Unicode else EngineVirtualKey.Unhandled
                    },
                    unicode = it.unicode,
                    rawSymbol = it.sym.sym
                )
            } else {
                EngineEffect.ForwardKey(
                    timestamp = it.timestamp,
                    androidKeyCode = it.sym.keyCode,
                    androidMetaState = it.states.metaState,
                    unicode = it.unicode,
                    keyUp = it.up
                )
            }
        }
        else -> null
    }
}
