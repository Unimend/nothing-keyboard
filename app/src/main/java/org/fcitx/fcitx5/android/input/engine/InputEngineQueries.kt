/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.engine

import org.fcitx.fcitx5.android.input.session.EngineCandidate
import org.fcitx.fcitx5.android.input.session.EngineCandidateAction
import org.fcitx.fcitx5.android.input.session.EngineInputMethod

/** Read-only input-engine boundary. No engine-specific type may cross this interface. */
interface InputEngineQueries {
    suspend fun candidates(offset: Int, limit: Int): List<EngineCandidate>
    suspend fun candidateActions(index: Int): List<EngineCandidateAction>
    suspend fun enabledInputMethods(): List<EngineInputMethod>
    suspend fun punctuationMapping(languageCode: String): Map<String, String>
}
