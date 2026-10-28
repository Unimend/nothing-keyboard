/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.feedback

import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.input.design.NothingDesignTokens
import org.fcitx.fcitx5.android.input.design.isNothingStyle
import org.fcitx.fcitx5.android.data.theme.Theme

enum class InputMotionRole(val durationMs: Long) {
    InstantFeedback(NothingDesignTokens.Motion.InstantFeedbackMs),
    DotStep(NothingDesignTokens.Motion.DotStepMs),
    QuickFeedback(NothingDesignTokens.Motion.QuickFeedbackMs),
    WindowTransition(NothingDesignTokens.Motion.WindowTransitionMs),
    CapsStateChange(NothingDesignTokens.Motion.CapsFeedbackMs),
    DestructiveConfirmation(NothingDesignTokens.Motion.ConfirmationActionDelayMs)
}

/** Pure motion decisions shared by visual feedback and delayed gesture commands. */
object InputMotionPolicy {
    fun duration(enabled: Boolean, durationMs: Long): Long = if (enabled) durationMs else 0L

    fun duration(enabled: Boolean, role: InputMotionRole): Long =
        duration(enabled, role.durationMs)

    fun confirmationActionDelay(enabled: Boolean, nothingStyle: Boolean): Long = duration(
        enabled && nothingStyle,
        InputMotionRole.DestructiveConfirmation.durationMs
    )
}

/** The single runtime gateway for animation preference and semantic motion timing. */
object InputMotionSystem {
    private val disableAnimation by AppPrefs.getInstance().advanced.disableAnimation

    val enabled: Boolean get() = !disableAnimation

    fun duration(role: InputMotionRole): Long = InputMotionPolicy.duration(enabled, role)

    fun confirmationActionDelay(theme: Theme): Long =
        InputMotionPolicy.confirmationActionDelay(enabled, theme.isNothingStyle)
}
