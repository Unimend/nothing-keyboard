/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.fcitx.fcitx5.android.input.session.PunctuationStateSink
import org.fcitx.fcitx5.android.input.session.InputSessionReader
import org.mechdancer.dependency.UniqueComponent

/** Resolves engine punctuation into session state through a narrowly scoped output port. */
class PunctuationController(
    private val sink: PunctuationStateSink,
    private val session: InputSessionReader,
    private val queries: InputEngineQueries,
    private val scope: CoroutineScope
) : UniqueComponent<PunctuationController>() {

    private var refreshJob: Job? = null
    private var disposed = false

    fun refresh() {
        if (disposed) return
        refreshJob?.cancel()
        val engine = session.current.engine
        val enabled = engine.statusActions.any {
            it.name == "punctuation" && it.icon == "fcitx-punc-active"
        }
        refreshJob = scope.launch {
            val mapping = if (enabled) {
                queries.punctuationMapping(
                    engine.inputMethod?.languageCode.orEmpty()
                )
            } else emptyMap()
            currentCoroutineContext().ensureActive()
            sink.publish(mapping)
        }
    }

    fun dispose() {
        disposed = true
        refreshJob?.cancel()
        refreshJob = null
    }
}
