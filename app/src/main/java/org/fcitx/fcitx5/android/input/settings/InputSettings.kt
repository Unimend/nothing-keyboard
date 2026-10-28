/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.input.settings

import org.fcitx.fcitx5.android.data.prefs.AppPrefs

/**
 * Composition boundary between input systems and persisted application preferences.
 *
 * Input code names the semantic category it consumes. Creation and location of the persisted
 * preference graph stay here, so storage can later change without another project-wide lookup.
 */
object InputSettings {
    private val persisted by lazy(AppPrefs::getInstance)

    val keyboard get() = persisted.keyboard
    val candidates get() = persisted.candidates
    val clipboard get() = persisted.clipboard
    val symbols get() = persisted.symbols
    val advanced get() = persisted.advanced
    val internal get() = persisted.internal
}
