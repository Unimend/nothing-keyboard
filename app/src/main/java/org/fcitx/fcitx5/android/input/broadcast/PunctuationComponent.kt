/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.broadcast

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.core.Action
import org.fcitx.fcitx5.android.input.dependency.inputMethodService
import org.mechdancer.dependency.Dependent
import org.mechdancer.dependency.UniqueComponent
import org.mechdancer.dependency.manager.ManagedHandler
import org.mechdancer.dependency.manager.managedHandler
import org.mechdancer.dependency.manager.must

class PunctuationComponent :
    UniqueComponent<PunctuationComponent>(), Dependent, ManagedHandler by managedHandler() {

    private val service by manager.inputMethodService()
    private val broadcaster: InputBroadcaster by manager.must()

    private var mapping: Map<String, String> = emptyMap()

    var enabled: Boolean = false
        private set

    fun transform(p: String) = mapping.getOrDefault(p, p)

    fun updatePunctuationMapping(actions: Array<Action>) {
        val engine = service.inputSession.current.engine
        enabled = engine.statusActions.any {
            // TODO: A better way to check if punctuation mapping is enabled
            it.name == "punctuation" && it.icon == "fcitx-punc-active"
        }
        service.lifecycleScope.launch {
            mapping = if (enabled) {
                val language = engine.inputMethod?.languageCode.orEmpty()
                service.engineQueries.punctuationMapping(language)
            } else emptyMap()
            broadcaster.onPunctuationUpdate(mapping)
        }
    }
}
