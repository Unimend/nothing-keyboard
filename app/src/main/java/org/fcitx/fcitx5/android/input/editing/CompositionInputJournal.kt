/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.input.editing

import org.fcitx.fcitx5.android.input.engine.EngineCommand
import org.fcitx.fcitx5.android.input.engine.EngineKey

/**
 * Records the actual virtual-key stream that produced the current engine composition.
 *
 * Rendered preedit is presentation data and can already contain converted Chinese. Undo needs
 * this source input instead so it can rebuild the same uncommitted pinyin through the engine.
 */
class CompositionInputJournal {
    private val text = StringBuilder()
    private var cursor = 0

    val rawInput: String
        get() = text.toString()

    fun observe(command: EngineCommand) {
        when (command) {
            is EngineCommand.SendKeyText -> {
                text.insert(cursor, command.text)
                cursor += command.text.length
            }
            is EngineCommand.SendKey -> when (command.key) {
                EngineKey.Backspace -> if (cursor > 0) {
                    val previous = Character.offsetByCodePoints(text, cursor, -1)
                    text.delete(previous, cursor)
                    cursor = previous
                }
                EngineKey.ArrowLeft -> if (cursor > 0) {
                    cursor = Character.offsetByCodePoints(text, cursor, -1)
                }
                EngineKey.ArrowRight -> if (cursor < text.length) {
                    cursor = Character.offsetByCodePoints(text, cursor, 1)
                }
                EngineKey.Return,
                EngineKey.Space,
                is EngineKey.Unicode,
                is EngineKey.KeypadDigit,
                is EngineKey.KeypadOperator -> Unit
            }
            EngineCommand.Reset,
            EngineCommand.ResetIfNotEmpty,
            is EngineCommand.Focus,
            EngineCommand.FocusOutIn,
            is EngineCommand.ActivateContext,
            is EngineCommand.DeactivateContext,
            is EngineCommand.ActivateInputMethod,
            is EngineCommand.EnumerateInputMethod,
            EngineCommand.ToggleInputMethod -> clear()
            is EngineCommand.SetCapabilities,
            EngineCommand.TriggerQuickPhrase,
            EngineCommand.TriggerUnicode,
            is EngineCommand.MoveCursor,
            is EngineCommand.SelectCandidate,
            is EngineCommand.OffsetCandidatePage,
            is EngineCommand.TriggerCandidateAction,
            is EngineCommand.TriggerCandidateTabAction,
            is EngineCommand.ActivateStatusAction,
            is EngineCommand.SetCandidatePagingMode,
            EngineCommand.ReloadConfiguration -> Unit
        }
    }

    fun clear() {
        text.clear()
        cursor = 0
    }
}
