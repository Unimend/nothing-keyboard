/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.engine

import org.fcitx.fcitx5.android.daemon.FcitxConnection
import org.fcitx.fcitx5.android.data.punctuation.PunctuationManager
import org.fcitx.fcitx5.android.input.debug.InputCoreDiagnostics

/** Fcitx implementation of the project-owned read boundary. */
class FcitxEngineQueries(private val connection: FcitxConnection) : InputEngineQueries {
    override suspend fun candidates(offset: Int, limit: Int) = connection.runOnReady {
        InputCoreDiagnostics.recordEngineQuery("Candidates")
        getCandidates(offset, limit).map(FcitxEngineStateAdapter::candidate)
    }

    override suspend fun candidateActions(index: Int) = connection.runOnReady {
        InputCoreDiagnostics.recordEngineQuery("CandidateActions")
        getCandidateActions(index).map(FcitxEngineStateAdapter::candidateAction)
    }

    override suspend fun enabledInputMethods() = connection.runOnReady {
        InputCoreDiagnostics.recordEngineQuery("EnabledInputMethods")
        enabledIme().map(FcitxEngineStateAdapter::inputMethod)
    }

    override suspend fun punctuationMapping(languageCode: String) = connection.runOnReady {
        InputCoreDiagnostics.recordEngineQuery("PunctuationMapping")
        buildMap {
            PunctuationManager.load(this@runOnReady, languageCode).forEach {
                putIfAbsent(it.key, it.mapping)
            }
        }
    }
}
