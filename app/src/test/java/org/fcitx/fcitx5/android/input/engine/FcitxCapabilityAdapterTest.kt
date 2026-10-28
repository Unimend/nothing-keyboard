package org.fcitx.fcitx5.android.input.engine

import org.fcitx.fcitx5.android.core.CapabilityFlag
import org.fcitx.fcitx5.android.input.session.EditorCapability
import org.junit.Assert.*
import org.junit.Test

class FcitxCapabilityAdapterTest {
    @Test
    fun `native flags are encoded only at the boundary`() {
        val flags = setOf(EditorCapability.Composition, EditorCapability.Password,
            EditorCapability.Private, EditorCapability.CursorAwareCommit).toFcitxCapabilities()
        assertEquals(CapabilityFlag.Preedit.flag or CapabilityFlag.Password.flag or
            CapabilityFlag.Sensitive.flag or CapabilityFlag.CommitStringWithCursor.flag, flags.flags)
    }

    @Test
    fun `each semantic requirement has a distinct native mapping`() {
        val encoded = EditorCapability.entries.map { setOf(it).toFcitxCapabilities().flags }
        assertEquals(EditorCapability.entries.size, encoded.toSet().size)
        assertFalse(0uL in encoded)
    }
}
