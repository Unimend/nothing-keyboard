/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.feedback

import android.view.View
import android.view.Gravity
import org.fcitx.fcitx5.android.data.InputFeedbacks
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.NothingDotMatrixDrawable
import org.fcitx.fcitx5.android.input.design.NothingDesignTokens
import org.fcitx.fcitx5.android.input.design.isNothingStyle
import splitties.dimensions.dp

/** Shared visual and haptic language for gestures on every keyboard surface. */
object KeyboardFeedback {
    fun confirmation(view: View, theme: Theme) {
        InputFeedbacks.hapticFeedback(view, longPress = true)
        showNothingConfirmation(view, theme)
    }

    fun step(view: View, enabled: Boolean) {
        if (enabled) InputFeedbacks.hapticFeedback(view)
    }

    private fun showNothingConfirmation(view: View, theme: Theme) {
        if (!theme.isNothingStyle || view.width <= 0 || view.height <= 0) return
        val tokens = NothingDesignTokens.GestureFeedback
        val dots = NothingDotMatrixDrawable(
            color = NothingDesignTokens.Color.AccentRed,
            columns = tokens.Columns,
            rows = tokens.Rows,
            dotDiameter = view.dp(tokens.DotDiameterDp).toFloat(),
            dotGap = view.dp(tokens.DotGapDp).toFloat(),
            gravity = Gravity.LEFT or Gravity.CENTER_VERTICAL,
            horizontalPadding = view.dp(tokens.EdgePaddingDp).toFloat(),
            initialActiveDots = 0,
            inactiveAlpha = tokens.InactiveAlpha
        ).apply { setBounds(0, 0, view.width, view.height) }
        view.overlay.add(dots)
        var active = 0
        val step = object : Runnable {
            override fun run() {
                active += 1
                dots.setActiveDots(active)
                if (active < tokens.Columns * tokens.Rows) {
                    view.postDelayed(this, NothingDesignTokens.Motion.DotStepMs)
                } else {
                    view.postDelayed(
                        { view.overlay.remove(dots) },
                        NothingDesignTokens.Motion.QuickFeedbackMs
                    )
                }
            }
        }
        view.post(step)
    }
}
