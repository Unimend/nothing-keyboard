/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.wm

import android.view.View
import android.widget.FrameLayout
import androidx.transition.Transition
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.input.dependency.UniqueViewComponent
import org.fcitx.fcitx5.android.input.dependency.context
import org.fcitx.fcitx5.android.input.design.NothingDesignTokens
import org.fcitx.fcitx5.android.input.session.SurfaceStateSink
import org.mechdancer.dependency.DynamicScope
import org.mechdancer.dependency.manager.must
import org.mechdancer.dependency.minusAssign
import org.mechdancer.dependency.plusAssign
import splitties.views.dsl.core.add
import splitties.views.dsl.core.frameLayout
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent
import timber.log.Timber

class InputWindowManager(private val surfaceSink: SurfaceStateSink) : UniqueViewComponent<InputWindowManager, FrameLayout>() {

    private val context by manager.context()
    private val lifecycle: InputWindowLifecycleDispatcher by manager.must()
    private lateinit var scope: DynamicScope

    private val essentialWindows = mutableMapOf<EssentialWindow.Key, Pair<InputWindow, View?>>()

    private var currentWindow: InputWindow? = null
    private var currentView: View? = null
    var isDisposed: Boolean = false
        private set

    private val disableAnimation by AppPrefs.getInstance().advanced.disableAnimation

    private fun prepareAnimation(
        exitAnimation: Transition?,
        enterAnimation: Transition?,
        remove: View,
        add: View
    ) {
        if (disableAnimation)
            return
        enterAnimation?.addTarget(add)
        exitAnimation?.addTarget(remove)
        TransitionManager.beginDelayedTransition(view, TransitionSet().apply {
            enterAnimation?.let { addTransition(it) }
            exitAnimation?.let { addTransition(it) }
            duration = NothingDesignTokens.Motion.WindowTransitionMs
        })
    }

    /**
     * Associate essential window with its key and add it to scope
     * If [createView] is `true`, the view will be created immediately.
     * Otherwise, it will be created on first attach
     */
    fun <R> addEssentialWindow(
        window: R,
        createView: Boolean = false
    ) where R : InputWindow, R : EssentialWindow {
        if (window.key in essentialWindows) {
            if (essentialWindows[window.key]!!.first === window)
                Timber.d("Skip adding essential window $window")
            else
                throw IllegalStateException("${window.key} is already occupied")
        }
        scope += window
        val view = if (createView) window.onCreateView() else null
        essentialWindows[window.key] = window to view
    }

    fun getEssentialWindow(windowKey: EssentialWindow.Key) =
        essentialWindows[windowKey]?.first
            ?: throw IllegalArgumentException("Unable to find essential window associated with $windowKey")

    /**
     * Attach an essential window by key
     * IMPORTANT: the window key must be known,
     * i.e. the essential window should be added first via [addEssentialWindow].
     * Moreover, [attachWindow] can also add the essential window with key.
     */
    fun attachWindow(windowKey: EssentialWindow.Key) {
        essentialWindows[windowKey]?.let { (window, _) ->
            attachWindow(window)
        } ?: throw IllegalStateException("$windowKey is not a known essential window key")
    }

    /**
     * Remove an essential window
     */
    fun removeEssentialWindow(windowKey: EssentialWindow.Key) {
        val (window, _) = essentialWindows[windowKey]
            ?: throw IllegalStateException("$windowKey is not a known essential window key")
        if (currentWindow === window)
            throw IllegalStateException("$windowKey cannot be removed when it's active")
        // remove from scope
        scope -= window
        // remove from map
        essentialWindows.remove(windowKey)
    }

    /**
     * Attach a new window, removing the old one
     * This function initialize the view for windows and save it for essential windows.
     * [attachWindow] includes the operation done by [addEssentialWindow].
     */
    fun attachWindow(window: InputWindow) {
        if (isDisposed) return
        if (window === currentWindow) {
            Timber.d("Skip attaching $window")
            // InputView survives across editor sessions, while InputViewFinished resets the
            // session surface to None. Re-publish the already attached surface without
            // rebuilding the view or emitting a false lifecycle transition.
            surfaceSink.publish(window.surface)
            return
        }
        val newView = if (window is EssentialWindow) {
            // keep the view for essential windows
            essentialWindows[window.key]?.second ?: window.onCreateView()
                .also { essentialWindows[window.key] = window to it }
        } else {
            // add the new window to scope, except essential windows (they are always in scope)
            scope += window
            window.onCreateView()
        }
        if (currentWindow != null) {
            val oldWindow = currentWindow!!
            val oldView = currentView!!
            prepareAnimation(
                oldWindow.exitAnimation(window),
                window.enterAnimation(oldWindow),
                oldView,
                newView
            )
            // notify the window that it will be detached
            oldWindow.onDetached()
            // remove the old window from layout
            view.removeView(oldView)
            // broadcast the old window was removed from layout
            lifecycle.detached(oldWindow)
            Timber.d("Detach $oldWindow")
            // finally remove the old window from scope only if it's not an essential window,
            if (oldWindow !is EssentialWindow)
                scope -= oldWindow
        }
        // call before attached for essential window
        if (window is EssentialWindow)
            window.beforeAttached()
        // add the new window to layout
        view.apply { add(newView, lParams(matchParent, matchParent)) }
        currentView = newView
        Timber.d("Attach $window")
        // notify the window it was attached
        window.onAttached()
        currentWindow = window
        surfaceSink.publish(window.surface)
        // broadcast the new window was added to layout
        lifecycle.attached(window)
    }

    override val view: FrameLayout by lazy { context.frameLayout(R.id.input_window) }

    fun bindScope(scope: DynamicScope) {
        this.scope = scope
    }

    fun isAttached(window: InputWindow) = currentWindow === window

    /** Release the active surface before its dependency scope disappears. Idempotent. */
    fun dispose() {
        if (isDisposed) return
        isDisposed = true
        val old = currentWindow
        currentWindow = null
        currentView = null
        if (old != null) {
            old.onDetached()
            TransitionManager.endTransitions(view)
            view.removeAllViews()
            lifecycle.detached(old)
        }
        essentialWindows.clear()
    }
}
