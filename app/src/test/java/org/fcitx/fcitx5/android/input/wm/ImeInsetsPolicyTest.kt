package org.fcitx.fcitx5.android.input.wm

import org.junit.Assert.assertEquals
import org.junit.Test

class ImeInsetsPolicyTest {
    @Test fun `virtual keyboard uses its current top`() {
        assertEquals(2000, ImeInsetsPolicy.top(true, 2000, 3120, 56))
    }
    @Test fun `hardware keyboard reserves only navigation area`() {
        assertEquals(3064, ImeInsetsPolicy.top(false, 2000, 3120, 56))
    }
    @Test fun `missing keyboard does not reuse previous coordinates`() {
        assertEquals(3064, ImeInsetsPolicy.top(true, null, 3120, 56))
    }
    @Test fun `transient invalid dimensions are bounded`() {
        assertEquals(0, ImeInsetsPolicy.top(true, -10, 100, 0))
        assertEquals(100, ImeInsetsPolicy.top(true, 300, 100, 0))
        assertEquals(0, ImeInsetsPolicy.top(false, null, -10, 20))
    }
}
