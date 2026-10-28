/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.engine

import android.view.KeyEvent
import org.fcitx.fcitx5.android.core.CapabilityFlags
import org.fcitx.fcitx5.android.core.FcitxAPI
import org.fcitx.fcitx5.android.core.FcitxKeyMapping
import org.fcitx.fcitx5.android.core.KeyState
import org.fcitx.fcitx5.android.core.KeyStates
import org.fcitx.fcitx5.android.core.KeySym
import org.fcitx.fcitx5.android.core.ScancodeMapping

/** The only place where project-owned [EngineCommand] values become Fcitx API calls. */
object FcitxEngineCommandAdapter {
    fun acceptsHardwareKey(event: KeyEvent): Boolean = KeySym.fromKeyEvent(event) != null

    suspend fun executeHardwareKey(api: FcitxAPI, event: KeyEvent, timestamp: Int): Boolean {
        val symbol = KeySym.fromKeyEvent(event) ?: return false
        api.sendKey(
            symbol.sym,
            KeyStates.fromKeyEvent(event).states,
            event.scanCode,
            event.action == KeyEvent.ACTION_UP,
            timestamp
        )
        return true
    }

    suspend fun execute(api: FcitxAPI, command: EngineCommand) {
        with(api) {
            when (command) {
                is EngineCommand.SendKeyText -> sendKey(
                    command.text,
                    command.modifiers.toFcitxStates().states,
                    command.text.firstOrNull()?.let(ScancodeMapping::charToScancode) ?: 0,
                    command.keyUp,
                    command.timestamp
                )
                is EngineCommand.SendKey -> sendKey(
                    command.key.toFcitxSymbol(),
                    command.modifiers.toFcitxStates().states,
                    0,
                    command.keyUp,
                    command.timestamp
                )
                EngineCommand.Reset -> reset()
                EngineCommand.ResetIfNotEmpty -> if (!isEmpty()) reset()
                is EngineCommand.Focus -> focus(command.focused)
                EngineCommand.FocusOutIn -> focusOutIn()
                is EngineCommand.ActivateContext -> activate(command.uid, command.packageName)
                is EngineCommand.DeactivateContext -> deactivate(command.uid)
                is EngineCommand.SetCapabilities -> setCapFlags(CapabilityFlags(command.flags))
                is EngineCommand.ActivateInputMethod -> activateIme(command.uniqueName)
                is EngineCommand.EnumerateInputMethod -> enumerateIme(command.forward)
                EngineCommand.ToggleInputMethod -> toggleIme()
                EngineCommand.TriggerQuickPhrase -> triggerQuickPhrase()
                EngineCommand.TriggerUnicode -> triggerUnicode()
                is EngineCommand.MoveCursor -> moveCursor(command.codePointPosition)
                is EngineCommand.SelectCandidate -> select(command.index)
                is EngineCommand.OffsetCandidatePage -> offsetCandidatePage(command.delta)
                is EngineCommand.TriggerCandidateAction ->
                    triggerCandidateAction(command.candidateIndex, command.actionId)
                is EngineCommand.TriggerCandidateTabAction ->
                    triggerCandidateListTabAction(command.actionId)
                is EngineCommand.ActivateStatusAction -> activateAction(command.actionId)
                is EngineCommand.SetCandidatePagingMode -> setCandidatePagingMode(command.mode)
                EngineCommand.ReloadConfiguration -> reloadConfig()
            }
        }
    }

    private fun EngineKey.toFcitxSymbol(): Int = when (this) {
        is EngineKey.Unicode -> codePoint
        is EngineKey.KeypadDigit -> FcitxKeyMapping.FcitxKey_KP_0 + digit
        is EngineKey.KeypadOperator -> when (operator) {
            EngineKey.KeypadOperator.Operator.Add -> FcitxKeyMapping.FcitxKey_KP_Add
            EngineKey.KeypadOperator.Operator.Subtract -> FcitxKeyMapping.FcitxKey_KP_Subtract
            EngineKey.KeypadOperator.Operator.Multiply -> FcitxKeyMapping.FcitxKey_KP_Multiply
            EngineKey.KeypadOperator.Operator.Divide -> FcitxKeyMapping.FcitxKey_KP_Divide
            EngineKey.KeypadOperator.Operator.Separator -> 0xffac
            EngineKey.KeypadOperator.Operator.Decimal -> FcitxKeyMapping.FcitxKey_KP_Decimal
            EngineKey.KeypadOperator.Operator.Equal -> 0xffbd
        }
        EngineKey.Backspace -> FcitxKeyMapping.FcitxKey_BackSpace
        EngineKey.Return -> FcitxKeyMapping.FcitxKey_Return
        EngineKey.Space -> FcitxKeyMapping.FcitxKey_space
        EngineKey.ArrowLeft -> FcitxKeyMapping.FcitxKey_Left
        EngineKey.ArrowRight -> FcitxKeyMapping.FcitxKey_Right
    }

    private fun Set<EngineKeyModifier>.toFcitxStates() = KeyStates(
        *map {
            when (it) {
                EngineKeyModifier.Shift -> KeyState.Shift
                EngineKeyModifier.CapsLock -> KeyState.CapsLock
                EngineKeyModifier.NumLock -> KeyState.NumLock
                EngineKeyModifier.Control -> KeyState.Ctrl
                EngineKeyModifier.Alt -> KeyState.Alt
                EngineKeyModifier.Super -> KeyState.Super
                EngineKeyModifier.Virtual -> KeyState.Virtual
            }
        }.toTypedArray()
    )
}
