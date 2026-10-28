/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.picker

import androidx.transition.Transition
import androidx.viewpager2.widget.ViewPager2
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.input.design.ReturnKeyGlyphPolicy
import org.fcitx.fcitx5.android.input.design.isNothingStyle
import org.fcitx.fcitx5.android.input.dependency.inputMethodService
import org.fcitx.fcitx5.android.input.dependency.theme
import org.fcitx.fcitx5.android.input.keyboard.CommonKeyActionListener
import org.fcitx.fcitx5.android.input.keyboard.KeyAction
import org.fcitx.fcitx5.android.input.keyboard.KeyActionListener
import org.fcitx.fcitx5.android.input.keyboard.KeyDef
import org.fcitx.fcitx5.android.input.navigation.InputNavigationCommand
import org.fcitx.fcitx5.android.input.navigation.InputNavigator
import org.fcitx.fcitx5.android.input.mode.PickerKind
import org.fcitx.fcitx5.android.input.mode.InputMode
import org.fcitx.fcitx5.android.input.popup.PopupAction
import org.fcitx.fcitx5.android.input.popup.PopupActionListener
import org.fcitx.fcitx5.android.input.popup.PopupComponent
import org.fcitx.fcitx5.android.input.session.InputSurface
import org.fcitx.fcitx5.android.input.session.InputPresentationReducer
import org.fcitx.fcitx5.android.input.wm.EssentialWindow
import org.fcitx.fcitx5.android.input.wm.InputWindow
import org.mechdancer.dependency.manager.must

class PickerWindow(
    override val key: Key,
    private val data: List<Pair<PickerData.Category, Array<String>>>,
    private val density: PickerPageUi.Density,
    private val switchKey: KeyDef,
    private val popupPreview: Boolean = true,
    private val followKeyBorder: Boolean = true,
    private val policy: PickerPolicy = DefaultPickerPolicy()
) : InputWindow.ExtendedInputWindow<PickerWindow>(), EssentialWindow {

    override val surface = InputSurface.Picker(key.name)

    enum class Key(val kind: PickerKind) : EssentialWindow.Key {
        Symbol(PickerKind.Symbol),
        Emoji(PickerKind.Emoji),
        Emoticon(PickerKind.Emoticon);

        companion object {
            fun fromKind(kind: PickerKind): Key = entries.first { it.kind == kind }
        }
    }

    private val theme by manager.theme()
    private val service by manager.inputMethodService()
    private val commonKeyActionListener: CommonKeyActionListener by manager.must()
    private val popup: PopupComponent by manager.must()
    private val navigator: InputNavigator by manager.must()
    private val interactionStore: PickerInteractionStore by manager.must()

    private val keyBorder by ThemeManager.prefs.keyBorder

    private lateinit var pickerLayout: PickerLayout
    private lateinit var pickerPagesAdapter: PickerPagesAdapter

    private fun syncPageUi(position: Int) {
        val pickerPosition = pickerPagesAdapter.getPositionOfPage(position) ?: return
        val range = pickerPagesAdapter.getCategoryRangeOfPage(position)
        pickerLayout.tabsUi.activateTab(pickerPosition.categoryIndex)
        pickerLayout.paginationUi.updatePageCount(range.run { last - first + 1 })
        pickerLayout.paginationUi.updateScrollProgress(pickerPosition.pageInCategory, 0f)
    }

    private fun restorePosition() {
        val page = pickerPagesAdapter.resolvePage(interactionStore.state.position(key.kind))
        pickerLayout.pager.setCurrentItem(page, false)
        syncPageUi(page)
    }

    override fun enterAnimation(lastWindow: InputWindow): Transition? = null

    override fun exitAnimation(nextWindow: InputWindow): Transition? = null

    private val keyActionListener = KeyActionListener { it, source ->
        when (it) {
            is KeyAction.LayoutSwitchAction -> {
                when (val target = it.target) {
                    is InputMode.Keyboard ->
                        navigator.navigate(InputNavigationCommand.ShowKeyboard(target.layout))
                    is InputMode.Picker ->
                        navigator.navigate(InputNavigationCommand.ShowPicker(target.kind))
                    null -> navigator.navigate(InputNavigationCommand.ShowKeyboard())
                }
            }

            is KeyAction.TextKeyAction -> {
                // we want the behavior of CommitAction (commit the character as-is),
                // but don't want to include it in recently used list
                commonKeyActionListener.listener.onKeyAction(KeyAction.CommitAction(it.act), source)
            }

            else -> {
                if (it is KeyAction.CommitAction) {
                    pickerPagesAdapter.insertRecent(it.text)
                }
                commonKeyActionListener.listener.onKeyAction(it, source)
            }
        }
    }

    private val popupActionListener: PopupActionListener by lazy {
        PopupActionListener {
            when (it) {
                is PopupAction.PreviewAction -> {
                    if (!popupPreview) return@PopupActionListener
                }
                is PopupAction.ShowKeyboardAction -> {
                    // prevent ViewPager from consuming swipe gesture when popup keyboard shown
                    pickerLayout.pager.isUserInputEnabled = false
                }
                is PopupAction.DismissAction -> {
                    // restore ViewPager scrolling
                    pickerLayout.pager.isUserInputEnabled = true
                }
                else -> {}
            }
            popup.listener.onPopupAction(it)
        }
    }

    override fun onCreateView() = PickerLayout(context, theme, switchKey).apply {
        pickerLayout = this
        val bordered = followKeyBorder && keyBorder
        pickerPagesAdapter = PickerPagesAdapter(
            theme, keyActionListener, popupActionListener, data,
            density, key.name, bordered, policy
        )
        tabsUi.apply {
            setTabs(pickerPagesAdapter.getCategoryList())
            setOnTabClickListener { i ->
                pager.setCurrentItem(pickerPagesAdapter.getRangeOfCategoryIndex(i).first, false)
            }
        }
        pager.apply {
            adapter = pickerPagesAdapter
            restorePosition()
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageScrolled(
                    position: Int,
                    positionOffset: Float,
                    positionOffsetPixels: Int
                ) {
                    val range = pickerPagesAdapter.getCategoryRangeOfPage(position)
                    if (range.isEmpty()) return
                    paginationUi.updatePageCount(range.run { last - first + 1 })
                    paginationUi.updateScrollProgress(position - range.first, positionOffset)
                }

                override fun onPageSelected(position: Int) {
                    val pickerPosition = pickerPagesAdapter.getPositionOfPage(position) ?: return
                    interactionStore.selectPage(
                        key.kind,
                        pickerPosition.categoryIndex,
                        pickerPosition.pageInCategory
                    )
                    tabsUi.activateTab(pickerPosition.categoryIndex)
                    popup.dismissAll()
                }
            })
        }
    }

    override fun onCreateBarExtension() = pickerLayout.tabsUi.root

    override fun onAttached() {
        pickerLayout.embeddedKeyboard.also {
            if (pickerPagesAdapter.refreshIfNeeded()) {
                pickerLayout.tabsUi.setTabs(pickerPagesAdapter.getCategoryList())
                restorePosition()
            }
            val snapshot = service.inputSession.current
            it.onReturnDrawableUpdate(ReturnKeyGlyphPolicy.resolve(
                snapshot.editor,
                InputPresentationReducer.reduce(snapshot.engine).preeditEmpty,
                theme.isNothingStyle
            ))
            it.keyActionListener = keyActionListener
        }
    }

    override fun onDetached() {
        popup.dismissAll()
        pickerLayout.embeddedKeyboard.keyActionListener = null
    }

    override val showTitle = false
}
