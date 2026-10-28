/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.bar.ui.idle

import android.content.Context
import androidx.annotation.DrawableRes
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.JustifyContent
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.bar.ui.ToolButton
import org.fcitx.fcitx5.android.input.design.inputSurfaceStyle
import splitties.dimensions.dp
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.view

class ButtonsBarUi(override val ctx: Context, private val theme: Theme) : Ui {

    private val visualStyle = theme.inputSurfaceStyle.toolbar

    override val root = view(::FlexboxLayout) {
        alignItems = AlignItems.CENTER
        justifyContent = JustifyContent.SPACE_AROUND
    }

    private fun toolButton(@DrawableRes icon: Int) = ToolButton(ctx, icon, theme).also {
        val size = ctx.dp(visualStyle.controlSizeDp)
        root.addView(it, FlexboxLayout.LayoutParams(size, size))
    }

    val undoButton = toolButton(R.drawable.ic_pixel_undo).apply {
        contentDescription = ctx.getString(R.string.undo)
    }

    val redoButton = toolButton(R.drawable.ic_pixel_redo).apply {
        contentDescription = ctx.getString(R.string.redo)
    }

    val cursorMoveButton = toolButton(R.drawable.ic_pixel_cursor_move).apply {
        contentDescription = ctx.getString(R.string.text_editing)
    }

    val clipboardButton = toolButton(R.drawable.ic_pixel_clipboard).apply {
        contentDescription = ctx.getString(R.string.clipboard)
    }

    val moreButton = toolButton(R.drawable.ic_pixel_more).apply {
        contentDescription = ctx.getString(R.string.status_area)
    }

}
