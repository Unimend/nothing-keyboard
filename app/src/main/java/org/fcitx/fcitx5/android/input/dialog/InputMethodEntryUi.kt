/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.dialog

import android.content.Context
import android.text.TextUtils
import android.view.ViewGroup
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.NothingDotMatrixDrawable
import org.fcitx.fcitx5.android.input.design.NothingTextStyle
import org.fcitx.fcitx5.android.input.design.setNothingText
import org.fcitx.fcitx5.android.input.design.toInputColorScheme
import splitties.dimensions.dp
import splitties.resources.styledDimenPxSize
import splitties.resources.styledDrawable
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.checkedTextView
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.wrapContent
import splitties.views.gravityVerticalCenter

// https://android.googlesource.com/platform/frameworks/base.git/+/refs/tags/android-11.0.0_r48/core/res/res/layout/input_method_switch_item.xml
class InputMethodEntryUi(override val ctx: Context, private val theme: Theme) : Ui {
    private val colors = theme.toInputColorScheme()

    override val root = checkedTextView {
        gravity = gravityVerticalCenter
        minHeight = styledDimenPxSize(android.R.attr.listPreferredItemHeightSmall)
        setTextColor(colors.keyContent)
        setPaddingRelative(
            styledDimenPxSize(android.R.attr.listPreferredItemPaddingStart), 0,
            styledDimenPxSize(android.R.attr.listPreferredItemPaddingEnd), 0
        )
        val marker = NothingDotMatrixDrawable(
            color = colors.activeContent,
            columns = 1,
            rows = 4,
            dotDiameter = dp(3).toFloat(),
            dotGap = dp(2).toFloat()
        ).apply { setBounds(0, 0, dp(3), dp(18)) }
        setCompoundDrawablesRelative(marker, null, null, null)
        compoundDrawablePadding = dp(16)
        ellipsize = TextUtils.TruncateAt.MARQUEE
        background = styledDrawable(android.R.attr.selectableItemBackground)
        isFocusable = true
        isClickable = true
        layoutParams = ViewGroup.LayoutParams(matchParent, wrapContent)
    }

    fun bind(name: String, selected: Boolean, onClick: () -> Unit) {
        root.isChecked = selected
        root.compoundDrawablesRelative.firstOrNull()?.alpha = if (selected) 255 else 0
        root.setNothingText(name, theme, NothingTextStyle.ControlLabel, sizeSp = 17f)
        root.setOnClickListener { onClick() }
    }
}
