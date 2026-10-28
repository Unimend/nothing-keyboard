/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.broadcast

import androidx.annotation.DrawableRes
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.input.design.NothingGlyphRegistry
import org.fcitx.fcitx5.android.input.design.isNothingStyle
import org.fcitx.fcitx5.android.input.session.EditorAction
import org.fcitx.fcitx5.android.input.session.EditorSnapshot
import org.mechdancer.dependency.Dependent
import org.mechdancer.dependency.UniqueComponent
import org.mechdancer.dependency.manager.ManagedHandler
import org.mechdancer.dependency.manager.managedHandler
import org.mechdancer.dependency.manager.must

class ReturnKeyDrawableComponent :
    UniqueComponent<ReturnKeyDrawableComponent>(), Dependent, ManagedHandler by managedHandler() {

    companion object {
        @DrawableRes
        val DEFAULT_DRAWABLE = R.drawable.ic_baseline_keyboard_return_24
    }

    private val broadcaster: InputBroadcaster by manager.must()

    @DrawableRes
    var resourceId: Int = DEFAULT_DRAWABLE
        private set

    @DrawableRes
    private var actionDrawable: Int = DEFAULT_DRAWABLE

    @DrawableRes
    private fun drawableFromEditor(editor: EditorSnapshot): Int {
        return themed(when (editor.action) {
            EditorAction.Go -> R.drawable.ic_baseline_arrow_forward_24
            EditorAction.Search -> R.drawable.ic_baseline_search_24
            EditorAction.Send -> R.drawable.ic_baseline_send_24
            EditorAction.Next -> R.drawable.ic_baseline_keyboard_tab_24
            EditorAction.Done -> R.drawable.ic_baseline_done_24
            EditorAction.Previous -> R.drawable.ic_baseline_keyboard_tab_reverse_24
            EditorAction.Return -> R.drawable.ic_baseline_keyboard_return_24
        })
    }

    @DrawableRes
    private fun themed(@DrawableRes drawable: Int): Int {
        if (!ThemeManager.activeTheme.isNothingStyle) return drawable
        return NothingGlyphRegistry.resolve(drawable)
    }

    fun updateDrawableOnEditor(editor: EditorSnapshot) {
        actionDrawable = drawableFromEditor(editor)
        if (resourceId == actionDrawable) return
        resourceId = actionDrawable
        broadcaster.onReturnKeyDrawableUpdate(resourceId)
    }

    fun updateDrawableOnPreedit(preeditEmpty: Boolean) {
        val newResId = if (preeditEmpty) actionDrawable else themed(DEFAULT_DRAWABLE)
        if (resourceId == newResId) return
        resourceId = newResId
        broadcaster.onReturnKeyDrawableUpdate(resourceId)
    }
}
