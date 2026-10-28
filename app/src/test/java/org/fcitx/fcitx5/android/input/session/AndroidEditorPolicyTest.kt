/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.input.session

import android.text.InputType
import android.view.inputmethod.EditorInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidEditorPolicyTest {
    @Test
    fun `raw editor uses key events and rejects engine focus`() {
        val info = EditorInfo().apply { inputType = InputType.TYPE_NULL }
        assertTrue(AndroidEditorPolicy.isRawEditor(info))
        assertFalse(AndroidEditorPolicy.acceptsEngineFocus(info))
        assertEquals(EditorReturnBehavior.SendEnterKey, AndroidEditorPolicy.returnBehavior(info))
    }

    @Test
    fun `custom and standard editor actions are resolved centrally`() {
        val custom = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT
            actionLabel = "Publish"
            actionId = 42
        }
        assertEquals(EditorReturnBehavior.PerformAction(42), AndroidEditorPolicy.returnBehavior(custom))

        val search = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT
            imeOptions = EditorInfo.IME_ACTION_SEARCH
        }
        assertEquals(
            EditorReturnBehavior.PerformAction(EditorInfo.IME_ACTION_SEARCH),
            AndroidEditorPolicy.returnBehavior(search)
        )
    }

    @Test
    fun `uri editor arrows use key events`() {
        val info = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        }
        assertTrue(AndroidEditorPolicy.useKeyEventsForArrows(info))
    }
}
