/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.engine

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.fcitx.fcitx5.android.input.dependency.inputMethodService
import org.fcitx.fcitx5.android.input.session.PunctuationStateSink
import org.mechdancer.dependency.Dependent
import org.mechdancer.dependency.UniqueComponent
import org.mechdancer.dependency.manager.ManagedHandler
import org.mechdancer.dependency.manager.managedHandler

/** Resolves engine punctuation into session state through a narrowly scoped output port. */
class PunctuationController(private val sink: PunctuationStateSink) :
    UniqueComponent<PunctuationController>(), Dependent, ManagedHandler by managedHandler() {

    private val service by manager.inputMethodService()
    private var refreshJob: Job? = null

    fun refresh() {
        refreshJob?.cancel()
        val engine = service.inputSession.current.engine
        val enabled = engine.statusActions.any {
            it.name == "punctuation" && it.icon == "fcitx-punc-active"
        }
        refreshJob = service.lifecycleScope.launch {
            val mapping = if (enabled) {
                service.engineQueries.punctuationMapping(
                    engine.inputMethod?.languageCode.orEmpty()
                )
            } else emptyMap()
            currentCoroutineContext().ensureActive()
            sink.publish(mapping)
        }
    }

    fun dispose() {
        refreshJob?.cancel()
        refreshJob = null
    }
}
