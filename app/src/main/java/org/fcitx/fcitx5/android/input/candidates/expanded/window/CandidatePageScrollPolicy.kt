/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.input.candidates.expanded.window

import androidx.recyclerview.widget.RecyclerView

/** Keeps paging controls valid while Paging is refreshing or still has no laid-out items. */
internal object CandidatePageScrollPolicy {
    fun previous(firstVisible: Int, itemCount: Int): Int? {
        if (itemCount <= 0) return null
        if (firstVisible == RecyclerView.NO_POSITION) return 0
        return (firstVisible - 1).coerceIn(0, itemCount - 1)
    }

    fun next(lastVisible: Int, itemCount: Int): Int? {
        if (itemCount <= 0) return null
        if (lastVisible == RecyclerView.NO_POSITION) return 0
        return (lastVisible + 1).coerceIn(0, itemCount - 1)
    }
}
