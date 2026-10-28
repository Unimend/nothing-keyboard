/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.input.wm

import android.inputmethodservice.InputMethodService.Insets
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.view.updateLayoutParams

/** Android full-window sizing and touch/content insets; Service supplies current platform views. */
class AndroidImeWindowLayout {
    private val keyboardLocation = IntArray(2)

    fun expandInputArea(content: View, input: View) {
        content.findViewById<View>(android.R.id.inputArea).updateLayoutParams<ViewGroup.LayoutParams> {
            height = ViewGroup.LayoutParams.MATCH_PARENT
        }
        input.updateLayoutParams<ViewGroup.LayoutParams> {
            height = ViewGroup.LayoutParams.MATCH_PARENT
        }
    }

    fun configure(window: Window) {
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    fun computeInsets(out: Insets, virtualKeyboard: Boolean, keyboard: View?, decor: View) {
        val keyboardTop = keyboard?.let {
            it.getLocationInWindow(keyboardLocation)
            keyboardLocation[1]
        }
        val navigationHeight = decor.findViewById<View>(android.R.id.navigationBarBackground)?.height ?: 0
        val top = ImeInsetsPolicy.top(virtualKeyboard, keyboardTop, decor.height, navigationHeight)
        out.contentTopInsets = top
        out.visibleTopInsets = top
        out.touchableInsets = Insets.TOUCHABLE_INSETS_VISIBLE
    }
}
