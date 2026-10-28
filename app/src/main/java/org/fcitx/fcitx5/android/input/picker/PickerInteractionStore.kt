/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.picker

import org.fcitx.fcitx5.android.input.mode.PickerKind
import org.mechdancer.dependency.UniqueComponent

class PickerInteractionStore : UniqueComponent<PickerInteractionStore>() {
    var state = PickerInteractionState()
        private set

    fun selectPage(kind: PickerKind, categoryIndex: Int, pageInCategory: Int) {
        state = PickerInteractionReducer.reduce(
            state,
            PickerInteractionEvent.PageSelected(kind, categoryIndex, pageInCategory)
        )
    }
}
