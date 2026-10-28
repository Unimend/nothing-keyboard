package org.fcitx.fcitx5.android.input.popup

import org.junit.Assert.*
import org.junit.Test

class PopupContractTest {
    @Test
    fun `focus result is returned without mutating the request`() {
        val request = PopupAction.ChangeFocusAction(7, 1f, 2f)
        val listener = PopupActionListener { PopupResult.FocusChanged(true) }
        assertEquals(PopupResult.FocusChanged(true), listener.onPopupAction(request))
        assertEquals(PopupAction.ChangeFocusAction(7, 1f, 2f), request)
    }

    @Test
    fun `empty trigger is explicit`() {
        val listener = PopupActionListener { PopupResult.Triggered(null) }
        assertNull((listener.onPopupAction(PopupAction.TriggerAction(7)) as PopupResult.Triggered).action)
    }
}
