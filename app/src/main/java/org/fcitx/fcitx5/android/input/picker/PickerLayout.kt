/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.picker

import android.annotation.SuppressLint
import android.content.Context
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.viewpager2.widget.ViewPager2
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.keyboard.*
import org.fcitx.fcitx5.android.input.mode.InputMode
import org.fcitx.fcitx5.android.input.mode.KeyboardLayout
import org.fcitx.fcitx5.android.input.design.inputSurfaceStyle
import splitties.dimensions.dp
import splitties.views.dsl.constraintlayout.above
import splitties.views.dsl.constraintlayout.below
import splitties.views.dsl.constraintlayout.bottomOfParent
import splitties.views.dsl.constraintlayout.centerHorizontally
import splitties.views.dsl.constraintlayout.lParams
import splitties.views.dsl.constraintlayout.matchConstraints
import splitties.views.dsl.constraintlayout.topOfParent
import splitties.views.dsl.core.add
import splitties.views.dsl.core.view
import splitties.views.imageResource

@SuppressLint("ViewConstructor")
class PickerLayout(context: Context, theme: Theme, switchKey: KeyDef) :
    ConstraintLayout(context) {

    class Keyboard(context: Context, theme: Theme, switchKey: KeyDef) : BaseKeyboard(
        context, theme, listOf(
            listOf(
                LayoutSwitchKey("ABC", InputMode.Keyboard(KeyboardLayout.Text)),
                PunctuationKey(",", theme.inputSurfaceStyle.picker.punctuationTextSizeSp),
                switchKey,
                SpaceKey(),
                PunctuationKey(".", theme.inputSurfaceStyle.picker.punctuationTextSizeSp),
                ReturnKey()
            )
        )
    ) {

        class PunctuationKey(val symbol: String, textSizeSp: Float) : KeyDef(
            Appearance.Text(
                displayText = symbol,
                textSize = textSizeSp,
                percentWidth = 0.1f,
                variant = Appearance.Variant.Alternative
            ),
            setOf(
                Behavior.Press(KeyAction.TextKeyAction(symbol))
            )
        )

        val `return`: ImageKeyView by lazy { findViewById(R.id.button_return) }

        override fun onReturnDrawableUpdate(returnDrawable: Int) {
            `return`.img.imageResource = returnDrawable
        }
    }

    val embeddedKeyboard = Keyboard(context, theme, switchKey)

    val pager = view(::ViewPager2) { }

    val tabsUi = PickerTabsUi(context, theme)

    val paginationUi = PickerPaginationUi(context, theme)

    private val visualStyle = theme.inputSurfaceStyle.picker

    init {
        add(pager, lParams {
            topOfParent()
            centerHorizontally()
            above(embeddedKeyboard)
        })
        add(embeddedKeyboard, lParams {
            below(pager)
            centerHorizontally()
            bottomOfParent()
            matchConstraintPercentHeight = visualStyle.embeddedKeyboardHeightFraction
        })
        add(paginationUi.root, lParams(matchConstraints, dp(visualStyle.paginationHeightDp)) {
            centerHorizontally()
            below(pager, dp(-1))
        })
    }
}
