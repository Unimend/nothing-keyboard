/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.session

import org.fcitx.fcitx5.android.core.FormattedText
import org.fcitx.fcitx5.android.core.InputMethodEntry
import org.fcitx.fcitx5.android.core.InputMethodSubMode

/** Converts project-owned state only at Android/Fcitx platform boundaries. */
object LegacyEngineStateAdapter {
    fun text(value: EngineText) = FormattedText(
        value.segments.map { it.text }.toTypedArray(),
        value.segments.map { it.formatFlags }.toIntArray(),
        value.cursor
    )

    fun inputMethod(value: EngineInputMethod) = InputMethodEntry(
        value.uniqueName,
        value.name,
        value.icon,
        value.nativeName,
        value.label,
        value.languageCode,
        value.addon,
        value.configurable,
        InputMethodSubMode(value.subModeName, value.subModeLabel, value.subModeIcon)
    )

}
