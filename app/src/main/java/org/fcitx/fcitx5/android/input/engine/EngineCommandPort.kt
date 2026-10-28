/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.engine

import kotlinx.coroutines.Job

/** Replaceable outbound boundary implemented by the active input engine host. */
fun interface EngineCommandPort {
    fun submit(command: EngineCommand): Job
}
