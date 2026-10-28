/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.engine

import org.fcitx.fcitx5.android.core.CapabilityFlags
import org.fcitx.fcitx5.android.core.FcitxAPI

/** The only place where project-owned [EngineCommand] values become Fcitx API calls. */
object FcitxEngineCommandAdapter {
    suspend fun execute(api: FcitxAPI, command: EngineCommand) {
        with(api) {
            when (command) {
                is EngineCommand.SendKeyText -> sendKey(
                    command.text,
                    command.states,
                    command.code,
                    command.keyUp,
                    command.timestamp
                )
                is EngineCommand.SendKey -> sendKey(
                    command.symbol,
                    command.states,
                    command.code,
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
}
