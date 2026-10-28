/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.platform

import android.app.Dialog
import android.os.IBinder
import android.view.WindowManager
import org.mechdancer.dependency.UniqueComponent

/** Owns the one dialog that may be attached to an IME window at a time. */
class ImeDialogController(
    private val windowToken: () -> IBinder?,
    private val dimAmount: () -> Float
) : UniqueComponent<ImeDialogController>() {
    private var showing: Dialog? = null

    fun show(dialog: Dialog) {
        showing?.dismiss()
        dialog.window?.also { window ->
            window.attributes = window.attributes.apply {
                token = windowToken()
                type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG
            }
            window.addFlags(
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM or
                    WindowManager.LayoutParams.FLAG_DIM_BEHIND
            )
            window.setDimAmount(dimAmount())
        }
        dialog.setOnDismissListener {
            if (showing === dialog) showing = null
        }
        dialog.show()
        showing = dialog
    }

    fun dismiss() {
        val dialog = showing ?: return
        showing = null
        dialog.dismiss()
    }
}
