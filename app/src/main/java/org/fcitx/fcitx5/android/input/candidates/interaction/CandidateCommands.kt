/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.candidates.interaction

import org.fcitx.fcitx5.android.input.engine.EngineCommandPort
import org.fcitx.fcitx5.android.input.engine.EngineCommand

/** Candidate command boundary for surfaces that live outside the InputView dependency scope. */
fun EngineCommandPort.selectCandidate(engineIndex: Int) {
    if (engineIndex >= 0) submit(EngineCommand.SelectCandidate(engineIndex))
}

fun EngineCommandPort.triggerCandidateTabAction(actionId: Int) {
    submit(EngineCommand.TriggerCandidateTabAction(actionId))
}

fun EngineCommandPort.triggerCandidateAction(engineIndex: Int, actionId: Int) {
    if (engineIndex >= 0) {
        submit(EngineCommand.TriggerCandidateAction(engineIndex, actionId))
    }
}

fun EngineCommandPort.offsetCandidatePage(delta: Int) {
    if (delta != 0) submit(EngineCommand.OffsetCandidatePage(delta))
}
