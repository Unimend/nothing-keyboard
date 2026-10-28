/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.engine

import android.os.SystemClock
import android.util.LruCache
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.inputmethod.InputConnection
import org.fcitx.fcitx5.android.core.ScancodeMapping
import timber.log.Timber

/** Owns Android/Fcitx hardware-key correlation and synthetic Android key sequences. */
class AndroidKeyEventController(
    private val connection: () -> InputConnection?,
    private val submitHardwareKey: (KeyEvent, Int) -> Boolean,
    private val commitText: (String) -> Unit,
    private val invalidateDestructiveHistory: () -> Unit
) {
    private val pendingEvents = LruCache<Int, KeyEvent>(78)
    private var nextEventId = 0
    private var lastMetaState = 0

    fun forwardToEngine(event: KeyEvent): Boolean {
        val eventId = nextEventId++
        pendingEvents.put(eventId, event)
        val accepted = submitHardwareKey(event, eventId)
        if (!accepted) {
            pendingEvents.remove(eventId)
        } else if (AndroidHardwareEditingPolicy.invalidatesDestructiveHistory(event)) {
            invalidateDestructiveHistory()
        }
        return accepted
    }

    fun handleForwarded(effect: EngineEffect.ForwardKey) {
        pendingEvents.remove(effect.timestamp)?.let { original ->
            val target = connection() ?: return
            if (original.unicodeChar == KeyCharacterMap.PICKER_DIALOG_INPUT.code) {
                target.sendKeyEvent(
                    KeyEvent(
                        original.downTime, original.eventTime, original.action, original.keyCode,
                        original.repeatCount, original.metaState, -1, original.scanCode,
                        original.flags, original.source
                    )
                )
                return
            }
            target.sendKeyEvent(original)
            if (KeyEvent.isModifierKey(original.keyCode)) {
                when (original.action) {
                    KeyEvent.ACTION_DOWN -> lastMetaState = original.metaState
                    KeyEvent.ACTION_UP -> {
                        target.clearMetaKeyStates(lastMetaState xor original.metaState)
                        lastMetaState = original.metaState
                    }
                }
            }
            return
        }

        if (effect.androidKeyCode != KeyEvent.KEYCODE_UNKNOWN) {
            val eventTime = SystemClock.uptimeMillis()
            if (effect.keyUp) {
                sendUp(eventTime, effect.androidKeyCode, effect.androidMetaState)
            } else {
                sendDown(eventTime, effect.androidKeyCode, effect.androidMetaState)
            }
        } else if (!effect.keyUp && effect.unicode > 0) {
            commitText(Character.toString(effect.unicode))
        } else {
            Timber.w("Unhandled forwarded engine key: $effect")
        }
    }

    fun sendDownUp(keyCode: Int, metaState: Int = 0) {
        val eventTime = SystemClock.uptimeMillis()
        sendDown(eventTime, keyCode, metaState)
        sendUp(eventTime, keyCode, metaState)
    }

    fun sendCombination(
        keyCode: Int,
        alt: Boolean = false,
        ctrl: Boolean = false,
        shift: Boolean = false
    ) {
        var metaState = 0
        if (alt) metaState = KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
        if (ctrl) metaState = metaState or KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
        if (shift) metaState = metaState or KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
        val eventTime = SystemClock.uptimeMillis()
        if (alt) sendDown(eventTime, KeyEvent.KEYCODE_ALT_LEFT)
        if (ctrl) sendDown(eventTime, KeyEvent.KEYCODE_CTRL_LEFT)
        if (shift) sendDown(eventTime, KeyEvent.KEYCODE_SHIFT_LEFT)
        sendDown(eventTime, keyCode, metaState)
        sendUp(eventTime, keyCode, metaState)
        if (shift) sendUp(eventTime, KeyEvent.KEYCODE_SHIFT_LEFT)
        if (ctrl) sendUp(eventTime, KeyEvent.KEYCODE_CTRL_LEFT)
        if (alt) sendUp(eventTime, KeyEvent.KEYCODE_ALT_LEFT)
    }

    fun reset() {
        pendingEvents.evictAll()
        // Do not reuse IDs: an old engine reply must not match a new editor's pending event.
        lastMetaState = 0
    }

    private fun sendDown(eventTime: Long, keyCode: Int, metaState: Int = 0) {
        connection()?.sendKeyEvent(
            KeyEvent(
                eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0, metaState,
                KeyCharacterMap.VIRTUAL_KEYBOARD,
                ScancodeMapping.keyCodeToScancode(keyCode),
                KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE
            )
        )
    }

    private fun sendUp(eventTime: Long, keyCode: Int, metaState: Int = 0) {
        connection()?.sendKeyEvent(
            KeyEvent(
                eventTime, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, keyCode, 0, metaState,
                KeyCharacterMap.VIRTUAL_KEYBOARD,
                ScancodeMapping.keyCodeToScancode(keyCode),
                KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE
            )
        )
    }
}

object AndroidHardwareEditingPolicy {
    fun invalidatesDestructiveHistory(event: KeyEvent): Boolean = invalidatesDestructiveHistory(
        event.action,
        event.keyCode,
        event.unicodeChar
    )

    fun invalidatesDestructiveHistory(action: Int, keyCode: Int, unicode: Int): Boolean {
        if (action != KeyEvent.ACTION_DOWN || keyCode in modifierKeys) return false
        return unicode > 0 || keyCode in setOf(
            KeyEvent.KEYCODE_DEL,
            KeyEvent.KEYCODE_FORWARD_DEL,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER,
            KeyEvent.KEYCODE_TAB,
            KeyEvent.KEYCODE_SPACE
        )
    }

    private val modifierKeys = setOf(
        KeyEvent.KEYCODE_SHIFT_LEFT,
        KeyEvent.KEYCODE_SHIFT_RIGHT,
        KeyEvent.KEYCODE_ALT_LEFT,
        KeyEvent.KEYCODE_ALT_RIGHT,
        KeyEvent.KEYCODE_CTRL_LEFT,
        KeyEvent.KEYCODE_CTRL_RIGHT,
        KeyEvent.KEYCODE_META_LEFT,
        KeyEvent.KEYCODE_META_RIGHT,
        KeyEvent.KEYCODE_CAPS_LOCK,
        KeyEvent.KEYCODE_NUM_LOCK,
        KeyEvent.KEYCODE_SCROLL_LOCK,
        KeyEvent.KEYCODE_SYM,
        KeyEvent.KEYCODE_FUNCTION
    )
}
