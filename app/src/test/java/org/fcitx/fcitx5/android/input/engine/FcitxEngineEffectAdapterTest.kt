/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.engine

import org.fcitx.fcitx5.android.core.FcitxEvent
import org.fcitx.fcitx5.android.core.FcitxKeyMapping
import org.fcitx.fcitx5.android.core.KeyState
import org.fcitx.fcitx5.android.core.KeyStates
import org.fcitx.fcitx5.android.core.KeySym
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FcitxEngineEffectAdapterTest {
    @Test
    fun commitEventBecomesEngineOwnedEffect() {
        val effect = FcitxEngineEffectAdapter.mapRelevantOrNull(
            FcitxEvent.CommitStringEvent(FcitxEvent.CommitStringEvent.Data("你好", 2))
        )

        assertEquals(EngineEffect.CommitText("你好", 2), effect)
    }

    @Test
    fun virtualBackspaceIsSemanticInsteadOfRawFcitxSymbol() {
        val effect = FcitxEngineEffectAdapter.mapRelevantOrNull(
            FcitxEvent.KeyEvent(
                FcitxEvent.KeyEvent.Data(
                    KeySym(FcitxKeyMapping.FcitxKey_BackSpace),
                    KeyStates(KeyState.Virtual), 0, false, 42
                )
            )
        ) as EngineEffect.VirtualKey

        assertEquals(EngineVirtualKey.Backspace, effect.key)
        assertEquals(FcitxKeyMapping.FcitxKey_BackSpace, effect.rawSymbol)
    }

    @Test
    fun stateOnlyCandidateEventProducesNoAndroidEffect() {
        assertNull(
            FcitxEngineEffectAdapter.mapRelevantOrNull(
                FcitxEvent.CandidateListEvent(FcitxEvent.CandidateListEvent.Data())
            )
        )
    }
}
