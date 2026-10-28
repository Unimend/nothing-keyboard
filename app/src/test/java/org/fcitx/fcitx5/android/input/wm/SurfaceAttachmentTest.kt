package org.fcitx.fcitx5.android.input.wm

import org.junit.Assert.*
import org.junit.Test

class SurfaceAttachmentTest {
    @Test
    fun `old asynchronous result cannot update detached or reattached surface`() {
        val attachment = SurfaceAttachment()
        assertNull(attachment.capture())
        attachment.attach()
        val first = attachment.capture()!!
        assertTrue(attachment.accepts(first))
        attachment.detach()
        assertFalse(attachment.accepts(first))
        attachment.attach()
        assertFalse(attachment.accepts(first))
        assertTrue(attachment.accepts(attachment.capture()!!))
    }
}
