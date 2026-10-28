/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.editorinfo

import android.content.Context
import android.widget.TableLayout
import android.widget.TableRow
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.design.NothingTextStyle
import org.fcitx.fcitx5.android.input.design.applyNothingTextStyle
import org.fcitx.fcitx5.android.input.design.toInputColorScheme
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.textView
import splitties.views.dsl.core.view
import splitties.views.dsl.core.wrapInHorizontalScrollView
import splitties.views.dsl.core.wrapInScrollView
import splitties.views.setPaddingDp

class EditorInfoUi(override val ctx: Context, private val theme: Theme) : Ui {
    private val colors = theme.toInputColorScheme()

    private fun createTextView(str: String) = textView {
        text = str
        applyNothingTextStyle(theme, NothingTextStyle.Body, sizeSp = null)
        setPaddingDp(3)
        setTextColor(colors.keyContent)
    }

    private fun TableLayout.addRow(label: String, value: String) {
        addView(view(::TableRow) {
            addView(createTextView(label))
            addView(createTextView(value))
        })
    }

    val table = view(::TableLayout) {
        isStretchAllColumns = true
    }

    override val root = table
        .wrapInHorizontalScrollView()
        .wrapInScrollView { isFillViewport = true }

    fun setValues(values: Map<String, String>) {
        table.apply {
            removeAllViews()
            values.forEach { (k, v) ->
                addRow(k, v)
            }
        }
    }
}
