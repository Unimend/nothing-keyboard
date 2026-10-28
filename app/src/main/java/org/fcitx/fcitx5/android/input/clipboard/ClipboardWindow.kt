/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.clipboard

import org.fcitx.fcitx5.android.input.editing.EditingCommand

import android.annotation.SuppressLint
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.PopupMenu
import androidx.annotation.Keep
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.view.updateLayoutParams
import androidx.paging.CombinedLoadStates
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.snackbar.BaseTransientBottomBar.BaseCallback
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.SnackbarContentLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.input.clipboard.ClipboardItem
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.prefs.ManagedPreference
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.input.dependency.inputView
import org.fcitx.fcitx5.android.input.dependency.theme
import org.fcitx.fcitx5.android.input.navigation.InputNavigationCommand
import org.fcitx.fcitx5.android.input.navigation.InputNavigator
import org.fcitx.fcitx5.android.input.session.InputSurface
import org.fcitx.fcitx5.android.input.runtime.InputEditingAccess
import org.fcitx.fcitx5.android.input.wm.InputWindow
import org.fcitx.fcitx5.android.input.wm.SurfaceAttachment
import org.fcitx.fcitx5.android.utils.AppUtil
import org.fcitx.fcitx5.android.utils.item
import org.fcitx.fcitx5.android.utils.styledColorOrDefault
import org.mechdancer.dependency.manager.must
import splitties.dimensions.dp
import splitties.resources.styledColor
import splitties.views.dsl.core.withTheme

class ClipboardWindow : InputWindow.ExtendedInputWindow<ClipboardWindow>() {

    override val surface = InputSurface.Clipboard

    private val inputView by manager.inputView()
    private val editing: InputEditingAccess by manager.must()
    private val theme by manager.theme()
    private val navigator: InputNavigator by manager.must()
    private val interaction: ClipboardInteractionController by manager.must()
    private val history: ClipboardHistory by manager.must()

    private val snackbarCtx by lazy {
        context.withTheme(R.style.InputViewSnackbarTheme)
    }
    private var snackbarInstance: Snackbar? = null
    private var renderedMode: ClipboardViewMode? = null
    private val attachment = SurfaceAttachment()

    private fun <T> launchAttachedResult(operation: suspend () -> T, render: (T) -> Unit) {
        val token = attachment.capture() ?: return
        inputView.surfaceScope.launch {
            val result = operation()
            if (attachment.accepts(token)) render(result)
        }
    }

    @Keep
    private val clipboardEnabledListener = ManagedPreference.OnChangeListener<Boolean> { _, it ->
        interaction.setListeningEnabled(it)
        renderViewMode()
    }

    private val prefs = AppPrefs.getInstance().clipboard

    private val clipboardEnabledPref = prefs.clipboardListening
    private val clipboardReturnAfterPaste by prefs.clipboardReturnAfterPaste
    private val clipboardMaskSensitive by prefs.clipboardMaskSensitive

    private val clipboardEntryRadius by ThemeManager.prefs.clipboardEntryRadius

    private val clipboardPages by lazy { history.pages() }
    private var adapterSubmitJob: Job? = null

    private val adapter: ClipboardAdapter by lazy {
        object : ClipboardAdapter(
            theme,
            context.dp(clipboardEntryRadius.toFloat()),
            clipboardMaskSensitive
        ) {
            override fun onPin(id: Int) {
                inputView.surfaceScope.launch { interaction.pin(id) }
            }

            override fun onUnpin(id: Int) {
                inputView.surfaceScope.launch { interaction.unpin(id) }
            }

            override fun onEdit(id: Int) {
                AppUtil.launchClipboardEdit(context, id)
            }

            override fun onShare(entry: ClipboardItem) {
                val target = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, entry.text)
                }
                val chooser = Intent.createChooser(target, null).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            }

            override fun onDelete(id: Int) {
                launchAttachedResult({ interaction.stageDelete(id) }) { showUndoSnackbar() }
            }

            override fun onPaste(entry: ClipboardItem) {
                editing.commands.execute(EditingCommand.CommitText(entry.text))
                if (clipboardReturnAfterPaste) {
                    navigator.navigate(InputNavigationCommand.ShowKeyboard())
                }
            }
        }
    }

    private val ui by lazy {
        ClipboardUi(context, theme).apply {
            recyclerView.apply {
                layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
                adapter = this@ClipboardWindow.adapter
            }
            ItemTouchHelper(object : ItemTouchHelper.Callback() {
                override fun getMovementFlags(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder
                ): Int {
                    return makeMovementFlags(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT)
                }

                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    return false
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val position = viewHolder.bindingAdapterPosition
                    if (position == RecyclerView.NO_POSITION) return
                    val entry = adapter.getEntryAt(position) ?: return
                    launchAttachedResult({ interaction.stageDelete(entry.id) }) { showUndoSnackbar() }
                }
            }).attachToRecyclerView(recyclerView)
            enableUi.enableButton.setOnClickListener {
                clipboardEnabledPref.setValue(true)
            }
            deleteAllButton.setOnClickListener {
                launchAttachedResult({ interaction.haveUnpinned() }) { promptDeleteAll(it) }
            }
        }
    }

    override fun onCreateView(): View = ui.root

    private var promptMenu: PopupMenu? = null

    private fun promptDeleteAll(skipPinned: Boolean) {
        promptMenu?.dismiss()
        promptMenu = PopupMenu(context, ui.deleteAllButton).apply {
            menu.add(buildSpannedString {
                bold {
                    color(context.styledColorOrDefault(android.R.attr.colorAccent, theme.genericActiveForegroundColor)) {
                        append(context.getString(if (skipPinned) R.string.delete_all_except_pinned else R.string.delete_all_pinned_items))
                    }
                }
            }).isEnabled = false
            menu.add(android.R.string.cancel)
            menu.item(android.R.string.ok) {
                launchAttachedResult({ interaction.stageDeleteAll(skipPinned) }) { showUndoSnackbar() }
            }
            setOnDismissListener {
                if (it === promptMenu) promptMenu = null
            }
            show()
        }
    }

    @SuppressLint("RestrictedApi")
    private fun showUndoSnackbar() {
        val pendingCount = interaction.state.pendingDeleteIds.size
        if (pendingCount == 0) return
        val str = context.resources.getString(R.string.num_items_deleted, pendingCount)
        snackbarInstance = Snackbar.make(snackbarCtx, ui.root, str, Snackbar.LENGTH_LONG)
            .setBackgroundTint(theme.popupBackgroundColor)
            .setTextColor(theme.popupTextColor)
            .setActionTextColor(theme.genericActiveBackgroundColor)
            .setAction(R.string.undo) {
                inputView.surfaceScope.launch {
                    interaction.undoPendingDeletes()
                }
            }
            .addCallback(object : Snackbar.Callback() {
                override fun onDismissed(transientBottomBar: Snackbar, event: Int) {
                    // A replaced or detached snackbar cannot finalize the newer undo batch.
                    if (snackbarInstance !== transientBottomBar) return
                    snackbarInstance = null
                    when (event) {
                        BaseCallback.DISMISS_EVENT_SWIPE,
                        BaseCallback.DISMISS_EVENT_MANUAL,
                        BaseCallback.DISMISS_EVENT_TIMEOUT -> {
                            inputView.surfaceScope.launch {
                                interaction.finalizePendingDeletes()
                            }
                        }
                        BaseCallback.DISMISS_EVENT_ACTION,
                        BaseCallback.DISMISS_EVENT_CONSECUTIVE -> {
                            // user clicked "undo" or deleted more items which makes a new snackbar
                        }
                    }
                }
            }).apply {
                val hMargin = snackbarCtx.dp(24)
                val vMargin = snackbarCtx.dp(16)
                view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    leftMargin = hMargin
                    rightMargin = hMargin
                    bottomMargin = vMargin
                }
                ((view as FrameLayout).getChildAt(0) as SnackbarContentLayout).apply {
                    messageView.letterSpacing = 0f
                    actionView.letterSpacing = 0f
                }
                show()
            }
    }

    override fun onAttached() {
        attachment.attach()
        renderedMode = null
        val isEmpty = history.itemCount == 0
        val isListening = clipboardEnabledPref.getValue()
        interaction.setListeningEnabled(isListening)
        interaction.setContentEmpty(isEmpty)
        renderViewMode()
        adapter.addLoadStateListener(loadStateListener)
        adapterSubmitJob = inputView.surfaceScope.launch {
            clipboardPages.collect {
                adapter.submitData(it)
            }
        }
        clipboardEnabledPref.registerOnChangeListener(clipboardEnabledListener)
    }

    override fun onDetached() {
        attachment.detach()
        clipboardEnabledPref.unregisterOnChangeListener(clipboardEnabledListener)
        adapter.onDetached()
        adapterSubmitJob?.cancel()
        adapter.removeLoadStateListener(loadStateListener)
        promptMenu?.dismiss()
        snackbarInstance?.let {
            snackbarInstance = null
            it.dismiss()
        }
        inputView.surfaceScope.launch { interaction.finalizePendingDeletes() }
    }

    private val loadStateListener: (CombinedLoadStates) -> Unit = {
        val empty = it.append.endOfPaginationReached && adapter.itemCount < 1
        interaction.setContentEmpty(empty)
        renderViewMode()
    }

    private fun renderViewMode() {
        val mode = interaction.state.viewMode
        if (mode == renderedMode) return
        renderedMode = mode
        ui.switchUiByState(mode)
    }

    override val title: String by lazy {
        context.getString(R.string.clipboard)
    }

    override fun onCreateBarExtension(): View = ui.extension
}
