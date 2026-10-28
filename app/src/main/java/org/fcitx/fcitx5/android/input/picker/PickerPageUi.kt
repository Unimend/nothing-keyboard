/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.picker

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.input.engine.EngineKey
import org.fcitx.fcitx5.android.input.settings.InputSettings
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.AutoScaleTextView
import org.fcitx.fcitx5.android.input.design.NothingTextStyle
import org.fcitx.fcitx5.android.input.design.setNothingText
import org.fcitx.fcitx5.android.input.gesture.BackspaceGestureController
import org.fcitx.fcitx5.android.input.keyboard.CustomGestureView
import org.fcitx.fcitx5.android.input.keyboard.CustomGestureView.OnGestureListener
import org.fcitx.fcitx5.android.input.keyboard.ImageKeyView
import org.fcitx.fcitx5.android.input.keyboard.KeyAction
import org.fcitx.fcitx5.android.input.keyboard.KeyAction.CommitAction
import org.fcitx.fcitx5.android.input.keyboard.KeyAction.ClearAllTextAction
import org.fcitx.fcitx5.android.input.keyboard.KeyAction.DeleteSelectionAction
import org.fcitx.fcitx5.android.input.keyboard.KeyAction.TextKeyAction
import org.fcitx.fcitx5.android.input.keyboard.KeyAction.MoveSelectionAction
import org.fcitx.fcitx5.android.input.keyboard.KeyAction.SymAction
import org.fcitx.fcitx5.android.input.keyboard.KeyAction.UndoAction
import org.fcitx.fcitx5.android.input.keyboard.KeyActionListener
import org.fcitx.fcitx5.android.input.keyboard.KeyActionListener.Source
import org.fcitx.fcitx5.android.input.keyboard.KeyDef
import org.fcitx.fcitx5.android.input.keyboard.KeyDef.Appearance
import org.fcitx.fcitx5.android.input.keyboard.KeyDef.Appearance.Border
import org.fcitx.fcitx5.android.input.keyboard.KeyDef.Appearance.Variant
import org.fcitx.fcitx5.android.input.keyboard.KeyView
import org.fcitx.fcitx5.android.input.keyboard.TextKeyView
import org.fcitx.fcitx5.android.input.design.inputSurfaceStyle
import org.fcitx.fcitx5.android.input.popup.PopupAction
import org.fcitx.fcitx5.android.input.popup.PopupResult
import org.fcitx.fcitx5.android.input.popup.PopupActionListener
import splitties.views.dsl.constraintlayout.below
import splitties.views.dsl.constraintlayout.bottomOfParent
import splitties.views.dsl.constraintlayout.bottomToTopOf
import splitties.views.dsl.constraintlayout.constraintLayout
import splitties.views.dsl.constraintlayout.lParams
import splitties.views.dsl.constraintlayout.leftOfParent
import splitties.views.dsl.constraintlayout.leftToRightOf
import splitties.views.dsl.constraintlayout.rightOfParent
import splitties.views.dsl.constraintlayout.rightToLeftOf
import splitties.views.dsl.constraintlayout.topOfParent
import splitties.views.dsl.constraintlayout.topToBottomOf
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.add
import splitties.views.dsl.core.matchParent

class PickerPageUi(
    override val ctx: Context,
    private val theme: Theme,
    density: Density,
    bordered: Boolean = false
) : Ui {

    private val visualStyle = theme.inputSurfaceStyle.picker

    enum class Density(
        val pageSize: Int,
        val columnCount: Int,
        val rowCount: Int,
        val textSize: Float,
        val autoScale: Boolean,
        val showBackspace: Boolean
    ) {
        // symbol: 10/10/8, backspace on bottom right
        High(28, 10, 3, 19f, false, true),

        // emoji: 7/7/6, backspace on bottom right
        Medium(20, 7, 3, 23.7f, false, true),

        // emoticon: 4/4/4, no backspace
        Low(12, 4, 3, 19f, true, false)
    }

    private val popupOnKeyPress by InputSettings.keyboard.popupOnKeyPress
    private val hapticOnRepeat by InputSettings.keyboard.hapticOnRepeat

    var keyActionListener: KeyActionListener? = null
    var popupActionListener: PopupActionListener? = null

    private val keyAppearance = Appearance.Text(
        displayText = "",
        textSize = if (density == Density.High) {
            visualStyle.denseSymbolTextSizeSp ?: density.textSize
        } else density.textSize,
        variant = Variant.Normal,
        border = if (bordered) Border.On else Border.Off
    )

    private val keyViews = Array(density.pageSize) {
        TextKeyView(ctx, theme, keyAppearance).apply {
            if (density.autoScale) {
                mainText.apply {
                    scaleMode = AutoScaleTextView.Mode.Proportional
                    setPadding(hMargin, vMargin, hMargin, vMargin)
                }
            }
        }
    }

    private val backspaceAppearance = Appearance.Image(
        src = R.drawable.ic_baseline_backspace_24,
        variant = Variant.Alternative,
        border = if (bordered) Border.On else Border.Off,
        viewId = R.id.button_backspace
    )

    private val backspaceKey by lazy {
        val backspaceAction = SymAction(EngineKey.Backspace)
        val action: (View) -> Unit = {
            keyActionListener?.onKeyAction(backspaceAction, Source.Keyboard)
        }
        val listener = View.OnClickListener { action.invoke(it) }
        ImageKeyView(ctx, theme, backspaceAppearance).apply {
            setOnClickListener(listener)
            repeatEnabled = true
            onRepeatListener = action
            BackspaceGestureController.bind(
                this,
                theme,
                hapticOnRepeat,
                BackspaceGestureController.Actions(
                    moveSelection = {
                        keyActionListener?.onKeyAction(MoveSelectionAction(it), Source.Keyboard)
                    },
                    deleteSelection = {
                        keyActionListener?.onKeyAction(DeleteSelectionAction(it), Source.Keyboard)
                    },
                    clearAll = {
                        keyActionListener?.onKeyAction(ClearAllTextAction, Source.Keyboard)
                    },
                    undo = {
                        keyActionListener?.onKeyAction(UndoAction, Source.Keyboard)
                    },
                    cancel = {
                        keyActionListener?.onKeyAction(KeyAction.CancelSelectionAction, Source.Keyboard)
                    }
                )
            )
        }
    }

    override val root = constraintLayout {
        val columnCount = density.columnCount
        val rowCount = density.rowCount
        val keyWidth = 1f / columnCount
        keyViews.forEachIndexed { i, keyView ->
            val row = i / columnCount
            val column = i % columnCount
            add(keyView, lParams {
                // layout_constraintTop_to
                if (row == 0) {
                    // first row, align top to top of parent
                    topOfParent()
                } else {
                    // not first row, align top to bottom of first view in last row
                    topToBottomOf(keyViews[(row - 1) * columnCount])
                }
                // layout_constraintBottom_to
                if (row == rowCount - 1) {
                    // last row, align bottom to bottom of parent
                    bottomOfParent()
                } else {
                    // not last row, align bottom to top of first view in next row
                    bottomToTopOf(keyViews[(row + 1) * columnCount])
                }
                // layout_constraintLeft_to
                if (column == 0) {
                    // first column, align start to start of parent
                    leftOfParent()
                } else {
                    // not first column, align start to end of last column
                    leftToRightOf(keyViews[i - 1])
                }
                matchConstraintPercentWidth = keyWidth
            })
        }
        if (density.showBackspace) {
            add(backspaceKey, lParams {
                // top to bottom of first view of second-last row
                below(keyViews[(rowCount - 2) * columnCount])
                // bottom/right corner
                bottomOfParent()
                rightOfParent()
                matchConstraintPercentWidth = 0.15f
            })
            keyViews.last().updateLayoutParams<ConstraintLayout.LayoutParams> {
                // align right of last key to left of backspace
                rightToLeftOf(backspaceKey)
            }
            keyViews[(rowCount - 1) * columnCount].updateLayoutParams<ConstraintLayout.LayoutParams> {
                // first key of last row, align its right to the left of its next sibling
                rightToLeftOf(keyViews[(rowCount - 1) * columnCount + 1])
                // pack the entire last row together, towards the backspace
                horizontalChainStyle = ConstraintLayout.LayoutParams.CHAIN_PACKED
            }
        }
        layoutParams = ViewGroup.LayoutParams(matchParent, matchParent)
    }

    fun setItems(items: List<String>, policy: PickerPolicy? = null) {
        keyViews.forEachIndexed { i, keyView ->
            keyView.apply {
                if (i >= items.size) {
                    isEnabled = false
                    mainText.text = ""
                    setOnClickListener(null)
                    setOnLongClickListener(null)
                    swipeEnabled = false
                    onGestureListener = null
                } else {
                    isEnabled = true
                    val item = items[i]
                    val transformed = policy?.transform(item) ?: item
                    mainText.setNothingText(
                        transformed,
                        theme,
                        NothingTextStyle.KeyPrimary,
                        sizeSp = keyAppearance.textSize,
                        bold = false
                    )
                    setOnClickListener {
                        onItemClick(transformed)
                    }
                    if (policy == null) {
                        setOnLongClickListener(null)
                        swipeEnabled = false
                        onGestureListener = null
                    } else {
                        setOnLongClickListener longClick@{ view ->
                            if (view !is KeyView) return@longClick false
                            val popup = policy.popup(item) ?: return@longClick false
                            onItemLongClick(view, popup)
                            false
                        }
                        swipeEnabled = true
                        onGestureListener = OnGestureListener { view, event ->
                            view as KeyView
                            when (event.type) {
                                CustomGestureView.GestureType.Down -> {
                                    onPopupShow(view, item)
                                    // never "consume" the gesture on touch down
                                    false
                                }
                                CustomGestureView.GestureType.Move -> {
                                    onPopupChangeFocus(view.id, event.x, event.y)
                                }
                                CustomGestureView.GestureType.Up -> {
                                    onPopupTrigger(view.id).also {
                                        onPopupAction(PopupAction.DismissAction(view.id))
                                    }
                                }
                                CustomGestureView.GestureType.Cancel -> {
                                    onPopupAction(PopupAction.DismissAction(view.id))
                                    true
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun onItemClick(item: String) {
        keyActionListener?.onKeyAction(CommitAction(item), Source.Keyboard)
    }

    private fun onPopupAction(action: PopupAction): PopupResult =
        popupActionListener?.onPopupAction(action) ?: PopupResult.None

    private fun onItemLongClick(view: KeyView, popup: KeyDef.Popup.Keyboard) {
        if (!popupOnKeyPress) {
            // in case "popup on keypress" is disabled, popup keyboard need to know
            // the actual bounds on press. see [^1] as well
            view.updateBounds()
        }
        onPopupAction(PopupAction.ShowKeyboardAction(view.id, popup, view.bounds))
    }

    private fun onPopupShow(view: KeyView, item: String) {
        if (!popupOnKeyPress) return
        // [^1]: bounds is first calculated in KeyView's onLayout(), it
        // not in screen viewport at the time of layout.
        // e.g. it's inside the next page of ViewPager
        // so update bounds when it's pressed
        view.updateBounds()
        onPopupAction(PopupAction.PreviewAction(view.id, item, view.bounds))
        return
    }

    private fun onPopupChangeFocus(viewId: Int, x: Float, y: Float): Boolean {
        val changeFocusAction = PopupAction.ChangeFocusAction(viewId, x, y)
        return (onPopupAction(changeFocusAction) as? PopupResult.FocusChanged)?.changed ?: false
    }

    private fun onPopupTrigger(viewId: Int): Boolean {
        val triggerAction = PopupAction.TriggerAction(viewId)
        val action = (onPopupAction(triggerAction) as? PopupResult.Triggered)?.action as? TextKeyAction ?: return false
        onItemClick(action.act)
        onPopupAction(PopupAction.DismissAction(viewId))
        return true
    }

}
