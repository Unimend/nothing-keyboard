/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.data.clipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build

/** Android clipboard boundary. Timestamp/hash de-duplication never leaks into storage. */
internal class PlatformClipboardSource(
    private val platform: ClipboardManager,
    private val now: () -> Long = System::currentTimeMillis
) : ClipboardManager.OnPrimaryClipChangedListener {
    private var listener: ((ClipData) -> Unit)? = null
    private var lastTimestamp = -1L
    private var lastHash = 0

    fun start(listener: (ClipData) -> Unit) {
        if (this.listener != null) return
        this.listener = listener
        platform.addPrimaryClipChangedListener(this)
    }

    fun stop() {
        if (listener == null) return
        platform.removePrimaryClipChangedListener(this)
        listener = null
    }

    override fun onPrimaryClipChanged() {
        val clip = platform.primaryClip ?: return
        if (isDuplicate(clip)) return
        listener?.invoke(clip)
    }

    private fun isDuplicate(clip: ClipData): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timestamp = clip.description.timestamp
            if (timestamp == lastTimestamp) return true
            lastTimestamp = timestamp
            return false
        }
        val timestamp = now()
        val hash = clip.hashCode()
        val duplicate = timestamp - lastTimestamp < LEGACY_DUPLICATE_WINDOW_MS && hash == lastHash
        lastTimestamp = timestamp
        lastHash = hash
        return duplicate
    }

    private companion object { const val LEGACY_DUPLICATE_WINDOW_MS = 100L }
}
