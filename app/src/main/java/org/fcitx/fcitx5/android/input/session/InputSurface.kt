/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.session

sealed interface InputSurface {
    data object None : InputSurface
    data object Keyboard : InputSurface
    data object ExpandedCandidates : InputSurface
    data object Editing : InputSurface
    data object Clipboard : InputSurface
    data class Picker(val kind: String) : InputSurface
    data object Status : InputSurface
    data object EditorInfo : InputSurface
    data object Auxiliary : InputSurface
}
