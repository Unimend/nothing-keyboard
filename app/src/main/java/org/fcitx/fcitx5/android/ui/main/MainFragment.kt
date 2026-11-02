/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2025 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceCategory
import org.fcitx.fcitx5.android.BuildConfig
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.ui.common.PaddingPreferenceFragment
import org.fcitx.fcitx5.android.ui.main.settings.SettingsRoute
import org.fcitx.fcitx5.android.utils.addCategory
import org.fcitx.fcitx5.android.utils.addPreference
import org.fcitx.fcitx5.android.utils.item
import org.fcitx.fcitx5.android.utils.navigateWithAnim

class MainFragment : PaddingPreferenceFragment() {

    override fun onViewCreated(
        view: View, savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        // AboutMenuProvider is tied to viewLifecycleOwner, so the about menu items
        // are automatically shown when this Fragment is visible and removed when it's not
        requireActivity().addMenuProvider(
            AboutMenuProvider(), viewLifecycleOwner, Lifecycle.State.STARTED
        )
    }

    private inner class AboutMenuProvider : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menu.item(R.string.about) {
                navigateWithAnim(SettingsRoute.About)
            }
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean = false
    }

    private fun PreferenceCategory.addDestinationPreference(
        @StringRes title: Int,
        @DrawableRes icon: Int,
        route: SettingsRoute
    ) {
        addPreference(title, icon = icon) {
            navigateWithAnim(route)
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceScreen = preferenceManager.createPreferenceScreen(requireContext()).apply {
            addCategory("Fcitx") {
                addDestinationPreference(
                    R.string.global_options,
                    R.drawable.ic_pixel_more,
                    SettingsRoute.GlobalConfig
                )
                addDestinationPreference(
                    R.string.input_methods,
                    R.drawable.ic_pixel_language,
                    SettingsRoute.InputMethodList
                )
                addDestinationPreference(
                    R.string.addons,
                    R.drawable.ic_pixel_more,
                    SettingsRoute.AddonList
                )
            }
            addCategory("Android") {
                addDestinationPreference(
                    R.string.virtual_keyboard,
                    R.drawable.ic_pixel_space,
                    SettingsRoute.VirtualKeyboard
                )
                addDestinationPreference(
                    R.string.candidates_window,
                    R.drawable.ic_pixel_expand_more,
                    SettingsRoute.CandidatesWindow
                )
                addDestinationPreference(
                    R.string.clipboard,
                    R.drawable.ic_pixel_clipboard,
                    SettingsRoute.Clipboard
                )
                addDestinationPreference(
                    R.string.emoji_and_symbols,
                    R.drawable.ic_pixel_face,
                    SettingsRoute.Symbol
                )
                addDestinationPreference(
                    R.string.advanced,
                    R.drawable.ic_pixel_more,
                    SettingsRoute.Advanced
                )
            }
            if (BuildConfig.DEBUG) {
                addCategory(getString(R.string.developer)) {
                    addPreference(R.string.input_core_diagnostics) {
                        startActivity(Intent(context, InputCoreDiagnosticsActivity::class.java))
                    }
                }
            }
        }
    }
}
