package org.fcitx.fcitx5.android.input.navigation

import org.fcitx.fcitx5.android.input.mode.KeyboardLayout
import org.fcitx.fcitx5.android.input.mode.PickerKind
import org.fcitx.fcitx5.android.input.session.InputSurface
import org.junit.Assert.assertEquals
import org.junit.Test

class ClipboardReturnPolicyTest {
    @Test
    fun `clipboard returns to exact originating surface`() {
        assertEquals(
            InputNavigationCommand.ShowKeyboard(KeyboardLayout.Number),
            ClipboardReturnPolicy.target(InputSurface.Keyboard, KeyboardLayout.Number)
        )
        assertEquals(
            InputNavigationCommand.ShowEditing,
            ClipboardReturnPolicy.target(InputSurface.Editing, KeyboardLayout.Text)
        )
        assertEquals(
            InputNavigationCommand.ShowPicker(PickerKind.Emoji),
            ClipboardReturnPolicy.target(InputSurface.Picker(PickerKind.Emoji.name), KeyboardLayout.Text)
        )
    }
}
