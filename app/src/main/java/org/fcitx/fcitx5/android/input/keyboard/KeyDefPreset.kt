/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.keyboard

import android.graphics.Typeface
import androidx.annotation.DrawableRes
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.InputFeedbacks
import org.fcitx.fcitx5.android.input.engine.EngineKey
import org.fcitx.fcitx5.android.input.engine.VirtualNumLockModifiers
import org.fcitx.fcitx5.android.input.keyboard.KeyDef.Appearance.Border
import org.fcitx.fcitx5.android.input.keyboard.KeyDef.Appearance.Variant
import org.fcitx.fcitx5.android.input.mode.KeyboardLayout
import org.fcitx.fcitx5.android.input.mode.PickerKind
import org.fcitx.fcitx5.android.input.mode.InputMode

class SymbolKey(
    val symbol: String,
    percentWidth: Float = 0.1f,
    variant: Variant = Variant.Normal,
    popup: Array<Popup>? = null
) : KeyDef(
    Appearance.Text(
        displayText = symbol,
        textSize = 23f,
        percentWidth = percentWidth,
        variant = variant
    ),
    setOf(
        Behavior.Press(KeyAction.TextKeyAction(symbol))
    ),
    popup ?: arrayOf(
        Popup.Preview(symbol),
        Popup.Keyboard.Preset(symbol)
    )
)

class AlphabetKey(
    val character: String,
    val punctuation: String,
    variant: Variant = Variant.Normal,
    popup: Array<Popup>? = null
) : KeyDef(
    Appearance.AltText(
        displayText = character,
        altText = punctuation,
        textSize = 23f,
        variant = variant
    ),
    setOf(
        Behavior.Press(KeyAction.TextKeyAction(character)),
        Behavior.Swipe(KeyAction.TextKeyAction(punctuation))
    ),
    popup ?: arrayOf(
        Popup.AltPreview(character, punctuation),
        Popup.Keyboard.Preset(character)
    )
)

class AlphabetDigitKey(
    val character: String,
    altText: String,
    val digit: Int,
    popup: Array<Popup>? = null
) : KeyDef(
    Appearance.AltText(
        displayText = character,
        altText = altText,
        textSize = 23f
    ),
    setOf(
        Behavior.Press(KeyAction.TextKeyAction(character)),
        Behavior.Swipe(KeyAction.SymAction(EngineKey.KeypadDigit(digit), VirtualNumLockModifiers))
    ),
    popup ?: arrayOf(
        Popup.AltPreview(character, altText),
        Popup.Keyboard.Preset(character)
    )
) {
    constructor(
        char: String,
        digit: Int,
        popup: Array<Popup>? = null
    ) : this(
        char,
        digit.toString(),
        digit,
        popup
    )
}

class CapsKey : KeyDef(
    Appearance.Image(
        src = R.drawable.ic_capslock_none,
        viewId = R.id.button_caps,
        percentWidth = 0.15f,
        variant = Variant.Alternative
    ),
    setOf(
        Behavior.Press(KeyAction.CapsAction(false)),
        Behavior.LongPress(KeyAction.CapsAction(true)),
        Behavior.DoubleTap(KeyAction.CapsAction(true))
    )
)

class LayoutSwitchKey(
    displayText: String,
    val to: InputMode? = null,
    percentWidth: Float = 0.15f,
    variant: Variant = Variant.Alternative
) : KeyDef(
    Appearance.Text(
        displayText,
        textSize = 16f,
        textStyle = Typeface.BOLD,
        percentWidth = percentWidth,
        variant = variant
    ),
    setOf(
        Behavior.Press(KeyAction.LayoutSwitchAction(to))
    )
)

class BackspaceKey(
    percentWidth: Float = 0.15f,
    variant: Variant = Variant.Alternative
) : KeyDef(
    Appearance.Image(
        src = R.drawable.ic_baseline_backspace_24,
        percentWidth = percentWidth,
        variant = variant,
        viewId = R.id.button_backspace,
        soundEffect = InputFeedbacks.SoundEffect.Delete
    ),
    setOf(
        Behavior.Press(KeyAction.SymAction(EngineKey.Backspace)),
        Behavior.Repeat(KeyAction.SymAction(EngineKey.Backspace))
    )
)

class QuickPhraseKey : KeyDef(
    Appearance.Image(
        src = R.drawable.ic_baseline_format_quote_24,
        variant = Variant.Alternative,
        viewId = R.id.button_quickphrase
    ),
    setOf(
        Behavior.Press(KeyAction.QuickPhraseAction),
        Behavior.LongPress(KeyAction.UnicodeAction)
    )
)

class CommaKey(
    percentWidth: Float,
    variant: Variant,
) : KeyDef(
    Appearance.ImageText(
        displayText = ",",
        textSize = 23f,
        percentWidth = percentWidth,
        variant = variant,
        src = R.drawable.ic_baseline_tag_faces_24
    ),
    setOf(
        Behavior.Press(KeyAction.TextKeyAction(","))
    ),
    arrayOf(
        Popup.Preview(","),
        Popup.Menu(
            arrayOf(
                Popup.Menu.Item(
                    "Emoji",
                    R.drawable.ic_baseline_tag_faces_24,
                    KeyAction.PickerSwitchAction(PickerKind.Emoji)
                ),
                Popup.Menu.Item(
                    "QuickPhrase",
                    R.drawable.ic_baseline_format_quote_24,
                    KeyAction.QuickPhraseAction
                ),
                Popup.Menu.Item(
                    "Unicode",
                    R.drawable.ic_logo_unicode,
                    KeyAction.UnicodeAction
                )
            )
        )
    )
)

class LanguageKey : KeyDef(
    Appearance.Image(
        src = R.drawable.ic_baseline_language_24,
        variant = Variant.AltForeground,
        viewId = R.id.button_lang
    ),
    setOf(
        Behavior.Press(KeyAction.LangSwitchAction),
        Behavior.LongPress(KeyAction.ShowInputMethodPickerAction)
    )
)

class SpaceKey : KeyDef(
    Appearance.Text(
        displayText = " ",
        textSize = 13f,
        percentWidth = 0f,
        border = Border.Special,
        viewId = R.id.button_space,
        soundEffect = InputFeedbacks.SoundEffect.SpaceBar
    ),
    setOf(
        Behavior.Press(KeyAction.SymAction(EngineKey.Space)),
        Behavior.LongPress(KeyAction.SpaceLongPressAction)
    )
)

class ReturnKey(percentWidth: Float = 0.15f) : KeyDef(
    Appearance.Image(
        src = R.drawable.ic_baseline_keyboard_return_24,
        percentWidth = percentWidth,
        variant = Variant.Accent,
        border = Border.Special,
        viewId = R.id.button_return,
        soundEffect = InputFeedbacks.SoundEffect.Return
    ),
    setOf(
        Behavior.Press(KeyAction.SymAction(EngineKey.Return))
    ),
    arrayOf(
        Popup.Menu(
            arrayOf(
                Popup.Menu.Item(
                    "Emoji",
                    R.drawable.ic_baseline_tag_faces_24,
                    KeyAction.PickerSwitchAction(PickerKind.Emoji)
                )
            )
        )
    ),
)

class ImageLayoutSwitchKey(
    @DrawableRes
    icon: Int,
    to: KeyboardLayout,
    percentWidth: Float = 0.1f,
    variant: Variant = Variant.AltForeground,
    viewId: Int = -1
) : KeyDef(
    Appearance.Image(
        src = icon,
        percentWidth = percentWidth,
        variant = variant,
        viewId = viewId
    ),
    setOf(
        Behavior.Press(KeyAction.LayoutSwitchAction(InputMode.Keyboard(to)))
    )
)

class ImageActionKey(
    @DrawableRes
    icon: Int,
    action: KeyAction,
    percentWidth: Float = 0.1f,
    variant: Variant = Variant.AltForeground,
    viewId: Int = -1
) : KeyDef(
    Appearance.Image(
        src = icon,
        percentWidth = percentWidth,
        variant = variant,
        viewId = viewId
    ),
    setOf(Behavior.Press(action))
)

class ImagePickerSwitchKey(
    @DrawableRes
    icon: Int,
    to: PickerKind,
    percentWidth: Float = 0.1f,
    variant: Variant = Variant.AltForeground,
    viewId: Int = -1
) : KeyDef(
    Appearance.Image(
        src = icon,
        percentWidth = percentWidth,
        variant = variant,
        viewId = viewId
    ),
    setOf(
        Behavior.Press(KeyAction.PickerSwitchAction(to))
    )
)

class TextPickerSwitchKey(
    text: String,
    to: PickerKind,
    percentWidth: Float = 0.1f,
    variant: Variant = Variant.AltForeground,
    viewId: Int = -1
) : KeyDef(
    Appearance.Text(
        displayText = text,
        textSize = 16f,
        percentWidth = percentWidth,
        variant = variant,
        viewId = viewId,
        textStyle = Typeface.BOLD
    ),
    setOf(
        Behavior.Press(KeyAction.PickerSwitchAction(to))
    )
)

class MiniSpaceKey : KeyDef(
    Appearance.Image(
        src = R.drawable.ic_baseline_space_bar_24,
        percentWidth = 0.15f,
        variant = Variant.Alternative,
        viewId = R.id.button_mini_space
    ),
    setOf(
        Behavior.Press(KeyAction.SymAction(EngineKey.Space))
    )
)

class NumPadKey(
    displayText: String,
    val key: EngineKey,
    textSize: Float = 16f,
    percentWidth: Float = 0.1f,
    variant: Variant = Variant.Normal
) : KeyDef(
    Appearance.Text(
        displayText,
        textSize = textSize,
        percentWidth = percentWidth,
        variant = variant
    ),
    setOf(
        Behavior.Press(KeyAction.SymAction(key, VirtualNumLockModifiers))
    )
)
