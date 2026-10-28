/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.input.wm

/** Decides whether Android must rebuild IME state for a configuration diff. */
object ImeConfigurationPolicy {
    fun shouldDelegateToSystem(diff: Int, locallyHandledFlags: Int): Boolean =
        diff and locallyHandledFlags != diff
}
