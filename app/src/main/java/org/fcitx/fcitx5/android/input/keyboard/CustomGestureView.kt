/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.data.InputFeedbacks
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.input.gesture.SwipeTracker
import org.fcitx.fcitx5.android.input.feedback.KeyboardFeedback

open class CustomGestureView(ctx: Context) : FrameLayout(ctx) {

    enum class GestureType { Down, Move, Up, Cancel }

    data class Event(
        val type: GestureType,
        val consumed: Boolean,
        val x: Float,
        val y: Float,
        val countX: Int,
        val countY: Int,
        val totalX: Int,
        val totalY: Int
    )

    fun interface OnGestureListener {
        fun onGesture(view: View, event: Event): Boolean

        companion object {
            val Empty = OnGestureListener { _, _ -> false }
        }
    }

    private val lifecycleScope by lazy {
        findViewTreeLifecycleOwner()?.lifecycleScope!!
    }

    @Volatile
    private var touchMovedOutside = false

    @Volatile
    private var longPressTriggered = false
    var longPressEnabled = false
    private var longPressJob: Job? = null

    @Volatile
    var longPressFeedbackEnabled = true

    @Volatile
    private var repeatStarted = false
    var repeatEnabled = false
    private var repeatJob: Job? = null

    var swipeEnabled = false
    var swipeRepeatEnabled = false
    var swipeThresholdX = 24f
    var swipeThresholdY = 24f

    private var swipeRepeatTriggered = false
    private val swipeTracker = SwipeTracker()
    private var gestureConsumed = false
    private var touchActive = false

    var doubleTapEnabled = false
    private var lastClickTime = 0L
    private var maybeDoubleTap = false

    var onDoubleTapListener: ((View) -> Unit)? = null
    var onRepeatListener: ((View) -> Unit)? = null
    var onGestureListener: OnGestureListener? = null

    var soundEffect: InputFeedbacks.SoundEffect = InputFeedbacks.SoundEffect.Standard

    private val touchSlop: Float = ViewConfiguration.get(ctx).scaledTouchSlop.toFloat()

    init {
        // disable system sound effect and haptic feedback
        isSoundEffectsEnabled = false
        isHapticFeedbackEnabled = false
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        if (!enabled) {
            cancelGestures()
        }
    }

    private fun pointInView(x: Float, y: Float): Boolean {
        return -touchSlop <= x &&
                -touchSlop <= y &&
                x < (width + touchSlop) &&
                y < (height + touchSlop)
    }

    private fun resetState() {
        touchActive = false
        touchMovedOutside = false
        // Flags can change while a touch is active; jobs must still be cancelled.
        longPressJob?.cancel()
        longPressJob = null
        repeatJob?.cancel()
        repeatJob = null
        longPressTriggered = false
        repeatStarted = false
        swipeRepeatTriggered = false
        swipeTracker.reset()
        gestureConsumed = false
        // double tap state should be preserved on touch up
    }

    fun cancelGestures() {
        // Both Android cancellation and keyboard teardown use this cleanup path.
        if (touchActive) {
            touchActive = false
            dispatchGestureEvent(GestureType.Cancel, 0f, 0f)
        }
        isPressed = false
        resetState()
        // reset double tap state on cancel
        if (doubleTapEnabled) {
            maybeDoubleTap = false
            lastClickTime = 0
        }
    }

    override fun onDetachedFromWindow() {
        cancelGestures()
        KeyboardFeedback.cancel(this)
        super.onDetachedFromWindow()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility != View.VISIBLE) {
            cancelGestures()
            KeyboardFeedback.cancel(this)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (!isEnabled) return false
                touchActive = true
                drawableHotspotChanged(x, y)
                isPressed = true
                InputFeedbacks.hapticFeedback(this)
                InputFeedbacks.soundEffect(soundEffect)
                dispatchGestureEvent(GestureType.Down, x, y)
                if (longPressEnabled) {
                    longPressJob?.cancel()
                    longPressJob = lifecycleScope.launch {
                        delay(longPressDelay.toLong())
                        if (longPressFeedbackEnabled) {
                            InputFeedbacks.hapticFeedback(this@CustomGestureView, true)
                        }
                        longPressTriggered = performLongClick()
                    }
                }
                if (repeatEnabled) {
                    repeatJob?.cancel()
                    repeatJob = lifecycleScope.launch {
                        delay(longPressDelay.toLong())
                        repeatStarted = true
                        var lastTriggerTime: Long
                        while (isActive && isEnabled) {
                            lastTriggerTime = SystemClock.uptimeMillis()
                            onRepeatListener?.invoke(this@CustomGestureView)
                            val t = lastTriggerTime + RepeatInterval - SystemClock.uptimeMillis()
                            if (t > 0) delay(t)
                        }
                    }
                }
                if (swipeEnabled) {
                    swipeTracker.start(x, y)
                }
            }
            MotionEvent.ACTION_UP -> {
                isPressed = false
                InputFeedbacks.hapticFeedback(this, longPress = true, keyUp = true)
                dispatchGestureEvent(GestureType.Up, event.x, event.y)
                val shouldPerformClick = !(touchMovedOutside ||
                        longPressTriggered ||
                        repeatStarted ||
                        swipeRepeatTriggered ||
                        gestureConsumed)
                resetState()
                if (shouldPerformClick) {
                    if (doubleTapEnabled) {
                        val now = SystemClock.uptimeMillis()
                        if (maybeDoubleTap && now - lastClickTime <= longPressDelay) {
                            maybeDoubleTap = false
                            onDoubleTapListener?.invoke(this)
                        } else {
                            maybeDoubleTap = true
                            performClick()
                        }
                        lastClickTime = now
                    } else {
                        performClick()
                    }
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!isEnabled) return false
                drawableHotspotChanged(x, y)
                if (!touchMovedOutside && !pointInView(x, y)) {
                    touchMovedOutside = true
                    if (longPressEnabled) {
                        longPressJob?.cancel()
                        longPressJob = null
                    }
                    if (repeatEnabled) {
                        repeatJob?.cancel()
                        repeatJob = null
                    }
                    if (repeatStarted || !swipeEnabled) {
                        isPressed = false
                    }
                }
                if (!swipeEnabled || longPressTriggered || repeatStarted) return true
                val step = swipeTracker.move(x, y, swipeThresholdX, swipeThresholdY)
                if (step.crossedThreshold) {
                    if (swipeRepeatEnabled) swipeRepeatTriggered = true
                    if (longPressEnabled && !longPressTriggered) {
                        longPressJob?.cancel()
                        longPressJob = null
                    }
                    if (repeatEnabled && !repeatStarted) {
                        repeatJob?.cancel()
                        repeatJob = null
                    }
                }
                dispatchGestureEvent(GestureType.Move, x, y, step.countX, step.countY)
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                cancelGestures()
                return true
            }
        }
        return true
    }

    private fun dispatchGestureEvent(
        type: GestureType,
        x: Float,
        y: Float,
        countX: Int = 0,
        countY: Int = 0
    ) {
        val event = Event(
            type, gestureConsumed, x, y, countX, countY,
            swipeTracker.totalX, swipeTracker.totalY
        )
        val consumed = onGestureListener?.onGesture(this, event) ?: return
        if (consumed && !gestureConsumed) {
            gestureConsumed = true
        }
    }

    override fun setOnLongClickListener(l: OnLongClickListener?) {
        longPressEnabled = l != null
        super.setOnLongClickListener(l)
    }

    companion object {
        val longPressDelay by AppPrefs.getInstance().keyboard.longPressDelay
        const val RepeatInterval = 50L
    }
}
