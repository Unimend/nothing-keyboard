/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.input.platform

import org.fcitx.fcitx5.android.input.session.EditorSessionSnapshot

interface InputViewPresentationHost {
    fun startVirtualInput(session: EditorSessionSnapshot)
    fun enableCursorMonitoring(): Boolean
    fun showCandidateFallback()
    fun showCurrentStatusIcon()
    fun invalidateCandidateAnchor()
    fun finishInputDeviceView()
    fun disableCursorMonitoring()
    fun hideStatusIcon()
    fun dismissDialogs()
}

/** Owns the ordered Android presentation work around input-view lifecycle transitions. */
class InputViewPresentationController(
    private val host: InputViewPresentationHost
) {
    fun start(isVirtualKeyboard: Boolean, session: EditorSessionSnapshot?) {
        if (isVirtualKeyboard) {
            session?.let(host::startVirtualInput)
            return
        }
        if (!host.enableCursorMonitoring()) host.showCandidateFallback()
        host.showCurrentStatusIcon()
    }

    fun finish(finishLifecycle: () -> Unit) {
        host.invalidateCandidateAnchor()
        host.finishInputDeviceView()
        finishLifecycle()
        host.disableCursorMonitoring()
        host.hideStatusIcon()
        host.dismissDialogs()
    }
}
