/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.preedit

import android.view.View
import org.fcitx.fcitx5.android.input.session.EngineInputPanel
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.design.InputThemeSettings
import org.fcitx.fcitx5.android.input.dependency.context
import org.fcitx.fcitx5.android.input.dependency.theme
import org.fcitx.fcitx5.android.input.design.toInputColorScheme
import org.mechdancer.dependency.Dependent
import org.mechdancer.dependency.UniqueComponent
import org.mechdancer.dependency.manager.ManagedHandler
import org.mechdancer.dependency.manager.managedHandler
import splitties.dimensions.dp
import splitties.views.backgroundColor
import splitties.views.horizontalPadding

class PreeditComponent : UniqueComponent<PreeditComponent>(), Dependent,
    ManagedHandler by managedHandler() {

    private val context by manager.context()
    private val theme by manager.theme()

    val ui by lazy {
        val keyBorder = InputThemeSettings.keyBorder.getValue()
        val colors = theme.toInputColorScheme()
        val bkgColor =
            if (!keyBorder && theme is Theme.Builtin) colors.barSurface else colors.background
        PreeditUi(context, theme, setupTextView = {
            backgroundColor = bkgColor
            horizontalPadding = dp(8)
        }).apply {
            // TODO make it customizable
            root.alpha = 0.8f
            root.visibility = View.INVISIBLE
        }
    }

    fun onInputPanelUpdate(data: EngineInputPanel) {
        ui.update(data)
        ui.root.visibility = if (ui.visible) View.VISIBLE else View.INVISIBLE
    }
}
