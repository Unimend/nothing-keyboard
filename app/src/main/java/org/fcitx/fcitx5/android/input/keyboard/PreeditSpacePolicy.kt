/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.input.keyboard

import org.fcitx.fcitx5.android.input.session.EngineState

/** Decides whether Space should leave Chinese composition as raw Latin input. */
object PreeditSpacePolicy {
    fun rawTextOrNull(engine: EngineState): String? {
        if (engine.inputMethod?.languageCode?.startsWith("zh") != true) return null
        if (engine.clientPreedit.isEmpty && engine.inputPanel.preedit.isEmpty) return null
        return engine.inputPanel.preedit.plainText.takeIf(String::isNotBlank)
            ?: engine.clientPreedit.plainText.takeIf(String::isNotBlank)
    }
}
