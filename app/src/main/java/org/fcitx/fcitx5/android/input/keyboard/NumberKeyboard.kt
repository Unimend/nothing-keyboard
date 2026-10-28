/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.keyboard

import org.fcitx.fcitx5.android.input.engine.EngineKey
import org.fcitx.fcitx5.android.input.engine.EngineKey.KeypadOperator.Operator

import android.annotation.SuppressLint
import android.content.Context
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.mode.KeyboardLayout
import org.fcitx.fcitx5.android.input.mode.PickerKind
import org.fcitx.fcitx5.android.input.mode.InputMode
import org.fcitx.fcitx5.android.input.popup.PopupAction
import org.fcitx.fcitx5.android.input.popup.PopupResult
import splitties.views.imageResource

@SuppressLint("ViewConstructor")
class NumberKeyboard(
    context: Context,
    theme: Theme,
) : BaseKeyboard(context, theme, Layout) {

    companion object {
        const val Name = "Number"

        val Layout: List<List<KeyDef>> = listOf(
            listOf(
                NumPadKey("+", EngineKey.KeypadOperator(Operator.Add), 23f, 0.15f, KeyDef.Appearance.Variant.Alternative),
                NumPadKey("1", EngineKey.KeypadDigit(1), 30f, 0f),
                NumPadKey("2", EngineKey.KeypadDigit(2), 30f, 0f),
                NumPadKey("3", EngineKey.KeypadDigit(3), 30f, 0f),
                NumPadKey("/", EngineKey.KeypadOperator(Operator.Divide), 23f, 0.15f, KeyDef.Appearance.Variant.Alternative),
            ),
            listOf(
                NumPadKey("-", EngineKey.KeypadOperator(Operator.Subtract), 23f, 0.15f, KeyDef.Appearance.Variant.Alternative),
                NumPadKey("4", EngineKey.KeypadDigit(4), 30f, 0f),
                NumPadKey("5", EngineKey.KeypadDigit(5), 30f, 0f),
                NumPadKey("6", EngineKey.KeypadDigit(6), 30f, 0f),
                MiniSpaceKey()
            ),
            listOf(
                NumPadKey("*", EngineKey.KeypadOperator(Operator.Multiply), 23f, 0.15f, KeyDef.Appearance.Variant.Alternative),
                NumPadKey("7", EngineKey.KeypadDigit(7), 30f, 0f),
                NumPadKey("8", EngineKey.KeypadDigit(8), 30f, 0f),
                NumPadKey("9", EngineKey.KeypadDigit(9), 30f, 0f),
                BackspaceKey()
            ),
            listOf(
                LayoutSwitchKey("ABC", InputMode.Keyboard(KeyboardLayout.Text)),
                NumPadKey(",", EngineKey.KeypadOperator(Operator.Separator), 23f, 0.1f, KeyDef.Appearance.Variant.Alternative),
                LayoutSwitchKey(
                    "!?#",
                    InputMode.Picker(PickerKind.Symbol),
                    0.13333f,
                    KeyDef.Appearance.Variant.AltForeground
                ),
                NumPadKey("0", EngineKey.KeypadDigit(0), 30f, 0.23334f),
                NumPadKey("=", EngineKey.KeypadOperator(Operator.Equal), 23f, 0.13333f, KeyDef.Appearance.Variant.AltForeground),
                NumPadKey(".", EngineKey.KeypadOperator(Operator.Decimal), 23f, 0.1f, KeyDef.Appearance.Variant.Alternative),
                ReturnKey()
            )
        )
    }

    val backspace: ImageKeyView by lazy { findViewById(R.id.button_backspace) }
    val space: TextKeyView by lazy { findViewById(R.id.button_mini_space) }
    val `return`: ImageKeyView by lazy { findViewById(R.id.button_return) }

    override fun onReturnDrawableUpdate(returnDrawable: Int) {
        `return`.img.imageResource = returnDrawable
    }

    @SuppressLint("MissingSuperCall")
    override fun onPopupAction(action: PopupAction): PopupResult {
        // NumberKeyboard deliberately has no popup surface.
        return PopupResult.None
    }

}
