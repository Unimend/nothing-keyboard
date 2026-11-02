/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.picker

import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.input.keyboard.ImageLayoutSwitchKey
import org.fcitx.fcitx5.android.input.keyboard.ImagePickerSwitchKey
import org.fcitx.fcitx5.android.input.keyboard.TextPickerSwitchKey
import org.fcitx.fcitx5.android.input.mode.KeyboardLayout
import org.fcitx.fcitx5.android.input.mode.PickerKind

fun symbolPicker(): PickerWindow = PickerWindow(
    key = PickerWindow.Key.Symbol,
    data = PickerData.Symbol,
    density = PickerPageUi.Density.High,
    switchKey = ImageLayoutSwitchKey(R.drawable.ic_pixel_number_pad, KeyboardLayout.Number)
)

fun emojiPicker(): PickerWindow = PickerWindow(
    key = PickerWindow.Key.Emoji,
    data = PickerData.Emoji,
    density = PickerPageUi.Density.Medium,
    switchKey = TextPickerSwitchKey(":-)", PickerKind.Emoticon),
    popupPreview = false,
    followKeyBorder = false,
    policy = EmojiPickerPolicy()
)

fun emoticonPicker(): PickerWindow = PickerWindow(
    key = PickerWindow.Key.Emoticon,
    data = PickerData.Emoticon,
    density = PickerPageUi.Density.Low,
    switchKey = ImagePickerSwitchKey(R.drawable.ic_pixel_face, PickerKind.Emoji),
    popupPreview = false,
    followKeyBorder = false
)
