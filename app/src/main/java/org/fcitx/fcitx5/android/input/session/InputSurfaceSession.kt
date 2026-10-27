/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class InputSurfaceSnapshot(
    val current: InputSurface = InputSurface.None,
    val previous: InputSurface = InputSurface.None,
    val revision: Long = 0
)

/** Single source of truth for the active keyboard surface. */
class InputSurfaceSession {
    private val mutableState = MutableStateFlow(InputSurfaceSnapshot())
    val state = mutableState.asStateFlow()

    val current: InputSurface
        get() = mutableState.value.current

    fun transitionTo(surface: InputSurface) {
        val old = mutableState.value
        if (old.current == surface) return
        mutableState.value = InputSurfaceSnapshot(
            current = surface,
            previous = old.current,
            revision = old.revision + 1
        )
    }
}
