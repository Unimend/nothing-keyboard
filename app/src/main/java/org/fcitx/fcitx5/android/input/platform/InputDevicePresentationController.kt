/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.input.platform

interface InputDevicePresentationHost {
    fun setCandidatePagingMode(mode: Int)
    fun monitorCursorAnchor(enabled: Boolean)
    fun hideStatusIcon()
    fun showCurrentStatusIcon()
    fun updateNavigationBar(isVirtualKeyboard: Boolean)
}

/** Applies every presentation side effect of a virtual/physical input-device transition. */
class InputDevicePresentationController(
    private val host: InputDevicePresentationHost
) {
    fun applyMode(isVirtualKeyboard: Boolean) {
        host.setCandidatePagingMode(if (isVirtualKeyboard) VIRTUAL_PAGING_MODE else HARDWARE_PAGING_MODE)
        host.monitorCursorAnchor(!isVirtualKeyboard)
        if (isVirtualKeyboard) host.hideStatusIcon() else host.showCurrentStatusIcon()
        host.updateNavigationBar(isVirtualKeyboard)
    }

    private companion object {
        const val VIRTUAL_PAGING_MODE = 0
        const val HARDWARE_PAGING_MODE = 1
    }
}
