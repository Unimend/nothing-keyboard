/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2025 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.bar.ui

import android.content.Context
import android.view.View
import androidx.core.view.isVisible
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.design.NothingTextStyle
import org.fcitx.fcitx5.android.input.design.applyNothingTextStyle
import org.fcitx.fcitx5.android.input.design.setNothingText
import org.fcitx.fcitx5.android.input.design.inputSurfaceStyle
import org.fcitx.fcitx5.android.input.design.toInputColorScheme
import splitties.dimensions.dp
import splitties.views.dsl.constraintlayout.after
import splitties.views.dsl.constraintlayout.bottomOfParent
import splitties.views.dsl.constraintlayout.centerHorizontally
import splitties.views.dsl.constraintlayout.centerVertically
import splitties.views.dsl.constraintlayout.constraintLayout
import splitties.views.dsl.constraintlayout.endOfParent
import splitties.views.dsl.constraintlayout.lParams
import splitties.views.dsl.constraintlayout.matchConstraints
import splitties.views.dsl.constraintlayout.startOfParent
import splitties.views.dsl.constraintlayout.topOfParent
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.add
import splitties.views.dsl.core.textView
import splitties.views.dsl.core.wrapContent
import splitties.views.gravityVerticalCenter

class TitleUi(override val ctx: Context, private val theme: Theme) : Ui {

    private val visualStyle = theme.inputSurfaceStyle.toolbar
    private val colors = theme.toInputColorScheme()

    private val backButton = ToolButton(ctx, R.drawable.ic_baseline_arrow_back_24, theme).apply {
        contentDescription = ctx.getString(R.string.back_to_keyboard)
    }

    private val titleText = textView {
        applyNothingTextStyle(theme, NothingTextStyle.ControlLabel)
        setTextColor(colors.alternateKeyContent)
        gravity = gravityVerticalCenter
    }

    private var extension: View? = null

    override val root = constraintLayout {
        add(backButton, lParams(dp(visualStyle.controlSizeDp), dp(visualStyle.controlSizeDp)) {
            topOfParent()
            startOfParent()
            bottomOfParent()
        })
        add(titleText, lParams(wrapContent, dp(visualStyle.controlSizeDp)) {
            topOfParent()
            after(backButton, dp(visualStyle.titleSpacingDp))
            bottomOfParent()
        })
    }

    fun setReturnButtonOnClickListener(block: () -> Unit) {
        backButton.setOnClickListener {
            block()
        }
    }

    fun setTitle(title: String) {
        titleText.setNothingText(title, theme, NothingTextStyle.ControlLabel)
        backButton.isVisible = true
        titleText.isVisible = true
    }

    fun showBlank() {
        removeExtension()
        titleText.text = ""
        backButton.isVisible = false
        titleText.isVisible = false
    }

    fun addExtension(view: View, showTitle: Boolean) {
        if (extension != null) {
            throw IllegalStateException("TitleBar extension is already present")
        }
        backButton.isVisible = showTitle
        titleText.isVisible = showTitle
        extension = view
        root.run {
            add(view, lParams(matchConstraints, dp(visualStyle.controlSizeDp)) {
                centerVertically()
                if (showTitle) {
                    endOfParent(dp(visualStyle.extensionEndInsetDp))
                } else {
                    centerHorizontally()
                }
            })
        }
    }

    fun removeExtension() {
        extension?.let {
            root.removeView(it)
            extension = null
        }
    }
}
