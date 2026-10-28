/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.clipboard

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.text.TextUtils
import android.view.View
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.NothingDotMatrixDrawable
import org.fcitx.fcitx5.android.input.design.NothingTextStyle
import org.fcitx.fcitx5.android.input.design.applyNothingTextStyle
import org.fcitx.fcitx5.android.input.design.setNothingText
import org.fcitx.fcitx5.android.input.design.NothingDesignTokens
import org.fcitx.fcitx5.android.input.design.inputSurfaceStyle
import org.fcitx.fcitx5.android.input.design.toInputColorScheme
import org.fcitx.fcitx5.android.input.keyboard.CustomGestureView
import splitties.dimensions.dp
import splitties.resources.drawable
import splitties.views.dsl.constraintlayout.bottomOfParent
import splitties.views.dsl.constraintlayout.centerVertically
import splitties.views.dsl.constraintlayout.constraintLayout
import splitties.views.dsl.constraintlayout.endOfParent
import splitties.views.dsl.constraintlayout.leftOfParent
import splitties.views.dsl.constraintlayout.lParams
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.add
import splitties.views.dsl.core.imageView
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.textView
import splitties.views.dsl.core.wrapContent
import splitties.views.imageDrawable
import splitties.views.setPaddingDp
import org.fcitx.fcitx5.android.input.design.NothingGlyphRegistry

class ClipboardEntryUi(override val ctx: Context, private val theme: Theme, radius: Float) : Ui {

    private val visualStyle = theme.inputSurfaceStyle.clipboard
    private val colors = theme.toInputColorScheme()
    private val resolvedRadius = if (visualStyle.entryCornerRadiusDp.isNaN()) {
        radius
    } else {
        ctx.dp(visualStyle.entryCornerRadiusDp)
    }

    val textView = textView {
        minLines = 1
        maxLines = 4
        textSize = visualStyle.entryTextSizeSp ?: 14f
        setPaddingDp(
            visualStyle.entryStartPaddingDp,
            8, 10, 8
        )
        ellipsize = TextUtils.TruncateAt.END
        setTextColor(colors.keyContent)
        applyNothingTextStyle(theme, NothingTextStyle.ClipboardEntry, sizeSp = null)
    }

    val pin = imageView {
        imageDrawable = drawable(NothingGlyphRegistry.resolveOrKeepOriginal(R.drawable.ic_baseline_push_pin_24, theme))!!.apply {
            setTint(colors.alternateKeyContent)
            setAlpha(0.3f)
        }
    }

    val layout = constraintLayout {
        if (visualStyle.showDotMarker) {
            add(View(ctx).apply {
                background = NothingDotMatrixDrawable(
                    color = colors.accentContainer,
                    columns = 1,
                    rows = NothingDesignTokens.Clipboard.MarkerRows,
                    dotDiameter = dp(NothingDesignTokens.Clipboard.MarkerDotDiameterDp).toFloat(),
                    dotGap = dp(NothingDesignTokens.Clipboard.MarkerDotGapDp).toFloat()
                )
            }, lParams(
                dp(NothingDesignTokens.Clipboard.MarkerWidthDp),
                dp(NothingDesignTokens.Clipboard.MarkerHeightDp)
            ) {
                centerVertically()
                leftOfParent(dp(8))
            })
        }
        add(textView, lParams(matchParent, wrapContent) {
            centerVertically()
        })
        add(pin, lParams(dp(12), dp(12)) {
            bottomOfParent(dp(2))
            endOfParent(dp(2))
        })
    }

    override val root = CustomGestureView(ctx).apply {
        isClickable = true
        minimumHeight = dp(
            visualStyle.entryMinimumHeightDp
        )
        foreground = RippleDrawable(
            ColorStateList.valueOf(colors.pressed), null,
            GradientDrawable().apply {
                cornerRadius = resolvedRadius
                setColor(Color.WHITE)
            }
        )
        background = GradientDrawable().apply {
            cornerRadius = resolvedRadius
            setColor(
                this@ClipboardEntryUi.colors.clipboardContainer
            )
        }
        add(layout, lParams(matchParent, matchParent))
    }

    fun setEntry(text: String, pinned: Boolean) {
        textView.setNothingText(text, theme, NothingTextStyle.ClipboardEntry, sizeSp = null)
        pin.visibility = if (pinned) View.VISIBLE else View.GONE
    }
}
