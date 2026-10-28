/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.input.wm

/** Geometry policy only. Unknown keyboard geometry must not reuse an old editor's location. */
object ImeInsetsPolicy {
    fun top(virtualKeyboard: Boolean, keyboardTop: Int?, decorHeight: Int, navigationHeight: Int): Int {
        val height = decorHeight.coerceAtLeast(0)
        val bottom = (height - navigationHeight.coerceAtLeast(0)).coerceAtLeast(0)
        return if (virtualKeyboard && keyboardTop != null) keyboardTop.coerceIn(0, height) else bottom
    }
}
