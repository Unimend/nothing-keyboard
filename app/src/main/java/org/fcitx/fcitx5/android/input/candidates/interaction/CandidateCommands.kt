/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.candidates.interaction

import org.fcitx.fcitx5.android.input.FcitxInputMethodService
import org.fcitx.fcitx5.android.input.engine.EngineCommand

/** Candidate command boundary for surfaces that live outside the InputView dependency scope. */
fun FcitxInputMethodService.selectCandidate(engineIndex: Int) {
    if (engineIndex >= 0) submitEngineCommand(EngineCommand.SelectCandidate(engineIndex))
}

fun FcitxInputMethodService.triggerCandidateTabAction(actionId: Int) {
    submitEngineCommand(EngineCommand.TriggerCandidateTabAction(actionId))
}

fun FcitxInputMethodService.triggerCandidateAction(engineIndex: Int, actionId: Int) {
    if (engineIndex >= 0) {
        submitEngineCommand(EngineCommand.TriggerCandidateAction(engineIndex, actionId))
    }
}

fun FcitxInputMethodService.offsetCandidatePage(delta: Int) {
    if (delta != 0) submitEngineCommand(EngineCommand.OffsetCandidatePage(delta))
}
