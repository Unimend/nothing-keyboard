/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.engine

import org.fcitx.fcitx5.android.core.FormattedText
import org.fcitx.fcitx5.android.core.InputMethodEntry
import org.fcitx.fcitx5.android.core.InputMethodSubMode
import org.fcitx.fcitx5.android.core.TextFormatFlag
import org.fcitx.fcitx5.android.input.session.EngineInputMethod
import org.fcitx.fcitx5.android.input.session.EngineText

/** Converts project-owned models back to Fcitx objects only at the platform boundary. */
object FcitxPlatformAdapter {
    fun text(value: EngineText) = FormattedText(
        value.segments.map { it.text }.toTypedArray(),
        value.segments.map { segment ->
            segment.styles.fold(0) { flags, style -> flags or when (style) {
                org.fcitx.fcitx5.android.input.session.EngineTextStyle.Underline -> TextFormatFlag.Underline.flag
                org.fcitx.fcitx5.android.input.session.EngineTextStyle.Highlight -> TextFormatFlag.HighLight.flag
                org.fcitx.fcitx5.android.input.session.EngineTextStyle.DontCommit -> TextFormatFlag.DontCommit.flag
                org.fcitx.fcitx5.android.input.session.EngineTextStyle.Bold -> TextFormatFlag.Bold.flag
                org.fcitx.fcitx5.android.input.session.EngineTextStyle.Strike -> TextFormatFlag.Strike.flag
                org.fcitx.fcitx5.android.input.session.EngineTextStyle.Italic -> TextFormatFlag.Italic.flag
            } }
        }.toIntArray(),
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
