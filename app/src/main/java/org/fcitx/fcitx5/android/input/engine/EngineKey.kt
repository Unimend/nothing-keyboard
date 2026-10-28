/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.engine

/** Engine-independent meaning of a virtual key. */
sealed interface EngineKey {
    data class Unicode(val codePoint: Int) : EngineKey
    data class KeypadDigit(val digit: Int) : EngineKey {
        init {
            require(digit in 0..9)
        }
    }
    data class KeypadOperator(val operator: Operator) : EngineKey {
        enum class Operator { Add, Subtract, Multiply, Divide, Separator, Decimal, Equal }
    }

    data object Backspace : EngineKey
    data object Return : EngineKey
    data object Space : EngineKey
    data object ArrowLeft : EngineKey
    data object ArrowRight : EngineKey
}

enum class EngineKeyModifier {
    Shift,
    CapsLock,
    NumLock,
    Control,
    Alt,
    Super,
    Virtual
}

val VirtualKeyModifiers = setOf(EngineKeyModifier.Virtual)
val VirtualNumLockModifiers = setOf(EngineKeyModifier.Virtual, EngineKeyModifier.NumLock)
