package org.fcitx.fcitx5.android.input.session

import android.text.InputType
import android.view.inputmethod.EditorInfo
import org.junit.Assert.*
import org.junit.Test

class EditorCapabilitiesTest {
    @Test
    fun `password detection covers text web and numeric editors`() {
        listOf(
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        ).forEach { type ->
            assertTrue(EditorCapability.Password in AndroidEditorCapabilities.fromAndroid(type, 0))
        }
        assertFalse(EditorCapability.Password in AndroidEditorCapabilities.fromAndroid(InputType.TYPE_CLASS_NUMBER, 0))
    }

    @Test
    fun `private ascii and visible-password requirements stay semantic`() {
        val capabilities = AndroidEditorCapabilities.fromAndroid(
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            EditorInfo.IME_FLAG_FORCE_ASCII or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
        )
        assertTrue(capabilities.containsAll(setOf(EditorCapability.Private, EditorCapability.Ascii,
            EditorCapability.NoSuggestions, EditorCapability.Composition)))
    }

    @Test
    fun `raw editors disable composition but retain cursor-aware commits`() {
        val capabilities = AndroidEditorCapabilities.fromAndroid(InputType.TYPE_NULL, 0)
        assertEquals(setOf(EditorCapability.NoSuggestions, EditorCapability.CommitOnBlur,
            EditorCapability.CursorAwareCommit), capabilities)
    }

    @Test
    fun `multiline and capitalization rules coexist`() {
        val capabilities = AndroidEditorCapabilities.fromAndroid(InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_WORDS or
            InputType.TYPE_TEXT_FLAG_AUTO_CORRECT, 0)
        assertTrue(capabilities.containsAll(setOf(EditorCapability.Multiline,
            EditorCapability.CapitalWords, EditorCapability.Spelling)))
    }
}
