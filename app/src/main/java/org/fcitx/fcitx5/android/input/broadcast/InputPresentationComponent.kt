/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.broadcast

import org.fcitx.fcitx5.android.input.session.EngineState
import org.fcitx.fcitx5.android.input.session.InputPresentationReducer
import org.fcitx.fcitx5.android.input.session.InputPresentationState
import org.mechdancer.dependency.Dependent
import org.mechdancer.dependency.UniqueComponent
import org.mechdancer.dependency.manager.ManagedHandler
import org.mechdancer.dependency.manager.managedHandler
import org.mechdancer.dependency.manager.must

class InputPresentationComponent :
    UniqueComponent<InputPresentationComponent>(), Dependent, ManagedHandler by managedHandler() {

    private val broadcaster: InputBroadcaster by manager.must()
    private val returnKeyDrawable: ReturnKeyDrawableComponent by manager.must()

    var state: InputPresentationState = InputPresentationState()
        private set

    fun update(engine: EngineState) {
        val next = InputPresentationReducer.reduce(engine)
        if (state == next) return
        state = next
        broadcaster.onInputPresentationUpdate(state)
        returnKeyDrawable.updateDrawableOnPreedit(state.preeditEmpty)
    }

}
