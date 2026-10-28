/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.lifecycle

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/** A single-use lifetime for one concrete Android input view and every surface it owns. */
class InputSurfaceLifetime(
    context: CoroutineContext = Dispatchers.Main.immediate
) : AutoCloseable {
    private val ownerJob = SupervisorJob(context[Job])
    val scope: CoroutineScope = CoroutineScope(context + ownerJob)
    val isClosed: Boolean get() = !ownerJob.isActive

    override fun close() {
        scope.cancel()
    }
}
