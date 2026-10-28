/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.dialog

import android.app.AlertDialog
import android.content.Context
import org.fcitx.fcitx5.android.input.engine.InputEngineQueries
import org.mechdancer.dependency.UniqueComponent

/** Coordinates engine and Android IME choices without exposing the input service to dialog UI. */
class InputMethodPickerController(
    private val queries: () -> InputEngineQueries,
    private val currentInputMethod: () -> String?,
    private val activateEngineInputMethod: (String) -> Unit,
    private val activatePlatformInputMethod: (String) -> Unit
) : UniqueComponent<InputMethodPickerController>() {
    suspend fun build(context: Context): AlertDialog {
        val entries = InputMethodData.resolve(queries(), context)
        return InputMethodPickerDialog.build(
            context = context,
            entries = entries,
            enabledInputMethod = currentInputMethod(),
            onSelected = { entry ->
                if (entry.ime) activatePlatformInputMethod(entry.uniqueName)
                else activateEngineInputMethod(entry.uniqueName)
            }
        )
    }
}
