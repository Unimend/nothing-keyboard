/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.input.engine

/** Owns the engine-context half of Android input binding. */
class InputContextLifecycleController(
    private val engineCommands: EngineCommandPort,
    private val synchronizePlatformInputMethod: () -> Unit
) {
    fun bind(uid: Int, packageName: String) {
        engineCommands.submit(EngineCommand.ActivateContext(uid, packageName))
        synchronizePlatformInputMethod()
    }

    fun unbind(uid: Int?) {
        uid ?: return
        engineCommands.submit(EngineCommand.DeactivateContext(uid))
    }
}
