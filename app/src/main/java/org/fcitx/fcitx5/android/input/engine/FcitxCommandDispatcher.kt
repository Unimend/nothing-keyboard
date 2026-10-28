package org.fcitx.fcitx5.android.input.engine

import android.view.KeyEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import org.fcitx.fcitx5.android.daemon.FcitxConnection
import org.fcitx.fcitx5.android.input.debug.InputCoreDiagnostics
import timber.log.Timber

/** Engine readiness, ordering and native translation are owned by the engine boundary. */
class FcitxCommandDispatcher(connection: FcitxConnection, scope: CoroutineScope) : EngineCommandPort {
    private val connection = connection
    private val queue = SerialCommandQueue(scope) { Timber.e(it, "Engine command failed") }

    override fun submit(command: EngineCommand): Job = submit(command) { true }

    fun submit(command: EngineCommand, guard: () -> Boolean): Job = queue.submit {
        connection.runOnReady {
            if (guard()) {
                InputCoreDiagnostics.recordEngineCommand(command::class.simpleName ?: "EngineCommand")
                FcitxEngineCommandAdapter.execute(this, command)
            }
        }
    }

    fun submitHardwareKey(event: KeyEvent, eventId: Int): Boolean {
        // Android needs a synchronous answer so unsupported keys can reach the superclass.
        if (!FcitxEngineCommandAdapter.acceptsHardwareKey(event)) return false
        queue.submit {
            connection.runOnReady {
                if (!FcitxEngineCommandAdapter.executeHardwareKey(this, event, eventId)) {
                    Timber.d("Skipped KeyEvent: $event")
                }
            }
        }
        return true
    }
}
