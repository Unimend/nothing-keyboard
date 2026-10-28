/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.core

import splitties.bitflags.hasFlag

/**
 * translated from
 * [fcitx-utils/capabilityflags.h](https://github.com/fcitx/fcitx5/blob/5.1.1/src/lib/fcitx-utils/capabilityflags.h)
 */
@Suppress("unused")
enum class CapabilityFlag(val flag: ULong) {
    NoFlag(0UL),

    /**
     * Deprecated, because this flag is not compatible with fcitx 4.
     */
    ClientSideUI(1UL shl 0),
    Preedit(1UL shl 1),
    ClientSideControlState(1UL shl 2),
    Password(1UL shl 3),
    FormattedPreedit(1UL shl 4),
    ClientUnfocusCommit(1UL shl 5),
    SurroundingText(1UL shl 6),
    Email(1UL shl 7),
    Digit(1UL shl 8),
    Uppercase(1UL shl 9),
    Lowercase(1UL shl 10),
    NoAutoUpperCase(1UL shl 11),
    Url(1UL shl 12),
    Dialable(1UL shl 13),
    Number(1UL shl 14),
    NoOnScreenKeyboard(1UL shl 15),
    SpellCheck(1UL shl 16),
    NoSpellCheck(1UL shl 17),
    WordCompletion(1UL shl 18),
    UppercaseWords(1UL shl 19),
    UppercaseSentences(1UL shl 20),
    Alpha(1UL shl 21),
    Name(1UL shl 22),
    GetIMInfoOnFocus(1UL shl 23),
    RelativeRect(1UL shl 24),
    // 25 ~ 31 are reserved for fcitx 4 compatibility.

    // New addition in fcitx 5.
    Terminal(1UL shl 32),
    Date(1UL shl 33),
    Time(1UL shl 34),
    Multiline(1UL shl 35),
    Sensitive(1UL shl 36),
    KeyEventOrderFix(1UL shl 37),

    /**
     * Whether client will set KeyState::Repeat on the key event.
     */
    ReportKeyRepeat(1UL shl 38),

    /**
     * Whether client display input panel by itself.
     */
    ClientSideInputPanel(1UL shl 39),

    /**
     * Whether client request input method to be disabled.
     * Usually this means only allow to type with raw keyboard.
     */
    Disable(1UL shl 40),

    /**
     * Whether client support commit string with cursor location.
     */
    CommitStringWithCursor(1UL shl 41),

    PasswordOrSensitive(Password.flag or Sensitive.flag);

}

operator fun ULong.plus(other: CapabilityFlag) = or(other.flag)
operator fun ULong.minus(other: CapabilityFlag) = and(other.flag.inv())

@JvmInline
value class CapabilityFlags constructor(val flags: ULong) {
    companion object {
        fun mergeFlags(arr: Array<out CapabilityFlag>): ULong =
            arr.fold(CapabilityFlag.NoFlag.flag) { acc, it -> acc or it.flag }

        val DefaultFlags = CapabilityFlags(
            CapabilityFlag.Preedit,
            CapabilityFlag.ClientUnfocusCommit,
            CapabilityFlag.CommitStringWithCursor
        )


    }

    constructor(vararg flags: CapabilityFlag) : this(mergeFlags(flags))

    fun has(flag: ULong) = flags.hasFlag(flag)

    fun has(flag: CapabilityFlag) = flags.hasFlag(flag.flag)

    fun toLong() = flags.toLong()
}
