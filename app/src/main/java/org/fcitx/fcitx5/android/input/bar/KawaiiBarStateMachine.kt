/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.bar

import org.fcitx.fcitx5.android.input.bar.KawaiiBarStateMachine.BooleanKey.ShowCandidates
import org.fcitx.fcitx5.android.input.bar.KawaiiBarStateMachine.State.Candidate
import org.fcitx.fcitx5.android.input.bar.KawaiiBarStateMachine.State.Idle
import org.fcitx.fcitx5.android.input.bar.KawaiiBarStateMachine.State.Title
import org.fcitx.fcitx5.android.utils.BuildTransitionEvent
import org.fcitx.fcitx5.android.utils.EventStateMachine
import org.fcitx.fcitx5.android.utils.TransitionBuildBlock

object KawaiiBarStateMachine {
    enum class State {
        Idle, Candidate, Title
    }

    enum class BooleanKey : EventStateMachine.BooleanStateKey {
        ShowCandidates
    }

    enum class TransitionEvent(val builder: TransitionBuildBlock<State, BooleanKey>) :
        EventStateMachine.TransitionEvent<State, BooleanKey> by BuildTransitionEvent(builder) {
        PresentationUpdated({
            from(Idle) transitTo Candidate on (ShowCandidates to true)
            from(Candidate) transitTo Idle on (ShowCandidates to false)
        }),
        ExtendedWindowAttached({
            from(Idle) transitTo Title
            from(Candidate) transitTo Title
        }),
        WindowDetached({
            // candidate state has higher priority so here it goes first
            from(Title) transitTo Candidate on (ShowCandidates to true)
            from(Title) transitTo Idle
        }),
    }

    fun new(block: (State) -> Unit) =
        EventStateMachine<State, TransitionEvent, BooleanKey>(
            initialState = Idle,
            externalBooleanStates = mutableMapOf(
                ShowCandidates to false
            )
        ).apply {
            onNewStateListener = block
        }

}

