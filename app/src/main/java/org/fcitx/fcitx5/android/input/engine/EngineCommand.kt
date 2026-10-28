/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.engine

/** Commands the Android input session may send to any input-engine implementation. */
sealed interface EngineCommand {
    data class SendKeyText(
        val text: String,
        val modifiers: Set<EngineKeyModifier> = VirtualKeyModifiers,
        val keyUp: Boolean = false,
        val timestamp: Int = -1
    ) : EngineCommand

    data class SendKey(
        val key: EngineKey,
        val modifiers: Set<EngineKeyModifier> = VirtualKeyModifiers,
        val keyUp: Boolean = false,
        val timestamp: Int = -1
    ) : EngineCommand

    data object Reset : EngineCommand
    data object ResetIfNotEmpty : EngineCommand
    data class Focus(val focused: Boolean) : EngineCommand
    data object FocusOutIn : EngineCommand
    data class ActivateContext(val uid: Int, val packageName: String) : EngineCommand
    data class DeactivateContext(val uid: Int) : EngineCommand
    data class SetCapabilities(val flags: ULong) : EngineCommand
    data class ActivateInputMethod(val uniqueName: String) : EngineCommand
    data class EnumerateInputMethod(val forward: Boolean = true) : EngineCommand
    data object ToggleInputMethod : EngineCommand
    data object TriggerQuickPhrase : EngineCommand
    data object TriggerUnicode : EngineCommand
    data class MoveCursor(val codePointPosition: Int) : EngineCommand
    data class SelectCandidate(val index: Int) : EngineCommand
    data class OffsetCandidatePage(val delta: Int) : EngineCommand
    data class TriggerCandidateAction(val candidateIndex: Int, val actionId: Int) : EngineCommand
    data class TriggerCandidateTabAction(val actionId: Int) : EngineCommand
    data class ActivateStatusAction(val actionId: Int) : EngineCommand
    data class SetCandidatePagingMode(val mode: Int) : EngineCommand
    data object ReloadConfiguration : EngineCommand
}
