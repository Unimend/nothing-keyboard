/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.session

import android.os.Build
import android.view.inputmethod.EditorInfo
import android.text.InputType
import splitties.bitflags.hasFlag

fun EditorInfo.toEditorSnapshot() = EditorSnapshot(
    packageName = packageName.orEmpty(),
    inputType = inputType,
    imeOptions = imeOptions,
    privateImeOptions = privateImeOptions,
    initialSelection = SessionRange(initialSelStart, initialSelEnd),
    inputKind = when (inputType and InputType.TYPE_MASK_CLASS) {
        InputType.TYPE_CLASS_TEXT -> EditorInputKind.Text
        InputType.TYPE_CLASS_NUMBER -> EditorInputKind.Number
        InputType.TYPE_CLASS_PHONE -> EditorInputKind.Phone
        else -> EditorInputKind.Other
    },
    action = if (imeOptions.hasFlag(EditorInfo.IME_FLAG_NO_ENTER_ACTION)) {
        EditorAction.Return
    } else when (imeOptions and EditorInfo.IME_MASK_ACTION) {
        EditorInfo.IME_ACTION_GO -> EditorAction.Go
        EditorInfo.IME_ACTION_SEARCH -> EditorAction.Search
        EditorInfo.IME_ACTION_SEND -> EditorAction.Send
        EditorInfo.IME_ACTION_NEXT -> EditorAction.Next
        EditorInfo.IME_ACTION_DONE -> EditorAction.Done
        EditorInfo.IME_ACTION_PREVIOUS -> EditorAction.Previous
        else -> EditorAction.Return
    },
    privateMode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        imeOptions.hasFlag(EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING)
)
