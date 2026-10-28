/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.broadcast

import org.fcitx.fcitx5.android.input.session.EngineCandidateList
import org.fcitx.fcitx5.android.input.session.EngineInputMethod
import org.fcitx.fcitx5.android.input.session.EngineInputPanel
import org.fcitx.fcitx5.android.input.session.EngineStatusAction
import org.fcitx.fcitx5.android.input.session.EngineText
import org.fcitx.fcitx5.android.input.session.EditorSessionSnapshot
import org.fcitx.fcitx5.android.input.session.EditorSessionChange
import org.mechdancer.dependency.Dependent
import org.mechdancer.dependency.ScopeEvent
import org.mechdancer.dependency.UniqueComponent
import java.util.concurrent.ConcurrentLinkedQueue

class InputBroadcaster : UniqueComponent<InputBroadcaster>(), Dependent, InputBroadcastReceiver {

    private val receivers = ConcurrentLinkedQueue<InputBroadcastReceiver>()

    override fun handle(scopeEvent: ScopeEvent) {
        when (scopeEvent) {
            is ScopeEvent.DependencyArrivedEvent -> {
                if (scopeEvent.dependency is InputBroadcastReceiver && scopeEvent.dependency !is InputBroadcaster) {
                    receivers.add(scopeEvent.dependency as InputBroadcastReceiver)
                }
            }

            is ScopeEvent.DependencyLeftEvent -> {
                if (scopeEvent.dependency is InputBroadcastReceiver && scopeEvent.dependency !is InputBroadcaster) {
                    receivers.remove(scopeEvent.dependency as InputBroadcastReceiver)
                }
            }
        }
    }

    override fun onClientPreeditUpdate(data: EngineText) {
        receivers.forEach { it.onClientPreeditUpdate(data) }
    }

    override fun onInputPanelUpdate(data: EngineInputPanel) {
        receivers.forEach { it.onInputPanelUpdate(data) }
    }

    override fun onEditorSessionUpdate(session: EditorSessionSnapshot, change: EditorSessionChange) {
        receivers.forEach { it.onEditorSessionUpdate(session, change) }
    }

    override fun onImeUpdate(ime: EngineInputMethod) {
        receivers.forEach { it.onImeUpdate(ime) }
    }

    override fun onCandidateUpdate(data: EngineCandidateList) {
        receivers.forEach { it.onCandidateUpdate(data) }
    }

    override fun onStatusAreaUpdate(actions: List<EngineStatusAction>) {
        receivers.forEach { it.onStatusAreaUpdate(actions) }
    }

    override fun onPunctuationUpdate(mapping: Map<String, String>) {
        receivers.forEach { it.onPunctuationUpdate(mapping) }
    }

    override fun onPreeditEmptyStateUpdate(empty: Boolean) {
        receivers.forEach { it.onPreeditEmptyStateUpdate(empty) }
    }

    override fun onReturnKeyDrawableUpdate(resourceId: Int) {
        receivers.forEach { it.onReturnKeyDrawableUpdate(resourceId) }
    }

}
