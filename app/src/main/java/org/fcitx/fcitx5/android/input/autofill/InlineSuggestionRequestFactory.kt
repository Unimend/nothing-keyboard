/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.input.autofill

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Size
import android.view.inputmethod.InlineSuggestionsRequest
import android.widget.inline.InlinePresentationSpec
import androidx.annotation.RequiresApi
import androidx.autofill.inline.UiVersions
import androidx.autofill.inline.common.ImageViewStyle
import androidx.autofill.inline.common.TextViewStyle
import androidx.autofill.inline.common.ViewStyle
import androidx.autofill.inline.v1.InlineSuggestionUi
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.design.toInputColorScheme
import splitties.dimensions.dp

/** Android autofill presentation construction, independent of IME lifecycle orchestration. */
object InlineSuggestionRequestFactory {
    @SuppressLint("RestrictedApi")
    @RequiresApi(Build.VERSION_CODES.R)
    fun create(context: Context, theme: Theme): InlineSuggestionsRequest {
        val colors = theme.toInputColorScheme()
        val chipDrawable =
            if (theme.isDark) R.drawable.bkg_inline_suggestion_dark else R.drawable.bkg_inline_suggestion_light
        val chipBg = Icon.createWithResource(context, chipDrawable).setTint(colors.keyContent)
        val style = InlineSuggestionUi.newStyleBuilder()
            .setSingleIconChipStyle(
                ViewStyle.Builder()
                    .setBackgroundColor(Color.TRANSPARENT)
                    .setPadding(0, 0, 0, 0)
                    .build()
            )
            .setChipStyle(
                ViewStyle.Builder()
                    .setBackground(chipBg)
                    .setPadding(context.dp(10), 0, context.dp(10), 0)
                    .build()
            )
            .setTitleStyle(
                TextViewStyle.Builder()
                    .setLayoutMargin(context.dp(4), 0, context.dp(4), 0)
                    .setTextColor(colors.keyContent)
                    .setTextSize(14f)
                    .build()
            )
            .setSubtitleStyle(
                TextViewStyle.Builder()
                    .setTextColor(colors.alternateKeyContent)
                    .setTextSize(12f)
                    .build()
            )
            .setStartIconStyle(
                ImageViewStyle.Builder()
                    .setTintList(ColorStateList.valueOf(colors.alternateKeyContent))
                    .build()
            )
            .setEndIconStyle(
                ImageViewStyle.Builder()
                    .setTintList(ColorStateList.valueOf(colors.alternateKeyContent))
                    .build()
            )
            .build()
        val styleBundle = UiVersions.newStylesBuilder()
            .addStyle(style)
            .build()
        val spec = InlinePresentationSpec
            .Builder(Size(0, 0), Size(Int.MAX_VALUE, Int.MAX_VALUE))
            .setStyle(styleBundle)
            .build()
        return InlineSuggestionsRequest.Builder(listOf(spec))
            .setMaxSuggestionCount(InlineSuggestionsRequest.SUGGESTION_COUNT_UNLIMITED)
            .build()
    }
}
