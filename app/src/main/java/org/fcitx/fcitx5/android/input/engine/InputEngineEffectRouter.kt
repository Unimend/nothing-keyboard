/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.input.engine

interface InputEngineEffectHost {
    fun forwardKey(effect: EngineEffect.ForwardKey)
    fun inputMethodChanged(effect: EngineEffect.InputMethodChanged)
    fun inputMethodSwitched(effect: EngineEffect.InputMethodSwitched)
}

/** Routes project-owned engine effects; editor effects always receive first refusal. */
class InputEngineEffectRouter(
    private val handleEditorEffect: (EngineEffect) -> Boolean,
    private val host: InputEngineEffectHost
) {
    fun handle(effect: EngineEffect) {
        if (handleEditorEffect(effect)) return
        when (effect) {
            is EngineEffect.ForwardKey -> host.forwardKey(effect)
            is EngineEffect.InputMethodChanged -> host.inputMethodChanged(effect)
            is EngineEffect.InputMethodSwitched -> host.inputMethodSwitched(effect)
            else -> Unit
        }
    }
}
