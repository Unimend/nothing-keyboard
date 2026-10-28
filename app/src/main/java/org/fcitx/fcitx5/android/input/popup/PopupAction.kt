/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.popup

import android.graphics.Rect
import org.fcitx.fcitx5.android.input.keyboard.KeyAction
import org.fcitx.fcitx5.android.input.keyboard.KeyDef

sealed class PopupAction {

    abstract val viewId: Int

    data class PreviewAction(
        override val viewId: Int,
        val content: String,
        val bounds: Rect
    ) : PopupAction()

    data class PreviewUpdateAction(
        override val viewId: Int,
        val content: String,
    ) : PopupAction()

    data class DismissAction(
        override val viewId: Int
    ) : PopupAction()

    data class ShowKeyboardAction(
        override val viewId: Int,
        val keyboard: KeyDef.Popup.Keyboard,
        val bounds: Rect
    ) : PopupAction()

    data class ShowMenuAction(
        override val viewId: Int,
        val menu: KeyDef.Popup.Menu,
        val bounds: Rect
    ) : PopupAction()

    data class ChangeFocusAction(
        override val viewId: Int,
        val x: Float,
        val y: Float
    ) : PopupAction()

    data class TriggerAction(
        override val viewId: Int
    ) : PopupAction()
}

/** Results are returned explicitly; consumers never write into a caller's command object. */
sealed interface PopupResult {
    data object None : PopupResult
    data class FocusChanged(val changed: Boolean) : PopupResult
    data class Triggered(val action: KeyAction?) : PopupResult
}
