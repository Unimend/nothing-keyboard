/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.navigation

import androidx.core.content.ContextCompat
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.input.FcitxInputMethodService
import org.fcitx.fcitx5.android.input.clipboard.ClipboardWindow
import org.fcitx.fcitx5.android.input.editing.TextEditingWindow
import org.fcitx.fcitx5.android.input.keyboard.KeyboardWindow
import org.fcitx.fcitx5.android.input.picker.PickerWindow
import org.fcitx.fcitx5.android.input.wm.InputWindowManager

/** The only user-navigation layer allowed to know concrete input-window classes. */
class InputNavigator(
    private val service: FcitxInputMethodService,
    private val windowManager: InputWindowManager
) {
    private var lastPickerType by AppPrefs.getInstance().internal.lastPickerType

    fun navigate(command: InputNavigationCommand) {
        when (command) {
            is InputNavigationCommand.ShowKeyboard -> showKeyboard(command.layout)
            InputNavigationCommand.ShowClipboard ->
                windowManager.attachWindow(ClipboardWindow())
            InputNavigationCommand.ShowEditing -> {
                service.finishComposing()
                windowManager.attachWindow(TextEditingWindow())
            }
            is InputNavigationCommand.ShowPicker -> {
                val key = command.key ?: rememberedPicker()
                command.key?.let { lastPickerType = it.name }
                windowManager.attachWindow(key)
            }
        }
    }

    private fun showKeyboard(layout: String?) {
        if (layout == null) {
            windowManager.attachWindow(KeyboardWindow)
            return
        }
        val keyboard = windowManager.getEssentialWindow(KeyboardWindow) as KeyboardWindow
        keyboard.switchLayout(layout)
        // switchLayout posts its layout mutation to the main executor. Queue attachment behind it.
        ContextCompat.getMainExecutor(service).execute {
            windowManager.attachWindow(KeyboardWindow)
        }
    }

    private fun rememberedPicker(): PickerWindow.Key =
        runCatching { PickerWindow.Key.valueOf(lastPickerType) }
            .getOrDefault(PickerWindow.Key.Emoji)
}
