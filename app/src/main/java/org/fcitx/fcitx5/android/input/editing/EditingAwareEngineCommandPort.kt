/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.input.editing

import kotlinx.coroutines.Job
import org.fcitx.fcitx5.android.input.engine.EngineCommand
import org.fcitx.fcitx5.android.input.engine.EngineCommandPort
import org.fcitx.fcitx5.android.input.engine.EngineKey

/** Invalidates editor history at text-intent time, before preedit or commit effects arrive. */
class EditingAwareEngineCommandPort(
    private val delegate: EngineCommandPort,
    private val invalidateDestructiveHistory: () -> Unit
) : EngineCommandPort {
    override fun submit(command: EngineCommand): Job {
        if (EngineCommandHistoryPolicy.invalidatesDestructiveHistory(command)) {
            invalidateDestructiveHistory()
        }
        return delegate.submit(command)
    }
}

object EngineCommandHistoryPolicy {
    fun invalidatesDestructiveHistory(command: EngineCommand): Boolean = when (command) {
        is EngineCommand.SendKeyText -> true
        is EngineCommand.SendKey -> when (command.key) {
            EngineKey.ArrowLeft, EngineKey.ArrowRight, EngineKey.Backspace -> false
            EngineKey.Return, EngineKey.Space,
            is EngineKey.Unicode, is EngineKey.KeypadDigit, is EngineKey.KeypadOperator -> true
        }
        is EngineCommand.SelectCandidate -> true
        else -> false
    }
}
