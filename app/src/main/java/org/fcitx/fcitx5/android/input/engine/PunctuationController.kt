/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.engine

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.input.dependency.inputMethodService
import org.fcitx.fcitx5.android.input.session.InputSessionEvent
import org.mechdancer.dependency.Dependent
import org.mechdancer.dependency.UniqueComponent
import org.mechdancer.dependency.manager.ManagedHandler
import org.mechdancer.dependency.manager.managedHandler

/** Resolves engine punctuation into session state; consumers read only InputSessionStore. */
class PunctuationController :
    UniqueComponent<PunctuationController>(), Dependent, ManagedHandler by managedHandler() {

    private val service by manager.inputMethodService()

    fun refresh() {
        val engine = service.inputSession.current.engine
        val enabled = engine.statusActions.any {
            it.name == "punctuation" && it.icon == "fcitx-punc-active"
        }
        service.lifecycleScope.launch {
            val mapping = if (enabled) {
                service.engineQueries.punctuationMapping(
                    engine.inputMethod?.languageCode.orEmpty()
                )
            } else emptyMap()
            service.inputSession.dispatch(InputSessionEvent.PunctuationChanged(mapping))
        }
    }
}
