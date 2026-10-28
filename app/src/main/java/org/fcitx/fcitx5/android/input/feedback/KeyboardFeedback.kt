/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.feedback

import android.view.View
import android.view.Gravity
import android.graphics.drawable.Drawable
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.InputFeedbacks
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.NothingDotMatrixDrawable
import org.fcitx.fcitx5.android.input.design.NothingDesignTokens
import org.fcitx.fcitx5.android.input.design.isNothingStyle
import splitties.dimensions.dp
import java.util.WeakHashMap

/** Shared visual and haptic language for gestures on every keyboard surface. */
object KeyboardFeedback {
    private val disableAnimation by AppPrefs.getInstance().advanced.disableAnimation

    private data class ActiveEffect(
        val drawable: Drawable,
        val step: Runnable,
        val removal: Runnable
    )

    private val activeEffects = WeakHashMap<View, ActiveEffect>()
    private val pendingActions = WeakHashMap<View, Runnable>()

    fun confirmation(view: View, theme: Theme) {
        InputFeedbacks.hapticFeedback(view, longPress = true)
        showNothingConfirmation(view, theme)
    }

    fun step(view: View, enabled: Boolean) {
        if (enabled) InputFeedbacks.hapticFeedback(view)
    }

    fun afterConfirmation(view: View, theme: Theme, action: () -> Unit) {
        confirmation(view, theme)
        pendingActions.remove(view)?.let(view::removeCallbacks)
        val delay = InputMotionPolicy.confirmationActionDelay(
            enabled = !disableAnimation,
            nothingStyle = theme.isNothingStyle
        )
        if (delay == 0L) {
            action()
            return
        }
        val command = Runnable {
            pendingActions.remove(view)
            if (view.isAttachedToWindow) action()
        }
        pendingActions[view] = command
        view.postDelayed(command, delay)
    }

    fun cancel(view: View) {
        pendingActions.remove(view)?.let(view::removeCallbacks)
        activeEffects.remove(view)?.let { effect ->
            view.removeCallbacks(effect.step)
            view.removeCallbacks(effect.removal)
            view.overlay.remove(effect.drawable)
        }
    }

    private fun showNothingConfirmation(view: View, theme: Theme) {
        if (disableAnimation || !theme.isNothingStyle || view.width <= 0 || view.height <= 0) return
        cancelVisual(view)
        val tokens = NothingDesignTokens.GestureFeedback
        val dots = NothingDotMatrixDrawable(
            color = NothingDesignTokens.Color.AccentRed,
            columns = tokens.Columns,
            rows = tokens.Rows,
            dotDiameter = view.dp(tokens.DotDiameterDp),
            dotGap = view.dp(tokens.DotGapDp),
            gravity = Gravity.LEFT or Gravity.CENTER_VERTICAL,
            horizontalPadding = view.dp(tokens.EdgePaddingDp),
            initialActiveDots = 0,
            inactiveAlpha = tokens.InactiveAlpha
        ).apply { setBounds(0, 0, view.width, view.height) }
        view.overlay.add(dots)
        var active = 0
        lateinit var step: Runnable
        val removal = Runnable { cancelVisual(view) }
        step = object : Runnable {
            override fun run() {
                if (!view.isAttachedToWindow) {
                    cancelVisual(view)
                    return
                }
                active += 1
                dots.setActiveDots(active)
                if (active < tokens.Columns * tokens.Rows) {
                    view.postDelayed(this, NothingDesignTokens.Motion.DotStepMs)
                } else {
                    view.postDelayed(removal, NothingDesignTokens.Motion.QuickFeedbackMs)
                }
            }
        }
        activeEffects[view] = ActiveEffect(dots, step, removal)
        view.post(step)
    }

    private fun cancelVisual(view: View) {
        activeEffects.remove(view)?.let { effect ->
            view.removeCallbacks(effect.step)
            view.removeCallbacks(effect.removal)
            view.overlay.remove(effect.drawable)
        }
    }
}
