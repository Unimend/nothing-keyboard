package org.fcitx.fcitx5.android.input

import org.fcitx.fcitx5.android.input.candidates.floating.FloatingCandidatesMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InputDeviceVisibilityPolicyTest {
    @Test fun `system mode delegates every visibility decision`() {
        val mode = FloatingCandidatesMode.SystemDefault
        assertTrue(InputDeviceVisibilityPolicy.onStartInputView(mode, false, true))
        assertFalse(InputDeviceVisibilityPolicy.onHardwareKey(mode, false))
        assertTrue(InputDeviceVisibilityPolicy.onEditorClicked(mode, true))
        assertFalse(InputDeviceVisibilityPolicy.onEditorTool(mode, true, false, true))
    }

    @Test fun `input device mode preserves start state and hides for hardware input`() {
        val mode = FloatingCandidatesMode.InputDevice
        assertTrue(InputDeviceVisibilityPolicy.onStartInputView(mode, true, false))
        assertFalse(InputDeviceVisibilityPolicy.onStartInputView(mode, false, true))
        assertFalse(InputDeviceVisibilityPolicy.onHardwareKey(mode, true))
        assertTrue(InputDeviceVisibilityPolicy.onEditorClicked(mode, false))
    }

    @Test fun `direct touch restores virtual keyboard while unknown tools preserve mode`() {
        val mode = FloatingCandidatesMode.InputDevice
        assertTrue(InputDeviceVisibilityPolicy.onEditorTool(mode, false, false, true))
        assertFalse(InputDeviceVisibilityPolicy.onEditorTool(mode, false, true, false))
        assertTrue(InputDeviceVisibilityPolicy.onEditorTool(mode, true, false, false))
    }

    @Test fun `disabled floating candidates always use virtual keyboard`() {
        val mode = FloatingCandidatesMode.Disabled
        assertTrue(InputDeviceVisibilityPolicy.onStartInputView(mode, false, false))
        assertTrue(InputDeviceVisibilityPolicy.onHardwareKey(mode, false))
        assertTrue(InputDeviceVisibilityPolicy.onEditorClicked(mode, false))
        assertTrue(InputDeviceVisibilityPolicy.onEditorTool(mode, false, false, false))
    }
}
