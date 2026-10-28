/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.input.wm

/** A result from a previous attachment must never update a newly attached surface. */
class SurfaceAttachment {
    private var generation = 0L
    private var attached = false
    fun attach() { generation += 1; attached = true }
    fun detach() { attached = false }
    fun capture(): Long? = if (attached) generation else null
    fun accepts(token: Long): Boolean = attached && generation == token
}
