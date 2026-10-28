/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.wm

import org.fcitx.fcitx5.android.input.debug.InputCoreDiagnostics
import org.mechdancer.dependency.Dependent
import org.mechdancer.dependency.ScopeEvent
import org.mechdancer.dependency.UniqueComponent
import java.util.concurrent.ConcurrentLinkedQueue

interface InputWindowLifecycleObserver {
    fun onWindowAttached(window: InputWindow) {}
    fun onWindowDetached(window: InputWindow) {}
}

/** Session-scoped window lifecycle channel, independent from engine/UI data broadcasts. */
class InputWindowLifecycleDispatcher : UniqueComponent<InputWindowLifecycleDispatcher>(), Dependent {
    private val observers = ConcurrentLinkedQueue<InputWindowLifecycleObserver>()

    override fun handle(scopeEvent: ScopeEvent) {
        when (scopeEvent) {
            is ScopeEvent.DependencyArrivedEvent ->
                (scopeEvent.dependency as? InputWindowLifecycleObserver)?.let(observers::add)
            is ScopeEvent.DependencyLeftEvent ->
                (scopeEvent.dependency as? InputWindowLifecycleObserver)?.let(observers::remove)
        }
    }

    fun attached(window: InputWindow) {
        InputCoreDiagnostics.recordWindowLifecycle("Attached(${window.surface})")
        observers.forEach { it.onWindowAttached(window) }
    }

    fun detached(window: InputWindow) {
        InputCoreDiagnostics.recordWindowLifecycle("Detached(${window.surface})")
        observers.forEach { it.onWindowDetached(window) }
    }
}
