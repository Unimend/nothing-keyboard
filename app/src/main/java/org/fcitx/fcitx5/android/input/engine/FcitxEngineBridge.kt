/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.core.FcitxEvent
import org.fcitx.fcitx5.android.daemon.FcitxConnection
import org.fcitx.fcitx5.android.input.debug.InputCoreDiagnostics
import org.fcitx.fcitx5.android.input.session.EngineStateEvent
import org.fcitx.fcitx5.android.input.session.FcitxEngineStateAdapter
import org.fcitx.fcitx5.android.input.session.InputSessionEvent
import org.fcitx.fcitx5.android.input.session.InputSessionStore

/**
 * The single service-level ingress for Fcitx events.
 *
 * Stateful events are translated into project-owned immutable state before effect events are
 * forwarded to Android. Views must observe [InputSessionStore] and never subscribe to Fcitx.
 */
class FcitxEngineBridge(
    private val connection: FcitxConnection,
    private val session: InputSessionStore,
    private val effectSink: (FcitxEvent<*>) -> Unit
) {
    private var collectionJob: Job? = null

    fun startIn(scope: CoroutineScope) {
        check(collectionJob == null) { "FcitxEngineBridge may only be started once" }
        collectionJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            connection.runImmediately { eventFlow }.collect { event ->
                InputCoreDiagnostics.recordEngineEvent(event::class.simpleName ?: "FcitxEvent")
                if (!session.current.engine.ready) {
                    session.dispatch(InputSessionEvent.EngineChanged(EngineStateEvent.Operational))
                }
                publishState(event)
                if (event is FcitxEvent.ReadyEvent) publishCachedState()
                effectSink(event)
            }
        }
        // UNDISPATCHED enters SharedFlow collection before returning, closing the gap between the
        // cache snapshot and live events for an engine that was already ready.
        val alreadyReady = connection.runImmediately { isReady }
        publishCachedState()
        if (alreadyReady) {
            session.dispatch(InputSessionEvent.EngineChanged(EngineStateEvent.Operational))
        }
    }

    private fun publishState(event: FcitxEvent<*>) {
        FcitxEngineStateAdapter.map(event)?.let {
            session.dispatch(InputSessionEvent.EngineChanged(it))
        }
    }

    /** Restores cache-backed state when attaching after the engine has already initialized. */
    private fun publishCachedState() {
        val events = connection.runImmediately {
            listOf(
                FcitxEvent.ClientPreeditEvent(clientPreeditCached),
                FcitxEvent.InputPanelEvent(inputPanelCached),
                FcitxEvent.IMChangeEvent(inputMethodEntryCached),
                FcitxEvent.StatusAreaEvent(
                    FcitxEvent.StatusAreaEvent.Data(statusAreaActionsCached, inputMethodEntryCached)
                )
            )
        }
        events.forEach(::publishState)
    }
}
