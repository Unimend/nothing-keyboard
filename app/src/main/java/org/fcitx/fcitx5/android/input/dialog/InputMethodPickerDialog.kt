/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.dialog

import android.app.AlertDialog
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.design.NothingTextStyle
import org.fcitx.fcitx5.android.input.design.applyNothingTextStyle
import org.fcitx.fcitx5.android.input.design.setNothingText
import org.fcitx.fcitx5.android.input.design.toInputColorScheme
import org.fcitx.fcitx5.android.utils.AppUtil
import splitties.dimensions.dp
import splitties.resources.styledDrawable
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.wrapContent
import splitties.views.dsl.recyclerview.recyclerView
import splitties.views.recyclerview.verticalLayoutManager
import splitties.views.topPadding
import splitties.views.backgroundColor
import splitties.views.dsl.core.textView
import splitties.views.gravityCenter

object InputMethodPickerDialog {
    fun build(
        context: Context,
        theme: Theme,
        entries: List<InputMethodData>,
        enabledInputMethod: String?,
        onSelected: (InputMethodData) -> Unit
    ): AlertDialog {
        val enabledIndex = entries.indexOfFirst { it.uniqueName == enabledInputMethod }
        val dividerIndex = entries.indexOfFirst { it.ime }
        val colors = theme.toInputColorScheme()
        val title = context.textView {
            setNothingText(
                context.getString(R.string.choose_input_method),
                theme,
                NothingTextStyle.ControlLabel,
                sizeSp = 22f
            )
            setTextColor(colors.keyContent)
            gravity = gravityCenter
            setPadding(dp(20), dp(18), dp(20), dp(12))
        }
        lateinit var dialog: AlertDialog
        dialog = AlertDialog.Builder(context)
            .setCustomTitle(title)
            .setView(context.recyclerView {
                backgroundColor = colors.surface
                layoutParams = ViewGroup.LayoutParams(matchParent, wrapContent)
                // add some padding because AlertDialog's `titleDividerNoCustom` won't show up...
                // but why?
                // https://android.googlesource.com/platform/frameworks/base.git/+/refs/tags/android-11.0.0_r48/core/res/res/layout/alert_dialog_title_material.xml#58
                topPadding = dp(8) // android.R.dimen.dialog_title_divider_material
                layoutManager = verticalLayoutManager()
                adapter = InputMethodListAdapter(entries, enabledIndex, theme) {
                    onSelected(it)
                    dialog.dismiss()
                }
                styledDrawable(android.R.attr.dividerHorizontal)?.let {
                    addItemDecoration(SingleDividerDecoration(it, dividerIndex))
                }
            })
            .setNeutralButton(R.string.input_methods) { _, _ ->
                AppUtil.launchMainToInputMethodList(context)
            }
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(colors.surface))
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.apply {
                setTextColor(colors.activeContent)
                applyNothingTextStyle(theme, NothingTextStyle.ControlLabel)
            }
        }
        return dialog
    }
}
