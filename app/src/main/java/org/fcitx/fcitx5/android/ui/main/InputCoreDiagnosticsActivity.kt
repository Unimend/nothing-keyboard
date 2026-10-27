/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.ui.main

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.input.debug.InputCoreDiagnostics
import org.fcitx.fcitx5.android.input.debug.InputCoreDiagnosticsSnapshot
import org.fcitx.fcitx5.android.input.session.EngineText

/** A bottom-level probe. It renders the core snapshot without using keyboard presentation code. */
class InputCoreDiagnosticsActivity : AppCompatActivity() {
    private lateinit var output: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setTitle(R.string.input_core_diagnostics)
        }
        val density = resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()
        output = TextView(this).apply {
            setTextColor(Color.rgb(230, 230, 230))
            setBackgroundColor(Color.BLACK)
            typeface = Typeface.MONOSPACE
            textSize = 13f
            setPadding(dp(16), dp(14), dp(16), dp(14))
            setTextIsSelectable(true)
        }
        val scroll = ScrollView(this).apply {
            addView(output, ViewGroup.LayoutParams(-1, -2))
        }
        val input = EditText(this).apply {
            hint = getString(R.string.input_core_diagnostics_hint)
            textSize = 18f
            minLines = 2
            gravity = Gravity.TOP
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
            addView(input, LinearLayout.LayoutParams(-1, -2))
        })
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                InputCoreDiagnostics.state.collect { output.text = render(it) }
            }
        }
        input.requestFocus()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }

    private fun render(value: InputCoreDiagnosticsSnapshot): String = buildString {
        val session = value.session
        val engine = session.engine
        appendLine("SOURCE      FcitxEngineBridge (single)")
        appendLine("EVENTS      ${value.engineEventCount}")
        appendLine("LAST EVENT  ${value.lastEngineEvent}")
        appendLine("REVISION    ${session.revision}")
        appendLine("LIFECYCLE   ${session.lifecycle}")
        appendLine("READY       ${engine.ready}")
        appendLine("EDITOR      ${session.editor?.packageName ?: "none"}")
        appendLine("SELECTION   ${session.selection.start}..${session.selection.end}")
        appendLine("SURFACE     ${session.surface}")
        appendLine("PREVIOUS    ${session.previousSurface}")
        appendLine("IME         ${engine.inputMethod?.uniqueName ?: "none"}")
        appendLine("LANGUAGE    ${engine.inputMethod?.languageCode ?: "none"}")
        appendLine("CLIENT PREEDIT  ${engine.clientPreedit.display()}")
        appendLine("PANEL PREEDIT   ${engine.inputPanel.preedit.display()}")
        appendLine("AUX UP          ${engine.inputPanel.auxiliaryUp.display()}")
        appendLine("AUX DOWN        ${engine.inputPanel.auxiliaryDown.display()}")
        appendLine("CANDIDATES  ${engine.candidates.items.size}/${engine.candidates.total}")
        engine.candidates.items.take(8).forEachIndexed { index, item ->
            appendLine("  [$index] ${item.label} | ${item.text} | ${item.comment}")
        }
        appendLine("PAGED       ${engine.pagedCandidates.items.size}")
        appendLine("STATUS      ${engine.statusActions.size}")
    }

    private fun EngineText.display() = "\"$plainText\" @ $cursor"
}
