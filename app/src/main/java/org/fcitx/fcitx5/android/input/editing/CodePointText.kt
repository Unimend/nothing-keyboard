/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.input.editing

/** Iterates Unicode scalar values without requiring the API 24 String.codePoints() stream. */
internal inline fun String.forEachCodePointCompat(action: (Int) -> Unit) {
    var index = 0
    while (index < length) {
        val codePoint = Character.codePointAt(this, index)
        action(codePoint)
        index += Character.charCount(codePoint)
    }
}
