/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2024-2026 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.input.candidates

import androidx.recyclerview.widget.RecyclerView
import org.fcitx.fcitx5.android.input.session.EngineCandidate

class CandidateViewHolder(val ui: CandidateItemUi) : RecyclerView.ViewHolder(ui.root) {
    var idx = -1
        private set

    var candidate: EngineCandidate = EngineCandidate.Empty
        private set

    fun update(newIndex: Int, newCandidate: EngineCandidate) {
        idx = newIndex
        if (candidate != newCandidate) {
            candidate = newCandidate
            ui.updateCandidate(newIndex, newCandidate)
        } else {
            ui.updateCandidate(newIndex, newCandidate)
        }
    }

    fun clear() {
        idx = -1
        candidate = EngineCandidate.Empty
        ui.release()
    }
}
