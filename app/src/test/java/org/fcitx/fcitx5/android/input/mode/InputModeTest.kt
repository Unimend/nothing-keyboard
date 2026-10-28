/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.mode

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InputModeTest {
    @Test
    fun persistedKeyboardLayoutsAreParsedWithoutUiTypes() {
        assertEquals(KeyboardLayout.Text, KeyboardLayout.fromStorage("Text"))
        assertEquals(KeyboardLayout.Number, KeyboardLayout.fromStorage("Number"))
        assertNull(KeyboardLayout.fromStorage("Symbol"))
    }

    @Test
    fun persistedPickerKindsAreParsedWithoutWindowTypes() {
        assertEquals(PickerKind.Symbol, PickerKind.fromStorage("Symbol"))
        assertEquals(PickerKind.Emoji, PickerKind.fromStorage("Emoji"))
        assertEquals(PickerKind.Emoticon, PickerKind.fromStorage("Emoticon"))
        assertNull(PickerKind.fromStorage("Number"))
    }
}
