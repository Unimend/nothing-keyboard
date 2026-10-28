/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.feedback

import org.fcitx.fcitx5.android.input.design.NothingDesignTokens

enum class InputMotionRole(val durationMs: Long) {
    InstantFeedback(NothingDesignTokens.Motion.InstantFeedbackMs),
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
