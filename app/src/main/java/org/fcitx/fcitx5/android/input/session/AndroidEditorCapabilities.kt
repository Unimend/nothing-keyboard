/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.session

import android.text.InputType
import android.view.inputmethod.EditorInfo
import splitties.bitflags.hasFlag

object AndroidEditorCapabilities {
    fun fromAndroid(inputType: Int, imeOptions: Int): Set<EditorCapability> {
        val flags = mutableSetOf(EditorCapability.Composition, EditorCapability.CommitOnBlur, EditorCapability.CursorAwareCommit)
        imeOptions.let {
            if (it.hasFlag(EditorInfo.IME_FLAG_FORCE_ASCII)) {
                flags += EditorCapability.Ascii
            }
            if (it.hasFlag(EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING)) {
                flags += EditorCapability.Private
            }
        }
        inputType.let {
            when (it and InputType.TYPE_MASK_CLASS) {
                InputType.TYPE_NULL -> {
                    flags -= EditorCapability.Composition
                    flags += EditorCapability.NoSuggestions
                }
                InputType.TYPE_CLASS_TEXT -> {
                    (it and InputType.TYPE_MASK_FLAGS).run {
                        if (hasFlag(InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE) ||
                            hasFlag(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)
                        ) {
                            flags += EditorCapability.NoSuggestions
                        }
                        if (hasFlag(InputType.TYPE_TEXT_FLAG_AUTO_CORRECT)) {
                            flags += EditorCapability.Spelling
                        }
                        if (hasFlag(InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS)) {
                            flags += EditorCapability.Uppercase
                        }
                        if (hasFlag(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)) {
                            flags += EditorCapability.CapitalSentences
                        }
                        if (hasFlag(InputType.TYPE_TEXT_FLAG_CAP_WORDS)) {
                            flags += EditorCapability.CapitalWords
                        }
                        if (hasFlag(InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE) ||
                            hasFlag(InputType.TYPE_TEXT_FLAG_MULTI_LINE)
                        ) {
                            flags += EditorCapability.Multiline
                        }
                    }
                    (it and InputType.TYPE_MASK_VARIATION).run {
                        if (equals(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS) ||
                            equals(InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS)
                        ) {
                            flags += EditorCapability.Email
                        }
                        if (equals(InputType.TYPE_TEXT_VARIATION_PASSWORD) ||
                            equals(InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)
                        ) {
                            flags += EditorCapability.Password
                        }
                        if (equals(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)) {
                            flags += EditorCapability.Private
                            flags += EditorCapability.NoSuggestions
                        }
                        if (equals(InputType.TYPE_TEXT_VARIATION_URI)) {
                            flags += EditorCapability.Url
                        }
                    }
                }
                InputType.TYPE_CLASS_NUMBER -> {
                    flags += EditorCapability.NoSuggestions
                    flags += EditorCapability.Number
                    if ((it and InputType.TYPE_MASK_VARIATION) == InputType.TYPE_NUMBER_VARIATION_PASSWORD) {
                        flags += EditorCapability.Password
                    }
                }
                InputType.TYPE_CLASS_PHONE -> {
                    flags += EditorCapability.NoSuggestions
                    flags += EditorCapability.Phone
                }
                InputType.TYPE_CLASS_DATETIME -> {
                    flags += EditorCapability.NoSuggestions
                    (it and InputType.TYPE_MASK_VARIATION).run {
                        if (hasFlag(InputType.TYPE_DATETIME_VARIATION_DATE)) {
                            flags += EditorCapability.Date
                        }
                        if (hasFlag(InputType.TYPE_DATETIME_VARIATION_TIME)) {
                            flags += EditorCapability.Time
                        }
                    }
                }
            }
        }
        return flags.toSet()
    }
}
