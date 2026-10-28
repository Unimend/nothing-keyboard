/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2024 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.input.editing

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import androidx.annotation.DrawableRes
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.design.NothingTextStyle
import org.fcitx.fcitx5.android.input.design.NothingGlyphRegistry
import org.fcitx.fcitx5.android.input.design.applyNothingTextStyle
import org.fcitx.fcitx5.android.input.design.isNothingStyle
import org.fcitx.fcitx5.android.input.design.inputSurfaceStyle
import org.fcitx.fcitx5.android.input.design.toInputColorScheme
import org.fcitx.fcitx5.android.input.keyboard.CustomGestureView
import org.fcitx.fcitx5.android.input.keyboard.shadowedKeyBackgroundDrawable
import org.fcitx.fcitx5.android.input.keyboard.insetRadiusDrawable
import org.fcitx.fcitx5.android.utils.borderDrawable
import org.fcitx.fcitx5.android.utils.pressHighlightDrawable
import org.fcitx.fcitx5.android.utils.rippleDrawable
import splitties.dimensions.dp
import splitties.views.dsl.core.add
import splitties.views.dsl.core.imageView
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.textView
import splitties.views.dsl.core.wrapContent
import splitties.views.gravityCenter
import splitties.views.imageResource
import kotlin.math.max

@SuppressLint("ViewConstructor")
class TextEditingButton(
    ctx: Context,
    private val theme: Theme,
    private val rippled: Boolean,
    private val bordered: Boolean,
    private val radius: Float,
    private val altStyle: Boolean = false
) : CustomGestureView(ctx) {

    private val visualStyle = theme.inputSurfaceStyle.editing
    private val colors = theme.toInputColorScheme()

    // bordered
    private val shadowWidth = dp(visualStyle.shadowWidthDp)
    private val hInset = dp(visualStyle.contentInsetDp)
    private val vInset = dp(visualStyle.contentInsetDp)

    // !bordered
    private val lineWidth = max(1, dp(visualStyle.dividerWidthDp) / 2)

    init {
        if (bordered) {
            val bkgColor = if (altStyle) colors.alternateKeyContainer else colors.keyContainer
            background = shadowedKeyBackgroundDrawable(
                bkgColor, colors.shadow,
                radius, shadowWidth, hInset, vInset
            )
            foreground = if (rippled) {
                RippleDrawable(
                    ColorStateList.valueOf(colors.pressed), null,
                    insetRadiusDrawable(hInset, vInset, radius)
                )
            } else {
                StateListDrawable().apply {
                    addState(
                        intArrayOf(android.R.attr.state_pressed),
                        insetRadiusDrawable(hInset, vInset, radius, colors.pressed)
                    )
                }
            }
        } else {
            background = borderDrawable(lineWidth, colors.divider)
            foreground =
                if (rippled) rippleDrawable(colors.pressed)
                else pressHighlightDrawable(colors.pressed)
        }
    }

    val textView = textView {
        isClickable = false
        isFocusable = false
        background = null
        setTextColor(if (altStyle) colors.alternateKeyContent else colors.keyContent)
    }

    val imageView = imageView {
        isClickable = false
        isFocusable = false
        imageTintList = ColorStateList.valueOf(colors.alternateKeyContent)
    }

    fun setText(id: Int) {
        textView.setText(id)
        textView.applyNothingTextStyle(theme, NothingTextStyle.ControlLabel, sizeSp = null)
        removeView(imageView)
        add(textView, lParams(wrapContent, wrapContent, gravityCenter))
    }

    fun setIcon(@DrawableRes icon: Int) {
        imageView.imageResource = if (theme.isNothingStyle) {
            NothingGlyphRegistry.resolve(icon)
        } else icon
        removeView(textView)
        add(imageView, lParams(wrapContent, wrapContent, gravityCenter))
    }

    fun enableActivatedState() {
        textView.setTextColor(
            ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_activated),
                    intArrayOf(android.R.attr.state_enabled)
                ),
                intArrayOf(
                    colors.activeContent,
                    if (altStyle) colors.alternateKeyContent else colors.keyContent
                )
            )
        )
        imageView.imageTintList = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_activated),
                intArrayOf(android.R.attr.state_enabled)
            ),
            intArrayOf(
                colors.activeContent,
                colors.alternateKeyContent
            )
        )
        background = if (bordered) {
            StateListDrawable().apply {
                addState(
                    intArrayOf(android.R.attr.state_activated),
                    shadowedKeyBackgroundDrawable(
                        colors.activeContainer, colors.shadow,
                        radius, shadowWidth, hInset, vInset
                    )
                )
                addState(
                    intArrayOf(android.R.attr.state_enabled),
                    shadowedKeyBackgroundDrawable(
                        colors.keyContainer, colors.shadow,
                        radius, shadowWidth, hInset, vInset
                    )
                )
            }
        } else {
            StateListDrawable().apply {
                addState(
                    intArrayOf(android.R.attr.state_activated),
                    borderDrawable(
                        lineWidth,
                        colors.divider,
                        colors.activeContainer
                    )
                )
                addState(
                    intArrayOf(android.R.attr.state_enabled),
                    borderDrawable(lineWidth, colors.divider)
                )
            }
        }
    }
}
