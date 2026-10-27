/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.input.candidates

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.text.inSpans
import org.fcitx.fcitx5.android.core.CandidateWord
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.AutoScaleTextView
import org.fcitx.fcitx5.android.input.NothingDotMatrixDrawable
import org.fcitx.fcitx5.android.input.NothingTypefaceSpan
import org.fcitx.fcitx5.android.input.applyNothingLegendTypography
import org.fcitx.fcitx5.android.input.nothingTypeface
import org.fcitx.fcitx5.android.input.keyboard.CustomGestureView
import org.fcitx.fcitx5.android.utils.pressHighlightDrawable
import splitties.dimensions.dp
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.add
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.view
import splitties.views.dsl.core.wrapContent
import splitties.views.gravityCenter
import android.view.animation.LinearInterpolator

class CandidateItemUi(override val ctx: Context, val theme: Theme) : Ui {

    private val nothingMarkerDrawable = if (theme.name == "NothingDark") {
        NothingDotMatrixDrawable(
            color = theme.candidateLabelColor,
            columns = 1,
            rows = 4,
            dotDiameter = ctx.dp(2).toFloat(),
            dotGap = ctx.dp(2).toFloat(),
            initialActiveDots = 0,
            inactiveAlpha = 0.2f
        )
    } else null

    private var markerAnimator: ValueAnimator? = null
    private var lastPrimaryText = ""

    private val primaryMarker = view(::View) {
        background = nothingMarkerDrawable ?: ColorDrawable(theme.candidateLabelColor)
        visibility = View.GONE
    }

    private val text = view(::AutoScaleTextView) {
        // Candidate words keep one stable reading size. Long words expand their item width and
        // are reached by scrolling; they must never become visually less important by shrinking.
        scaleMode = AutoScaleTextView.Mode.None
        textSize = 20f // sp
        isSingleLine = true
        gravity = Gravity.START or Gravity.CENTER_VERTICAL
        setTextColor(theme.candidateTextColor)
        // Chinese candidates use the crisp system CJK font. Latin/pinyin is switched to Doto
        // when content is bound, instead of forcing the whole mixed line through font fallback.
    }

    override val root = view(::CustomGestureView) {
        background = pressHighlightDrawable(theme.keyPressHighlightColor)

        /**
         * candidate long press feedback is handled by [org.fcitx.fcitx5.android.input.BaseInputView.showCandidateActionMenu]
         */
        longPressFeedbackEnabled = false

        add(text, lParams(wrapContent, matchParent) {
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            // A fixed leading rail prevents one- and multi-character candidates from
            // appearing to jump relative to the primary marker.
            leftMargin = dp(if (theme.name == "NothingDark") 16 else 12)
        })
        add(primaryMarker, lParams(if (theme.name == "NothingDark") dp(4) else dp(3), dp(18)) {
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
        })
    }

    fun updateCandidate(index: Int, candidate: CandidateWord) {
        primaryMarker.visibility = if (index == 0) View.VISIBLE else View.GONE
        if (index == 0 && candidate.text.isNotEmpty() && candidate.text != lastPrimaryText) {
            lastPrimaryText = candidate.text
            markerAnimator?.cancel()
            markerAnimator = ValueAnimator.ofInt(0, 4).apply {
                duration = 120L
                interpolator = LinearInterpolator()
                addUpdateListener {
                    nothingMarkerDrawable?.setActiveDots(it.animatedValue as Int)
                }
                start()
            }
        } else if (index != 0) {
            markerAnimator?.cancel()
            nothingMarkerDrawable?.setActiveDots(0)
            lastPrimaryText = ""
        }
        val fg = theme.candidateTextColor
        val altFg = theme.candidateCommentColor
        text.applyNothingLegendTypography(theme, candidate.text)
        val commentTypeface = text.nothingTypeface(theme, bold = false)
        text.text = buildSpannedString {
            color(fg) {
                append(candidate.text)
            }
            if (candidate.comment.isNotBlank()) {
                if (candidate.spaceBetweenComment) {
                    append(" ")
                }
                color(altFg) {
                    if (commentTypeface != null) {
                        inSpans(NothingTypefaceSpan(commentTypeface)) {
                            append(candidate.comment)
                        }
                    } else {
                        append(candidate.comment)
                    }
                }
            }
        }
    }
}
