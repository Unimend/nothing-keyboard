/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.feedback

import org.fcitx.fcitx5.android.input.design.NothingDesignTokens

/** Pure motion decisions shared by visual feedback and delayed gesture commands. */
object InputMotionPolicy {
    fun duration(enabled: Boolean, durationMs: Long): Long = if (enabled) durationMs else 0L

    fun confirmationActionDelay(enabled: Boolean, nothingStyle: Boolean): Long = duration(
        enabled && nothingStyle,
        NothingDesignTokens.Motion.ConfirmationActionDelayMs
    )
}
