/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.session

import android.view.inputmethod.EditorInfo

fun EditorInfo.toEditorSnapshot() = EditorSnapshot(
    packageName = packageName.orEmpty(),
    inputType = inputType,
    imeOptions = imeOptions,
    privateImeOptions = privateImeOptions,
    initialSelection = SessionRange(initialSelStart, initialSelEnd)
)
